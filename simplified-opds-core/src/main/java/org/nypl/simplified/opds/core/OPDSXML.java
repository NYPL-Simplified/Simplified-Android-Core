package org.nypl.simplified.opds.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

final class OPDSXML
{
  static List<Element> getChildElementsWithName(
    final Node node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final List<Element> xs = new ArrayList<Element>();
    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = children.item(index);
      assert child != null;
      if (OPDSXML.nodeHasName(child, namespace, name)) {
        final Element e = OPDSXML.nodeAsElement(child);
        xs.add(e);
      }
    }

    return xs;
  }

  static List<Element> getChildElementsWithNameNonEmpty(
    final Node node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final List<Element> xs = new ArrayList<Element>();
    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = children.item(index);
      assert child != null;
      if (OPDSXML.nodeHasName(child, namespace, name)) {
        final Element e = OPDSXML.nodeAsElement(child);
        xs.add(e);
      }
    }

    if (xs.size() < 1) {
      final StringBuilder m = new StringBuilder();
      m.append("Expected at least one required element:\n");
      m.append("  Namespace: ");
      m.append(namespace);
      m.append("\n");
      m.append("  Local:     ");
      m.append(name);
      m.append("\n");
      throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
    }
    return xs;
  }

  static String getFirstChildElementTextWithName(
    final Node node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
  {
    return NullCheck.notNull(OPDSXML
      .getFirstChildElementWithName(node, namespace, name)
      .getTextContent()
      .trim());
  }

  static OptionType<String> getFirstChildElementTextWithNameOptional(
    final Node node,
    final URI namespace,
    final String name)
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = children.item(index);
      assert child != null;
      if (OPDSXML.nodeHasName(child, namespace, name)) {
        return Option.some(child.getTextContent().trim());
      }
    }

    return Option.none();
  }

  static Element getFirstChildElementWithName(
    final Node node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(node);
    NullCheck.notNull(namespace);
    NullCheck.notNull(name);

    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = children.item(index);
      assert child != null;
      if (OPDSXML.nodeHasName(child, namespace, name)) {
        return OPDSXML.nodeAsElement(child);
      }
    }

    final StringBuilder m = new StringBuilder();
    m.append("Expected required element:\n");
    m.append("  Namespace: ");
    m.append(namespace);
    m.append("\n");
    m.append("  Local:     ");
    m.append(name);
    m.append("\n");
    throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
  }

  static Element nodeAsElement(
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

  static Element nodeAsElementWithName(
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
    m.append("Expected element:\n");
    m.append("  Namespace: ");
    m.append(namespace);
    m.append("\n");
    m.append("  Local:     ");
    m.append(name);
    m.append("Got: ");
    m.append("  Namespace: ");
    m.append(e.getNamespaceURI());
    m.append("\n");
    m.append("  Local:     ");
    m.append(e.getLocalName());
    m.append("\n");
    throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
  }

  /**
   * <p>
   * Unfortunately, {@link Node#lookupNamespaceURI(String)} is broken on at
   * least Oracle and OpenJDK - specifically
   * {@link com.sun.org.apache.xerces.internal.dom.NodeImpl#lookupNamespaceURI(String)}
   * . It will always return <code>null</code> when passed <code>null</code>.
   * </p>
   * <p>
   * This function will simply return the value of the <code>xmlns</code>
   * attribute on the given node or its closest ancestor.
   * </p>
   *
   * @param node
   *          The node to check
   * @return The default namespace URI, if any
   */

  static OptionType<String> nodeGetDefaultNamespace(
    final Node node)
  {
    final NamedNodeMap attrs = node.getAttributes();
    if (attrs != null) {
      for (int index = 0; index < attrs.getLength(); ++index) {
        final Attr attr = (Attr) attrs.getNamedItemNS(null, "xmlns");
        if (attr != null) {
          return Option.some(attr.getNodeValue());
        }
      }
    }

    final Node parent = node.getParentNode();
    if (parent != null) {
      return OPDSXML.nodeGetDefaultNamespace(parent);
    }

    return Option.none();
  }

  static boolean nodeHasName(
    final Node node,
    final URI namespace,
    final String name)
  {
    final String node_namespace;
    final String node_prefix = node.getPrefix();
    if (node_prefix == null) {
      final OptionType<String> r = OPDSXML.nodeGetDefaultNamespace(node);
      if (r.isNone()) {
        return false;
      }
      final Some<String> s = (Some<String>) r;
      node_namespace = s.get();
    } else {
      node_namespace = node.lookupNamespaceURI(node_prefix);
    }

    final boolean ns_ok = node_namespace.equals(namespace.toString());
    final String got_local = node.getNodeName();
    final boolean lo_ok = got_local.equals(name);
    return ns_ok && lo_ok;
  }
}
