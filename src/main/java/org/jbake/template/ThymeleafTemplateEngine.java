package org.jbake.template;

import java.io.File;
import java.io.Writer;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.lang.LocaleUtils;
import org.jbake.app.ConfigUtil.Keys;
import org.jbake.app.ContentStore;
import org.jbake.app.Crawler.Attributes;
import org.jbake.app.DBUtil;
import org.jbake.app.DocumentList;
import org.jbake.model.DocumentTypes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * <p>A template engine which renders pages using Thymeleaf.</p>
 *
 * <p>This template engine is not recommended for large sites because the whole model
 * is loaded into memory due to Thymeleaf internal limitations.</p>
 *
 * <p>The default rendering mode is "HTML", but it is possible to use another mode
 * for each document type, by adding a key in the configuration, for example:</p>
 * <p/>
 * <code>
 *     template.feed.thymeleaf.mode=XML
 * </code>
 *
 * @author Cédric Champeau
 */
public class ThymeleafTemplateEngine extends AbstractTemplateEngine {
    private final ReentrantLock lock = new ReentrantLock();

    private TemplateEngine templateEngine;
    private FileTemplateResolver templateResolver;

	private String templateMode;

    public ThymeleafTemplateEngine(final CompositeConfiguration config, final ContentStore db, final File destination, final File templatesPath) {
        super(config, db, destination, templatesPath);
    }

    private void initializeTemplateEngine(String mode) {
    	if (mode.equals(templateMode)) {
    		return;
    	}
        templateMode = mode;
        templateResolver = new FileTemplateResolver();
        templateResolver.setPrefix(templatesPath.getAbsolutePath() + File.separatorChar);
        templateResolver.setCharacterEncoding(config.getString(Keys.TEMPLATE_ENCODING));
        templateResolver.setTemplateMode(mode);
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        try {
            IDialect condCommentDialect = (IDialect) Class.forName("org.thymeleaf.extras.conditionalcomments.dialect.ConditionalCommentsDialect").newInstance();
            templateEngine.addDialect(condCommentDialect);
        } catch (Exception e) {
            // Sad, but true and not a real problem
        }
        templateEngine.addDialect(new DocumentsDialect());
    }

