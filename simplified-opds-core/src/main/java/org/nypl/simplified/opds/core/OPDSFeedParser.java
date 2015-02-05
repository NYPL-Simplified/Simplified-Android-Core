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

import org.nypl.simplified.opds.core.OPDSAcquisition.Type;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

/**
 * <p>
 * The default implementation of the {@link OPDSFeedParserType}.
 * </p>
 * <p>
 * The implementation generally assumes that all sections of the OPDS
 * specification that are denoted as "SHOULD" will in practice mean
 * "WILL NOT".
 * </p>
 */

public final class OPDSFeedParser implements OPDSFeedParserType
{
  private static final URI ACQUISITION_URI_PREFIX;
  private static final URI ATOM_URI;
  private static final URI FEATURED_URI_PREFIX;
  private static final URI IMAGE_URI;
  private static final URI THUMBNAIL_URI;

  static {
    ATOM_URI = NullCheck.notNull(URI.create("http://www.w3.org/2005/Atom"));
    ACQUISITION_URI_PREFIX =
      NullCheck.notNull(URI.create("http://opds-spec.org/acquisition"));
    FEATURED_URI_PREFIX =
      NullCheck.notNull(URI.create("http://opds-spec.org/featured"));
    THUMBNAIL_URI =
      NullCheck.notNull(URI.create("http://opds-spec.org/image/thumbnail"));
    IMAGE_URI = NullCheck.notNull(URI.create("http://opds-spec.org/image"));
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

  private static void findAcquisitionAuthors(
    final Element e,
    final OPDSAcquisitionFeedEntryBuilderType eb)
    throws OPDSFeedParseException
  {
    final List<Element> e_authors =
      OPDSXML.getChildElementsWithName(e, OPDSFeedParser.ATOM_URI, "author");
    for (final Element ea : e_authors) {
      assert ea != null;
      final String name =
        OPDSXML.getFirstChildElementTextWithName(
          ea,
          OPDSFeedParser.ATOM_URI,
          "name");
      eb.addAuthor(name);
    }
  }

  private static OptionType<URI> findAcquisitionCover(
    final List<Element> e_links)
    throws URISyntaxException,
      OPDSFeedParseException
  {
    return OPDSFeedParser.findLinkWithURIRel(
      e_links,
      OPDSFeedParser.IMAGE_URI);
  }

  private static void findAcquisitionRelations(
    final List<Element> e_links,
    final OPDSAcquisitionFeedEntryBuilderType eb)
    throws OPDSFeedParseException,
      URISyntaxException
  {
    for (final Element e : e_links) {
      if (e.hasAttribute("rel")) {
        final String text = e.getAttribute("rel");
        assert text != null;

        for (final Type v : OPDSAcquisition.Type.values()) {
          final String uri_text = NullCheck.notNull(v.getURI().toString());
          if (text.equals(uri_text)) {
            final URI href = OPDSFeedParser.getLinkHref(e, uri_text);
            eb.addAcquisition(new OPDSAcquisition(v, href));
          }
        }
      }
    }
  }

  private static OptionType<URI> findAcquisitionThumbnail(
    final List<Element> e_links)
    throws URISyntaxException,
      OPDSFeedParseException
  {
    return OPDSFeedParser.findLinkWithURIRel(
      e_links,
      OPDSFeedParser.THUMBNAIL_URI);
  }

  private static String findID(
    final Element ee)
    throws OPDSFeedParseException
  {
    return OPDSXML.getFirstChildElementTextWithName(
      ee,
      OPDSFeedParser.ATOM_URI,
      "id");
  }

  private static OptionType<URI> findLinkWithURIRel(
    final List<Element> e_links,
    final URI uri)
    throws OPDSFeedParseException,
      URISyntaxException
  {
    for (final Element e : e_links) {
      if (e.hasAttribute("rel")) {
        final String rel = e.getAttribute("rel");
        final String uri_text = NullCheck.notNull(uri.toString());
        if (rel.equals(uri_text)) {
          return Option.some(OPDSFeedParser.getLinkHref(e, uri_text));
        }
      }
    }

    return Option.none();
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
        final String uri_text =
          NullCheck.notNull(OPDSFeedParser.FEATURED_URI_PREFIX.toString());
        if (text.equals(uri_text)) {
          return Option.some(OPDSFeedParser.getLinkHref(e, uri_text));
        }
      }
    }

