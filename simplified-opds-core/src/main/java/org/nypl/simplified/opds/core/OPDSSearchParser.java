package org.nypl.simplified.opds.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.io7m.jnull.NullCheck;

/**
 * <p>
 * The default implementation of the {@link OPDSSearchParserType}.
 * </p>
 */

public final class OPDSSearchParser implements OPDSSearchParserType
{
  private static final Logger LOG;
  private static final URI    OPEN_SEARCH_URI;
  private static final String OPEN_SEARCH_URI_TEXT;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(OPDSFeedParser.class));

    OPEN_SEARCH_URI_TEXT = "http://a9.com/-/spec/opensearch/1.1/";
    OPEN_SEARCH_URI =
      NullCheck.notNull(URI.create(OPDSSearchParser.OPEN_SEARCH_URI_TEXT));
  }

  public static OPDSSearchParserType newParser()
  {
    return new OPDSSearchParser();
  }

  private OPDSSearchParser()
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

  @Override public OPDSOpenSearch1_1 parse(
    final URI uri,
    final InputStream s)
      throws OPDSParseException
  {
    NullCheck.notNull(s);

    final long time_pre_parse = System.nanoTime();
    long time_post_parse = time_pre_parse;

    try {
      OPDSSearchParser.LOG.debug("parsing: {}", uri);

      final Document d = OPDSSearchParser.parseStream(s);
      time_post_parse = System.nanoTime();

      final Node root = NullCheck.notNull(d.getFirstChild());
      final Element e_search =
        OPDSXML.nodeAsElementWithName(
          root,
          OPDSSearchParser.OPEN_SEARCH_URI,
          "OpenSearchDescription");

      final Element e_url =
        OPDSXML.getFirstChildElementWithName(
          e_search,
          OPDSSearchParser.OPEN_SEARCH_URI,
          "Url");

      return new OPDSOpenSearch1_1(NullCheck.notNull(e_url
        .getAttribute("template")));

    } catch (final ParserConfigurationException e) {
      throw new OPDSParseException(e);
    } catch (final SAXException e) {
      throw new OPDSParseException(e);
    } catch (final OPDSParseException e) {
      throw e;
    } catch (final IOException e) {
      throw new OPDSParseException(e);
    } catch (final DOMException e) {
      throw new OPDSParseException(e);
    } finally {
      final long time_now = System.nanoTime();
      final long time_parse = time_post_parse - time_pre_parse;
      final long time_interp = time_now - time_post_parse;
      OPDSSearchParser.LOG.debug(
        "parsing completed ({}ms - parse: {}ms, interp: {}ms): {}",
        TimeUnit.MILLISECONDS.convert(
          time_parse + time_interp,
          TimeUnit.NANOSECONDS),
          TimeUnit.MILLISECONDS.convert(time_parse, TimeUnit.NANOSECONDS),
          TimeUnit.MILLISECONDS.convert(time_interp, TimeUnit.NANOSECONDS),
          uri);
    }
  }
}
