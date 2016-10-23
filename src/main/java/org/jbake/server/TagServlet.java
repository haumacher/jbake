package org.jbake.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.app.ConfigUtil.Keys;
import org.jbake.app.Oven;

/**
 * {@link HttpServlet} rendering the tag pages.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class TagServlet extends JBakeServlet {

	private String _outputExtension;

	public TagServlet(Oven oven) {
		super(oven);
		CompositeConfiguration config = oven.getConfig();
		_outputExtension = config.getString(Keys.OUTPUT_EXTENSION);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String tag = req.getPathInfo();
			if (tag.startsWith("/")) {
				tag = tag.substring(1);
			}
			if (tag.endsWith(_outputExtension)) {
				tag = tag.substring(0, tag.length() - _outputExtension.length());
			}
			resp.setContentType("text/html");
			resp.setCharacterEncoding("utf-8");
			resp.setHeader("Cache-Control", "public, max-age=0, s-maxage=0");
			oven().getRenderer().renderTag(resp.getWriter(), tag);
		} catch (Exception ex) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
