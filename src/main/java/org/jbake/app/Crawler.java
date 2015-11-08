package org.jbake.app;

import static java.io.File.separator;

import com.orientechnologies.orient.core.record.impl.ODocument;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.model.DocumentStatus;
import org.jbake.model.DocumentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crawls a file system looking for content.
 *
 * @author Jonathan Bullock <jonbullock@gmail.com>
 */
public class Crawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Crawler.class);

    private CompositeConfiguration config;
    private Parser parser;
    private final ContentStore db;
    private String contentPath;

    /**
     * Creates new instance of Crawler.
     */
    public Crawler(ContentStore db, File source, CompositeConfiguration config) {
        this.db = db;
        this.config = config;
        this.contentPath = source.getPath() + separator + config.getString(ConfigUtil.Keys.CONTENT_FOLDER);
        this.parser = new Parser(config, contentPath);
    }

    /**
     * Crawl all files and folders looking for content.
     *
     * @param path Folder to start from
     */
    public void crawl(File path) {
        File[] contents = path.listFiles(FileUtil.getFileFilter());
        if (contents != null) {
            Arrays.sort(contents);
            for (File sourceFile : contents) {
                if (sourceFile.isFile()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Processing [").append(sourceFile.getPath()).append("]... ");
                    String sha1 = buildHash(sourceFile);
                    String uri = buildURI(sourceFile);
                    boolean process = true;
                    DocumentStatus status = DocumentStatus.NEW;
                    for (String docType : DocumentTypes.getDocumentTypes()) {
                        status = findDocumentStatus(docType, uri, sha1);
                        switch (status) {
                            case UPDATED:
                                sb.append(" : modified ");
                                db.deleteContent(docType, uri);
                                break;
                            case IDENTICAL:
                                sb.append(" : same ");
                                process = false;
                        }
                        if (!process) {
                            break;
                        }
                    }
                    if (DocumentStatus.NEW == status) {
                        sb.append(" : new ");
                    }
                    if (process) { // new or updated
                        crawlSourceFile(sourceFile, sha1, uri);
                    }
                    LOGGER.info(sb.toString());
                }
                if (sourceFile.isDirectory()) {
                    crawl(sourceFile);
                }
            }
        }
    }

    private String buildHash(final File sourceFile) {
        String sha1;
        try {
            sha1 = FileUtil.sha1(sourceFile);
        } catch (Exception e) {
            e.printStackTrace();
            sha1 = "";
        }
        return sha1;
    }
    
    private String buildURI(final File sourceFile) {
    	String uri = FileUtil.asPath(sourceFile.getPath()).replace(FileUtil.asPath( contentPath), "");
    	// strip off leading / to enable generating non-root based sites
    	if (uri.startsWith("/")) {
    		uri = uri.substring(1, uri.length());
    	}
        return uri;
    }

    private void crawlSourceFile(final File sourceFile, final String sha1, final String uri) {
        Map<String, Object> fileContents = parse(uri, sourceFile);
        if (fileContents != null) {
        	fileContents.put("sha1", sha1);
        	String documentType = (String) fileContents.get("type");
        	save(documentType, fileContents);
        }
    }

	public Map<String, Object> parse(final String uri, final File sourceFile) {
		Map<String, Object> fileContents = parser.processFile(sourceFile);
        if (fileContents != null) {
        	fileContents.put("rootpath", getPathToRoot(sourceFile));
            fileContents.put("rendered", false);
            if (fileContents.get("tags") != null) {
                // store them as a String[]
                String[] tags = (String[]) fileContents.get("tags");
                fileContents.put("tags", tags);
            }
            fileContents.put("file", sourceFile.getPath());
            
            fileContents.put("sourceURI", uri);
            fileContents.put("uri", uri.substring(0, uri.lastIndexOf(".")) + FileUtil.findExtension(config, fileContents.get("type").toString()));

            if (fileContents.get("status").equals("published-date")) {
                if (fileContents.get("date") != null && (fileContents.get("date") instanceof Date)) {
                    if (new Date().after((Date) fileContents.get("date"))) {
                        fileContents.put("status", "published");
                    }
                }
            }
        } else {
            LOGGER.warn("{} has an invalid header, it has been ignored!", sourceFile);
        }
		return fileContents;
    }

	private void save(String documentType, Map<String, Object> fileContents) {
		ODocument doc = new ODocument(documentType);
		doc.fields(fileContents);
		boolean cached = fileContents.get("cached") != null ? Boolean.valueOf((String)fileContents.get("cached")):true;
		doc.field("cached", cached);
		doc.save();
	}

    public String getPathToRoot(File sourceFile) {
    	File rootPath = new File(contentPath);
    	File parentPath = sourceFile.getParentFile();
    	int parentCount = 0;
    	while (!parentPath.equals(rootPath)) {
    		parentPath = parentPath.getParentFile();
    		parentCount++;
    	}
    	StringBuffer sb = new StringBuffer();
    	for (int i = 0; i < parentCount; i++) {
    		sb.append("../");
    	}
    	return sb.toString();
    }
    
    public int getDocumentCount(String docType) {
        return (int) db.countClass(docType);
    }

    public Set<String> getTags() {
        return db.getTags();
    }

    private DocumentStatus findDocumentStatus(String docType, String uri, String sha1) {
        List<ODocument> match = db.getDocumentStatus(docType, uri);
        if (!match.isEmpty()) {
            ODocument entries = match.get(0);
            String oldHash = entries.field("sha1");
            if (!(oldHash.equals(sha1)) || Boolean.FALSE.equals(entries.field("rendered"))) {
                return DocumentStatus.UPDATED;
            } else {
                return DocumentStatus.IDENTICAL;
            }
        } else {
            return DocumentStatus.NEW;
        }
    }
}
