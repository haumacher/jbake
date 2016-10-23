package org.jbake.app;

import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.app.ConfigUtil.Keys;
import org.jbake.app.Crawler.Attributes;
import org.jbake.template.DelegatingTemplateEngine;
import org.jbake.template.RenderingException;
import org.jbake.util.FileOut;
import org.jbake.util.Out;
import org.jbake.util.WriterOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Render output to a file.
 *
 * @author Jonathan Bullock <jonbullock@gmail.com>
 */
public class Renderer {
	
	private interface RenderingConfig {

		Writer getWriter() throws IOException;

		String getName();

		String getTemplate();

		Map<String, Object> getModel();
	}
	
	private static abstract class AbstractRenderingConfig implements RenderingConfig{

		private final Out out;
		protected final String name;
		protected final String template;

		public AbstractRenderingConfig(Out out, String name, String template) {
			super();
			this.out = out;
			this.name = name;
			this.template = template;
		}
		
		@Override
		public Writer getWriter() throws IOException {
			return out.getWriter();
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getTemplate() {
			return template;
		}
		
	}
	public static class ModelRenderingConfig extends AbstractRenderingConfig {
		private final Map<String, Object> model;

		public ModelRenderingConfig(Out out, String name, Map<String, Object> model, String template) {
			super(out, name, template);
			this.model = model;
		}
		
		@Override
		public Map<String, Object> getModel() {
			return model;
		}
	}
	
	class DefaultRenderingConfig extends AbstractRenderingConfig {

		private final Object content;
		
		private DefaultRenderingConfig(File path, Charset encoding, String allInOneName) {
			this(new FileOut(path, encoding), allInOneName);
		}
		
		public DefaultRenderingConfig(String filename, Charset encoding, String allInOneName) {
			this(fileOut(filename, encoding), allInOneName);
		}
		
		public DefaultRenderingConfig(Out out, String allInOneName) {
			super(out, allInOneName, findTemplateName(allInOneName));
			this.content = buildSimpleModel(allInOneName);
		}
		
		/**
		 * Constructor added due to known use of a allInOneName which is used for name, template and content
		 * @param allInOneName
		 */
		public DefaultRenderingConfig(String allInOneName, Charset encoding) {
			this(new File(destination.getPath() + File.separator + allInOneName + config.getString(Keys.OUTPUT_EXTENSION)), encoding, 
							allInOneName);
		}

		@Override
		public Map<String, Object> getModel() {
	        Map<String, Object> model = new HashMap<String, Object>();
	        model.put("renderer", renderingEngine);
	        model.put("content", content);
	        return model;
		}
		
	}

    private final static Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

    // TODO: should all content be made available to all templates via this class??

    private final File destination;
    private final CompositeConfiguration config;
    private final DelegatingTemplateEngine renderingEngine;
    private final ContentStore db;
    /**
     * Creates a new instance of Renderer with supplied references to folders.
     *
     * @param db            The database holding the content
     * @param destination   The destination folder
     * @param templatesPath The templates folder
     * @param config        
     */
    public Renderer(ContentStore db, File destination, File templatesPath, CompositeConfiguration config) {
        this.destination = destination;
        this.config = config;
        this.renderingEngine = new DelegatingTemplateEngine(config, db, destination, templatesPath);
        this.db = db;
    }

    private String findTemplateName(String docType) {
        String templateKey = "template."+docType+".file";
		String returned = config.getString(templateKey);
        return returned;
    }

