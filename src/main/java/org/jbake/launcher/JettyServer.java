package org.jbake.launcher;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jbake.app.Oven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides Jetty server related functions
 * 
 * @author Jonathan Bullock <jonbullock@gmail.com>
 *
 */
public class JettyServer {
    private final static Logger LOGGER = LoggerFactory.getLogger(JettyServer.class);

	/**
	 * Run Jetty web server serving out supplied path on supplied port
	 * 
	 * @param path
	 * @param port
	 */
	public static void run(String path, String port, Oven oven) {
		Server server = new Server();
		SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(Integer.parseInt(port));
        server.addConnector(connector);
 
        ServletContextHandler servletHandler = new ServletContextHandler(null, "/", true, false);
        
        DefaultServlet sourceServlet = new DefaultServlet();
        ServletHolder sourceHolder = new ServletHolder(sourceServlet);
        sourceHolder.setInitParameter("resourceBase", oven.getContentsPath().getAbsolutePath());
        sourceHolder.setInitParameter("pathInfoOnly", "true");
        sourceHolder.setInitParameter("cacheControl", "public, max-age=0, s-maxage=0");
        servletHandler.addServlet(sourceHolder, "/jb/source/*");
        
        servletHandler.addServlet(new ServletHolder(new UpdateServlet(oven)), "/jb/update/*");
        servletHandler.addServlet(new ServletHolder(new WikiServlet(oven)), "/*");
        
        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setCacheControl("public, max-age=0, s-maxage=0");
        resource_handler.setDirectoriesListed(true);
        resource_handler.setResourceBase(oven.getAssetsPath().getAbsolutePath());
 
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resource_handler, servletHandler, new DefaultHandler() });
        server.setHandler(handlers);
 
        LOGGER.info("Serving out contents of: [{}] on http://localhost:{}/", path, port);
        LOGGER.info("(To stop server hit CTRL-C)");
        
        try {
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
