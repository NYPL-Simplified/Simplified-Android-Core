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
import com.io7m.junreachable.UnreachableCodeException;

public final class OPDSXML
{
  public static List<Element> getChildElementsWithName(
    final Element node,
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
      final Node child = NullCheck.notNull(children.item(index));
      if (child instanceof Element) {
        if (OPDSXML.nodeHasName((Element) child, namespace, name)) {
          final Element e = OPDSXML.nodeAsElement(child);
          xs.add(e);
        }
      }
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

    final List<Element> xs = new ArrayList<Element>();
    final NodeList children = node.getChildNodes();
    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = NullCheck.notNull(children.item(index));
      if (child instanceof Element) {
        if (OPDSXML.nodeHasName((Element) child, namespace, name)) {
          final Element e = OPDSXML.nodeAsElement(child);
          xs.add(e);
        }
      }
    }

    if (xs.size() < 1) {
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
    return xs;
  }

  public static OptionType<String> getElementPrefix(
    final Element e)
  {
    NullCheck.notNull(e);
    final String name = NullCheck.notNull(e.getNodeName());
    final String[] segments = name.split(":");
    if (segments.length > 1) {
      return Option.some(NullCheck.notNull(segments[0]));
    }
    return Option.none();
  }

  public static String getFirstChildElementTextWithName(
    final Element node,
    final URI namespace,
    final String name)
    throws OPDSFeedParseException
  {
    return NullCheck.notNull(OPDSXML
      .getFirstChildElementWithName(node, namespace, name)
      .getTextContent()
      .trim());
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
          return Option.some(child.getTextContent().trim());
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
          return OPDSXML.nodeAsElement(child);
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

  public static OptionType<String> getNodeNamespace(
    final Element e)
  {
    NullCheck.notNull(e);

    final String node_namespace;
    final OptionType<String> node_prefix_opt = OPDSXML.getElementPrefix(e);

    if (node_prefix_opt.isSome()) {
      final Some<String> some = (Some<String>) node_prefix_opt;
      final String node_prefix = some.get();
      node_namespace = e.lookupNamespaceURI(node_prefix);
    } else {
      final OptionType<String> r = OPDSXML.nodeGetDefaultNamespace(e);
      if (r.isNone()) {
        return r;
      }
      final Some<String> s = (Some<String>) r;
      node_namespace = s.get();
    }

    if (node_namespace == null) {
      return Option.none();
    }

    return Option.some(node_namespace);
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

  public static OptionType<String> nodeGetDefaultNamespace(
    final Node node)
  {
    final NamedNodeMap attrs = node.getAttributes();
    if (attrs != null) {
      final Attr attr = (Attr) attrs.getNamedItem("xmlns");
      if (attr != null) {
        return Option.some(attr.getNodeValue());
      }
    }

    final Node parent = node.getParentNode();
    if (parent != null) {
      return OPDSXML.nodeGetDefaultNamespace(parent);
    }

    return Option.none();
  }

  public static boolean nodeHasName(
    final Element node,
    final URI namespace,
    final String name)
  {
    final OptionType<String> node_namespace = OPDSXML.getNodeNamespace(node);
    if (node_namespace.isNone()) {
      return false;
    }

    final Some<String> some = (Some<String>) node_namespace;
    final boolean ns_ok = some.get().equals(namespace.toString());
    final String got_local = node.getLocalName();
    final boolean lo_ok = got_local.equals(name);
    return ns_ok && lo_ok;
  }

  private OPDSXML()
  {
    throw new UnreachableCodeException();
  }
}
