package org.jbake.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jbake.app.Oven;
import org.jbake.util.WriterOut;

/**
 * {@link HttpServlet} rendering the post index page.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class IndexServlet extends JBakeServlet {

	public IndexServlet(Oven oven) {
		super(oven);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			resp.setContentType("text/html");
			resp.setCharacterEncoding("utf-8");
			resp.setHeader("Cache-Control", "public, max-age=0, s-maxage=0");

			String path = req.getPathInfo();
			int suffixIndex = path.lastIndexOf('.');
			int pageStartIndex = suffixIndex - 1;
			while (pageStartIndex > 0 && Character.isDigit(path.charAt(pageStartIndex - 1))) {
				pageStartIndex--;
			}
			String pageString = path.substring(pageStartIndex, suffixIndex);
			int page;
			if (pageString.isEmpty()) {
				page = 1;
			} else {
				page = Integer.parseInt(pageString);
			}
			
			String indexName = path.substring(0, pageStartIndex) + path.substring(suffixIndex);
			
			oven().getRenderer().renderIndexPage(page, indexName, new WriterOut(resp.getWriter()));
		} catch (Exception ex) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
