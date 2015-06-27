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

import org.jbake.app.ContentStore;
import org.jbake.app.Crawler;
import org.jbake.app.Oven;
import org.jbake.app.Renderer;
import org.jbake.parser.Engines;

public class UpdateServlet extends HttpServlet {

	public static final String JB_ASSETS_URI = "/jb/assets/";

	public static final String JB_OUTPUT_URI = "/jb/output/";

	private final Oven oven;
	
	private final File source;

	private Crawler crawler;

	private Renderer renderer;

	private String[] sourceExtensions;

	private File assets;

	public UpdateServlet(Oven oven) {
		this.oven = oven;
		
		ContentStore db = oven.init();
    	crawler = oven.crawl(db);
    	renderer = oven.createRenderer(db);
    	source = oven.getContentsPath();
    	assets = oven.getAssetsPath();
    	sourceExtensions = Engines.getRecognizedExtensions().toArray(new String[0]);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String path = req.getPathInfo();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		
		if (path.contains("..") || path.contains(":") || path.contains("\\")) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		File testFile = new File(source, path);
		if (testFile.isDirectory()) {
			if (!path.endsWith("/")) {
				resp.sendRedirect(req.getContextPath() + "/" + path + "/");
				return;
			}

			path = path + "index.html";
		}
		
		String barePath;
		int extIndex = path.lastIndexOf('.');
		if (extIndex >= 0) {
			barePath = path.substring(0, extIndex);
		} else {
			barePath = path;
		}
		
		File sourceFile = null;
		for (String sourceExtension : sourceExtensions) {
			sourceFile = new File(source, barePath + '.' + sourceExtension);
			if (sourceFile.exists() && !sourceFile.isDirectory()) {
				break;
			}
		}
		
		if (sourceFile != null) {
			Map<String, Object> document = crawler.parse(path, sourceFile);
			try {
				renderer.render(document);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			
			req.getRequestDispatcher(JB_OUTPUT_URI + path).forward(req, resp);
		} else {
			File assetFile = new File(assets, path);
			if (assetFile.exists()) {
				req.getRequestDispatcher(JB_ASSETS_URI + path).forward(req, resp);
			} else {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
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
    	File outputFile;
		try {
			outputFile = renderer.render(document);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		PrintWriter out = resp.getWriter();
		out.write("{\"body\":\"");
		CharSequence buffer = (CharSequence) document.get("body");
		int start = 0;
		int limit = buffer.length();
		for (int n = start; n < limit; n++) {
			char ch = buffer.charAt(n);
			switch (ch) {
			case '\b': //  Backspace (ascii code 08)
			{
				out.append(buffer, start, n);
				out.append("\\b");
				start = n + 1;
				break;
			}
			case '\f': // Form feed (ascii code 0C)
			{
				out.append(buffer, start, n);
				out.append("\\f");
				start = n + 1;
				break;
			}
			case '\n': // New line
			{
				out.append(buffer, start, n);
				out.append("\\n");
				start = n + 1;
				break;
			}
			case '\r': // Carriage return
			{
				out.append(buffer, start, n);
				out.append("\\r");
				start = n + 1;
				break;
			}
			case '\t': // Tab
			{
				out.append(buffer, start, n);
				out.append("\\t");
				start = n + 1;
				break;
			}
			case '\"': // Double quote
			case '\\': // Backslash caracter
			{
				out.append(buffer, start, n);
				out.append("\\");
				start = n;
				break;
			}
			}
		}
		out.append(buffer, start, limit);
		out.write("\"}");
	}

}