    /**
     * Render the supplied content to a file.
     *
     * @param content The content to renderDocument
     * @throws Exception
     */
    public void render(Map<String, Object> content) throws Exception {
    	String docType = docType(content);
        String outputFilename = destination.getPath() + File.separatorChar + content.get(Attributes.URI);
        if (outputFilename.lastIndexOf(".") > 0) {
        	outputFilename = outputFilename.substring(0, outputFilename.lastIndexOf("."));
        }

        // delete existing versions if they exist in case status has changed either way
        File draftFile = new File(outputFilename + config.getString(Keys.DRAFT_SUFFIX) + FileUtil.findExtension(config, docType));
        if (draftFile.exists()) {
            draftFile.delete();
        }

        File publishedFile = new File(outputFilename + FileUtil.findExtension(config, docType));
        if (publishedFile.exists()) {
            publishedFile.delete();
        }

        if (content.get(Crawler.Attributes.STATUS).equals(Crawler.Attributes.Status.DRAFT)) {
            outputFilename = outputFilename + config.getString(Keys.DRAFT_SUFFIX);
        }

        File outputFile = new File(outputFilename + FileUtil.findExtension(config,docType));
        StringBuilder sb = new StringBuilder();
        sb.append("Rendering [").append(outputFile).append("]... ");

        try {
            Writer out = createWriter(outputFile);
            
            renderPage(out, content, docType);
            out.close();
            sb.append("done!");
            LOGGER.info(sb.toString());
        } catch (Exception e) {
            sb.append("failed!");
            LOGGER.error(sb.toString(), e);
            throw new Exception("Failed to render file. Cause: " + e.getMessage(), e);
        }
    }

	public String docType(Map<String, Object> content) {
		return (String) content.get(Crawler.Attributes.TYPE);
	}

	public void renderPage(Writer out, Map<String, Object> content, String docType)
			throws RenderingException {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("content", content);
		model.put("renderer", renderingEngine);
		renderingEngine.renderDocument(model, findTemplateName(docType), out);
	}

    private Writer createWriter(File file) throws IOException {
    	return FileOut.createWriter(file, getEncoding());
    }

	private Charset getEncoding() {
		return Charset.forName(config.getString(ConfigUtil.Keys.RENDER_ENCODING));
	}

    private void render(RenderingConfig renderConfig) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Rendering ").append(renderConfig.getName()).append(" [").append(renderConfig.getName()).append("]...");

        try {
            Writer out = renderConfig.getWriter();
            renderingEngine.renderDocument(renderConfig.getModel(), renderConfig.getTemplate(), out);
            out.close();
            sb.append("done!");
            LOGGER.info(sb.toString());
        } catch (Exception e) {
            sb.append("failed!");
            LOGGER.error(sb.toString(), e);
            throw new Exception("Failed to render "+renderConfig.getName(), e);
        }
    }
    
    /**
     * Render an index file using the supplied content.
     *
     * @param indexFile The name of the output file
     * @throws Exception
     */
    public void renderIndex(String indexFile) throws Exception {
      long totalPosts = db.getDocumentCount("post");
      boolean paginate = config.getBoolean(Keys.PAGINATE_INDEX, false);
      int postsPerPage = config.getInt(Keys.POSTS_PER_PAGE, -1);
      int start = 0;

      if (paginate) {
          db.setLimit(postsPerPage);
      }

      try {
          int page = 1;
          while (start < totalPosts) {
              renderIndexPage(totalPosts, paginate, postsPerPage, start, page, indexFile, null);
              
              if (paginate) {
                  start += postsPerPage;
                  page++;
              } else {
                  break; // TODO: eww
              }
          }
          db.resetPagination();
      } catch (Exception e) {
          throw new Exception("Failed to render index. Cause: " + e.getMessage(), e);
      }
    }

    public void renderIndexPage(int page, String indexName, Out out) throws Exception {
        long totalPosts = db.getDocumentCount("post");
        boolean paginate = config.getBoolean(Keys.PAGINATE_INDEX, false);
        int postsPerPage = config.getInt(Keys.POSTS_PER_PAGE, -1);
        int start = (page - 1) * postsPerPage;
		renderIndexPage(totalPosts, paginate, postsPerPage, start, postsPerPage, indexName, out);
    }
    
	private void renderIndexPage(long totalPosts, boolean paginate,
			int postsPerPage, int start, int page, String indexName, Out out)
			throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("renderer", renderingEngine);
		model.put("content", buildSimpleModel("masterindex"));
		
		String pageName;
		if (paginate) {
			db.setStart(start);
			int index = indexName.lastIndexOf(".");
			String baseName = indexName.substring(0, index);
			String suffix = indexName.substring(index);
			if (page != 1) {
				model.put("previousFileName", indexPageName(baseName, suffix, page - 1));
			}

			// If this iteration won't consume the remaining posts, calculate
			// the next file name
			if ((start + postsPerPage) < totalPosts) {
				model.put("nextFileName", indexPageName(baseName, suffix, page + 1));
			}
			// Add page number to file name
			pageName = indexPageName(baseName, suffix, page);
		} else {
			pageName = indexName;
		}
		
