package org.jbake.processors;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.jbake.template.RenderingException;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;

/**
 * Algorithm for post-processing generated contents.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class PostProcessor {

	/**
	 * Post-processes the given buffered content and flushes it the the given
	 * {@link Writer}.
	 * 
	 * @param buffer
	 *        The buffered content.
	 * @param out
	 *        The {@link Writer} to write the postprocessing result to.
	 */
	public static void postProcess(StringWriter buffer, Writer out) throws RenderingException {
		try {
			Document document = Jsoup2Dom.dom(Jsoup.parse(buffer.toString()));
			MailtoObfuscator.processDocument(document);
			HTML5Serializer.serialize(out, document);
		} catch (XMLStreamException ex) {
			throw new RenderingException(ex);
		} catch (FactoryConfigurationError ex) {
			throw new RenderingException(ex);
		} catch (ParserConfigurationException ex) {
			throw new RenderingException(ex);
		}
	}

}
