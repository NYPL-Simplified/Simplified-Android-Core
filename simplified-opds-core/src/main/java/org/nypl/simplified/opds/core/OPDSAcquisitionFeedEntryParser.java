package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.joda.time.DateTime;
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation;
import org.nypl.simplified.parser.api.ParseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import one.irradia.mime.api.MIMEType;
import one.irradia.mime.vanilla.MIMEParser;

import static org.nypl.simplified.opds.core.OPDSFeedConstants.ACQUISITION_URI_PREFIX_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.ALTERNATE_REL_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.ANNOTATION_URI_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.ATOM_URI;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.BIBFRAME_URI;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.CIRCULATION_ANALYTICS_OPEN_BOOK_REL_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.DUBLIN_CORE_TERMS_URI;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.GROUP_REL_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.IMAGE_URI_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.ISSUES_REL_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.OPDS_URI;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.RELATED_REL_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.REVOKE_URI_TEXT;
import static org.nypl.simplified.opds.core.OPDSFeedConstants.THUMBNAIL_URI_TEXT;

/**
 * The default implementation of the {@link OPDSAcquisitionFeedEntryParserType}
 * type.
 */

public final class OPDSAcquisitionFeedEntryParser implements OPDSAcquisitionFeedEntryParserType {

  private static final Logger LOG = LoggerFactory.getLogger(OPDSAcquisitionFeedEntryParser.class);

  private OPDSAcquisitionFeedEntryParser() {

  }

  private void findAcquisitionAuthors(
    final Element element,
    final OPDSAcquisitionFeedEntryBuilderType eb)
    throws OPDSParseException {

    final List<Element> e_authors =
      OPDSXML.getChildElementsWithName(element, ATOM_URI, "author");
    for (final Element ea : e_authors) {
      final String name =
        OPDSXML.getFirstChildElementTextWithName(Objects.requireNonNull(ea), ATOM_URI, "name");
      eb.addAuthor(name);
    }
  }

  private OptionType<String> findPublisher(final Element element) {
    return OPDSXML.getFirstChildElementTextWithNameOptional(
      element, DUBLIN_CORE_TERMS_URI, "publisher");
  }

  private String findDistribution(final Element element) {
    return OPDSXML.getFirstChildElementTextWithName(
      element, BIBFRAME_URI, "distribution", "ProviderName");
  }

  /**
   * @return A new feed entry parser
   */

  public static OPDSAcquisitionFeedEntryParserType newParser() {
    return new OPDSAcquisitionFeedEntryParser();
  }

  private OPDSAcquisitionFeedEntry parseAcquisitionEntry(
    final URI source,
    final Element element)
    throws OPDSParseException, ParseException {

    final String id =
      OPDSAtom.findID(element);
    final String title =
      OPDSAtom.findTitle(element);
    final DateTime updated =
      OPDSAtom.findUpdated(element);

    final OPDSAcquisitionFeedEntryBuilderType entry_builder =
      OPDSAcquisitionFeedEntry.newBuilder(id, title, updated, OPDSAvailabilityLoanable.get());

    final List<Element> e_links =
      OPDSXML.getChildElementsWithNameNonEmpty(element, ATOM_URI, "link");

    /*
     * First, locate a revocation link, if any. This is required to be found
     * first as it needs to be used later in availability information.
     */

    final OptionType<URI> revoke = findRevocationLink(source, entry_builder, e_links);

    /*
     * Now, handle any other types of links.
     */

    for (final Element e_link : e_links) {
      if (e_link.hasAttribute("rel")) {
        final String rel_text = Objects.requireNonNull(e_link.getAttribute("rel"));

        if (tryConsumeLinkGroup(source, entry_builder, e_link, rel_text)) {
          continue;
        }

        if (tryConsumeLinkIssues(source, entry_builder, e_link, rel_text)) {
          continue;
        }

        if (tryConsumeLinkAlternate(source, entry_builder, e_link, rel_text)) {
          continue;
        }

        if (tryConsumeLinkAnalytics(source, entry_builder, e_link, rel_text)) {
          continue;
        }

        if (tryConsumeLinkRelated(source, entry_builder, e_link, rel_text)) {
          continue;
        }

        if (tryConsumeLinkAnnotation(source, entry_builder, e_link, rel_text)) {
          continue;
        }

        if (tryConsumeLinkThumbnail(source, entry_builder, e_link, rel_text)) {
          continue;
        }

        if (tryConsumeLinkCover(source, entry_builder, e_link, rel_text)) {
          continue;
        }

        tryConsumeAcquisitions(source, entry_builder, revoke, e_link, rel_text);
      }
    }

    parseCategories(element, entry_builder);

    findAcquisitionAuthors(element, entry_builder);
    entry_builder.setPublisherOption(findPublisher(element));
    entry_builder.setDistribution(findDistribution(element));
    entry_builder.setPublishedOption(OPDSAtom.findPublished(element));
    entry_builder.setSummaryOption(
      OPDSXML.getFirstChildElementTextWithNameOptional(element, ATOM_URI, "summary"));

    return entry_builder.build();
  }

