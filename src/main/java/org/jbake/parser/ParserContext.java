package org.jbake.parser;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.app.JDocument;

public class ParserContext {
    private final File file;
    private final List<String> fileLines;
    private final CompositeConfiguration config;
    private final String contentPath;
    private final boolean hasHeader;
    private final JDocument contents;

    public ParserContext(
            File file,
            List<String> fileLines,
            CompositeConfiguration config,
            String contentPath,
            boolean hasHeader,
            JDocument content) {
        this.file = file;
        this.fileLines = fileLines;
        this.config = config;
        this.contentPath = contentPath;
        this.hasHeader = hasHeader;
        this.contents = content;
    }

    public File getFile() {
        return file;
    }

    public List<String> getFileLines() {
        return fileLines;
    }

    public CompositeConfiguration getConfig() {
        return config;
    }

    public String getContentPath() {
        return contentPath;
    }

    public JDocument getContents() {
        return contents;
    }

    public boolean hasHeader() {
        return hasHeader;
    }

    // short methods for common use
    public String getBody() {
        return contents.getBody();
    }

    public void setBody(String str) {
        contents.setBody(str);
    }
}
