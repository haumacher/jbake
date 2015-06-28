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
import org.jbake.app.Oven;

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
		String sourceuri = req.getPathInfo();
		if (sourceuri.startsWith("/")) {
			sourceuri = sourceuri.substring(1);
		}
		
		if (sourceuri.contains("..") || sourceuri.contains(":") || sourceuri.contains("\\")) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		File sourceFile = new File(source, sourceuri);
		File dir = sourceFile.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		resp.setContentType("text/json");
		resp.setCharacterEncoding("utf-8");
		
		File backup;
		if (sourceFile.exists()) {
			backup = new File(source, sourceuri + "~");
			if (backup.exists()) {
				backup.delete();
			}
			
			sourceFile.renameTo(backup);
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
			
			document = oven.getCrawler().parse(sourceuri, sourceFile);
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
		
		PrintWriter out = resp.getWriter();
		new JSON().append(out, document);
	}

	private void revert(File sourceFile, File backup) {
		if (backup != null) {
			sourceFile.delete();
			backup.renameTo(sourceFile);
		}
	}

}
