/*
 * TimeCollect records time you spent on your development work.
 * Copyright (C) 2015 Bernhard Haumacher and others
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jbake.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.ajax.JSON;
import org.jbake.app.ContentStore;
import org.jbake.app.Crawler;
import org.jbake.app.Oven;
import org.jbake.app.Renderer;

public class UpdateServlet extends HttpServlet {

	public static final String JB_ASSETS_URI = "/jb/assets/";

	private final Oven oven;
	
	private final File source;

	private Crawler crawler;

	private Renderer renderer;

	public UpdateServlet(Oven oven) {
		this.oven = oven;
		
		ContentStore db = oven.init();
    	crawler = oven.createCrawler(db);
    	renderer = oven.createRenderer(db);
    	source = oven.getContentsPath();
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String sourceuri = req.getPathInfo();
		if (sourceuri.startsWith("/")) {
			sourceuri = sourceuri.substring(1);
		}
		
		if (sourceuri.contains("..") || sourceuri.contains(":") || sourceuri.contains("\\")) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		File sourceFile = new File(source, sourceuri);
		if (!sourceFile.exists()) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		resp.setContentType("text/json");
		resp.setCharacterEncoding("utf-8");
		
		File backup = new File(source, sourceuri + "~");
		if (backup.exists()) {
			backup.delete();
		}
		
		sourceFile.renameTo(backup);
		try {
			FileOutputStream out = new FileOutputStream(sourceFile);
			try {
				OutputStreamWriter writer = new OutputStreamWriter(out, "utf-8");
				try {
					BufferedReader in = req.getReader();
					CharBuffer buffer = CharBuffer.allocate(1024);
					while (in.read(buffer) >= 0) {
						buffer.flip();
						System.out.println(buffer);
						writer.append(buffer);
						buffer.clear();
					}
				} finally {
					writer.close();
				}
			} finally {
				out.close();
			}
		} catch (Error ex) {
			backup.renameTo(sourceFile);
			throw ex;
		} catch (IOException ex) {
			backup.renameTo(sourceFile);
			throw ex;
		} catch (RuntimeException ex) {
			backup.renameTo(sourceFile);
			throw ex;
		}
		
		Map<String, Object> document = crawler.parse(sourceuri, sourceFile);
		try {
			renderer.render(document);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		PrintWriter out = resp.getWriter();
		new JSON().append(out, document);
	}

}
