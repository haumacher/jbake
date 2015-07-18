/*
 * The MIT License
 *
 * Copyright 2015 jdlee.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jbake.app;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/**
 *
 * @author jdlee
 */
public class ContentStore {
	
    private static final String SIGNATURES_CLASS = "Signatures";

    /**
     * The node class to store all kinds of documents in.
     */
	public static final String DOCUMENT_CLASS = "Document";

    private ODatabaseDocumentTx db;

    public ContentStore(final String type, String name) {
        db = new ODatabaseDocumentTx(type + ":" + name);
        boolean exists = db.exists();
        if (!exists) {
            db.create();
        }
        db = ODatabaseDocumentPool.global().acquire(type + ":" + name, "admin", "admin");
        ODatabaseRecordThreadLocal.INSTANCE.set(db);
        if (!exists) {
            updateSchema();
        }
    }

    public final void updateSchema() {
        OSchema schema = db.getMetadata().getSchema();
        if (schema.getClass(DOCUMENT_CLASS) == null) {
        	createDocumentClass(schema);
        }
        if (schema.getClass(SIGNATURES_CLASS) == null) {
            // create the sha1 signatures class
            OClass signatures = schema.createClass(SIGNATURES_CLASS);
            signatures.createProperty("key", OType.STRING).setNotNull(true);
            signatures.createProperty("sha1", OType.STRING).setNotNull(true);
        }
    }

    public void close() {
        db.close();
    }

    public void drop() {
        db.drop();
    }

    public long countClass(String docType) {
    	return query("select count(1) from " + DOCUMENT_CLASS + " where type=?", docType).get(0).field("count");
    }

    public List<ODocument> getDocumentStatus(String uri) {
        return query("select sha1,rendered from " + DOCUMENT_CLASS + " where sourceuri=?", uri);

    }

    public List<ODocument> getPublishedPosts() {
        return getPublishedContent("post");
    }

    public List<ODocument> getPublishedPostsByTag(String tag) {
        return query("select * from " + DOCUMENT_CLASS + " where type='post' and status='published' and ? in tags order by date desc", tag);
    }

    public List<ODocument> getPublishedPages() {
        return getPublishedContent("page");
    }

    public List<ODocument> getPublishedContent(String docType) {
        return query("select * from " + DOCUMENT_CLASS + " where type=? and status='published' order by date desc", docType);
    }

    public List<ODocument> getAllContent(String docType) {
        return query("select * from " + DOCUMENT_CLASS + " where type=? order by date desc", docType);
    }

    public List<ODocument> getAllTagsFromPublishedPosts() {
        return query("select tags from " + DOCUMENT_CLASS + " where type='post' and status='published'");
    }

    public List<ODocument> getSignaturesForTemplates() {
        return query("select sha1 from Signatures where key='templates'");
    }

    public List<ODocument> getUnrenderedContent(String docType) {
        return query("select * from " + DOCUMENT_CLASS + " where type=? and rendered=false", docType);
    }

    public void deleteContent(String uri) {
        executeCommand("delete from " + DOCUMENT_CLASS + " where sourceuri=?", uri);
    }

    public void markConentAsRendered(String docType) {
        executeCommand("update " + docType + " set rendered=true where rendered=false and cached=true");
    }

    public void updateSignatures(String currentTemplatesSignature) {
        executeCommand("update Signatures set sha1=? where key='templates'", currentTemplatesSignature);
    }

    public void deleteAllByDocType(String docType) {
        executeCommand("delete from " + DOCUMENT_CLASS + " where type=?", docType);
    }

    public void insertSignature(String currentTemplatesSignature) {
        executeCommand("insert into Signatures(key,sha1) values('templates',?)", currentTemplatesSignature);
    }

    private List<ODocument> query(String sql) {
        return db.query(new OSQLSynchQuery<ODocument>(sql));
    }
    
    private List<ODocument> query(String sql, Object... args) {
        return db.command(new OSQLSynchQuery<ODocument>(sql)).execute(args);
    }

    private void executeCommand(String query, Object... args) {
        db.command(new OCommandSQL(query)).execute(args);
    }

    public Set<String> getTags() {
		List<ODocument> query = getAllTagsFromPublishedPosts(); //query(new OSQLSynchQuery<ODocument>("select tags from post where status='published'"));
	    Set<String> result = new HashSet<String>();
	    for (ODocument document : query) {
	        String[] tags = DBUtil.toStringArray(document.field("tags"));
	        Collections.addAll(result, tags);
	    }
	    return result;
	}

    private static void createDocumentClass(final OSchema schema) {
        OClass page = schema.createClass(DOCUMENT_CLASS);
        page.createProperty(JDocument.TYPE, OType.STRING).setNotNull(true);
        page.createProperty(JDocument.SHA1, OType.STRING).setNotNull(true);
        page.createProperty(JDocument.SOURCE_URI, OType.STRING).setNotNull(true);
        page.createProperty(JDocument.RENDERED, OType.BOOLEAN).setNotNull(true);
        page.createProperty(JDocument.CACHED, OType.BOOLEAN).setNotNull(true);

        // commented out because for some reason index seems to be written
        // after the database is closed to this triggers an exception
        //page.createIndex("uriIdx", OClass.INDEX_TYPE.UNIQUE, "uri");
        //page.createIndex("renderedIdx", OClass.INDEX_TYPE.NOTUNIQUE, "rendered");
    }
}