    @Override
    public void renderDocument(final Map<String, Object> model, final String templateName, final Writer writer) throws RenderingException {
        Locale locale = locale();
        Context context = new Context(locale, wrap(model));
        lock.lock();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) model.get("config");
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) model.get("content");
            String mode = "HTML";
            if (config != null && content != null) {
                String key = "template_" + content.get(Attributes.TYPE) + "_thymeleaf_mode";
                String configMode = (String) config.get(key);
                if (configMode != null) {
                    mode = configMode;
                }
            }
            initializeTemplateEngine(mode);
            templateEngine.process(templateName, context, writer);
        } finally {
            lock.unlock();
        }
    }

	Locale locale() {
		String localeString = config.getString(Keys.THYMELEAF_LOCALE);
        return localeString != null ? LocaleUtils.toLocale(localeString) : Locale.getDefault();
	}

    private Map<String, Object> wrap(final Map<String, Object> model) {
        HashMap<String, Object> result = new HashMap<String, Object>(model);
        for(String key : extractors.keySet()) {
        	try {
				result.put(key, extractors.extractAndTransform(db, key, model, new TemplateEngineAdapter.NoopAdapter()));
			} catch (NoModelExtractorException e) {
				// should never happen, as we iterate over existing extractors
			}
        }
        result.put("db", db);
        result.put("alltags", getAllTags());
        result.put("tag_posts", getTagPosts(result.get("tag")));
        result.put("published_date", new Date());
        String[] documentTypes = DocumentTypes.getDocumentTypes();
        for (String docType : documentTypes) {
        	result.put(docType + "s", db.getAllContent(docType));
        	result.put("published_" + docType + "s", db.getPublishedContent(docType));
        }
        result.put("published_content", getPublishedContent());
        result.put("all_content", getAllContent());
		return result;
    }

    private Object getTagPosts(Object tagName) {
        if (tagName != null) {
            String tag = tagName.toString();
            // fetch the tag posts from db
            DocumentList query = db.getPublishedPostsByTag(tag);
            return query;
        } else {
            return Collections.emptyList();
        }
    }

    private Object getAllTags() {
    	DocumentList query = db.getAllTagsFromPublishedPosts();
        Set<String> result = new HashSet<String>();
        for (Map<String, Object> document : query) {
            String[] tags = DBUtil.toStringArray(document.get(Attributes.TAGS));
            Collections.addAll(result, tags);
        }
        return result;
    }
    
    private Object getPublishedContent() {
    	List<Map<String, Object>> publishedContent = new ArrayList<Map<String, Object>>();
    	String[] documentTypes = DocumentTypes.getDocumentTypes();
    	for (String docType : documentTypes) {
    		List<Map<String, Object>> query = db.getPublishedContent(docType);
    		publishedContent.addAll(query);
    	}
    	return publishedContent;
    }
    
    private Object getAllContent() {
    	List<Map<String, Object>> allContent = new ArrayList<Map<String, Object>>();
    	String[] documentTypes = DocumentTypes.getDocumentTypes();
    	for (String docType : documentTypes) {
    		List<Map<String, Object>> query = db.getAllContent(docType);
    		allContent.addAll(query);
    	}
    	return allContent;
    }

    /**
     * {@link IExpressionEnhancingDialect} that provides {@link DocumentsUtility} as <code>#documents</code>.
     */
    public class DocumentsDialect extends AbstractDialect implements IExpressionObjectDialect {
    	private static final String DOCUMENTS_OBJECT = "documents";
		private static final String DOCUMENTS_PREFIX = "documents";

		public DocumentsDialect() {
			super(DOCUMENTS_PREFIX);
		}

		@Override
		public IExpressionObjectFactory getExpressionObjectFactory() {
			return new IExpressionObjectFactory() {

				@Override
				public Set<String> getAllExpressionObjectNames() {
					return Collections.singleton(DOCUMENTS_OBJECT);
				}

				@Override
				public Object buildObject(IExpressionContext context, String expressionObjectName) {
					return new DocumentsUtility();
				}

				@Override
				public boolean isCacheable(String expressionObjectName) {
					return true;
				}
				
			};
		}

    	/**
    	 * Utility functions for sorting documents.
    	 */
    	public class DocumentsUtility {
    		
    		/**
    		 * Delegates a generic sql query to the {@link ContentStore}.
    		 * 
    		 * @param sql The query.
    		 * @return The query result.
    		 */
    		public List<Map<String,Object>> query(String sql) {
    			return db.query(sql);
    		}
    		
			/**
			 * Creates a {@link Comparator} for sorting documents according to a
			 * given specification.
			 * 
			 * @param spec
			 *        A list of document property names separated by
			 *        <code>'|'</code> according to which the resulting
			 *        {@link Comparator} sorts documents.
			 * @return A {@link Comparator} for document objects.
			 */
			public Comparator<Map<String,Object>> collator(String spec) {
				List<String> choicesList = new ArrayList<String>();
				for (String choice : spec.split("\\|")) {
					choicesList.add(choice.trim());
				}
				final String[] choices = choicesList.toArray(new String[choicesList.size()]);
				
    			return new Comparator<Map<String,Object>>() {

    				private final Collator _collator = Collator.getInstance(locale());
    				
					@Override
					public int compare(Map<String, Object> d1, Map<String, Object> d2) {
						Object key1 = sortKey(d1);
						Object key2 = sortKey(d2);
						if (key1 == null) {
							if (key2 == null) {
								return 0;
							} else {
								return 1;
							}
						} else {
							if (key2 == null) {
								return -1;
							} else {
								return _collator.compare(key1, key2);
							}
						}
					}

					private Object sortKey(Map<String, Object> d1) {
						for (String key : choices) {
							Object result = d1.get(key);
							if (result != null) {
								return result;
							}
						}
						return null;
					}
				};
    		}
    	}
    }
}
