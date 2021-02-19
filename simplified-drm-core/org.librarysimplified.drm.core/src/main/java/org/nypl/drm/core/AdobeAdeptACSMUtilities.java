package org.nypl.drm.core;

import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

//@formatter:off

/**
 * <p>Utility functions for processing ACSM files.</p>
 *
 * <p>ACSM files are almost never exposed to the programmer. This is typically
 * only of any use when performing the Join Accounts workflow.</p>
 *
 * @see AdobeAdeptJoinAccountDispatcherType
 */

//@formatter:on

public final class AdobeAdeptACSMUtilities
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(AdobeAdeptACSMUtilities.class);
  }

  private AdobeAdeptACSMUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Check if the given raw ACSM XML is a success messsage.
   *
   * @param text The ACSM text
   *
   * @return <tt>true</tt> if the ACSM represents a success message
   */

  public static boolean acsmIsSuccessful(final String text)
  {
    try {
      final ByteArrayInputStream ss =
        new ByteArrayInputStream(text.getBytes("UTF-8"));

      final Document doc = AdobeAdeptACSMUtilities.parseStream(ss);
      final Element root = doc.getDocumentElement();
      return "success".equals(root.getLocalName());
    } catch (final ParserConfigurationException e) {
      throw new UnreachableCodeException(e);
    } catch (final SAXException e) {
      AdobeAdeptACSMUtilities.LOG.error("failed to parse xml: ", e);
    } catch (final UnsupportedEncodingException e) {
      throw new UnreachableCodeException(e);
    } catch (final IOException e) {
      AdobeAdeptACSMUtilities.LOG.error("i/o error: ", e);
    }

    return false;
  }

  private static Document parseStream(
    final InputStream s)
    throws ParserConfigurationException, SAXException, IOException
  {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();
    return Objects.requireNonNull(db.parse(s));
  }
}
