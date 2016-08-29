package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.assertions.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p> The default implementation of the {@link OPDSFeedParserType}. </p> <p>
 * The implementation generally assumes that all sections of the OPDS
 * specification that are denoted as "SHOULD" will in practice mean "WILL NOT".
 * </p>
 */

public final class OPDSFeedParser implements OPDSFeedParserType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(OPDSFeedParser.class));
  }

  private final OPDSAcquisitionFeedEntryParserType entry_parser;

  private OPDSFeedParser(
    final OPDSAcquisitionFeedEntryParserType in_entry_parser)
  {
    this.entry_parser = NullCheck.notNull(in_entry_parser);
  }

  /**
   * @param in_entry_parser A feed entry parser
   *
   * @return A new feed  parser
   */

  public static OPDSFeedParserType newParser(
    final OPDSAcquisitionFeedEntryParserType in_entry_parser)
  {
    return new OPDSFeedParser(in_entry_parser);
  }

  private static OptionType<OPDSFacet> parseFacet(
    final Element e)
    throws URISyntaxException
  {
    final boolean has_name = OPDSXML.nodeHasName(
      NullCheck.notNull(e), OPDSFeedConstants.ATOM_URI, "link");

    Assertions.checkPrecondition(has_name, "Node has name 'link'");

    boolean has_everything = true;
    has_everything = e.hasAttribute("title");
    has_everything = has_everything && e.hasAttribute("href");
    has_everything = has_everything && e.hasAttribute("rel");
    has_everything = has_everything && e.hasAttributeNS(
      OPDSFeedConstants.OPDS_URI_TEXT, "facetGroup");

    if (has_everything) {
      final String title = NullCheck.notNull(e.getAttribute("title"));
      final String rel = NullCheck.notNull(e.getAttribute("rel"));
      final String href = NullCheck.notNull(e.getAttribute("href"));
      final String group = NullCheck.notNull(
        e.getAttributeNS(
          OPDSFeedConstants.OPDS_URI_TEXT, "facetGroup"));

      if (OPDSFeedConstants.FACET_URI_TEXT.equals(rel)) {
        final boolean in_active;
        if (e.hasAttributeNS(OPDSFeedConstants.OPDS_URI_TEXT, "activeFacet")) {
          final String text =
            e.getAttributeNS(OPDSFeedConstants.OPDS_URI_TEXT, "activeFacet");
          final Boolean b = Boolean.valueOf(text);
          in_active = b;
        } else {
          in_active = false;
        }

        final OPDSFacet f =
          new OPDSFacet(in_active, new URI(href), group, title);
        return Option.some(f);
      }
    }

    return Option.none();
  }

  private static OptionType<URI> parseNextLink(
    final Element e)
    throws URISyntaxException
  {
    Assertions.checkPrecondition(
      "link".equals(e.getLocalName()),
      "localname %s == %s",
      e.getLocalName(),
      "link");

    final String rel = e.getAttribute("rel");
    if ("next".equals(rel)) {
      if (e.hasAttribute("href")) {
        final URI uri = new URI(e.getAttribute("href"));
        return Option.some(uri);
      }
    }

    return Option.none();
  }

  private static OptionType<OPDSSearchLink> parseSearchLink(
    final Element e)
    throws URISyntaxException
  {
    final boolean has_name = OPDSXML.nodeHasName(
      NullCheck.notNull(e), OPDSFeedConstants.ATOM_URI, "link");

    Assertions.checkPrecondition(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("type") && e.hasAttribute("rel") && e.hasAttribute("href");

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
    throws ParserConfigurationException, SAXException, IOException
  {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();
    return NullCheck.notNull(db.parse(s));
  }

  private static OptionType<URI> parseTermsOfService(final Element e)
    throws URISyntaxException
  {
    NullCheck.notNull(e);

    final boolean has_name = OPDSXML.nodeHasName(
      NullCheck.notNull(e), OPDSFeedConstants.ATOM_URI, "link");

    Assertions.checkPrecondition(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String r = NullCheck.notNull(e.getAttribute("rel"));
      final String h = NullCheck.notNull(e.getAttribute("href"));

      if ("terms-of-service".equals(r)) {
        return Option.some(new URI(h));
      }
    }

    return Option.none();
  }

  private static OptionType<URI> parseAbout(final Element e)
          throws URISyntaxException
  {
    NullCheck.notNull(e);

    final boolean has_name = OPDSXML.nodeHasName(
            NullCheck.notNull(e), OPDSFeedConstants.ATOM_URI, "link");

    Assertions.checkPrecondition(has_name, "Node has name 'link'");

    final boolean has_everything =
            e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String r = NullCheck.notNull(e.getAttribute("rel"));
      final String h = NullCheck.notNull(e.getAttribute("href"));

      if ("about".equals(r)) {
        return Option.some(new URI(h));
      }
    }

    return Option.none();
  }

  private static OptionType<URI> parsePrivacyPolicy(final Element e)
    throws URISyntaxException
  {
    NullCheck.notNull(e);

    final boolean has_name = OPDSXML.nodeHasName(
      NullCheck.notNull(e), OPDSFeedConstants.ATOM_URI, "link");

    Assertions.checkPrecondition(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String r = NullCheck.notNull(e.getAttribute("rel"));
      final String h = NullCheck.notNull(e.getAttribute("href"));

      if ("privacy-policy".equals(r)) {
        return Option.some(new URI(h));
      }
    }

    return Option.none();
  }

  @Override public OPDSAcquisitionFeed parse(
    final URI uri,
    final InputStream s)
    throws OPDSParseException
  {
    NullCheck.notNull(s);

    final long time_pre_parse = System.nanoTime();
    long time_post_parse = time_pre_parse;

    try {
      OPDSFeedParser.LOG.debug("parsing: {}", uri);

      final Document d = OPDSFeedParser.parseStream(s);
      time_post_parse = System.nanoTime();

      final Node root = NullCheck.notNull(d.getFirstChild());
      if (root instanceof Element) {
        final Element root_e = (Element) root;
        if (OPDSXML.nodeHasName(root_e, OPDSFeedConstants.ATOM_URI, "feed")) {
          return this.parseAsFeed(uri, root_e);
        }
        if (OPDSXML.nodeHasName(root_e, OPDSFeedConstants.ATOM_URI, "entry")) {
          return this.parseAsEntry(uri, root_e);
        }

        throw new OPDSParseException(
          String.format(
            "Feed root is '%s', expected 'feed' or 'entry'",
            root_e.getLocalName()));
      } else {
        throw new OPDSParseException("Feed root is not 'feed' or 'entry'");
      }

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
    } catch (final ParseException e) {
      throw new OPDSParseException(e);
    } catch (final URISyntaxException e) {
      throw new OPDSParseException(e);
    } finally {
      final long time_now = System.nanoTime();
      final long time_parse = time_post_parse - time_pre_parse;
      final long time_interp = time_now - time_post_parse;
      OPDSFeedParser.LOG.debug(
        "parsing completed ({}ms - parse: {}ms, interp: {}ms): {}",
          TimeUnit.MILLISECONDS.convert(
            time_parse + time_interp, TimeUnit.NANOSECONDS),
          TimeUnit.MILLISECONDS.convert(
            time_parse, TimeUnit.NANOSECONDS),
          TimeUnit.MILLISECONDS.convert(
            time_interp, TimeUnit.NANOSECONDS),
        uri);
    }
  }

  private OPDSAcquisitionFeed parseAsEntry(
    final URI uri,
    final Element e)
    throws OPDSParseException
  {
    OPDSFeedParser.LOG.debug("parsing feed as single entry: {}", uri);

    final String id = "urn:simplified-entry";
    final Calendar updated = Calendar.getInstance();
    final String title = "Entry";
    final OPDSAcquisitionFeedBuilderType b =
      OPDSAcquisitionFeed.newBuilder(uri, id, updated, title);
    b.addEntry(this.entry_parser.parseEntry(e));
    return b.build();
  }

  private OPDSAcquisitionFeed parseAsFeed(
    final URI uri,
    final Node root)
    throws OPDSParseException, ParseException, URISyntaxException
  {
    OPDSFeedParser.LOG.debug("parsing feed as ordinary feed: {}", uri);

    final Element e_feed = OPDSXML.nodeAsElementWithName(
      root, OPDSFeedConstants.ATOM_URI, "feed");

    final String id = OPDSAtom.findID(e_feed);
    final String title = OPDSAtom.findTitle(e_feed);
    final Calendar updated = OPDSAtom.findUpdated(e_feed);

    final OPDSAcquisitionFeedBuilderType b =
      OPDSAcquisitionFeed.newBuilder(uri, id, updated, title);

    final List<Element> links = new ArrayList<Element>(32);
    final NodeList children = e_feed.getChildNodes();

    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = NullCheck.notNull(children.item(index));

      if (child instanceof Element) {

        /**
         * Links.
         */

        if (OPDSXML.nodeHasName(
          (Element) child, OPDSFeedConstants.ATOM_URI, "link")) {

          final Element e = OPDSXML.nodeAsElement(child);
          links.add(e);

          /**
           * Search links.
           */

          {
            final OptionType<OPDSSearchLink> search_opt =
              OPDSFeedParser.parseSearchLink(e);
            if (search_opt.isSome()) {
              b.setSearchOption(search_opt);
              continue;
            }
          }

          /**
           * Next links.
           */

          {
            final OptionType<URI> next_opt = OPDSFeedParser.parseNextLink(e);
            if (next_opt.isSome()) {
              b.setNextOption(next_opt);
              continue;
            }
          }

          /**
           * Facet links.
           */

          {
            final OptionType<OPDSFacet> facet_opt =
              OPDSFeedParser.parseFacet(e);
            if (facet_opt.isSome()) {
              b.addFacet(((Some<OPDSFacet>) facet_opt).get());
              continue;
            }
          }

          /**
           * App About links.
           */

          {
            final OptionType<URI> about_opt =
                    OPDSFeedParser.parseAbout(e);
            if (about_opt.isSome()) {
              b.setAboutOption(about_opt);
              continue;
            }
          }

          /**
           * Terms of service links.
           */

          {
            final OptionType<URI> tos_opt =
              OPDSFeedParser.parseTermsOfService(e);
            if (tos_opt.isSome()) {
              b.setTermsOfServiceOption(tos_opt);
              continue;
            }
          }

          /**
           * Privacy policy links.
           */

          {
            final OptionType<URI> pp_opt = OPDSFeedParser.parsePrivacyPolicy(e);
            if (pp_opt.isSome()) {
              b.setPrivacyPolicyOption(pp_opt);
              continue;
            }
          }

          continue;
        }

        /**
         * Entries.
         */

        if (OPDSXML.nodeHasName(
          (Element) child, OPDSFeedConstants.ATOM_URI, "entry")) {
          final Element e = OPDSXML.nodeAsElement(child);
          b.addEntry(this.entry_parser.parseEntry(e));
//          continue;
        }
      }
    }

    return b.build();
  }
}
