package org.jbake.parser;

import java.util.Date;

import org.jbake.app.JDocument;

/**
 * An internal rendering engine used to notify the user that the markup format he used requires an engine that couldn't
 * be loaded.
 *
 * @author Cédric Champeau
 */
public class ErrorEngine extends MarkupEngine {
    private final String engineName;

    public ErrorEngine() {
        this("unknown");
    }

    public ErrorEngine(final String name) {
        engineName = name;
    }

    @Override
    public void processHeader(final ParserContext context) {
        JDocument contents = context.getContents();
        contents.setType("post");
        contents.setStatus("published");
        contents.setTitle("Rendering engine missing");
        contents.setDate(new Date());
        contents.setTags(new String[0]);
        contents.setID(context.getFile().getName());
    }

    @Override
    public void processBody(final ParserContext context) {
        context.setBody("The markup engine [" + engineName + "] for [" + context.getFile() + "] couldn't be loaded");
    }
}
