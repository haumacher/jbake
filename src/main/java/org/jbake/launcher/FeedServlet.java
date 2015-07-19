package org.jbake.launcher;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jbake.app.Oven;
import org.jbake.template.RenderingException;

/**
 * {@link HttpServlet} rendering the feed page.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class FeedServlet extends HttpServlet {

	private final Oven _oven;

	public FeedServlet(Oven oven) {
		_oven = oven;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			resp.setContentType("text/xml");
			resp.setCharacterEncoding("utf-8");
			resp.setHeader("Cache-Control", "public, max-age=0, s-maxage=0");
			_oven.getRenderer().renderFeed(resp.getWriter(), true);
		} catch (RenderingException ex) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
