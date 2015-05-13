package org.nypl.simplified.opds.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.opds.core.OPDSAcquisition.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
  private static final URI    ACQUISITION_URI_PREFIX;
  private static final String ACQUISITION_URI_PREFIX_TEXT;
  private static final URI    ATOM_URI;
  private static final URI    BLOCK_URI;
  private static final String BLOCK_URI_TEXT;
  private static final URI    DUBLIN_CORE_TERMS_URI;
  private static final URI    IMAGE_URI;
  private static final String IMAGE_URI_TEXT;
  private static final Logger LOG;
  private static final URI    THUMBNAIL_URI;
  private static final String THUMBNAIL_URI_TEXT;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(OPDSFeedParser.class));
    ATOM_URI = NullCheck.notNull(URI.create("http://www.w3.org/2005/Atom"));

    DUBLIN_CORE_TERMS_URI =
      NullCheck.notNull(URI.create("http://purl.org/dc/terms/"));

    ACQUISITION_URI_PREFIX =
      NullCheck.notNull(URI.create("http://opds-spec.org/acquisition"));
    ACQUISITION_URI_PREFIX_TEXT =
      NullCheck.notNull(OPDSFeedParser.ACQUISITION_URI_PREFIX.toString());

    BLOCK_URI = NullCheck.notNull(URI.create("http://opds-spec.org/block"));
    BLOCK_URI_TEXT = NullCheck.notNull(OPDSFeedParser.BLOCK_URI.toString());

    THUMBNAIL_URI =
      NullCheck.notNull(URI.create("http://opds-spec.org/image/thumbnail"));
    THUMBNAIL_URI_TEXT =
      NullCheck.notNull(OPDSFeedParser.THUMBNAIL_URI.toString());

    IMAGE_URI = NullCheck.notNull(URI.create("http://opds-spec.org/image"));
    IMAGE_URI_TEXT = NullCheck.notNull(OPDSFeedParser.IMAGE_URI.toString());
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

  private static OptionType<URI> findAcquisitionNext(
    final List<Element> e_links)
    throws URISyntaxException
  {
    return OPDSFeedParser.findLinkWithURIRelText(e_links, "next");
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
    final String text = er.getTextContent();
    final String trimmed = text.trim();
    return OPDSRFC3339Formatter.parseRFC3339Date(NullCheck.notNull(trimmed));
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

    for (final Element e_link : e_links) {
      if (e_link.hasAttribute("rel")) {
        final String rel_text = NullCheck.notNull(e_link.getAttribute("rel"));

        /**
         * Block definition.
         */

        if (rel_text.equals(OPDSFeedParser.BLOCK_URI_TEXT)) {
          final String uri_text =
            NullCheck.notNull(e_link.getAttribute("href"));
          final String link_title =
            NullCheck.notNull(e_link.getAttribute("title"));
          final URI uri = NullCheck.notNull(URI.create(uri_text));
          eb.addBlock(uri, link_title);
          continue;
        }

        /**
         * Thumbnail.
         */

        if (rel_text.equals(OPDSFeedParser.THUMBNAIL_URI_TEXT)) {
          if (e_link.hasAttribute("href")) {
            final URI u =
              NullCheck.notNull(URI.create(e_link.getAttribute("href")));
            eb.setThumbnailOption(Option.some(u));
            continue;
          }
        }

        /**
         * Image/Cover.
         */

        if (rel_text.equals(OPDSFeedParser.IMAGE_URI_TEXT)) {
          if (e_link.hasAttribute("href")) {
            final URI u =
              NullCheck.notNull(URI.create(e_link.getAttribute("href")));
            eb.setCoverOption(Option.some(u));
            continue;
          }
        }

        /**
         * Acquisitions.
         */

        if (rel_text.startsWith(OPDSFeedParser.ACQUISITION_URI_PREFIX_TEXT)) {
          for (final Type v : OPDSAcquisition.Type.values()) {
            final String uri_text = NullCheck.notNull(v.getURI().toString());
            if (rel_text.equals(uri_text)) {
              final URI href = new URI(e_link.getAttribute("href"));
              eb.addAcquisition(new OPDSAcquisition(v, href));
              break;
            }
          }
        }
      }
    }

    OPDSFeedParser.findAcquisitionAuthors(e, eb);
    eb.setPublisherOption(OPDSFeedParser.findPublisher(e));
    eb.setSubtitleOption(OPDSFeedParser.findSubtitle(e));
    eb.setSummaryOption(OPDSXML.getFirstChildElementTextWithNameOptional(
      e,
      OPDSFeedParser.ATOM_URI,
      "summary"));

    return eb.build();
  }

  private static OptionType<OPDSSearchLink> parseSearchLink(
    final Element e)
    throws URISyntaxException
  {
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

  @Override public OPDSAcquisitionFeed parse(
    final URI uri,
    final InputStream s)
    throws OPDSFeedParseException
  {
    NullCheck.notNull(s);

    final long time_pre_parse = System.nanoTime();
    long time_post_parse = time_pre_parse;

    try {
      OPDSFeedParser.LOG.debug("parsing: {}", uri);

      final Document d = OPDSFeedParser.parseStream(s);
      time_post_parse = System.nanoTime();

      final Node root = NullCheck.notNull(d.getFirstChild());
      final Element e_feed =
        OPDSXML.nodeAsElementWithName(root, OPDSFeedParser.ATOM_URI, "feed");

      final String id = OPDSFeedParser.findID(e_feed);
      final String title = OPDSFeedParser.findTitle(e_feed);
      final Calendar updated = OPDSFeedParser.findUpdated(e_feed);

      final OPDSAcquisitionFeedBuilderType b =
        OPDSAcquisitionFeed.newBuilder(uri, id, updated, title);

      final List<Element> links = new ArrayList<Element>();
      final NodeList children = e_feed.getChildNodes();

      for (int index = 0; index < children.getLength(); ++index) {
        final Node child = NullCheck.notNull(children.item(index));

        if (child instanceof Element) {

          /**
           * Links.
           */

          if (OPDSXML.nodeHasName(
            (Element) child,
            OPDSFeedParser.ATOM_URI,
            "link")) {

            final Element e = OPDSXML.nodeAsElement(child);
            links.add(e);

            final OptionType<OPDSSearchLink> search_opt =
              OPDSFeedParser.parseSearchLink(e);
            if (search_opt.isSome()) {
              b.setSearchOption(search_opt);
            }
            continue;
          }

          /**
           * Entries.
           */

          if (OPDSXML.nodeHasName(
            (Element) child,
            OPDSFeedParser.ATOM_URI,
            "entry")) {
            final Element e = OPDSXML.nodeAsElement(child);
            b.addEntry(OPDSFeedParser.parseAcquisitionEntry(e));
            continue;
          }
        }
      }

      b.setNextOption(OPDSFeedParser.findAcquisitionNext(links));
      return b.build();

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
    } finally {
      final long time_now = System.nanoTime();
      final long time_parse = time_post_parse - time_pre_parse;
      final long time_interp = time_now - time_post_parse;
      OPDSFeedParser.LOG.debug(
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
