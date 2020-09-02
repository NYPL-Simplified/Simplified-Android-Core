package org.nypl.simplified.opds.core;

import com.google.common.base.Preconditions;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.joda.time.DateTime;
import org.nypl.simplified.parser.api.ParseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.nypl.simplified.opds.core.OPDSFeedConstants.ATOM_URI;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.AUTHENTICATION_DOCUMENT_RELATION_URI_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.DRM_URI;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.FACET_URI_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.OPDS_URI_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.SIMPLIFIED_URI_TEXT;

/**
 * <p>The default implementation of the {@link OPDSFeedParserType}.</p>
 *
 * <p>
 * The implementation generally assumes that all sections of the OPDS
 * specification that are denoted as "SHOULD" will in practice mean "WILL NOT".
 * </p>
 */

public final class OPDSFeedParser implements OPDSFeedParserType {
  private static final Logger LOG;

  static {
    LOG = Objects.requireNonNull(LoggerFactory.getLogger(OPDSFeedParser.class));
  }

  private final OPDSAcquisitionFeedEntryParserType entry_parser;

  private OPDSFeedParser(final OPDSAcquisitionFeedEntryParserType in_entry_parser) {
    this.entry_parser = Objects.requireNonNull(in_entry_parser);
  }

  /**
   * @param in_entry_parser A feed entry parser
   * @return A new feed  parser
   */

  public static OPDSFeedParserType newParser(
    final OPDSAcquisitionFeedEntryParserType in_entry_parser) {
    return new OPDSFeedParser(in_entry_parser);
  }

  private static OptionType<OPDSFacet> parseFacet(
    final URI source,
    final OPDSAcquisitionFeedBuilderType builder,
    final Element e) {
    final boolean has_name =
      OPDSXML.nodeHasName(Objects.requireNonNull(e), ATOM_URI, "link");

    Preconditions.checkArgument(has_name, "Node has name 'link'");

    boolean has_everything = e.hasAttribute("title");
    has_everything = has_everything && e.hasAttribute("href");
    has_everything = has_everything && e.hasAttribute("rel");
    has_everything = has_everything && e.hasAttributeNS(OPDS_URI_TEXT, "facetGroup");

    if (has_everything) {
      final String title =
        Objects.requireNonNull(e.getAttribute("title"));
      final String rel =
        Objects.requireNonNull(e.getAttribute("rel"));
      final String href =
        Objects.requireNonNull(e.getAttribute("href"));
      final String group =
        Objects.requireNonNull(e.getAttributeNS(OPDS_URI_TEXT, "facetGroup"));

      if (FACET_URI_TEXT.equals(rel)) {
        final OptionType<String> group_type = parseFacetGroupType(e);
        final boolean active = parseFacetIsActive(e);
        try {
          return Option.some(new OPDSFacet(active, scrubURI(source,href), group, title, group_type));
        } catch (URISyntaxException ex) {
          builder.addParseError(invalidURI(source, hrefAttributeOfLinkRel(FACET_URI_TEXT), ex));
          return Option.none();
        }
      }
    }

    return Option.none();
  }

  private static OptionType<String> parseFacetGroupType(Element e) {
    if (e.hasAttributeNS(SIMPLIFIED_URI_TEXT, "facetGroupType")) {
      return Option.some(e.getAttributeNS(SIMPLIFIED_URI_TEXT, "facetGroupType"));
    } else {
      return Option.none();
    }
  }

  private static boolean parseFacetIsActive(Element e) {
    if (e.hasAttributeNS(OPDS_URI_TEXT, "activeFacet")) {
      return Boolean.valueOf(e.getAttributeNS(OPDS_URI_TEXT, "activeFacet"));
    } else {
      return false;
    }
  }

  private static OptionType<URI> parseNextLink(
    final URI source,
    final OPDSAcquisitionFeedBuilderType builder,
    final Element e) {
    Preconditions.checkArgument(
      "link".equals(e.getLocalName()),
      "localname %s == %s",
      e.getLocalName(),
      "link");

    final String rel = e.getAttribute("rel");
    if ("next".equals(rel)) {
      if (e.hasAttribute("href")) {
        try {
          final URI uri = scrubURI(source,e.getAttribute("href"));
          return Option.some(uri);
        } catch (URISyntaxException ex) {
          builder.addParseError(invalidURI(source, hrefAttributeOfLinkRel("next"), ex));
          return Option.none();
        }
      }
    }

    return Option.none();
  }

