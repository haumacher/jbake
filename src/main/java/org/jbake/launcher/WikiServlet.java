package org.jbake.launcher;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jbake.app.Oven;
import org.jbake.parser.Engines;

public class WikiServlet extends HttpServlet {

	private final Oven oven;
	
	private final File source;

	private String[] sourceExtensions;

	public WikiServlet(Oven oven) {
		this.oven = oven;
		
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
			Map<String, Object> document = oven.getCrawler().parse(sourceUri, sourceFile);
			try {
				resp.setContentType("text/html");
				resp.setHeader("cacheControl", "public, max-age=0, s-maxage=0");

				document.put("wikibake", Boolean.TRUE);
				oven.getRenderer().renderDocument(document, resp.getWriter());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
}
