package org.nypl.simplified.opds.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

public final class OPDSXML
{
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

    final List<Element> xs = new ArrayList<Element>();
    for (int index = 0; index < children.getLength(); ++index) {
      xs.add((Element) children.item(index));
    }

    return xs;
  }

  public static List<Element> getChildElementsWithNameNonEmpty(
    final Element node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children =
      node.getElementsByTagNameNS(namespace.toString(), name);
    if (children.getLength() >= 1) {
      final List<Element> xs = new ArrayList<Element>();
      for (int index = 0; index < children.getLength(); ++index) {
        xs.add((Element) children.item(index));
      }
      return xs;
    }

    final StringBuilder m = new StringBuilder();
    m.append("Missing at least one required element.\n");
    m.append("Expected namespace: ");
    m.append(namespace);
    m.append("\n");
    m.append("Expected name:      ");
    m.append(name);
    m.append("\n");
    throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
  }

  public static String getFirstChildElementTextWithName(
    final Element node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
  {
    final Element e =
      OPDSXML.getFirstChildElementWithName(node, namespace, name);
    return NullCheck.notNull(e.getTextContent().trim());
  }

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

  public static Element getFirstChildElementWithName(
    final Element node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
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

    final StringBuilder m = new StringBuilder();
    m.append("Expected required element.\n");
    m.append("Expected namespace: ");
    m.append(namespace);
    m.append("\n");
    m.append("Expected name:      ");
    m.append(name);
    m.append("\n");
    throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
  }

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

  public static Element nodeAsElement(
    final Node node)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(node);

    if ((node instanceof Element) == false) {
      final StringBuilder m = new StringBuilder();
      m.append("Expected element but got node of type ");
      m.append(node.getNodeName());
      throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
    }
    return (Element) node;
  }

  public static Element nodeAsElementWithName(
    final Node node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final Element e = OPDSXML.nodeAsElement(node);
    if (OPDSXML.nodeHasName(e, namespace, name)) {
      return e;
    }

    final StringBuilder m = new StringBuilder();
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
    throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
  }

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

  private OPDSXML()
  {
    throw new UnreachableCodeException();
  }
}