		if (out == null) {
			out = fileOut(pageName, getEncoding());
		}
		render(new DefaultRenderingConfig(out, "masterindex"));
	}

	private String indexPageName(String baseName, String suffix, int page) {
		return baseName + indexPageId(page) + suffix;
	}

	private Object indexPageId(int page) {
		return page > 1 ? page : "";
	}

    /**
     * Render an XML sitemap file using the supplied content.
     * @throws Exception 
     *
     * @see <a href="https://support.google.com/webmasters/answer/156184?hl=en&ref_topic=8476">About Sitemaps</a>
     * @see <a href="http://www.sitemaps.org/">Sitemap protocol</a>
     */
    public void renderSitemap(String sitemapFile) throws Exception {
    	render(new DefaultRenderingConfig(sitemapFile, getEncoding(), "sitemap"));
    }

    public void renderSitemap(Writer out) throws Exception {
    	render(new DefaultRenderingConfig(new WriterOut(out), "sitemap"));
    }
    
    /**
     * Render an XML feed file using the supplied content.
     *
     * @param feedFile The name of the output file
     * @throws Exception 
     */
    public void renderFeed(String feedFile) throws Exception {
    	render(new DefaultRenderingConfig(feedFile, getEncoding(), "feed"));
    }

    public void renderFeed(Writer out) throws Exception {
    	render(new DefaultRenderingConfig(new WriterOut(out), "feed"));
    }
    
    /**
     * Render an archive file using the supplied content.
     *
     * @param archiveFile The name of the output file
     * @throws Exception 
     */
    public void renderArchive(String archiveFile) throws Exception {
    	render(new DefaultRenderingConfig(archiveFile, getEncoding(), "archive"));
    }

    public void renderArchive(Writer out) throws Exception {
    	render(new DefaultRenderingConfig(new WriterOut(out), "archive"));
    }
    
    /**
     * Render tag files using the supplied content.
     *
     * @param tagPath The output path
     * @throws Exception 
     */
    public int renderTags(String tagPath) throws Exception {
    	int renderedCount = 0;
    	final List<Throwable> errors = new LinkedList<Throwable>();
        for (String tag : db.getTags()) {
            try {
            	tag = tag.trim().replace(" ", "-");
            	File path = new File(destination.getPath() + File.separator + tagPath + File.separator + tag + config.getString(Keys.OUTPUT_EXTENSION));
            	renderTag(path, tag);
                renderedCount++;
            } catch (Exception e) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
        	StringBuilder sb = new StringBuilder();
        	sb.append("Failed to render tags. Cause(s):");
        	for(Throwable error: errors) {
        		sb.append("\n" + error.getMessage());
        	}
        	throw new Exception(sb.toString(), errors.get(0));
        } else {
        	return renderedCount;
        }
    }

	private void renderTag(File path, String tag) throws Exception {
		renderTag(new FileOut(path, getEncoding()), tag);
	}

	public void renderTag(Writer out, String tag) throws Exception {
		renderTag(new WriterOut(out), tag);
	}
	
	private void renderTag(Out out, String tag) throws Exception {
		render(new ModelRenderingConfig(out, Attributes.TAG, createTagModel(tag), findTemplateName(Attributes.TAG)));
	}

	private Map<String, Object> createTagModel(String tag) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("renderer", renderingEngine);
		model.put(Attributes.TAG, tag);
		Map<String, Object> map = buildSimpleModel(Attributes.TAG);
		map.put(Attributes.ROOTPATH, "../");
		model.put("content", map);
		return model;
	}
    
    /**
     * Builds simple map of values, which are exposed when rendering index/archive/sitemap/feed/tags.
     * 
     * @param type
     * @return
     */
    private Map<String, Object> buildSimpleModel(String type) {
    	Map<String, Object> content = new HashMap<String, Object>();
    	content.put(Attributes.TYPE, type);
    	content.put(Attributes.ROOTPATH, "");
    	// add any more keys here that need to have a default value to prevent need to perform null check in templates
    	return content;
    }

    FileOut fileOut(String filename, Charset encoding) {
		return new FileOut(new File(destination.getPath() + File.separator + filename), encoding);
	}

}
