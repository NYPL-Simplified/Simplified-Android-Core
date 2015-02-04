package org.nypl.simplified.opds.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;

/**
 * The default implementation of the {@link OPDSFeedParserType}.
 */

public final class OPDSFeedParser implements OPDSFeedParserType
{
  private static final URI ACQUISITION_URI_PREFIX;
  private static final URI ATOM_URI;
  private static final URI FEATURED_URI_PREFIX;

  static {
    ATOM_URI = NullCheck.notNull(URI.create("http://www.w3.org/2005/Atom"));
    ACQUISITION_URI_PREFIX =
      NullCheck.notNull(URI.create("http://opds-spec.org/acquisition"));
    FEATURED_URI_PREFIX =
      NullCheck.notNull(URI.create("http://opds-spec.org/featured"));
  }

  /**
   * Check all links in the given entry element. Return <code>true</code> if a
   * <code>link</code> exists with a <code>rel</code> attribute with a value
   * that begins with {@link #ACQUISITION_URI_PREFIX}.
   */

  private static boolean entryIsFromAcquisitionFeed(
    final Element e)
    throws OPDSFeedParseException
  {
    final List<Element> links =
      OPDSXML.getChildElementsWithNameNonEmpty(
        e,
        OPDSFeedParser.ATOM_URI,
        "link");

    for (final Element link : links) {
      final String rel = link.getAttribute("rel");
      if (rel != null) {
        if (rel.startsWith(OPDSFeedParser.ACQUISITION_URI_PREFIX.toString())) {
          return true;
        }
      }
    }

    return false;
  }

  private static OptionType<URI> findNavigationFeatured(
    final List<Element> e_links)
    throws URISyntaxException,
      OPDSFeedParseException
  {
    for (final Element e : e_links) {
      if (e.hasAttribute("rel")) {
        final String text = e.getAttribute("rel");
        assert text != null;
        if (text.equals(OPDSFeedParser.FEATURED_URI_PREFIX.toString())) {
          if (e.hasAttribute("href") == false) {
            final StringBuilder m = new StringBuilder();
            m.append("A link was given with a rel attribute of '");
            m.append(OPDSFeedParser.FEATURED_URI_PREFIX);
            m.append("' but without a href attribute");
            throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
          }

          final String href = e.getAttribute("href");
          final URI u = new URI(href);
          return Option.some(u);
        }
      }
    }

    return Option.none();
  }

  private static URI findNavigationTarget(
    final String id,
    final List<Element> e_links)
    throws OPDSFeedParseException,
      URISyntaxException
  {
    /**
     * First, look for a link that has a rel attribute with a value equal to
     * "subsection". Use the value of the href attribute if one exists.
     */

    for (final Element e : e_links) {
      if (e.hasAttribute("rel")) {
        final String text = e.getAttribute("rel");
        assert text != null;
        if (text.equals("subsection")) {
          if (e.hasAttribute("href") == false) {
            final StringBuilder m = new StringBuilder();
            m.append("A link was given with a rel attribute of '");
            m.append("subsection");
            m.append("' but without a href attribute");
            throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
          }

          final String href = e.getAttribute("href");
          return new URI(href);
        }
      }
    }

    /**
     * Otherwise, look for a link that contains a single href attribute.
     */

    for (final Element e : e_links) {
      if (e.getAttributes().getLength() == 1) {
        if (e.hasAttribute("href")) {
          final String href = e.getAttribute("href");
          return new URI(href);
        }
      }
    }

    /**
     * The feed is invalid; no usable links were found.
     */

    final StringBuilder m = new StringBuilder();
    m
      .append("No usable target links were found in the navigation feed entry with id '");
    m.append(id);
    m.append("'");
    throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
  }

  private static Calendar getUpdated(
    final Element ee)
    throws OPDSFeedParseException,
      ParseException
  {
    final Element e_updated =
      OPDSXML.getFirstChildElementWithName(
        ee,
        OPDSFeedParser.ATOM_URI,
        "updated");
    final String e_updated_raw =
      NullCheck.notNull(e_updated.getTextContent().trim());
    final Calendar updated =
      OPDSRFC3339Formatter.parseRFC3339Date(e_updated_raw);
    return updated;
  }

  public static OPDSFeedParserType newParser()
  {
    return new OPDSFeedParser();
  }

