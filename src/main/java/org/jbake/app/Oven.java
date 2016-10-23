package org.jbake.app;

import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.app.ConfigUtil.Keys;
import org.jbake.model.DocumentAttributes;
import org.jbake.model.DocumentTypes;
import org.jbake.render.RenderingTool;
import org.jbake.template.ModelExtractorsDocumentTypeListener;
import org.jbake.template.RenderingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * All the baking happens in the Oven!
 *
 * @author Jonathan Bullock <jonbullock@gmail.com>
 */
public class Oven {

    private final static Logger LOGGER = LoggerFactory.getLogger(Oven.class);

    private final static Pattern TEMPLATE_DOC_PATTERN = Pattern.compile("(?:template\\.)([a-zA-Z0-9]+)(?:\\.file)");

    private CompositeConfiguration config;
	private File source;
	private File destination;
	private File templatesPath;
	private File contentsPath;
	private File assetsPath;
	private boolean isClearCache;
	private List<Throwable> errors = new LinkedList<Throwable>();
	private int renderedCount = 0;

	private ContentStore db;

	private Renderer renderer;

	private Crawler crawler;

    /**
     * Delegate c'tor to prevent API break for the moment.
     */
    public Oven(final File source, final File destination, final boolean isClearCache) throws Exception {
        this(source, destination, ConfigUtil.load(source), isClearCache);
    }

    /**
     * Creates a new instance of the Oven with references to the source and destination folders.
     *
     * @param source      The source folder
     * @param destination The destination folder
     */
    public Oven(final File source, final File destination, final CompositeConfiguration config, final boolean isClearCache) {
        this.source = source;
        this.destination = destination;
        this.config = config;
        this.isClearCache = isClearCache;
    }

    public CompositeConfiguration getConfig() {
        return config;
    }

    // TODO: do we want to use this. Else, config could be final
    public void setConfig(final CompositeConfiguration config) {
        this.config = config;
    }

    private void ensureSource() {
        if (!FileUtil.isExistingFolder(source)) {
            throw new JBakeException("Error: Source folder must exist: " + source.getAbsolutePath());
        }
        if (!source.canRead()) {
            throw new JBakeException("Error: Source folder is not readable: " + source.getAbsolutePath());
        }
    }

    private void ensureDestination() {
        if (null == destination) {
            destination = new File(source, config.getString(Keys.DESTINATION_FOLDER));
        }
        if (!destination.exists()) {
            destination.mkdirs();
        }
        if (!destination.canWrite()) {
            throw new JBakeException("Error: Destination folder is not writable: " + destination.getAbsolutePath());
        }
    }

    private File setupPathFromConfig(String key) {
        return new File(source, config.getString(key));
    }

    /**
     * Checks source path contains required sub-folders (i.e. templates) and setups up variables for them.
     *
     * @throws JBakeException If template or contents folder don't exist
     */
    public void setupPaths() {
        ensureSource();
        templatesPath = setupRequiredFolderFromConfig(Keys.TEMPLATE_FOLDER);
        contentsPath = setupRequiredFolderFromConfig(Keys.CONTENT_FOLDER);
        assetsPath = setupPathFromConfig(Keys.ASSET_FOLDER);
        if (!assetsPath.exists()) {
            LOGGER.warn("No asset folder was found!");
        }
        ensureDestination();
    }
    
    public File getContentsPath() {
		return contentsPath;
	}
    
    public File getAssetsPath() {
		return assetsPath;
	}
    
    public File getTemplatesPath() {
		return templatesPath;
	}

	private File setupRequiredFolderFromConfig(final String key) {
		final File path = setupPathFromConfig(key);
		if (!FileUtil.isExistingFolder(path)) {
			throw new JBakeException("Error: Required folder cannot be found! Expected to find [" + key + "] at: " + path.getAbsolutePath());
		}
		return path;
	}

	/**
	 * All the good stuff happens in here...
	 *
	 * @throws JBakeException
	 */
	public void bake() {
		setupDB();
        try {
        	bakeIncremental();
        } finally {
            shutdownDB();
        }
    }

