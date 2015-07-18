package org.jbake.app;

import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.app.ConfigUtil.Keys;
import org.jbake.template.DelegatingTemplateEngine;
import org.jbake.template.RenderingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Render output to a file.
 *
 * @author Jonathan Bullock <jonbullock@gmail.com>
 */
public class Renderer {

    private final static Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

    // TODO: should all content be made available to all templates via this class??

    private final ContentStore db;
    
    private File destination;
    private CompositeConfiguration config;
    private final DelegatingTemplateEngine renderingEngine;

    /**
     * Creates a new instance of Renderer with supplied references to folders.
     *
     * @param destination   The destination folder
     * @param templatesPath The templates folder
     */
    public Renderer(ContentStore db, File destination, File templatesPath, CompositeConfiguration config) {
        this.db = db;
		this.destination = destination;
        this.config = config;
        this.renderingEngine = new DelegatingTemplateEngine(config, db, destination, templatesPath);
    }

    private String findTemplateName(String docType) {
        return config.getString("template."+docType+".file");
    }

    /**
     * Render the supplied content to a file.
     *
     * @param content The content to renderDocument
     * @return The rendered/updated {@link File}.
     * @throws Exception
     */
    public File render(JDocument content) throws Exception {
    	String docType = docType(content);
        String outputFilename = destination.getPath() + File.separatorChar + content.getURI();
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

        if (content.getStatus().equals("draft")) {
            outputFilename = outputFilename + config.getString(Keys.DRAFT_SUFFIX);
        }

        File outputFile = new File(outputFilename + FileUtil.findExtension(config,docType));
        StringBuilder sb = new StringBuilder();
        sb.append("Rendering [").append(outputFile).append("]... ");

        try {
            Writer out = createWriter(outputFile);
            renderDocument(content, out, false);
            out.close();
            sb.append("done!");
            LOGGER.info(sb.toString());
        } catch (Exception e) {
            sb.append("failed!");
            LOGGER.error(sb.toString(), e);
            throw new Exception("Failed to render file. Cause: " + e.getMessage(), e);
        }
        
        return outputFile;
    }

	public void renderDocument(JDocument content, Writer out, boolean wikiMode) throws RenderingException {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("content", content.asMap());
		model.put("renderer", renderingEngine);
		if (wikiMode) {
			setWikiMode(model);
		}
		renderingEngine.renderDocument(model, findTemplateName(docType(content)), out);
	}

	private String docType(JDocument content) {
		return content.getType();
	}

    private Writer createWriter(File file) throws IOException {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        return new OutputStreamWriter(new FileOutputStream(file), config.getString(ConfigUtil.Keys.RENDER_ENCODING));
    }

    /**
     * Render an index file using the supplied content.
     *
     * @param indexFile The name of the output file
     * @throws Exception 
     */
    public void renderIndex(String indexFile) throws Exception {
        File outputFile = new File(destination.getPath() + File.separator + indexFile);
        StringBuilder sb = new StringBuilder();
        sb.append("Rendering index [").append(outputFile).append("]...");

        try {
            Writer out = createWriter(outputFile);
            renderIndex(out, false);
            out.close();
            sb.append("done!");
            LOGGER.info(sb.toString());
        } catch (Exception e) {
            sb.append("failed!");
            LOGGER.error(sb.toString(), e);
            throw new Exception("Failed to render index. Cause: " + e.getMessage(), e);
        }
    }

	public void renderIndex(Writer out, boolean wikiMode) throws RenderingException {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("renderer", renderingEngine);
		model.put("content", buildSimpleModel("masterindex"));
		if (wikiMode) {
			setWikiMode(model);
		}
		renderingEngine.renderDocument(model, findTemplateName("masterindex"), out);
	}

    /**
     * Render an XML sitemap file using the supplied content.
     * @throws Exception 
     *
     * @see <a href="https://support.google.com/webmasters/answer/156184?hl=en&ref_topic=8476">About Sitemaps</a>
     * @see <a href="http://www.sitemaps.org/">Sitemap protocol</a>
     */
    public void renderSitemap(String sitemapFile) throws Exception {
        File outputFile = new File(destination.getPath() + File.separator + sitemapFile);
        StringBuilder sb = new StringBuilder();
        sb.append("Rendering sitemap [").append(outputFile).append("]... ");

        try {
            Writer out = createWriter(outputFile);
            renderSitemap(out, false);
            sb.append("done!");
            out.close();
            LOGGER.info(sb.toString());
        } catch (Exception e) {
            sb.append("failed!");
            LOGGER.error(sb.toString(), e);
            throw new Exception("Failed to render sitemap. Cause: " + e.getMessage(), e);
        }
    }

