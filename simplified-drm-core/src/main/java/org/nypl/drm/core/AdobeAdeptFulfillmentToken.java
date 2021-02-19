package org.nypl.drm.core;

import java.util.Objects;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A parsed fulfillment token.
 */

public final class AdobeAdeptFulfillmentToken
{
  private static final URI ADOBE_ADEPT_NS;
  private static final URI DUBLIN_CORE_NS;

  static {
    ADOBE_ADEPT_NS = URI.create("http://ns.adobe.com/adept");
    DUBLIN_CORE_NS = URI.create("http://purl.org/dc/elements/1.1/");
  }

  private final Document document;
  private final String   format;

  private AdobeAdeptFulfillmentToken(
    final Document in_document,
    final String in_format_text)
  {
    this.document = Objects.requireNonNull(in_document);
    this.format = Objects.requireNonNull(in_format_text);
  }

  /**
   * Parse the given bytes as a fulfillment token.
   *
   * @param data The data
   *
   * @return A parsed fulfillment token
   *
   * @throws AdobeAdeptACSMException On errors
   */

  public static AdobeAdeptFulfillmentToken parseFromBytes(final byte[] data)
    throws AdobeAdeptACSMException
  {
    Objects.requireNonNull(data);
    return AdobeAdeptFulfillmentToken.parseFromStream(
      new ByteArrayInputStream(data));
  }

  /**
   * Parse the given stream as a fulfillment token.
   *
   * @param s The stream
   *
   * @return A parsed fulfillment token
   *
   * @throws AdobeAdeptACSMException On errors
   */

  public static AdobeAdeptFulfillmentToken parseFromStream(final InputStream s)
    throws AdobeAdeptACSMException
  {
    Objects.requireNonNull(s);

    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document document = Objects.requireNonNull(db.parse(s));
      return AdobeAdeptFulfillmentToken.parseFromDocument(document);
    } catch (final ParserConfigurationException e) {
      throw new AdobeAdeptACSMException(e);
    } catch (final SAXException e) {
      throw new AdobeAdeptACSMException(e);
    } catch (final IOException e) {
      throw new AdobeAdeptACSMException(e);
    }
  }

  /**
   * Parse the given bytes as a fulfillment token.
   *
   * @param document The document
   *
   * @return A parsed fulfillment token
   *
   * @throws AdobeAdeptACSMException On errors
   */

  public static AdobeAdeptFulfillmentToken parseFromDocument(
    final Document document)
    throws AdobeAdeptACSMException
  {
    Objects.requireNonNull(document);

    final Element root = document.getDocumentElement();
    AdobeAdeptFulfillmentToken.checkName(
      root, AdobeAdeptFulfillmentToken.ADOBE_ADEPT_NS, "fulfillmentToken");
    final Element res = AdobeAdeptFulfillmentToken.getChildElement(
      root, AdobeAdeptFulfillmentToken.ADOBE_ADEPT_NS, "resourceItemInfo");
    final Element meta = AdobeAdeptFulfillmentToken.getChildElement(
      root, AdobeAdeptFulfillmentToken.ADOBE_ADEPT_NS, "metadata");
    final Element format = AdobeAdeptFulfillmentToken.getChildElement(
      root, AdobeAdeptFulfillmentToken.DUBLIN_CORE_NS, "format");

    final String format_text = format.getTextContent();
    return new AdobeAdeptFulfillmentToken(document, format_text);
  }

  private static Element getChildElement(
    final Element root,
    final URI uri,
    final String local)

    throws AdobeAdeptACSMException
  {
    final NodeList es = root.getElementsByTagNameNS(uri.toString(), local);
    if (es.getLength() == 0) {
      final StringBuilder sb = new StringBuilder(256);
      sb.append("Cannot parse document as fulfillment token.\n");
      sb.append("  Expected at least one node: ");
      sb.append(local);
      sb.append(" (namespace ");
      sb.append(uri);
      sb.append(")");
      sb.append("\n");
      final String text = sb.toString();
      throw new AdobeAdeptACSMException(text);
    }

    final Node item = es.item(0);
    if (item instanceof Element) {
      return Objects.requireNonNull((Element) item);
    }

    final StringBuilder sb = new StringBuilder(256);
    sb.append("Cannot parse document as fulfillment token.\n");
    sb.append("  Expected an element: ");
    sb.append(local);
    sb.append(" (namespace ");
    sb.append(uri);
    sb.append(")");
    sb.append("\n");
    sb.append("  Got a node: ");
    sb.append(item);
    sb.append("\n");
    final String text = sb.toString();
    throw new AdobeAdeptACSMException(text);
  }

  private static void checkName(
    final Element e,
    final URI uri,
    final String name)
    throws AdobeAdeptACSMException
  {
    final String got_name = Objects.requireNonNull(e.getLocalName());
    final String got_uri = Objects.requireNonNull(e.getNamespaceURI());
    final boolean name_ok = name.equals(got_name);
    final boolean uri_ok = uri.toString().equals(got_uri);
    if ((name_ok && uri_ok) == false) {
      final StringBuilder sb = new StringBuilder(256);
      sb.append("Cannot parse document as fulfillment token.\n");
      sb.append("  Expected node: ");
      sb.append(name);
      sb.append("  (namespace ");
      sb.append(uri);
      sb.append(")\n");
      sb.append("  Got node:      ");
      sb.append(got_name);
      sb.append("  (namespace ");
      sb.append(got_uri);
      sb.append(")\n");
      final String text = sb.toString();
      throw new AdobeAdeptACSMException(text);
    }
  }

  /**
   * @return The format of the target book
   */

  public String getFormat()
  {
    return this.format;
  }
}
