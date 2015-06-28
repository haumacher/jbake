package org.jbake.launcher;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jbake.app.Oven;
import org.jbake.template.RenderingException;

/**
 * {@link HttpServlet} rendering the post archive.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class ArchiveServlet extends HttpServlet {

	private final Oven _oven;

	public ArchiveServlet(Oven oven) {
		_oven = oven;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			resp.setContentType("text/html");
			resp.setCharacterEncoding("utf-8");
			resp.setHeader("cacheControl", "public, max-age=0, s-maxage=0");
			_oven.getRenderer().renderArchive(resp.getWriter());
		} catch (RenderingException ex) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