	public void renderSitemap(Writer out, boolean wikiMode) throws RenderingException {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("renderer", renderingEngine);
		model.put("content", buildSimpleModel("sitemap"));
		if (wikiMode) {
			setWikiMode(model);
		}
		renderingEngine.renderDocument(model, findTemplateName("sitemap"), out);
	}

    /**
     * Render an XML feed file using the supplied content.
     *
     * @param feedFile The name of the output file
     * @throws Exception 
     */
    public void renderFeed(String feedFile) throws Exception {
        File outputFile = new File(destination.getPath() + File.separator + feedFile);
        StringBuilder sb = new StringBuilder();
        sb.append("Rendering feed [").append(outputFile).append("]... ");

        try {
            Writer out = createWriter(outputFile);
            renderFeed(out, false);
            out.close();
            sb.append("done!");
            LOGGER.info(sb.toString());
        } catch (Exception e) {
            sb.append("failed!");
            LOGGER.error(sb.toString(), e);
            throw new Exception("Failed to render feed. Cause: " + e.getMessage(), e);
        }
    }

	public void renderFeed(Writer out, boolean wikiMode) throws RenderingException {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("renderer", renderingEngine);
		model.put("content", buildSimpleModel("feed"));
		if (wikiMode) {
			setWikiMode(model);
		}
		renderingEngine.renderDocument(model, findTemplateName("feed"), out);
	}

    /**
     * Render an archive file using the supplied content.
     *
     * @param archiveFile The name of the output file
     * @throws Exception 
     */
    public void renderArchive(String archiveFile) throws Exception {
        File outputFile = new File(destination.getPath() + File.separator + archiveFile);
        StringBuilder sb = new StringBuilder();
        sb.append("Rendering archive [").append(outputFile).append("]... ");

        try {
            Writer out = createWriter(outputFile);
            renderArchive(out, false);
            out.close();
            sb.append("done!");
            LOGGER.info(sb.toString());
        } catch (Exception e) {
            sb.append("failed!");
            LOGGER.error(sb.toString(), e);
            throw new Exception("Failed to render archive. Cause: " + e.getMessage(), e);
        }
    }

	public void renderArchive(Writer out, boolean wikiMode) throws RenderingException {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("renderer", renderingEngine);
		model.put("content", buildSimpleModel("archive"));
		if (wikiMode) {
			setWikiMode(model);
		}
		renderingEngine.renderDocument(model, findTemplateName("archive"), out);
	}

    /**
     * Render tag files using the supplied content.
     *
     * @param tags    The content to renderDocument
     * @param tagPath The output path
     * @throws Exception 
     */
    public void renderTags(String tagPath) throws Exception {
    	Set<String> tags = db.getTags();
    	final List<Throwable> errors = new LinkedList<Throwable>();
        for (String tag : tags) {
            File outputFile = new File(destination.getPath() + File.separator + tagPath + File.separator + tag + config.getString(Keys.OUTPUT_EXTENSION));
            StringBuilder sb = new StringBuilder();
            sb.append("Rendering tags [").append(outputFile).append("]... ");

            try {
                Writer out = createWriter(outputFile);
                renderTag(tag, out, false);
                out.close();
                sb.append("done!");
                LOGGER.info(sb.toString());
            } catch (Exception e) {
                sb.append("failed!");
                LOGGER.error(sb.toString(), e);
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
        }
    }

	public void renderTag(String tag, Writer out, boolean wikiMode) throws RenderingException {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("renderer", renderingEngine);
		model.put("tag", tag);
		Map<String, Object> map = buildSimpleModel("tag");
		map.put("rootpath", "../");
		model.put("content", map);
		if (wikiMode) {
			setWikiMode(model);
		}
		renderingEngine.renderDocument(model, findTemplateName("tag"), out);
	}
    
    /**
     * Builds simple map of values, which are exposed when rendering index/archive/sitemap/feed/tags.
     * 
     * @param type
     * @return
     */
    private Map<String, Object> buildSimpleModel(String type) {
    	Map<String, Object> content = new HashMap<String, Object>();
    	content.put("type", type);
    	content.put("rootpath", "");
    	// add any more keys here that need to have a default value to prevent need to perform null check in templates
    	return content;
    }

    /**
     * Sets the flag indicating the the document is rendered for online view. 
     * 
     * @param document The document properties to modify.
     */
	private static void setWikiMode(Map<String, Object> document) {
		document.put("wikibake", Boolean.TRUE);
	}
}
