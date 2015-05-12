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

import org.nypl.simplified.assertions.Assertions;
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
  private static final URI BLOCK_URI;
  private static final URI DUBLIN_CORE_TERMS_URI;
  private static final URI FEATURED_URI_PREFIX;
  private static final URI IMAGE_URI;
  private static final URI THUMBNAIL_URI;

  static {
    ATOM_URI = NullCheck.notNull(URI.create("http://www.w3.org/2005/Atom"));
    DUBLIN_CORE_TERMS_URI =
      NullCheck.notNull(URI.create("http://purl.org/dc/terms/"));
    ACQUISITION_URI_PREFIX =
      NullCheck.notNull(URI.create("http://opds-spec.org/acquisition"));
    FEATURED_URI_PREFIX =
      NullCheck.notNull(URI.create("http://opds-spec.org/featured"));
    BLOCK_URI = NullCheck.notNull(URI.create("http://opds-spec.org/block"));
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
      final String name =
        OPDSXML.getFirstChildElementTextWithName(
          NullCheck.notNull(ea),
          OPDSFeedParser.ATOM_URI,
          "name");
      eb.addAuthor(name);
    }
  }

  private static OptionType<URI> findAcquisitionCover(
    final List<Element> e_links)
    throws URISyntaxException
  {
    return OPDSFeedParser.findLinkWithURIRel(
      e_links,
      OPDSFeedParser.IMAGE_URI);
  }

  private static OptionType<URI> findAcquisitionNext(
    final List<Element> e_links)
    throws URISyntaxException
  {
    return OPDSFeedParser.findLinkWithURIRelText(e_links, "next");
  }

  private static void findAcquisitionRelations(
    final List<Element> e_links,
    final OPDSAcquisitionFeedEntryBuilderType eb)
    throws URISyntaxException
  {
    for (final Element e : e_links) {
      if (e.hasAttribute("rel")) {
        final String text = NullCheck.notNull(e.getAttribute("rel"));

        for (final Type v : OPDSAcquisition.Type.values()) {
          final String uri_text = NullCheck.notNull(v.getURI().toString());
          if (text.equals(uri_text)) {
            final URI href = new URI(e.getAttribute("href"));
            eb.addAcquisition(new OPDSAcquisition(v, href));
          }
        }
      }
    }
  }

  private static OptionType<URI> findAcquisitionThumbnail(
    final List<Element> e_links)
    throws URISyntaxException
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
    throws URISyntaxException
  {
    final String uri_text = NullCheck.notNull(uri.toString());
    return OPDSFeedParser.findLinkWithURIRelText(e_links, uri_text);
  }

  private static OptionType<URI> findLinkWithURIRelText(
    final List<Element> e_links,
    final String text)
    throws URISyntaxException
  {
    for (final Element e : e_links) {
      if (e.hasAttribute("rel")) {
        final String rel = e.getAttribute("rel");
        if (rel.equals(text)) {
          return Option.some(new URI(e.getAttribute("href")));
        }
      }
    }

    return Option.none();
  }

  private static OptionType<URI> findNavigationFeatured(
    final List<Element> e_links)
    throws URISyntaxException
  {
    for (final Element e : e_links) {
      if (e.hasAttribute("rel")) {
        final String text = NullCheck.notNull(e.getAttribute("rel"));
        final String uri_text =
          NullCheck.notNull(OPDSFeedParser.FEATURED_URI_PREFIX.toString());
        if (text.equals(uri_text)) {
          return Option.some(new URI(e.getAttribute("href")));
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
     * Look for a link that has a <i>type</i> attribute that designates it as
     * some sort of feed, and has a <i>rel</i> attribute that designates the
     * feed as a <tt>subsection</tt>.
     */

    for (final Element e : e_links) {
      if (e.hasAttribute("type")) {
        final String type_text = NullCheck.notNull(e.getAttribute("type"));

        /**
         * Acquisition feeds are often missing <tt>rel</tt> attributes, and
         * the links that are missing them are typically the link that should
         * be followed.
         */

        if ("application/atom+xml;profile=opds-catalog;kind=acquisition"
          .equals(type_text)) {
          if (e.hasAttribute("rel")) {
            final String rel_text = e.getAttribute("rel");
            if (rel_text.equals("subsection")) {
              return new URI(e.getAttribute("href"));
            }
          } else {
            return new URI(e.getAttribute("href"));
          }
        }

        /**
         * Navigation feeds are supposed to be designated with
         * <tt>rel=subsection</tt>.
         */

        if ("application/atom+xml;profile=opds-catalog;kind=navigation"
          .equals(type_text)) {
          if (e.hasAttribute("rel")) {
            final String rel_text = e.getAttribute("rel");
            if (rel_text.equals("subsection")) {
              return new URI(e.getAttribute("href"));
            }
          }
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

  private static Calendar findPublished(
    final Element e)
    throws OPDSFeedParseException,
      DOMException,
      ParseException
  {
    final Element er =
      OPDSXML.getFirstChildElementWithName(
        e,
        OPDSFeedParser.ATOM_URI,
        "published");
    return OPDSRFC3339Formatter.parseRFC3339Date(er.getTextContent().trim());
  }

  private static OptionType<String> findPublisher(
    final Element e)
  {
    return OPDSXML.getFirstChildElementTextWithNameOptional(
      e,
      OPDSFeedParser.DUBLIN_CORE_TERMS_URI,
      "publisher");
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

  public static OPDSFeedParserType newParser()
  {
    return new OPDSFeedParser();
  }

  private static OPDSFeedType parseAcquisition(
    final URI uri,
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
      OPDSAcquisitionFeed.newBuilder(uri, id, updated, title);

    b.setSearchOption(OPDSFeedParser.parseSearchLinks(links));

    for (final Element ee : entries) {
      b.addEntry(OPDSFeedParser.parseAcquisitionEntry(NullCheck.notNull(ee)));
    }

    b.setNextOption(OPDSFeedParser.findAcquisitionNext(links));
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
    final Calendar published = OPDSFeedParser.findPublished(e);

    final OPDSAcquisitionFeedEntryBuilderType eb =
      OPDSAcquisitionFeedEntry.newBuilder(id, title, updated, published);

    final List<Element> e_links =
      OPDSXML.getChildElementsWithNameNonEmpty(
        e,
        OPDSFeedParser.ATOM_URI,
        "link");

    OPDSFeedParser.findAcquisitionAuthors(e, eb);
    OPDSFeedParser.findAcquisitionRelations(e_links, eb);
    OPDSFeedParser.findBlockLinks(e_links, eb);
    eb.setThumbnailOption(OPDSFeedParser.findAcquisitionThumbnail(e_links));
    eb.setCoverOption(OPDSFeedParser.findAcquisitionCover(e_links));
    eb.setPublisherOption(OPDSFeedParser.findPublisher(e));
    eb.setSubtitleOption(OPDSFeedParser.findSubtitle(e));
    eb.setSummaryOption(OPDSXML.getFirstChildElementTextWithNameOptional(
      e,
      OPDSFeedParser.ATOM_URI,
      "summary"));

    return eb.build();
  }

  private static void findBlockLinks(
    final List<Element> e_links,
    final OPDSAcquisitionFeedEntryBuilderType eb)
  {
    for (final Element e : e_links) {
      if (e.hasAttribute("rel")) {
        final String text = NullCheck.notNull(e.getAttribute("rel"));

        if (text.equals(OPDSFeedParser.BLOCK_URI.toString())) {
          final String uri_text = NullCheck.notNull(e.getAttribute("href"));
          final String title = NullCheck.notNull(e.getAttribute("title"));
          final URI uri = NullCheck.notNull(URI.create(uri_text));
          eb.addBlock(uri, title);
        }
      }
    }
  }

  private static OPDSFeedType parseNavigation(
    final URI uri,
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
      OPDSNavigationFeed.newBuilder(uri, id, updated, title);

    b.setSearchOption(OPDSFeedParser.parseSearchLinks(links));

    for (final Element ee : entries) {
      b.addEntry(OPDSFeedParser.parseNavigationEntry(NullCheck.notNull(ee)));
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

  private static OptionType<OPDSSearchLink> parseSearchLinks(
    final List<Element> links)
    throws URISyntaxException
  {
    for (final Element e : links) {
      final boolean has_name =
        OPDSXML.nodeHasName(
          NullCheck.notNull(e),
          OPDSFeedParser.ATOM_URI,
          "link");

      Assertions.checkPrecondition(has_name, "Node has name 'link'");

      final boolean has_everything =
        e.hasAttribute("type")
          && e.hasAttribute("rel")
          && e.hasAttribute("href");

      if (has_everything) {
        final String t = NullCheck.notNull(e.getAttribute("type"));
        final String r = NullCheck.notNull(e.getAttribute("rel"));
        final String h = NullCheck.notNull(e.getAttribute("href"));

        if ("search".equals(r)) {
          final URI u = NullCheck.notNull(new URI(h));
          final OPDSSearchLink sl = new OPDSSearchLink(t, u);
          return Option.some(sl);
        }
      }
    }

    return Option.none();
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

  private OPDSFeedParser()
  {
    // No state
  }

  @Override public OPDSFeedType parse(
    final URI uri,
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
        OPDSXML.getChildElementsWithName(
          e_feed,
          OPDSFeedParser.ATOM_URI,
          "entry");

      /**
       * Because the OPDS specification makes the "type" attribute optional,
       * it's necessary to examine the entries in the feed to determine if the
       * feed is a Navigation or Acquisition feed. The first entry in the list
       * decides what type the feed is.
       *
       * If there aren't any entries in the feed, then the feed is assumed to
       * be an acquisition feed. The justification for this is that search
       * results are acquisition feeds, any may be empty.
       */

      if (entries.size() == 0) {
        return OPDSFeedParser.parseAcquisition(
          uri,
          id,
          title,
          updated,
          links,
          entries);
      }

      final Element e0 = NullCheck.notNull(entries.get(0));
      if (OPDSFeedParser.entryIsFromAcquisitionFeed(e0)) {
        return OPDSFeedParser.parseAcquisition(
          uri,
          id,
          title,
          updated,
          links,
          entries);
      }

      return OPDSFeedParser.parseNavigation(
        uri,
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