    return Option.none();
  }

  /**
   * Try to find any usable link as the target of a navigation field entry.
   */

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
          return OPDSFeedParser.getLinkHref(e, "subsection");
        }
      }
    }

    /**
     * Otherwise, look for a link that contains a single href attribute.
     */

    for (final Element e : e_links) {
      if (e.getAttributes().getLength() == 1) {
        if (e.hasAttribute("href")) {
          return new URI(e.getAttribute("href"));
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

  private static OptionType<String> findSubtitle(
    final Element e)
  {
    return OPDSXML.getFirstChildElementTextWithNameOptional(
      e,
      OPDSFeedParser.ATOM_URI,
      "alternativeHeadline");
  }

  private static String findTitle(
    final Element e)
    throws OPDSFeedParseException
  {
    return OPDSXML.getFirstChildElementTextWithName(
      e,
      OPDSFeedParser.ATOM_URI,
      "title");
  }

  private static Calendar findUpdated(
    final Element e)
    throws OPDSFeedParseException,
      ParseException
  {
    final String e_updated_raw =
      OPDSXML.getFirstChildElementTextWithName(
        e,
        OPDSFeedParser.ATOM_URI,
        "updated");
    return OPDSRFC3339Formatter.parseRFC3339Date(e_updated_raw);
  }

  /**
   * Fetch the contents of the <code>href</code> attribute from the given
   * element, assuming that the <code>rel</code> attribute contained
   * <code>rel</code>.
   */

  private static URI getLinkHref(
    final Element e,
    final String rel)
    throws OPDSFeedParseException,
      URISyntaxException
  {
    if (e.hasAttribute("href") == false) {
      final StringBuilder m = new StringBuilder();
      m.append("A link was given with a rel attribute of '");
      m.append(rel);
      m.append("' but without a href attribute");
      throw new OPDSFeedParseException(NullCheck.notNull(m.toString()));
    }

    return new URI(e.getAttribute("href"));
  }

  public static OPDSFeedParserType newParser()
  {
    return new OPDSFeedParser();
  }

  private static OPDSFeedType parseAcquisition(
    final Element e_feed,
    final String id,
    final String title,
    final Calendar updated,
    final List<Element> links,
    final List<Element> entries)
    throws OPDSFeedParseException,
      ParseException,
      URISyntaxException
  {
    final OPDSAcquisitionFeedBuilderType b =
      OPDSAcquisitionFeed.newBuilder(id, updated, title);

    for (final Element ee : entries) {
      b.addEntry(OPDSFeedParser.parseAcquisitionEntry(ee));
    }

    return b.build();
  }

  private static OPDSAcquisitionFeedEntry parseAcquisitionEntry(
    final Element e)
    throws OPDSFeedParseException,
      ParseException,
      URISyntaxException
  {
    final String id = OPDSFeedParser.findID(e);
    final String title = OPDSFeedParser.findTitle(e);
    final Calendar updated = OPDSFeedParser.findUpdated(e);

    final OPDSAcquisitionFeedEntryBuilderType eb =
      OPDSAcquisitionFeedEntry.newBuilder(id, title, updated);

    final List<Element> e_links =
      OPDSXML.getChildElementsWithNameNonEmpty(
        e,
        OPDSFeedParser.ATOM_URI,
        "link");

    OPDSFeedParser.findAcquisitionAuthors(e, eb);
    OPDSFeedParser.findAcquisitionRelations(e_links, eb);
    eb.setThumbnailOption(OPDSFeedParser.findAcquisitionThumbnail(e_links));
    eb.setCoverOption(OPDSFeedParser.findAcquisitionCover(e_links));
    eb.setSubtitleOption(OPDSFeedParser.findSubtitle(e));
    eb.setSummaryOption(OPDSXML.getFirstChildElementTextWithNameOptional(
      e,
      OPDSFeedParser.ATOM_URI,
      "summary"));

    return eb.build();
  }

  private static OPDSFeedType parseNavigation(
    final Element e_feed,
    final String id,
    final String title,
    final Calendar updated,
    final List<Element> links,
    final List<Element> entries)
    throws OPDSFeedParseException,
      ParseException,
      URISyntaxException
  {
    final OPDSNavigationFeedBuilderType b =
      OPDSNavigationFeed.newBuilder(id, updated, title);

    for (final Element ee : entries) {
      b.addEntry(OPDSFeedParser.parseNavigationEntry(ee));
    }

    return b.build();
  }

  private static OPDSNavigationFeedEntry parseNavigationEntry(
    final Element ee)
    throws OPDSFeedParseException,
      ParseException,
      URISyntaxException
  {
    final String id = OPDSFeedParser.findID(ee);
    final String title = OPDSFeedParser.findTitle(ee);
    final Calendar updated = OPDSFeedParser.findUpdated(ee);

    final List<Element> e_links =
      OPDSXML.getChildElementsWithNameNonEmpty(
        ee,
        OPDSFeedParser.ATOM_URI,
        "link");

    final OptionType<URI> featured =
      OPDSFeedParser.findNavigationFeatured(e_links);

    final URI target = OPDSFeedParser.findNavigationTarget(id, e_links);

    return OPDSNavigationFeedEntry.newEntry(
      id,
      title,
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
    // No state
  }

  @Override public OPDSFeedType parse(
    final InputStream s)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(s);

    try {
      final Document d = OPDSFeedParser.parseStream(s);
      final Node root = NullCheck.notNull(d.getFirstChild());
      final Element e_feed =
        OPDSXML.nodeAsElementWithName(root, OPDSFeedParser.ATOM_URI, "feed");

      final String id = OPDSFeedParser.findID(e_feed);
      final String title = OPDSFeedParser.findTitle(e_feed);
      final Calendar updated = OPDSFeedParser.findUpdated(e_feed);

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
          id,
          title,
          updated,
          links,
          entries);
      }

      return OPDSFeedParser.parseNavigation(
        e_feed,
        id,
        title,
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
