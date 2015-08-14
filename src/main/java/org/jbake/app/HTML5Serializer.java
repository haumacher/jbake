package org.jbake.app;

import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Algorithm to serialize a HTML {@link Document} in a HTML5-compatible way.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class HTML5Serializer {

	private static final Set<String> VOID_ELEMENTS = new HashSet<String>(
			Arrays.asList("area", "base", "br", "col", "command", "embed", "hr",
					"img", "input", "keygen", "link", "meta", "param", "source",
					"track", "wbr"));

	/**
	 * Serializes the given {@link Document} to the given {@link Writer}.
	 * 
	 * @param out
	 *        The {@link Writer} to write the XML contents to.
	 * @param document
	 *        The {@link Document} to serialize.
	 */
	public static void serialize(Writer out, Document document)
			throws XMLStreamException, FactoryConfigurationError {
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
		XMLStreamWriter xml = factory.createXMLStreamWriter(out);
		try {
			serialize(xml, document);
		} finally {
			xml.close();
		}
	}

	/**
	 * Serializes the given {@link Document} to the given {@link XMLStreamWriter}.
	 * 
	 * @param writer
	 *        The {@link XMLStreamWriter} to write the XML contents to.
	 * @param document
	 *        The {@link Document} to serialize.
	 */
	public static void serialize(XMLStreamWriter writer, Document document)
			throws XMLStreamException {
		writer.writeStartDocument("utf-8", "1.0");
		serialize(writer, document.getDocumentElement());
		writer.writeEndDocument();
	}

	private static void serialize(XMLStreamWriter xml, Element element)
			throws XMLStreamException {
		if (isVoid(element.getTagName())) {
			xml.writeEmptyElement(element.getTagName());
			serializeAttributes(xml, element);
		} else {
			xml.writeStartElement(element.getTagName());
			serializeAttributes(xml, element);
			for (Node child = element
					.getFirstChild(); child != null; child = child
							.getNextSibling()) {
				serialize(xml, child);
			}
			xml.writeEndElement();
		}
	}

	private static void serialize(XMLStreamWriter xml, Node child)
			throws XMLStreamException {
		switch (child.getNodeType()) {
		case Node.COMMENT_NODE:
			// Remove.
			break;
		case Node.ELEMENT_NODE:
			serialize(xml, (Element) child);
			break;
		case Node.TEXT_NODE:
		case Node.CDATA_SECTION_NODE:
			xml.writeCharacters(child.getTextContent());
			break;
		}
	}

	private static void serializeAttributes(XMLStreamWriter xml,
			Element element) throws DOMException, XMLStreamException {
		NamedNodeMap attributes = element.getAttributes();
		for (int n = 0, cnt = attributes.getLength(); n < cnt; n++) {
			Node attribute = attributes.item(n);
			xml.writeAttribute(attribute.getNodeName(),
					attribute.getNodeValue());
		}
	}

	private static boolean isVoid(String localName) {
		return VOID_ELEMENTS.contains(localName);
	}

}
