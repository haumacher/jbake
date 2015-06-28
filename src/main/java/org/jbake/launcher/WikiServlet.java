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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jbake.app.Crawler;
import org.jbake.app.Oven;
import org.jbake.app.Renderer;
import org.jbake.parser.Engines;

public class WikiServlet extends HttpServlet {

	private final Oven oven;
	
	private final File source;

	private Crawler crawler;

	private Renderer renderer;

	private String[] sourceExtensions;

	public WikiServlet(Oven oven) {
		this.oven = oven;
		
    	crawler = oven.getCrawler();
    	renderer = oven.getRenderer();
    	source = oven.getContentsPath();
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
			if (!path.endsWith("/") && !path.isEmpty()) {
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
		
		String sourceUri = null;
		File sourceFile = null;
		for (String sourceExtension : sourceExtensions) {
			sourceUri = barePath + '.' + sourceExtension;
			sourceFile = new File(source, sourceUri);
			if (sourceFile.exists() && !sourceFile.isDirectory()) {
				break;
			}
		}
		
		if (sourceUri != null) {
			Map<String, Object> document = crawler.parse(sourceUri, sourceFile);
			try {
				resp.setContentType("text/html");
				resp.setHeader("cacheControl", "public, max-age=0, s-maxage=0");

				document.put("wikibake", Boolean.TRUE);
				renderer.renderDocument(document, resp.getWriter());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
}
