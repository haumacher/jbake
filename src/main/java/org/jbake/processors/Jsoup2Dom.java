package org.jbake.processors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Conversion between the {@link Jsoup} {@link org.jsoup.nodes.Document}
 * representation to the standard W3C {@link Document} model.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class Jsoup2Dom {

	/**
	 * Creates a W3C {@link Document} from a {@link Jsoup} document.
	 * 
	 * @param document
	 *        The {@link Jsoup} document to transform.
	 * @return The newly created W3C {@link Document} with approximately the
	 *         same content as the given {@link Jsoup} document.
	 * @throws ParserConfigurationException
	 */
	public static Document dom(org.jsoup.nodes.Document document)
			throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		Document result = factory.newDocumentBuilder().newDocument();
		domDocument(result, document);
		return result;
	}

	private static void domDocument(Document result, org.jsoup.nodes.Document document) {
		domNode(result, document);
	}

	private static void domElement(org.w3c.dom.Element result, org.jsoup.nodes.Element element) {
		domNode(result, element);
		for (Attribute attribute : element.attributes()) {
			result.setAttribute(attribute.getKey(), attribute.getValue());
		}
	}

	private static void domNode(Node result, org.jsoup.nodes.Node node) {
		for (org.jsoup.nodes.Node child : node.childNodes()) {
			appendChild(result, createNode(result, child));
		}
	}

	private static void appendChild(Node result, Node newChild) {
		if (newChild != null) {
			result.appendChild(newChild);
		}
	}

	private static Node createNode(Node parent, org.jsoup.nodes.Node child) {
		if (child instanceof Element) {
			org.w3c.dom.Element newElement = ownerDocument(parent).createElement(child.nodeName());
			domElement(newElement, (Element) child);
			return newElement;
		}
		if (child instanceof TextNode) {
			return ownerDocument(parent).createTextNode(((TextNode) child).text());
		}
		if (child instanceof Comment) {
			return ownerDocument(parent).createComment(((Comment) child).getData());
		}
		if (child instanceof DataNode) {
			return ownerDocument(parent).createCDATASection(((DataNode) child).getWholeData());
		}
		return null;
	}

	private static Document ownerDocument(Node parent) {
		if (parent instanceof Document) {
			return (Document) parent;
		} else {
			return parent.getOwnerDocument();
		}
	}

}
