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
		
    	crawler = oven.getCrawler();
    	renderer = oven.getRenderer();
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
			
			document = crawler.parse(sourceuri, sourceFile);
			try {
				renderer.render(document);
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
		
		PrintWriter out = resp.getWriter();
		new JSON().append(out, document);
	}

	private void revert(File sourceFile, File backup) {
		sourceFile.delete();
		backup.renameTo(sourceFile);
	}

}