  private static URI scrubURI(
    final URI base,
    final String text) throws URISyntaxException {

    final URI unresolvedURI = new URI(text.trim());
    if (unresolvedURI.isAbsolute()) {
      return unresolvedURI;
    }
    return base.resolve(unresolvedURI);
  }

  private static OptionType<OPDSSearchLink> parseSearchLink(
    final URI source,
    final OPDSAcquisitionFeedBuilderType builder,
    final Element e) {
    final boolean has_name = OPDSXML.nodeHasName(
      Objects.requireNonNull(e), ATOM_URI, "link");

    Preconditions.checkArgument(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("type") && e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String t = Objects.requireNonNull(e.getAttribute("type"));
      final String r = Objects.requireNonNull(e.getAttribute("rel"));
      final String h = Objects.requireNonNull(e.getAttribute("href"));

      if ("search".equals(r)) {
        try {
          final URI u = Objects.requireNonNull(scrubURI(source,h));
          final OPDSSearchLink sl = new OPDSSearchLink(t, u);
          return Option.some(sl);
        } catch (URISyntaxException ex) {
          builder.addParseError(invalidURI(source, hrefAttributeOfLinkRel("search"), ex));
          return Option.none();
        }
      }
    }

    return Option.none();
  }

  private static Document parseStream(
    final InputStream s)
    throws ParserConfigurationException, SAXException, IOException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();
    return Objects.requireNonNull(db.parse(s));
  }

  private static OptionType<URI> parseTermsOfService(
    final URI source,
    final OPDSAcquisitionFeedBuilderType builder,
    final Element e) {
    Objects.requireNonNull(e);

    final boolean has_name = OPDSXML.nodeHasName(
      Objects.requireNonNull(e), ATOM_URI, "link");

    Preconditions.checkArgument(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String r = Objects.requireNonNull(e.getAttribute("rel"));
      final String h = Objects.requireNonNull(e.getAttribute("href"));

      if ("terms-of-service".equals(r)) {
        try {
          return Option.some(scrubURI(source,h));
        } catch (URISyntaxException ex) {
          builder.addParseError(invalidURI(source, hrefAttributeOfLinkRel("terms-of-service"), ex));
          return Option.none();
        }
      }
    }

    return Option.none();
  }

  private static OptionType<URI> parseAbout(
    final URI source,
    final OPDSAcquisitionFeedBuilderType builder,
    final Element e) {
    Objects.requireNonNull(e);

    final boolean has_name = OPDSXML.nodeHasName(
      Objects.requireNonNull(e), ATOM_URI, "link");

    Preconditions.checkArgument(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String r = Objects.requireNonNull(e.getAttribute("rel"));
      final String h = Objects.requireNonNull(e.getAttribute("href"));

      if ("about".equals(r)) {
        try {
          return Option.some(scrubURI(source,h));
        } catch (URISyntaxException ex) {
          builder.addParseError(invalidURI(source, hrefAttributeOfLinkRel("about"), ex));
          return Option.none();
        }
      }
    }

    return Option.none();
  }

  private static OptionType<URI> parsePrivacyPolicy(
    final URI source,
    final OPDSAcquisitionFeedBuilderType builder,
    final Element e) {
    Objects.requireNonNull(e);

    final boolean has_name = OPDSXML.nodeHasName(
      Objects.requireNonNull(e), ATOM_URI, "link");

    Preconditions.checkArgument(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String r = Objects.requireNonNull(e.getAttribute("rel"));
      final String h = Objects.requireNonNull(e.getAttribute("href"));

      if ("privacy-policy".equals(r)) {
        try {
          return Option.some(scrubURI(source,h));
        } catch (URISyntaxException ex) {
          builder.addParseError(invalidURI(source, hrefAttributeOfLinkRel("privacy-policy"), ex));
          return Option.none();
        }
      }
    }

    return Option.none();
  }

  private static OptionType<URI> parseAuthenticationDocumentLink(
    final URI source,
    final OPDSAcquisitionFeedBuilderType builder,
    final Element e) {
    Objects.requireNonNull(e);

    final boolean has_name = OPDSXML.nodeHasName(
      Objects.requireNonNull(e), ATOM_URI, "link");

    Preconditions.checkArgument(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String r = Objects.requireNonNull(e.getAttribute("rel"));
      final String h = Objects.requireNonNull(e.getAttribute("href"));

      if (AUTHENTICATION_DOCUMENT_RELATION_URI_TEXT.equals(r)) {
        try {
          return Option.some(scrubURI(source,h));
        } catch (URISyntaxException ex) {
          builder.addParseError(invalidURI(source, hrefAttributeOfLinkRel(AUTHENTICATION_DOCUMENT_RELATION_URI_TEXT), ex));
          return Option.none();
        }
      }
    }

    return Option.none();
  }

  private static OptionType<URI> parseAnnotationsLink(
    final URI source,
    final OPDSAcquisitionFeedBuilderType builder,
    final Element e) {
    Objects.requireNonNull(e);

    final boolean has_name = OPDSXML.nodeHasName(
      Objects.requireNonNull(e), ATOM_URI, "link");

    Preconditions.checkArgument(has_name, "Node has name 'link'");

    final boolean has_everything =
      e.hasAttribute("rel") && e.hasAttribute("href");

    if (has_everything) {
      final String r = Objects.requireNonNull(e.getAttribute("rel"));
      final String h = Objects.requireNonNull(e.getAttribute("href"));

      if ("http://www.w3.org/ns/oa#annotationService".equals(r)) {
        try {
          return Option.some(scrubURI(source,h));
        } catch (URISyntaxException ex) {
          builder.addParseError(invalidURI(source, hrefAttributeOfLinkRel("http://www.w3.org/ns/oa#annotationService"), ex));
          return Option.none();
        }
      }
    }

    return Option.none();
  }

  private static String hrefAttributeOfLinkRel(String relValue) {
    return "'href' attribute of 'link' with 'rel' " + relValue;
  }

  private static ParseError invalidURI(
    final URI source,
    final String sourceLocation,
    final Exception e) {
    final StringBuilder builder = new StringBuilder(128);
    builder.append("Could not parse URI: ");
    builder.append(sourceLocation);
    builder.append(": ");
    builder.append(e.getMessage());

    return new ParseError(
      source,
      builder.toString(),
      -1,
      0,
      e);
  }

  @Override
  public OPDSAcquisitionFeed parse(
    final URI uri,
    final InputStream s)
    throws OPDSParseException {
    Objects.requireNonNull(s);

    final long time_pre_parse = System.nanoTime();
    long time_post_parse = time_pre_parse;

    try {
      LOG.debug("parsing: {}", uri);

      final Document d = OPDSFeedParser.parseStream(s);
      time_post_parse = System.nanoTime();

      final Node root = Objects.requireNonNull(d.getFirstChild());
      if (root instanceof Element) {
        final Element root_e = (Element) root;
        if (OPDSXML.nodeHasName(root_e, ATOM_URI, "feed")) {
          return this.parseAsFeed(uri, root_e);
        }
        if (OPDSXML.nodeHasName(root_e, ATOM_URI, "entry")) {
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
    } finally {
      final long time_now = System.nanoTime();
      final long time_parse = time_post_parse - time_pre_parse;
      final long time_interp = time_now - time_post_parse;
      LOG.debug(
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
    throws OPDSParseException {
    LOG.debug("parsing feed as single entry: {}", uri);

    final String id = "urn:simplified-entry";
    final DateTime updated = DateTime.now();
    final String title = "Entry";
    final OPDSAcquisitionFeedBuilderType b =
      OPDSAcquisitionFeed.newBuilder(uri, id, updated, title);
    final OPDSAcquisitionFeedEntry entry =
      this.entry_parser.parseEntry(uri, e);

    if (!entry.getAcquisitions().isEmpty()) {
      b.addEntry(entry);
    }

    return b.build();
  }

  private OPDSAcquisitionFeed parseAsFeed(
    final URI uri,
    final Node root)
    throws OPDSParseException {
    LOG.debug("parsing feed as ordinary feed: {}", uri);

    final Element e_feed = OPDSXML.nodeAsElementWithName(
      root, ATOM_URI, "feed");

    final String id = OPDSAtom.findID(e_feed);
    final String title = OPDSAtom.findTitle(e_feed);
    final DateTime updated = OPDSAtom.findUpdated(e_feed);

    final OPDSAcquisitionFeedBuilderType builder =
      OPDSAcquisitionFeed.newBuilder(uri, id, updated, title);

    final List<Element> links = new ArrayList<>(32);
    final NodeList children = e_feed.getChildNodes();

    for (int index = 0; index < children.getLength(); ++index) {
      final Node child = Objects.requireNonNull(children.item(index));

      if (child instanceof Element) {

        /*
         * Links.
         */

        if (OPDSXML.nodeHasName(
          (Element) child, ATOM_URI, "link")) {

          final Element e = OPDSXML.nodeAsElement(child);
          links.add(e);

          /*
           * Search links.
           */

          {
            final OptionType<OPDSSearchLink> search_opt =
              OPDSFeedParser.parseSearchLink(uri, builder, e);
            if (search_opt.isSome()) {
              builder.setSearchOption(search_opt);
              continue;
            }
          }

          /*
           * Next links.
           */

          {
            final OptionType<URI> next_opt =
              OPDSFeedParser.parseNextLink(uri, builder, e);
            if (next_opt.isSome()) {
              builder.setNextOption(next_opt);
              continue;
            }
          }

          /*
           * Facet links.
           */

          {
            final OptionType<OPDSFacet> facet_opt =
              OPDSFeedParser.parseFacet(uri, builder, e);
            if (facet_opt.isSome()) {
              builder.addFacet(((Some<OPDSFacet>) facet_opt).get());
              continue;
            }
          }

          /*
           * App About links.
           */

          {
            final OptionType<URI> about_opt =
              OPDSFeedParser.parseAbout(uri, builder, e);
            if (about_opt.isSome()) {
              builder.setAboutOption(about_opt);
              continue;
            }
          }

          /*
           * Terms of service links.
           */

          {
            final OptionType<URI> tos_opt =
              OPDSFeedParser.parseTermsOfService(uri, builder, e);
            if (tos_opt.isSome()) {
              builder.setTermsOfServiceOption(tos_opt);
              continue;
            }
          }

          /*
           * Privacy policy links.
           */

          {
            final OptionType<URI> pp_opt =
              OPDSFeedParser.parsePrivacyPolicy(uri, builder, e);
            if (pp_opt.isSome()) {
              builder.setPrivacyPolicyOption(pp_opt);
              continue;
            }
          }

          /*
           * Authentication document links.
           */

          {
            final OptionType<URI> pp_opt =
              OPDSFeedParser.parseAuthenticationDocumentLink(uri, builder, e);
            if (pp_opt.isSome()) {
              builder.setAuthenticationDocumentLink(pp_opt);
              continue;
            }
          }

          /*
           * Annotations links.
           */

          {
            final OptionType<URI> annotOpt =
              OPDSFeedParser.parseAnnotationsLink(uri, builder, e);
            if (annotOpt.isSome()) {
              builder.setAnnotationsOption(annotOpt);
              continue;
            }
          }

          continue;
        }

        // parse licensor
        {
          if (OPDSXML.nodeHasName(
            (Element) child, DRM_URI, "licensor")) {
            final Element e = OPDSXML.nodeAsElement(child);
            final String in_vendor = e.getAttribute("drm:vendor");
            String in_client_token = null;
            OptionType<String> in_device_manager = Option.none();
            for (int i = 0; i < e.getChildNodes().getLength(); ++i) {
              final Node node = e.getChildNodes().item(i);

              if (node.getNodeName().contains("clientToken")) {
                in_client_token = node.getFirstChild().getNodeValue();
              }

              if (node.getNodeName().contains("link")) {
                final Element element = OPDSXML.nodeAsElement(node);

                final boolean has_everything =
                  element.hasAttribute("rel") && element.hasAttribute("href");

                if (has_everything) {
                  final String r = Objects.requireNonNull(element.getAttribute("rel"));
                  final String h = Objects.requireNonNull(element.getAttribute("href"));

                  if ("http://librarysimplified.org/terms/drm/rel/devices".equals(r)) {

                    in_device_manager = Option.some(h);

                  }
                }
              }
              if (in_vendor != null && in_client_token != null) {
                final DRMLicensor licensor = new DRMLicensor(in_vendor, in_client_token, in_device_manager);
                builder.setLisensor(Option.some(licensor));
              }
            }
          }

        }

        /*
         * Entries.
         */

        if (OPDSXML.nodeHasName((Element) child, ATOM_URI, "entry")) {
          final Element e = OPDSXML.nodeAsElement(child);
          final OPDSAcquisitionFeedEntry entry = this.entry_parser.parseEntry(uri, e);
          if (!entry.getAcquisitions().isEmpty()) {
            builder.addEntry(entry);
          }
        }
      }
    }

    return builder.build();
  }
}
