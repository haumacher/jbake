package org.jbake.processors;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Transformation rewriting <code>mailto:</code> links using JavaScript code
 * that makes hard grabbing e-mail addresses from the page source.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class MailtoObfuscator {

	/**
	 * Transforms <code>mailto:</code> links in the given {@link Document}.
	 * 
	 * <p>
	 * The given {@link Document} is internally modified.
	 * </p>
	 */
	public static void processDocument(Document document) {
		processChildren(document);
	}

	private static void processElement(Element element) {
		encodeMailTo(element);
		processChildren(element);
	}

	/**
	 * Descends to the children of the given {@link Node}.
	 * 
	 * @param node
	 *        The {@link Node} whose children should be processed.
	 */
	private static void processChildren(Node node) {
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			processNode(child);
		}
	}

	/**
	 * Processes the given {@link Node}.
	 * 
	 * @param node
	 *        The {@link Node} to process the concrete transformation method is
	 *        called depending on the concrete type of this {@link Node}.
	 * 
	 * @see #processElement(Element)
	 * @see #processText(Text)
	 * @see #processComment(Comment)
	 */
	private static void processNode(Node node) {
		switch (node.getNodeType()) {
		case Node.COMMENT_NODE:
			processComment((Comment) node);
			break;
		case Node.ELEMENT_NODE:
			processElement((Element)node);
			break;
		case Node.TEXT_NODE:
		case Node.CDATA_SECTION_NODE:
			processText((Text)node);
			break;
		}
	}

	/**
	 * Processes the given {@link Text} node.
	 * 
	 * @param text
	 *        The {@link Text} node to process.
	 */
	private static void processText(Text text) {
		text.setTextContent(text.getTextContent().replaceAll("\\s\\s+", " "));
	}

	/**
	 * Processes the given {@link Comment} node.
	 * 
	 * @param comment
	 *        The {@link Comment} node to process.
	 */
	private static void processComment(Comment comment) {
		// No action.
	}

	/**
	 * Transforms the given {@link Element} node, if it is a
	 * <code>mailto:</code> link.
	 */
	private static void encodeMailTo(Element element) {
		if (isMailToLink(element)) {
			Document document = element.getOwnerDocument();
			
			String mailto = element.getAttribute("href");
			
			/*
			 * var addr = 'haui' + String.fromCharCode(64) + 'haumacher' + String.fromCharCode(46) + 'de';
			 * var link = document.createElement('a');
			 * link.setAttribute('href', 'mai' + 'lto' + ':' + addr);
			 * link.appendChild(document.createTextNode(addr));
			 * this.parentNode.replaceChild(link, this);
			 * return false;
			 */
			mailto = mailto.replace("mailto:", "");
			mailto = mailto.replace(".", "' + String.fromCharCode(46) + '");
			mailto = mailto.replace("@", "' + String.fromCharCode(64) + '");
			
			StringBuilder script = new StringBuilder();
			script.append("var addr = '" + mailto + "';");
			script.append("var link = document.createElement('a');");
			script.append("link.setAttribute('href', 'mai' + 'lto' + ':' + addr);");
			script.append("link.appendChild(document.createTextNode(addr));");
			script.append("this.parentNode.replaceChild(link, this);");
			script.append("return false;");
			
			String labelBefore = element.getTextContent();
			int start = labelBefore.indexOf('<');
			int at = labelBefore.indexOf('@');
			int stop = labelBefore.lastIndexOf('>');
			
			String newLabel;
			if (at >= 0) {
				if (start >= 0 && stop >= start) {
					newLabel = labelBefore.substring(start + 1, at);
				} else {
					newLabel = labelBefore.substring(0, at);
				}
			} else {
				newLabel = labelBefore;
			}
			
			element.setAttribute("href", "#");
			Element button = document.createElement("button");
			button.setAttribute("onclick", script.toString());
			Text label = document.createTextNode(newLabel + "@...");
			button.appendChild(label);
			
			Node parent = element.getParentNode();
			if (start >= 0) {
				parent.insertBefore(document.createTextNode(labelBefore.substring(0, start) + " <"), element);
			}
			parent.insertBefore(button, element);
			if (start >= 0) {
				parent.insertBefore(document.createTextNode(">"), element);
			}
			
			parent.removeChild(element);
		}
	}

	/**
	 * Decides whether the given {@link Element} is a <code>mailto:</code> link.
	 */
	private static boolean isMailToLink(Element element) {
		if (!element.getTagName().equals("a")) {
			return false;
		}
		
		String href = element.getAttribute("href");
		if (href == null) {
			return false;
		}
		
		return href.startsWith("mailto:");
	}

}