  private void tryConsumeDRMLicensorInformation(
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link)
    throws OPDSParseException {

    final OptionType<Element> licensor_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
        e_link, OPDSFeedConstants.DRM_URI, "licensor");

    if (licensor_opt.isSome()) {
      final Some<Element> licensor_some = (Some<Element>) licensor_opt;

      final Element licensor_element = OPDSXML.nodeAsElement(licensor_some.get());
      final String vendor = licensor_element.getAttribute("drm:vendor");
      String client_token = null;
      OptionType<String> device_manager = Option.none();

      final NodeList licensor_children = licensor_element.getChildNodes();
      for (int i = 0; i < licensor_children.getLength(); ++i) {
        final Node node = licensor_children.item(i);

        if (node.getNodeName().contains("clientToken")) {
          client_token = node.getFirstChild().getNodeValue();
        }

        if (node.getNodeName().contains("link")) {
          final Element element = OPDSXML.nodeAsElement(node);
          final boolean has_everything =
            element.hasAttribute("rel") && element.hasAttribute("href");

          if (has_everything) {
            final String r = Objects.requireNonNull(element.getAttribute("rel"));
            final String h = Objects.requireNonNull(element.getAttribute("href"));

            if ("http://librarysimplified.org/terms/drm/rel/devices".equals(r)) {
              device_manager = Option.some(h);
            }
          }
        }
        if (vendor != null && client_token != null) {
          final DRMLicensor licensor = new DRMLicensor(vendor, client_token, device_manager);
          entry_builder.setLicensorOption(Option.some(licensor));
        }
      }
    }
  }

  private OPDSIndirectAcquisition parseIndirectAcquisition(
    final Element acquisition
  ) throws OPDSParseException {
    try {
      final String attributeText =
        acquisition.getAttribute("type");
      final MIMEType type =
        MIMEParser.Companion.parseRaisingException(attributeText);
      final List<OPDSIndirectAcquisition> next_acquisitions =
        parseIndirectAcquisitions(acquisition);
      return new OPDSIndirectAcquisition(type, next_acquisitions);
    } catch (final Exception e) {
      throw new OPDSParseException(e);
    }
  }

  private List<OPDSIndirectAcquisition> parseIndirectAcquisitions(
    final Element element
  ) throws OPDSParseException {
    final List<Element> indirect_elements =
      OPDSXML.getChildElementsWithName(element, OPDS_URI, "indirectAcquisition");
    final List<OPDSIndirectAcquisition> indirects =
      new ArrayList<>(indirect_elements.size());

    for (final Element indirect_element : indirect_elements) {
      indirects.add(parseIndirectAcquisition(indirect_element));
    }
    return indirects;
  }

  private void tryConsumeAcquisitions(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final OptionType<URI> revoke,
    final Element link,
    final String rel_text)
    throws OPDSParseException {

    if (rel_text.startsWith(ACQUISITION_URI_PREFIX_TEXT)) {
      for (final Relation v : Relation.values()) {
        final String uri_text = v.getUri().toString();
        if (rel_text.equals(uri_text)) {
          final URI href;
          try {
            href = scrubURI(source, link.getAttribute("href"));
          } catch (URISyntaxException e) {
            entry_builder.addParseError(
              this.invalidURI(source, "'href' attribute of element with relation " + v, e));
            continue;
          }

          final List<OPDSIndirectAcquisition> indirects =
            parseIndirectAcquisitions(link);
          final OptionType<MIMEType> typeOpt =
            typeAttributeWithSupportedValue(link);

          if (typeOpt.isSome()) {
            final MIMEType type = ((Some<MIMEType>) typeOpt).get();
            final OPDSAcquisition acquisition = new OPDSAcquisition(v, href, type, indirects);
            entry_builder.addAcquisition(acquisition);

            if (v == Relation.ACQUISITION_OPEN_ACCESS) {
              entry_builder.setAvailability(OPDSAvailabilityOpenAccess.get(revoke));
            } else {
              tryAvailability(entry_builder, link, revoke);
            }
            break;
          }
        }
      }

      tryConsumeDRMLicensorInformation(entry_builder, link);
    }
  }

  private void parseCategories(
    final Element element,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder) {

    final List<Element> e_categories =
      OPDSXML.getChildElementsWithName(element, ATOM_URI, "category");

    for (final Element ce : e_categories) {
      final String term = Objects.requireNonNull(ce.getAttribute("term"));
      final String scheme = Objects.requireNonNull(ce.getAttribute("scheme"));

      final OptionType<String> label;
      if (ce.hasAttribute("label")) {
        label = Option.some(ce.getAttribute("label"));
      } else {
        label = Option.none();
      }

      entry_builder.addCategory(new OPDSCategory(term, scheme, label));
    }
  }

  /**
   * Check if the given link refers to a cover image. If it is, add it to the builder and
   * return {@code true}.
   */

  private boolean tryConsumeLinkCover(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link,
    final String rel_text) {

    if (rel_text.equals(IMAGE_URI_TEXT)) {
      if (e_link.hasAttribute("href")) {
        try {
          final URI u = scrubURI(source, e_link.getAttribute("href"));
          entry_builder.setCoverOption(Option.some(u));
        } catch (URISyntaxException e) {
          entry_builder.addParseError(
            this.invalidURI(source, hrefAttributeOfLinkRel(IMAGE_URI_TEXT), e));
        }
        return true;
      }
    }
    return false;
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

  /**
   * Check if the given link refers to a thumbnail. If it is, add it to the builder and
   * return {@code true}.
   */

  private boolean tryConsumeLinkThumbnail(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link,
    final String rel_text) {
    if (rel_text.equals(THUMBNAIL_URI_TEXT)) {
      if (e_link.hasAttribute("href")) {
        try {
          final URI u = scrubURI(source, e_link.getAttribute("href"));
          entry_builder.setThumbnailOption(Option.some(u));
        } catch (URISyntaxException e) {
          entry_builder.addParseError(
            this.invalidURI(source, hrefAttributeOfLinkRel(THUMBNAIL_URI_TEXT), e));
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the given link refers to an annotation. If it does, add it to the builder and
   * return {@code true}.
   */

  private boolean tryConsumeLinkAnnotation(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link,
    final String rel_text) {

    if (rel_text.equals(ANNOTATION_URI_TEXT)) {
      if (e_link.hasAttribute("href")) {
        try {
          final URI u = scrubURI(source, e_link.getAttribute("href"));
          entry_builder.setAnnotationsOption(Option.some(u));
        } catch (URISyntaxException e) {
          entry_builder.addParseError(
            this.invalidURI(source, hrefAttributeOfLinkRel(ANNOTATION_URI_TEXT), e));
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the given link refers to related books. If it does, add it to the builder and
   * return {@code true}.
   */

  private boolean tryConsumeLinkRelated(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link,
    final String rel_text) {

    if (rel_text.equals(RELATED_REL_TEXT)) {
      if (e_link.hasAttribute("href")) {
        try {
          final URI u = scrubURI(source, e_link.getAttribute("href"));
          entry_builder.setRelatedOption(Option.some(u));
        } catch (URISyntaxException e) {
          entry_builder.addParseError(
            this.invalidURI(source, hrefAttributeOfLinkRel(RELATED_REL_TEXT), e));
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the given link refers to an "alternate". If it does, add it to the builder and
   * return {@code true}.
   */

  private boolean tryConsumeLinkAlternate(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link,
    final String rel_text) {

    if (rel_text.equals(ALTERNATE_REL_TEXT)) {
      try {
        final String uri_text = Objects.requireNonNull(e_link.getAttribute("href"));
        final URI uri = scrubURI(source, uri_text);
        entry_builder.setAlternateOption(Option.some(uri));
      } catch (URISyntaxException e) {
        entry_builder.addParseError(
          this.invalidURI(source, hrefAttributeOfLinkRel(ALTERNATE_REL_TEXT), e));
      }
      return true;
    }
    return false;
  }

  /**
   * Check if the given link refers to analytics. If it does, add it to the builder and
   * return {@code true}.
   */

  private boolean tryConsumeLinkAnalytics(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link,
    final String rel_text) {

    if (rel_text.equals(CIRCULATION_ANALYTICS_OPEN_BOOK_REL_TEXT)) {
      try {
        final String uri_text = Objects.requireNonNull(e_link.getAttribute("href"));
        final URI uri = scrubURI(source, uri_text);
        entry_builder.setAnalyticsOption(Option.some(uri));
      } catch (URISyntaxException e) {
        entry_builder.addParseError(
          this.invalidURI(source, hrefAttributeOfLinkRel(CIRCULATION_ANALYTICS_OPEN_BOOK_REL_TEXT), e));
      }
      return true;
    }
    return false;
  }

  /**
   * Check if the given link refers to an issue system. If it does, add it to the builder and
   * return {@code true}.
   */

  private boolean tryConsumeLinkIssues(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link,
    final String rel_text) {

    if (rel_text.equals(ISSUES_REL_TEXT)) {
      try {
        final String uri_text = Objects.requireNonNull(e_link.getAttribute("href"));
        final URI uri = scrubURI(source, uri_text);
        entry_builder.setIssuesOption(Option.some(uri));
      } catch (URISyntaxException e) {
        entry_builder.addParseError(
          this.invalidURI(source, hrefAttributeOfLinkRel(ISSUES_REL_TEXT), e));
      }
      return true;
    }
    return false;
  }

  /**
   * Check if the given link refers to a group. If it does, add it to the builder and
   * return {@code true}.
   */

  private boolean tryConsumeLinkGroup(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final Element e_link,
    final String rel_text) {
    if (rel_text.equals(GROUP_REL_TEXT)) {
      try {
        final String uri_text = Objects.requireNonNull(e_link.getAttribute("href"));
        final String link_title = Objects.requireNonNull(e_link.getAttribute("title"));
        final URI uri = scrubURI(source, uri_text);
        entry_builder.addGroup(uri, link_title);
      } catch (URISyntaxException e) {
        entry_builder.addParseError(
          this.invalidURI(source, hrefAttributeOfLinkRel(GROUP_REL_TEXT), e));
      }
      return true;
    }
    return false;
  }

  private static String hrefAttributeOfLinkRel(String relValue) {
    return "'href' attribute of 'link' with 'rel' " + relValue;
  }

  private ParseError invalidURI(
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

  private OptionType<URI> findRevocationLink(
    final URI source,
    final OPDSAcquisitionFeedEntryBuilderType entry_builder,
    final List<Element> e_links) {

    for (final Element e_link : e_links) {
      if (e_link.hasAttribute("rel")) {
        final String rel_text = Objects.requireNonNull(e_link.getAttribute("rel"));
        if (rel_text.equals(REVOKE_URI_TEXT)) {
          if (e_link.hasAttribute("href")) {
            try {
              final URI u = scrubURI(source, e_link.getAttribute("href"));
              return Option.some(u);
            } catch (URISyntaxException e) {
              entry_builder.addParseError(
                this.invalidURI(source, hrefAttributeOfLinkRel(REVOKE_URI_TEXT), e));
              return Option.none();
            }
          }
        }
      }
    }
    return Option.none();
  }

  private OptionType<MIMEType> typeAttributeWithSupportedValue(final Element acquisition) {
    final String attributeText = acquisition.getAttribute("type");
    if (attributeText == null) {
      return Option.none();
    }
    if (attributeText.isEmpty()) {
      return Option.none();
    }

    try {
      final MIMEType elementType = MIMEParser.Companion.parseRaisingException(attributeText);
      return Option.of(elementType);
    } catch (final Exception e) {
      LOG.warn("unparseable MIME type: ", e);
      return Option.none();
    }
  }

  private void tryAvailability(
    final OPDSAcquisitionFeedEntryBuilderType eb,
    final Element element,
    final OptionType<URI> revoke)
    throws OPDSParseException {

    final OptionType<Element> copies_opt =
      OPDSXML.getFirstChildElementWithNameOptional(element, OPDS_URI, "copies");
    final OptionType<Element> holds_opt =
      OPDSXML.getFirstChildElementWithNameOptional(element, OPDS_URI, "holds");

    eb.setAvailability(inferAvailability(element, copies_opt, holds_opt, revoke));
  }

  private OPDSAvailabilityType inferAvailability(
    final Element element,
    final OptionType<Element> copies_opt,
    final OptionType<Element> holds_opt,
    final OptionType<URI> revoke)
    throws OPDSParseException {

    final OptionType<Element> available_opt =
      OPDSXML.getFirstChildElementWithNameOptional(element, OPDS_URI, "availability");

    if (available_opt.isSome()) {
      final Some<Element> available_some = (Some<Element>) available_opt;
      final Element available = available_some.get();
      final String status = available.getAttribute("status");

      if ("ready".equals(status)) {
        final OptionType<DateTime> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        return OPDSAvailabilityHeldReady.get(end_date, revoke);
      }

      if ("reserved".equals(status)) {
        final OptionType<DateTime> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        final OptionType<DateTime> start_date =
          OPDSXML.getAttributeRFC3339Optional(available, "since");
        OptionType<Integer> queue = Option.none();
        if (holds_opt.isSome()) {
          final Some<Element> holds_some = (Some<Element>) holds_opt;
          queue = OPDSXML.getAttributeIntegerOptional(holds_some.get(), "position");
        }
        return OPDSAvailabilityHeld.get(start_date, queue, end_date, revoke);
      }

      if ("available".equals(status)) {
        final OptionType<DateTime> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        final OptionType<DateTime> start_date =
          OPDSXML.getAttributeRFC3339Optional(available, "since");
        final String rel = Objects.requireNonNull(element.getAttribute("rel"));
        if (Relation.ACQUISITION_BORROW.getUri().toString().equals(rel)) {
          return OPDSAvailabilityLoanable.get();
        } else if (Relation.ACQUISITION_GENERIC.getUri().toString().equals(rel)) {
          return OPDSAvailabilityLoaned.get(start_date, end_date, revoke);
        }
      }
    }

    /*
     * The user has never seen the book before, and the book
     * did not have an availability:available element for its
     * borrow link, so it must be holdable.
     */

    if (copies_opt.isSome()) {
      final Some<Element> copies_some = (Some<Element>) copies_opt;
      final int copies_available =
        OPDSXML.getAttributeInteger(copies_some.get(), "available");

      if (copies_available > 0) {
        return OPDSAvailabilityLoanable.get();
      }
    }

    return OPDSAvailabilityHoldable.get();
  }

  @Override
  public OPDSAcquisitionFeedEntry parseEntry(
    final URI source,
    final Element element)
    throws OPDSParseException {

    Objects.requireNonNull(element, "Element");
    try {
      final OPDSAcquisitionFeedEntry entry = parseAcquisitionEntry(source, element);
      for (final ParseError error : entry.getErrors()) {
        LOG.error("{}: parse error: {}: ", source, error.getMessage(), error.getException());
      }
      return entry;
    } catch (final ParseException ex) {
      throw new OPDSParseException(ex);
    }
  }

  @Override
  public OPDSAcquisitionFeedEntry parseEntryStream(
    final URI source,
    final InputStream stream)
    throws OPDSParseException {

    Objects.requireNonNull(stream, "Stream");
    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document d = Objects.requireNonNull(db.parse(stream));
      final Element e = Objects.requireNonNull(d.getDocumentElement());
      return this.parseEntry(source, e);
    } catch (final ParserConfigurationException | IOException | SAXException ex) {
      throw new OPDSParseException(ex);
    }
  }
}
