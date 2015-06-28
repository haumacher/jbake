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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.app.ConfigUtil.Keys;
import org.jbake.app.Oven;
import org.jbake.app.Renderer;
import org.jbake.template.RenderingException;

/**
 * {@link HttpServlet} rendering the tag pages.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class TagServlet extends HttpServlet {

	private final Oven _oven;
	private final Renderer _renderer;
	private String _outputExtension;

	public TagServlet(Oven oven) {
		_oven = oven;
		_renderer = oven.getRenderer();
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
			resp.setHeader("cacheControl", "public, max-age=0, s-maxage=0");
			_renderer.renderTag(tag, resp.getWriter());
		} catch (RenderingException ex) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
