package org.jbake.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.ajax.JSON;
import org.jbake.app.Crawler;
import org.jbake.app.FileUtil;
import org.jbake.app.Oven;
import org.jbake.parser.Engines;
import org.jbake.parser.ParserEngine;

public class UpdateServlet extends HttpServlet {

	public static final String JB_ASSETS_URI = "/jb/assets/";

	private final Oven oven;
	
	private final File source;

	public UpdateServlet(Oven oven) {
		this.oven = oven;
		
    	source = oven.getContentsPath();
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String sourceURI = getSourceURI(req);
		
		if (!check(resp, sourceURI)) {
			return;
		}
		
		File sourceFile = new File(source, sourceURI);
		File dir = sourceFile.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		if (sourceFile.isDirectory()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
					"The resource '" + sourceURI + "' is a directory, please choose another name.");
			return;
		}
		
        String extension = FileUtil.fileExt(sourceURI);
		ParserEngine engine = Engines.get(extension);
        if (engine==null) {
        	ArrayList<String> possibleExtensions = new ArrayList<String>(Engines.getRecognizedExtensions());
        	Collections.sort(possibleExtensions);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
        			"The file extension '" + extension + "' is not known. Possible file extensions are: " + possibleExtensions);
        	return;
        }
		
		File backup;
		if (sourceFile.exists()) {
			String create = req.getParameter("create");
			if (create != null && create.equals("true")) {
				resp.sendError(HttpServletResponse.SC_CONFLICT, 
						"The resource '" + sourceURI + "' already exists, please choose another name.");
				return;
			}
			backup = backup(sourceURI, sourceFile);
		} else {
			backup = null;
		}
		
		Map<String, Object> document;
		try {
			FileOutputStream out = new FileOutputStream(sourceFile);
			try {
				OutputStreamWriter writer = new OutputStreamWriter(out, "utf-8");
				try {
					BufferedReader in = req.getReader();
					CharBuffer buffer = CharBuffer.allocate(1024);
					while (in.read(buffer) >= 0) {
						buffer.flip();
						writer.append(buffer);
						buffer.clear();
					}
				} finally {
					writer.close();
				}
			} finally {
				out.close();
			}
			
			Crawler crawler = oven.getCrawler();
			document = crawler.crawlSourceFile(sourceFile, crawler.buildHash(sourceFile), sourceURI);
			try {
				oven.getRenderer().render(document);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		} catch (Error ex) {
			revert(sourceFile, backup);
			throw ex;
		} catch (IOException ex) {
			revert(sourceFile, backup);
			throw ex;
		} catch (RuntimeException ex) {
			revert(sourceFile, backup);
			throw ex;
		}
		
		sendJSON(resp, document);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String sourceURI = getSourceURI(req);
		
		if (!check(resp, sourceURI)) {
			return;
		}
		
		File sourceFile = new File(source, sourceURI);
		if (!sourceFile.exists()) {
			sendNotFound(resp);
			return;
		}
		
		boolean success = sourceFile.delete();
		if (! success) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot delete resource.");
			return;
		}
		
		oven.getDB().deleteContent(sourceURI);
		
		sendJSON(resp, Collections.singletonMap("uri", "index.html"));
	}
	
	private String getSourceURI(HttpServletRequest req) {
		String sourceURI = req.getPathInfo();
		if (sourceURI.startsWith("/")) {
			sourceURI = sourceURI.substring(1);
		}
		return sourceURI;
	}

	private boolean check(HttpServletResponse resp, String sourceURI) {
		if (sourceURI.contains("..") || sourceURI.contains(":") || sourceURI.contains("\\")) {
			sendNotFound(resp);
			return false;
		}
		return true;
	}

	private File backup(String sourceURI, File sourceFile) {
		File backup = new File(source, sourceURI + "~");
		if (backup.exists()) {
			backup.delete();
		}
		
		sourceFile.renameTo(backup);
		return backup;
	}

	private void sendJSON(HttpServletResponse resp, Map<String, ?> result)
			throws IOException {
		resp.setContentType("text/json");
		resp.setCharacterEncoding("utf-8");
		
		PrintWriter out = resp.getWriter();
		new JSON().append(out, result);
	}

	private void sendNotFound(HttpServletResponse resp) {
		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	private void revert(File sourceFile, File backup) {
		if (backup != null) {
			sourceFile.delete();
			backup.renameTo(sourceFile);
		}
	}

}
