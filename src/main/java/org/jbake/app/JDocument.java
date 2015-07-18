package org.jbake.app;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.app.ConfigUtil.Keys;
import org.json.simple.JSONValue;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Representation of a document in JBake during parsing.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class JDocument {

	/**
	 * Property name of {@link #getRootPath()}.
	 */
	public static final String ROOTPATH = "rootpath";

	/**
	 * Property name of {@link #getFile()}.
	 */
	public static final String FILE = "file";

	/**
	 * Property name of {@link #getURI()}.
	 */
	public static final String URI = "uri";

	/**
	 * Property name of {@link #getSourceURI()}.
	 */
	public static final String SOURCE_URI = "sourceuri";

	/**
	 * Property name of {@link #getTitle()}.
	 */
	public static final String TITLE = "title";

	/**
	 * Property name of {@link #getDate()}.
	 */
	public static final String DATE = "date";

	/**
	 * Property name of {@link #getType()}.
	 */
	public static final String TYPE = "type";

	/**
	 * Property name of {@link #getStatus()}.
	 */
	public static final String STATUS = "status";

	/**
	 * Property name of {@link #getTags()}.
	 */
	public static final String TAGS = "tags";

	/**
	 * Property name of {@link #getBody()}.
	 */
	public static final String BODY = "body";

	/**
	 * Property name of {@link #getID()}.
	 */
	public static final String ID = "id";

	/**
	 * Property name of {@link #getSHA1()}.
	 */
	public static final String SHA1 = "sha1";

	/**
	 * Property name of {@link #getCached()}.
	 */
	public static final String CACHED = "cached";

	/**
	 * Property name of {@link #getRendered()}.
	 */
	public static final String RENDERED = "rendered";

	private final CompositeConfiguration config;
	
	private final Map<String, Object> content;
	
	/**
	 * Creates an empty {@link JDocument}.
	 */
	public JDocument(CompositeConfiguration config) {
		this(config, new HashMap<String, Object>());
	}

	/**
	 * Creates a {@link JDocument} from the given content.
	 */
	public JDocument(CompositeConfiguration config, Map<String, Object> content) {
		this.config = config;
		this.content = content;
	}

	/**
	 * The file name from which this {@link JDocument} was loaded.
	 */
	public String getFile() {
		return getAsString(FILE);
	}

	/**
	 * @see #getFile()
	 */
	public void setFile(String file) {
		content.put(FILE, file);
	}

	/**
	 * The relative path to the root of the web site for this {@link JDocument}.
	 */
	public String getRootPath() {
		return getAsString(ROOTPATH);
	}

	/**
	 * @see #getRootPath()
	 */
	public void setRootPath(String rootPath) {
		content.put(ROOTPATH, rootPath);
	}

	/**
	 * The URI (relative URI path) that will display the contents of this {@link JDocument} on the client.
	 */
	public String getURI() {
		return getAsString(URI);
	}

	/**
	 * @see #getURI()
	 */
	public void setURI(String uri) {
		content.put(URI, uri);
	}

	/**
	 * The URI (relative URI path) to retrieve the source code of this
	 * {@link JDocument} from (for dynamically updating in wiki mode).
	 */
	public String getSourceURI() {
		return getAsString(SOURCE_URI);
	}
	
	/**
	 * @see #getSourceURI()
	 */
	public void setSourceURI(String sourceURI) {
		content.put(SOURCE_URI, sourceURI);
	}

	/**
	 * The title of this {@link JDocument}.
	 */
	public String getTitle() {
		return getAsString(TITLE);
	}

	/**
	 * @see #getTitle()
	 */
	public void setTitle(String title) {
		content.put(TITLE, title);
	}

	/**
	 * The publish/last modification date of this {@link JDocument}.
	 */
	public Date getDate() {
		return getAsDate(DATE);
	}
	
	/**
	 * @see #getDate()
	 */
	public void setDate(Date date) {
		content.put(DATE, date);
	}

	/**
	 * The publishing status of this {@link JDocument}, <code>draft</code> or <code>published</code>.
	 */
	public String getStatus() {
		return getAsString(STATUS);
	}

	/**
	 * @see #getStatus()
	 */
	public void setStatus(String status) {
		content.put(STATUS, status);
	}

	/**
	 * The type of this {@link JDocument}, <code>page</code> or <code>post</code>.
	 */
	public String getType() {
		return getAsString(TYPE);
	}

	/**
	 * @see #getType()
	 */
	public void setType(String type) {
		content.put(TYPE, type);
	}

	/**
	 * The tags associated with this {@link JDocument}.
	 */
	public String[] getTags() {
		return (String[]) content.get(TAGS);
	}

	/**
	 * @see #getTags()
	 */
	public void setTags(String[] tags) {
		content.put(TAGS, tags);
	}

	/**
	 * The rendered body contents of this {@link JDocument}.
	 */
	public String getBody() {
		return content.get(BODY).toString();
	}

	/**
	 * @see #getBody()
	 */
	public void setBody(String str) {
		content.put(BODY, str);
	}

	/**
	 * Whether this {@link JDocument} has been stored to the internal database.
	 */
	public boolean getCached() {
		return getAsBoolean(CACHED, true);
	}

	/**
	 * @see #getCached()
	 */
	public void setCached(boolean value) {
		content.put(CACHED, Boolean.valueOf(value));
	}

	/**
	 * Whether this {@link JDocument} has already been rendered.
	 */
	public boolean getRendered() {
		return getAsBoolean(RENDERED, false);
	}
		
	/**
	 * @see #getRendered()
	 */
	public void setRendered(boolean value) {
		content.put(RENDERED, Boolean.valueOf(value));
	}

	/**
	 * The local name of the source file of this {@link JDocument}.
	 */
	public String getID() {
		return getAsString(ID);
	}

	/**
	 * @see #getID()
	 */
	public void setID(String id) {
		content.put(ID, id);
	}

	/**
	 * The SHA1 hash sum of the source of this {@link JDocument}.
	 */
	public String getSHA1() {
		return getAsString(SHA1);
	}
	
	/**
	 * @see #getSHA1()
	 */
	public void setSHA1(String sha1) {
		content.put(SHA1, sha1);
	}

	private String getAsString(String key) {
		return (String) content.get(key);
	}

	private Date getAsDate(String key) {
		return (Date) content.get(key);
	}

	private boolean getAsBoolean(String key, boolean defaultValue) {
		Boolean value = (Boolean) content.get(key);
		if (value == null) {
			return defaultValue;
		}
		return value.booleanValue();
	}

	/**
	 * The document contents as generic {@link Map} of key value pairs.
	 */
	public Map<String, Object> asMap() {
		return content;
	}

	/**
	 * Generically gets a property value.
	 * 
	 * @param key The property name.
	 * @return The property value.
	 */
	public Object genericGet(String key) {
		return content.get(key);
	}

	/**
	 * Generically sets a property value.
	 * 
	 * @param key The property name.
	 * @param value The property value.
	 */
	public void genericSet(String key, Object value) {
	    if (key.equalsIgnoreCase(DATE)) {
	        DateFormat df = new SimpleDateFormat(config.getString(Keys.DATE_FORMAT));
	        try {
	        	Date date = df.parse((String) value);
	            content.put(key, date);
	        } catch (ParseException e) {
	            e.printStackTrace();
	        }
	    } else if (key.equalsIgnoreCase(TAGS)) {
	        String[] tags = ((String) value).split(",");
	        content.put(key, tags);
	    } else if (value instanceof String && ((String) value).startsWith("{") && ((String) value).endsWith("}")) {
	        // Json type
	        content.put(key, JSONValue.parse((String) value));
	    } else {
	        content.put(key, value);
	    }
	}

	/**
	 * Pushes the contents of this {@link JDocument} to the given {@link ODocument}.
	 * 
	 * @param doc The {@link ODocument} to update.
	 */
	public void update(ODocument doc) {
		doc.fields(content);
		
		if (!content.containsKey(CACHED)) {
			// Apply default value.
			doc.field(CACHED, true);
		}
	}
	
}