  private static OPDSFeedType parseAcquisition(
    final Element e_feed,
    final Element e_id,
    final Element e_title,
    final Calendar updated,
    final List<Element> links,
    final List<Element> entries)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  private static OPDSFeedType parseNavigation(
    final Element e_feed,
    final Element e_id,
    final Element e_title,
    final Calendar updated,
    final List<Element> links,
    final List<Element> entries)
    throws OPDSFeedParseException,
      ParseException,
      URISyntaxException
  {
    final OPDSNavigationFeedBuilderType b =
      OPDSNavigationFeed.newBuilder(
        NullCheck.notNull(e_id.getTextContent()),
        updated,
        NullCheck.notNull(e_title.getTextContent()));

    for (final Element ee : entries) {
      final OPDSNavigationFeedEntry e =
        OPDSFeedParser.parseNavigationEntry(ee);
      b.addEntry(e);
    }

    return b.build();
  }

  private static OPDSNavigationFeedEntry parseNavigationEntry(
    final Element ee)
    throws OPDSFeedParseException,
      ParseException,
      URISyntaxException
  {
    final Element e_id =
      OPDSXML.getFirstChildElementWithName(ee, OPDSFeedParser.ATOM_URI, "id");
    final String id = NullCheck.notNull(e_id.getTextContent().trim());

    final Element e_title =
      OPDSXML.getFirstChildElementWithName(
        ee,
        OPDSFeedParser.ATOM_URI,
        "title");

    final Calendar updated = OPDSFeedParser.getUpdated(ee);

    final List<Element> e_links =
      OPDSXML.getChildElementsWithNameNonEmpty(
        ee,
        OPDSFeedParser.ATOM_URI,
        "link");

    final OptionType<URI> featured =
      OPDSFeedParser.findNavigationFeatured(e_links);

    final URI target = OPDSFeedParser.findNavigationTarget(id, e_links);

    return OPDSNavigationFeedEntry.newEntry(
      NullCheck.notNull(e_id.getTextContent()),
      NullCheck.notNull(e_title.getTextContent()),
      updated,
      featured,
      target);
  }

  private static Document parseStream(
    final InputStream s)
    throws ParserConfigurationException,
      SAXException,
      IOException
  {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db = dbf.newDocumentBuilder();
    final Document d = NullCheck.notNull(db.parse(s));
    return d;
  }

  private OPDSFeedParser()
  {

  }

  @Override public OPDSFeedType parse(
    final InputStream s)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(s);

    try {
      final Document d = OPDSFeedParser.parseStream(s);

      final Element e_feed =
        OPDSXML.nodeAsElementWithName(
          d.getFirstChild(),
          OPDSFeedParser.ATOM_URI,
          "feed");
      final Element e_id =
        OPDSXML.getFirstChildElementWithName(
          e_feed,
          OPDSFeedParser.ATOM_URI,
          "id");
      final Element e_title =
        OPDSXML.getFirstChildElementWithName(
          e_feed,
          OPDSFeedParser.ATOM_URI,
          "title");
      final Calendar updated = OPDSFeedParser.getUpdated(e_feed);

      final List<Element> links =
        OPDSXML.getChildElementsWithNameNonEmpty(
          e_feed,
          OPDSFeedParser.ATOM_URI,
          "link");
      final List<Element> entries =
        OPDSXML.getChildElementsWithNameNonEmpty(
          e_feed,
          OPDSFeedParser.ATOM_URI,
          "entry");

      /**
       * Because the OPDS specification makes the "type" attribute optional,
       * it's necessary to examine the entries in the feed to determine if the
       * feed is a Navigation or Acquisition feed. The first entry in the list
       * decides what type the feed is.
       */

      final Element e0 = NullCheck.notNull(entries.get(0));
      if (OPDSFeedParser.entryIsFromAcquisitionFeed(e0)) {
        return OPDSFeedParser.parseAcquisition(
          e_feed,
          e_id,
          e_title,
          updated,
          links,
          entries);
      }

      return OPDSFeedParser.parseNavigation(
        e_feed,
        e_id,
        e_title,
        updated,
        links,
        entries);

    } catch (final ParserConfigurationException e) {
      throw new OPDSFeedParseException(e);
    } catch (final SAXException e) {
      throw new OPDSFeedParseException(e);
    } catch (final OPDSFeedParseException e) {
      throw e;
    } catch (final IOException e) {
      throw new OPDSFeedParseException(e);
    } catch (final DOMException e) {
      throw new OPDSFeedParseException(e);
    } catch (final ParseException e) {
      throw new OPDSFeedParseException(e);
    } catch (final URISyntaxException e) {
      throw new OPDSFeedParseException(e);
    }
  }
}
