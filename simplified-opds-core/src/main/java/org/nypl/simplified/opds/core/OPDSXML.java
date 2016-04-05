package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.rfc3339.core.RFC3339Formatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Convenient XML handling functions.
 */

public final class OPDSXML
{

  private OPDSXML()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Return all child elements of {@code node} that have name {@code name} in
   * namespace {@code namespace}.
   *
   * @param node      The parent node
   * @param namespace The namespace
   * @param name      The element name
   *
   * @return A list of elements
   */

  public static List<Element> getChildElementsWithName(
    final Element node,
    final URI namespace,
    final String name)
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children =
      node.getElementsByTagNameNS(namespace.toString(), name);

    final List<Element> xs = new ArrayList<Element>(children.getLength());
    for (int index = 0; index < children.getLength(); ++index) {
      xs.add((Element) children.item(index));
    }

    return xs;
  }

  /**
   * Return all child elements of {@code node} that have name {@code name} in
   * namespace {@code namespace}.
   *
   * @param node      The parent node
   * @param namespace The namespace
   * @param name      The element name
   *
   * @return A list of elements
   *
   * @throws OPDSParseException If there are no matching elements
   */

  public static List<Element> getChildElementsWithNameNonEmpty(
    final Element node,
    final URI namespace,
    final String name)
    throws OPDSParseException
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children =
      node.getElementsByTagNameNS(namespace.toString(), name);
    if (children.getLength() >= 1) {
      final List<Element> xs = new ArrayList<Element>(children.getLength());
      for (int index = 0; index < children.getLength(); ++index) {
        xs.add((Element) children.item(index));
      }
      return xs;
    }

    final StringBuilder m = new StringBuilder(128);
    m.append("Missing at least one required element.\n");
    m.append("Expected namespace: ");
    m.append(namespace);
    m.append("\n");
    m.append("Expected name:      ");
    m.append(name);
    m.append("\n");
    throw new OPDSParseException(NullCheck.notNull(m.toString()));
  }

  /**
   * Return the text of the first child element of {@code node} that has name
   * {@code name} in namespace {@code namespace}.
   *
   * @param node      The node
   * @param namespace The child namespace
   * @param name      The child name
   *
   * @return The text of the child element
   *
   * @throws OPDSParseException If there are no matching child elements
   */

  public static String getFirstChildElementTextWithName(
    final Element node,
    final URI namespace,
    final String name)
    throws OPDSParseException
  {
    final Element e =
      OPDSXML.getFirstChildElementWithName(node, namespace, name);
    return NullCheck.notNull(e.getTextContent().trim());
  }

  /**
   * Return the (optional) text of the first child element of {@code node} that
   * has name {@code name} in namespace {@code namespace}.
   *
   * @param node      The node
   * @param namespace The child namespace
   * @param name      The child name
   *
   * @return The text of the child element, if any
   */

  public static OptionType<String> getFirstChildElementTextWithNameOptional(
    final Element node,
    final URI namespace,
    final String name)
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = NullCheck.notNull(children.item(index));
      if (child instanceof Element) {
        if (OPDSXML.nodeHasName((Element) child, namespace, name)) {
          final String text = child.getTextContent();
          return Option.some(NullCheck.notNull(text.trim()));
        }
      }
    }

    return Option.none();
  }

  /**
   * Return the (optional) text of the first child element of {@code node} that
   * has name {@code name} in namespace {@code namespace}.
   *
   * @param node      The node
   * @param namespace The child namespace
   * @param name      The child name
   * @param attribute      The child name
   *
   * @return The text of the child element, if any
   */

  public static String getFirstChildElementTextWithName(
    final Element node,
    final URI namespace,
    final String name,
    final String attribute)
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = NullCheck.notNull(children.item(index));
      if (child instanceof Element) {
        if (OPDSXML.nodeHasName((Element) child, namespace, name)) {

          final String text = ((Element) child).getAttributes().getNamedItemNS(namespace.toString(), attribute).getNodeValue();
          return NullCheck.notNull(text.trim());
        }
      }
    }

    return "";
  }

  /**
   * Return the first child element of {@code node} that has name {@code name}
   * in namespace {@code namespace}.
   *
   * @param node      The node
   * @param namespace The child namespace
   * @param name      The child name
   *
   * @return The child element
   *
   * @throws OPDSParseException If no matching element exists
   */

  public static Element getFirstChildElementWithName(
    final Element node,
    final URI namespace,
    final String name)
    throws OPDSParseException
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = NullCheck.notNull(children.item(index));
      if (child instanceof Element) {
        if (OPDSXML.nodeHasName((Element) child, namespace, name)) {
          return (Element) child;
        }
      }
    }

    final StringBuilder m = new StringBuilder(128);
    m.append("Expected required element.\n");
    m.append("Expected namespace: ");
    m.append(namespace);
    m.append("\n");
    m.append("Expected name:      ");
    m.append(name);
    m.append("\n");
    throw new OPDSParseException(NullCheck.notNull(m.toString()));
  }

  /**
   * Return the first child element of {@code node} that has name {@code name}
   * in namespace {@code namespace}, if any.
   *
   * @param node      The node
   * @param namespace The child namespace
   * @param name      The child name
   *
   * @return The child element, if any
   */

  public static OptionType<Element> getFirstChildElementWithNameOptional(
    final Element node,
    final URI namespace,
    final String name)
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = NullCheck.notNull(children.item(index));
      if (child instanceof Element) {
        if (OPDSXML.nodeHasName((Element) child, namespace, name)) {
          return Option.some((Element) child);
        }
      }
    }

    return Option.none();
  }

  /**
   * @param e The element
   *
   * @return The namespace of the given element, if any
   */

  public static OptionType<String> getNodeNamespace(
    final Element e)
  {
    NullCheck.notNull(e);

    final String ns = e.getNamespaceURI();
    if (ns != null) {
      return Option.some(ns);
    }
    return Option.none();
  }

  /**
   * Cast the given node to an {@link Element}, raising an exception if it is
   * not an element.
   *
   * @param node The node
   *
   * @return The node as an element
   *
   * @throws OPDSParseException If the node is not an {@link Element}
   */

  public static Element nodeAsElement(
    final Node node)
    throws OPDSParseException
  {
    NullCheck.notNull(node);

    if ((node instanceof Element) == false) {
      final StringBuilder m = new StringBuilder(128);
      m.append("Expected element but got node of type ");
      m.append(node.getNodeName());
      throw new OPDSParseException(NullCheck.notNull(m.toString()));
    }
    return (Element) node;
  }

  /**
   * Cast the given node to an {@link Element}, raising an exception if it is
   * not an element and/or does not have the given {@code name} and {@code
   * namespace}.
   *
   * @param node      The node
   * @param name      The expected element name
   * @param namespace The expected element namespace
   *
   * @return The node as an element
   *
   * @throws OPDSParseException If the node is not an {@link Element} or has the
   *                            wrong name
   */

  public static Element nodeAsElementWithName(
    final Node node,
    final URI namespace,
    final String name)
    throws OPDSParseException
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final Element e = OPDSXML.nodeAsElement(node);
    if (OPDSXML.nodeHasName(e, namespace, name)) {
      return e;
    }

    final StringBuilder m = new StringBuilder(128);
    m.append("Missing required element.\n");
    m.append("Expected namespace: ");
    m.append(namespace);
    m.append("\n");
    m.append("Expected name:      ");
    m.append(name);
    m.append("\n");
    m.append("Got namespace:      ");
    m.append(OPDSXML.getNodeNamespace(e));
    m.append("\n");
    m.append("Got name:           ");
    m.append(e.getNodeName());
    m.append("\n");
    throw new OPDSParseException(NullCheck.notNull(m.toString()));
  }

  /**
   * @param node      The element
   * @param namespace The namespace
   * @param name      The name
   *
   * @return {@code true} if the given element has the given name and namespace
   */

  public static boolean nodeHasName(
    final Element node,
    final URI namespace,
    final String name)
  {
    final String node_local = node.getLocalName();
    if (node_local.equals(name)) {
      return namespace.toString().equals(node.getNamespaceURI());
    }
    return false;
  }

  /**
   * Parse the contents of attribute {@code name} of element {@code e} as an
   * RFC3339 date, if the attribute exists.
   *
   * @param e    The element
   * @param name The attribute name
   *
   * @return A date, if any
   *
   * @throws OPDSParseException On parse errors
   */

  public static OptionType<Calendar> getAttributeRFC3339Optional(
    final Element e,
    final String name)
    throws OPDSParseException
  {
    NullCheck.notNull(e);
    NullCheck.notNull(name);

    try {
      final OptionType<Calendar> end_date;
      if (e.hasAttribute(name)) {
        return Option.some(
          RFC3339Formatter.parseRFC3339Date(e.getAttribute(name)));
      }

      return Option.none();
    } catch (final ParseException x) {
      throw new OPDSParseException(x);
    }
  }

  /**
   * Parse the contents of attribute {@code name} of element {@code e} as an
   * RFC3339 date.
   *
   * @param e    The element
   * @param name The attribute name
   *
   * @return A date
   *
   * @throws OPDSParseException On parse errors
   */

  public static Calendar getAttributeRFC3339(
    final Element e,
    final String name)
    throws OPDSParseException
  {
    NullCheck.notNull(e);
    NullCheck.notNull(name);

    final OptionType<Calendar> end_date;
    if (e.hasAttribute(name)) {
      try {
        return RFC3339Formatter.parseRFC3339Date(e.getAttribute(name));
      } catch (final ParseException x) {
        throw new OPDSParseException(x);
      }
    }

    final StringBuilder m = new StringBuilder(128);
    m.append("Expected required attribute.\n");
    m.append("Expected name:      ");
    m.append(name);
    m.append("\n");
    throw new OPDSParseException(NullCheck.notNull(m.toString()));
  }

  /**
   * Convenient function to serialize the given document to the given output
   * stream.
   *
   * @param d The document
   * @param o The output stream
   *
   * @throws OPDSSerializationException If any errors occur on serialization
   */

  public static void serializeDocumentToStream(
    final Document d,
    final OutputStream o)
    throws OPDSSerializationException
  {
    NullCheck.notNull(d);
    NullCheck.notNull(o);

    try {
      final TransformerFactory tf =
        NullCheck.notNull(TransformerFactory.newInstance());

      final Transformer t = NullCheck.notNull(tf.newTransformer());
      t.setOutputProperty(OutputKeys.INDENT, "yes");
      t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      final DOMSource source = new DOMSource(d);
      final StreamResult target = new StreamResult(o);
      t.transform(source, target);
    } catch (final TransformerConfigurationException ex) {
      throw new OPDSSerializationException(ex);
    } catch (final TransformerFactoryConfigurationError ex) {
      throw new OPDSSerializationException(ex);
    } catch (final TransformerException ex) {
      throw new OPDSSerializationException(ex);
    }
  }

  /**
   * Parse the contents of attribute {@code name} of element {@code e} as an
   * integer, if the attribute exists.
   *
   * @param e    The element
   * @param name The attribute name
   *
   * @return An integer, if any
   *
   * @throws OPDSParseException On parse errors
   */

  public static OptionType<Integer> getAttributeIntegerOptional(
    final Element e,
    final String name)
    throws OPDSParseException
  {
    NullCheck.notNull(e);
    NullCheck.notNull(name);

    if (e.hasAttribute(name)) {
      try {
        return Option.some(Integer.valueOf(e.getAttribute(name)));
      } catch (final NumberFormatException x) {
        throw new OPDSParseException(x);
      }
    }

    return Option.none();
  }

  /**
   * Parse the contents of attribute {@code name} of element {@code e} as an
   * integer.
   *
   * @param e    The element
   * @param name The attribute name
   *
   * @return An integer
   *
   * @throws OPDSParseException On parse errors
   */

  public static int getAttributeInteger(
    final Element e,
    final String name)
    throws OPDSParseException
  {
    NullCheck.notNull(e);
    NullCheck.notNull(name);

    if (e.hasAttribute(name)) {
      try {
        return Integer.valueOf(e.getAttribute(name));
      } catch (final NumberFormatException x) {
        throw new OPDSParseException(x);
      }
    }

    final StringBuilder m = new StringBuilder(128);
    m.append("Expected required attribute.\n");
    m.append("Expected name:      ");
    m.append(name);
    m.append("\n");
    throw new OPDSParseException(NullCheck.notNull(m.toString()));
  }
}
