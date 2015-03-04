package org.nypl.simplified.opds.tests.contracts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.nypl.simplified.opds.core.OPDSXML;
import org.nypl.simplified.opds.tests.utilities.TestUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

public final class OPDSXMLContract implements OPDSXMLContractType
{
  public OPDSXMLContract()
  {
    // Nothing
  }

  private static Document parseStream(
    final InputStream s)
    throws ParserConfigurationException,
      SAXException,
      IOException
  {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();
    final Document d = NullCheck.notNull(db.parse(s));
    return d;
  }

  public static InputStream getResource(
    final String name)
    throws Exception
  {
    return NullCheck.notNull(OPDSXMLContract.class.getResourceAsStream(name));
  }

  @Override public void testNamespaces_0()
    throws Exception
  {
    final Document d =
      OPDSXMLContract.parseStream(OPDSXMLContract
        .getResource("namespaces-0.xml"));
    final Element r = d.getDocumentElement();
    TestUtilities.assertEquals("feed", r.getNodeName());

    final NodeList p = r.getElementsByTagName("dcterms:publisher");
    TestUtilities.assertEquals(1, p.getLength());
    final Element pub_actual = (Element) p.item(0);

    final Some<String> pub_prefix =
      (Some<String>) OPDSXML.getElementPrefix(pub_actual);
    TestUtilities.assertEquals(pub_prefix.get(), "dcterms");

    final Some<String> pub_ns =
      (Some<String>) OPDSXML.getNodeNamespace(pub_actual);
    TestUtilities.assertEquals(pub_ns.get(), "http://purl.org/dc/terms/");

    TestUtilities.assertTrue(OPDSXML.nodeHasName(
      pub_actual,
      URI.create("http://purl.org/dc/terms/"),
      "publisher"));

    final Element pub =
      OPDSXML.getFirstChildElementWithName(
        r,
        URI.create("http://purl.org/dc/terms/"),
        "publisher");
  }
}
