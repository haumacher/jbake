package org.jbake.launcher;

import java.io.File;
import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.CompositeConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.jbake.app.ConfigUtil.Keys;
import org.jbake.server.ArchiveServlet;
import org.jbake.server.FeedServlet;
import org.jbake.server.IndexServlet;
import org.jbake.server.SitemapServlet;
import org.jbake.server.TagServlet;
import org.jbake.server.UpdateServlet;
import org.jbake.server.WikiServlet;
import org.jbake.app.Oven;
import org.jbake.app.Renderer;
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
		watch(oven);
		
		Server server = new Server();
		SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(Integer.parseInt(port));
        server.addConnector(connector);
 
        ServletContextHandler servletHandler = new ServletContextHandler(null, "/", true, false);
        
        CompositeConfiguration config = oven.getConfig();
        if (config.getBoolean(Keys.RENDER_INDEX)) {
        	String uri = baseUri(uri(config.getString(Keys.INDEX_FILE)));
			servletHandler.addServlet(new ServletHolder(new IndexServlet(oven)), "/" + uri + "*");
        }
        if (config.getBoolean(Keys.RENDER_TAGS)) {
        	String tagUri = uri(config.getString(Keys.TAG_PATH));
			servletHandler.addServlet(new ServletHolder(new TagServlet(oven)), "/" + tagUri + "/*");
        }
        if (config.getBoolean(Keys.RENDER_ARCHIVE)) {
        	String uri = uri(config.getString(Keys.ARCHIVE_FILE));
			servletHandler.addServlet(new ServletHolder(new ArchiveServlet(oven)), "/" + uri);
        }
        if (config.getBoolean(Keys.RENDER_FEED)) {
        	String uri = uri(config.getString(Keys.FEED_FILE));
			servletHandler.addServlet(new ServletHolder(new FeedServlet(oven)), "/" + uri);
        }
        if (config.getBoolean(Keys.RENDER_SITEMAP)) {
        	String uri = uri(config.getString(Keys.SITEMAP_FILE));
			servletHandler.addServlet(new ServletHolder(new SitemapServlet(oven)), "/" + uri);
        }
        
        DefaultServlet jbServlet = new DefaultServlet();
        ServletHolder jbHolder = new ServletHolder(jbServlet);
        jbHolder.setInitParameter("resourceBase", JettyServer.class.getResource("/org/jbake/server/webapp").toExternalForm());
        jbHolder.setInitParameter("pathInfoOnly", "true");
        jbHolder.setInitParameter("cacheControl", "public, max-age=0, s-maxage=0");
        servletHandler.addServlet(jbHolder, "/jb/wiki/*");
        
        DefaultServlet sourceServlet = new DefaultServlet();
        ServletHolder sourceHolder = new ServletHolder(sourceServlet);
        sourceHolder.setInitParameter("resourceBase", oven.getContentsPath().getAbsolutePath());
        sourceHolder.setInitParameter("pathInfoOnly", "true");
        sourceHolder.setInitParameter("cacheControl", "public, max-age=0, s-maxage=0");
        servletHandler.addServlet(sourceHolder, "/jb/source/*");

        servletHandler.addServlet(new ServletHolder(new UpdateServlet(oven)), "/jb/update/*");
        servletHandler.addServlet(new ServletHolder(new WikiServlet(oven)), "/*");
        
        ResourceHandler resource_handler = new ResourceHandler() {
        	@Override
        	protected Resource getResource(HttpServletRequest request)
        			throws MalformedURLException {
        		Resource resource = super.getResource(request);
        		if (resource.isDirectory()) {
        			// Use base handler for directories.
        			return null;
        		}
				return resource;
        	}
        };
        resource_handler.setCacheControl("public, max-age=0, s-maxage=0");
        resource_handler.setDirectoriesListed(false);
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

	/**
	 * Reset the {@link Renderer} whenever the templates change in the filesystem.
	 * 
	 * <p>
	 * Note: With Java 7, this could be formulated more efficiently using the Path API.
	 * </p>
	 * 
	 * @param oven The {@link Oven} to watch.
	 */
	private static void watch(final Oven oven) {
		Thread watcher = new Thread("Filesystem watcher") {

			long _timestamp = System.currentTimeMillis();
			
			@Override
			public void run() {
				while (true) {
					oven.bakeIncremental();
					
					if (check(oven.getTemplatesPath())) {
						LOGGER.info("Templates changed, resetting renderer.");
						_timestamp = System.currentTimeMillis();
						oven.resetRenderer();
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException ex) {
						break;
					}
				}
			}

			private boolean check(File path) {
				for (File file : path.listFiles()) {
					if (file.getName().startsWith(".")) {
						continue;
					}
					if (file.isDirectory()) {
						if (check(file)) {
							return true;
						}
					} else {
						long lastModified = file.lastModified();
						if (lastModified > _timestamp) {
							return true;
						}
					}
				}
				return false;
			}
			
		};
		
		watcher.setDaemon(true);
		watcher.start();
	}

	private static String uri(String path) {
		return path.replace('\\', '/');
	}
	
	private static String baseUri(String path) {
		int suffixIndex = path.lastIndexOf('.');
		if (suffixIndex >= 0) {
			return path.substring(0, suffixIndex);
		} else {
			return path;
		}
	}
}
