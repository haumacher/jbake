package org.jbake.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jbake.app.Oven;

/**
 * {@link HttpServlet} rendering the post archive.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class ArchiveServlet extends JBakeServlet {

	public ArchiveServlet(Oven oven) {
		super(oven);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			resp.setContentType("text/html");
			resp.setCharacterEncoding("utf-8");
			resp.setHeader("Cache-Control", "public, max-age=0, s-maxage=0");
			oven().getRenderer().renderArchive(resp.getWriter());
		} catch (Exception ex) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