	public void bakeIncremental() {
		errors.clear();
		renderedCount = 0;
		
		final long start = new Date().getTime();
		LOGGER.info("Baking has started...");

		    // process source content
		    int processed = getCrawler().crawl(contentsPath);
		    LOGGER.info("Content detected:");
		    for (String docType : DocumentTypes.getDocumentTypes()) {
		    	long count = db.getDocumentCount(docType);
		    	if (count > 0) {
		    		LOGGER.info("Parsed {} files of type: {}", count, docType);
				}
		    }

		    if (processed > 0) {
		    	Renderer renderer = getRenderer();
		    	
		    	for(RenderingTool tool : ServiceLoader.load(RenderingTool.class)) {
		    		try {
		    			renderedCount += tool.render(renderer, db, destination, templatesPath, config);
		    		} catch(RenderingException e) {
		    			errors.add(e);
		    		}
		    	}
		    	
		    	// mark docs as rendered
		    	for (String docType : DocumentTypes.getDocumentTypes()) {
		    		db.markConentAsRendered(docType);
		    	}
		    }
		    
		    // copy assets
		    Asset asset = new Asset(source, destination, config);
		    asset.copy(assetsPath);
		    errors.addAll(asset.getErrors());

		    LOGGER.info("Baking finished!");
		    long end = new Date().getTime();
		    LOGGER.info("Baked {} items in {}ms", renderedCount, end - start);
		    if (errors.size() > 0) {
		            LOGGER.error("Failed to bake {} item(s)!", errors.size());
		    }
	}

	public Crawler getCrawler() {
		if (crawler == null) {
			crawler = new Crawler(db, source, config);
		}
		return crawler;
	}

	/**
	 * Prepares for baking.
	 */
	public void setupDB() {
		db = DBUtil.createDataStore(config.getString(Keys.DB_STORE), config.getString(Keys.DB_PATH));
        updateDocTypesFromConfiguration();
        DBUtil.updateSchema(db);
        clearCacheIfNeeded(db);
	}
	
	/**
	 * The source {@link ContentStore}.
	 */
	public ContentStore getDB() {
		return db;
	}
	
	/**
	 * Frees resources after baking.
	 */
	public void shutdownDB() {
		db.close();
		db.shutdown();
	}
	
	/**
	 * Allocates the {@link Renderer}.
	 */
	public Renderer getRenderer() {
		if (renderer == null) {
			renderer = new Renderer(db, destination, templatesPath, config);
		}
		return renderer;
	}
	
    public void resetRenderer() {
    	renderer = null;
    }

    /**
     * Iterates over the configuration, searching for keys like "template.index.file=..."
     * in order to register new document types.
     */
    private void updateDocTypesFromConfiguration() {
        ModelExtractorsDocumentTypeListener listener = new ModelExtractorsDocumentTypeListener();
        DocumentTypes.addListener(listener);

        Iterator<String> keyIterator = config.getKeys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Matcher matcher = TEMPLATE_DOC_PATTERN.matcher(key);
            if (matcher.find()) {
                DocumentTypes.addDocumentType(matcher.group(1));
            }
        }
    }

    private void clearCacheIfNeeded(final ContentStore db) {
        boolean needed = isClearCache;
        if (!needed) {
            DocumentList docs = db.getSignaturesForTemplates();
            String currentTemplatesSignature;
            try {
                currentTemplatesSignature = FileUtil.sha1(templatesPath);
            } catch (Exception e) {
                currentTemplatesSignature = "";
            }
            if (!docs.isEmpty()) {
                String sha1 = (String) docs.get(0).get(String.valueOf(DocumentAttributes.SHA1));
                needed = !sha1.equals(currentTemplatesSignature);
                if (needed) {
                    db.updateSignatures(currentTemplatesSignature);
                }
            } else {
                // first computation of templates signature
                db.insertSignature(currentTemplatesSignature);
                needed = true;
            }
        }
        if (needed) {
            for (String docType : DocumentTypes.getDocumentTypes()) {
                try {
                    db.deleteAllByDocType(docType);
                } catch (Exception e) {
                    // maybe a non existing document type
                }
            }
            DBUtil.updateSchema(db);
        }
    }

	public List<Throwable> getErrors() {
		return new ArrayList<Throwable>(errors);
	}

}
