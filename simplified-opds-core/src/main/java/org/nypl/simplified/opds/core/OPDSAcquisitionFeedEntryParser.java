package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSAcquisition.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

/**
 * The default implementation of the {@link OPDSAcquisitionFeedEntryParserType}
 * type.
 */

public final class OPDSAcquisitionFeedEntryParser
  implements OPDSAcquisitionFeedEntryParserType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(OPDSAcquisitionFeedEntryParser.class));
  }

  private OPDSAcquisitionFeedEntryParser()
  {

  }

  private static void findAcquisitionAuthors(
    final Element e,
    final OPDSAcquisitionFeedEntryBuilderType eb)
    throws OPDSParseException
  {
    final List<Element> e_authors = OPDSXML.getChildElementsWithName(
      e, OPDSFeedConstants.ATOM_URI, "author");
    for (final Element ea : e_authors) {
      final String name = OPDSXML.getFirstChildElementTextWithName(
        NullCheck.notNull(ea), OPDSFeedConstants.ATOM_URI, "name");
      eb.addAuthor(name);
    }
  }

  private static OptionType<String> findPublisher(
    final Element e)
  {
    return OPDSXML.getFirstChildElementTextWithNameOptional(
      e, OPDSFeedConstants.DUBLIN_CORE_TERMS_URI, "publisher");
  }

  private static String findDistribution(
    final Element e)
  {
    return OPDSXML.getFirstChildElementTextWithName(
      e, OPDSFeedConstants.BIBFRAME_URI, "distribution", "ProviderName");
  }

  /**
   * @return A new feed entry parser
   */

  public static OPDSAcquisitionFeedEntryParserType newParser()
  {
    return new OPDSAcquisitionFeedEntryParser();
  }

  private static OPDSAcquisitionFeedEntry parseAcquisitionEntry(
    final Element e)
    throws OPDSParseException, ParseException, URISyntaxException
  {
    final String id = OPDSAtom.findID(e);
    final String title = OPDSAtom.findTitle(e);
    final Calendar updated = OPDSAtom.findUpdated(e);

    final OPDSAcquisitionFeedEntryBuilderType eb =
      OPDSAcquisitionFeedEntry.newBuilder(
        id, title, updated, OPDSAvailabilityLoanable.get());

    final List<Element> e_links = OPDSXML.getChildElementsWithNameNonEmpty(
      e, OPDSFeedConstants.ATOM_URI, "link");

    /**
     * First, locate a revocation link, if any. This is required to be found
     * first as it needs to be used later in availability information.
     */

    OptionType<URI> revoke = Option.none();
    for (final Element e_link : e_links) {
      if (e_link.hasAttribute("rel")) {
        final String rel_text = NullCheck.notNull(e_link.getAttribute("rel"));
        if (rel_text.equals(OPDSFeedConstants.REVOKE_URI_TEXT)) {
          if (e_link.hasAttribute("href")) {
            final URI u = new URI(e_link.getAttribute("href"));
            revoke = Option.some(u);
            break;
          }
        }
      }
    }

    /**
     * Now, handle any other types of links.
     */

    for (final Element e_link : e_links) {
      if (e_link.hasAttribute("rel")) {
        final String rel_text = NullCheck.notNull(e_link.getAttribute("rel"));

        /**
         * Block definition.
         */

        if (rel_text.equals(OPDSFeedConstants.GROUP_REL_TEXT)) {
          final String uri_text =
            NullCheck.notNull(e_link.getAttribute("href"));
          final String link_title =
            NullCheck.notNull(e_link.getAttribute("title"));
          final URI uri = new URI(uri_text);
          eb.addGroup(uri, link_title);
          continue;
        }

        if (rel_text.equals(OPDSFeedConstants.ISSUES_REL_TEXT)) {
          final String uri_text =
            NullCheck.notNull(e_link.getAttribute("href"));
          final URI uri = new URI(uri_text);
          eb.setIssuesOption(Option.some(uri));
          continue;
        }

        if (rel_text.equals(OPDSFeedConstants.ALTERNATE_REL_TEXT)) {
          final String uri_text =
            NullCheck.notNull(e_link.getAttribute("href"));
          final URI uri = new URI(uri_text);
          eb.setAlternateOption(Option.some(uri));

          final String uri_text_analytics =
            uri_text.replace("/works/", "/analytics/");

          final URI uri_analytics = new URI(uri_text_analytics);
          eb.setAnalyticsOption(Option.some(uri_analytics));
          continue;
        }

        if (rel_text.equals(OPDSFeedConstants.RELATED_REL_TEXT)) {
          if (e_link.hasAttribute("href")) {
            final URI u = new URI(e_link.getAttribute("href"));
            eb.setRelatedOption(Option.some(u));
            continue;
          }
        }

        if (rel_text.equals(OPDSFeedConstants.ANNOTATION_URI_TEXT)) {
          if (e_link.hasAttribute("href")) {
            final URI u = new URI(e_link.getAttribute("href"));
            eb.setAnnotationsOption(Option.some(u));
            continue;
          }
        }

        /**
         * Thumbnail.
         */

        if (rel_text.equals(OPDSFeedConstants.THUMBNAIL_URI_TEXT)) {
          if (e_link.hasAttribute("href")) {
            final URI u = new URI(e_link.getAttribute("href"));
            eb.setThumbnailOption(Option.some(u));
            continue;
          }
        }

        /**
         * Image/Cover.
         */

        if (rel_text.equals(OPDSFeedConstants.IMAGE_URI_TEXT)) {
          if (e_link.hasAttribute("href")) {
            final URI u = new URI(e_link.getAttribute("href"));
            eb.setCoverOption(Option.some(u));
            continue;
          }
        }

        /**
         * Acquisitions.
         */

        if (rel_text.startsWith(
          OPDSFeedConstants.ACQUISITION_URI_PREFIX_TEXT)
          && OPDSAcquisitionFeedEntryParser.linkIsSupported(e_link)) {

          boolean open_access = false;
          for (final Type v : OPDSAcquisition.Type.values()) {
            final String uri_text = NullCheck.notNull(v.getURI().toString());
            if (rel_text.equals(uri_text)) {
              final URI href = new URI(e_link.getAttribute("href"));
              eb.addAcquisition(new OPDSAcquisition(v, href));
              open_access = open_access || v == Type.ACQUISITION_OPEN_ACCESS;
              break;
            }
          }

          if (open_access) {
            eb.setAvailability(OPDSAvailabilityOpenAccess.get(revoke));
          } else {
            OPDSAcquisitionFeedEntryParser.tryAvailability(eb, e_link, revoke);
          }
        }
      }
    }

    final List<Element> e_categories = OPDSXML.getChildElementsWithName(
      e, OPDSFeedConstants.ATOM_URI, "category");

    for (final Element ce : e_categories) {
      final String term = NullCheck.notNull(ce.getAttribute("term"));
      final String scheme = NullCheck.notNull(ce.getAttribute("scheme"));

      final OptionType<String> label;
      if (ce.hasAttribute("label")) {
        label = Option.some(ce.getAttribute("label"));
      } else {
        label = Option.none();
      }

      eb.addCategory(new OPDSCategory(term, scheme, label));
    }

    OPDSAcquisitionFeedEntryParser.findAcquisitionAuthors(e, eb);
    eb.setPublisherOption(OPDSAcquisitionFeedEntryParser.findPublisher(e));
    eb.setDistribution(OPDSAcquisitionFeedEntryParser.findDistribution(e));
    eb.setPublishedOption(OPDSAtom.findPublished(e));
    eb.setSummaryOption(
      OPDSXML.getFirstChildElementTextWithNameOptional(
        e, OPDSFeedConstants.ATOM_URI, "summary"));

    return eb.build();
  }

  private static boolean linkIsSupported(
    final Element link)
  {
    final List<Element> top_level_list = OPDSXML.getChildElementsWithName(
      link, OPDSFeedConstants.OPDS_URI, "indirectAcquisition");

    if (top_level_list.size() == 0) {
      return true;
    }

    for (Element top_level_e : top_level_list) {
      if ("vnd.adobe/adept+xml".equals(top_level_e.getAttribute("type"))) {
        final List<Element> second_level_list = OPDSXML.getChildElementsWithName(
          link, OPDSFeedConstants.OPDS_URI, "indirectAcquisition");
        for (Element second_level_e : second_level_list) {
          if ("application/epub+zip".equals(second_level_e.getAttribute("type"))) {
            return true;
          }
        }
      } else if ("application/epub+zip".equals(top_level_e.getAttribute("type"))) {
        return true;
      }
    }

    return false;
  }

  private static void tryAvailability(
    final OPDSAcquisitionFeedEntryBuilderType eb,
    final Element e,
    final OptionType<URI> revoke)
    throws OPDSParseException, ParseException
  {
    final OptionType<Element> copies_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
        e, OPDSFeedConstants.OPDS_URI, "copies");
    final OptionType<Element> holds_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
        e, OPDSFeedConstants.OPDS_URI, "holds");

    eb.setAvailability(
      OPDSAcquisitionFeedEntryParser.inferAvailability(
        e, copies_opt, holds_opt, revoke));
  }

  private static OPDSAvailabilityType inferAvailability(
    final Element e,
    final OptionType<Element> copies_opt,
    final OptionType<Element> holds_opt,
    final OptionType<URI> revoke)
    throws OPDSParseException
  {
    final OptionType<Element> available_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
          e, OPDSFeedConstants.OPDS_URI, "availability");

    if (available_opt.isSome()) {
      final Some<Element> available_some = (Some<Element>) available_opt;
      final Element available = available_some.get();
      final String status = available.getAttribute("status");

      if ("ready".equals(status)) {
        final OptionType<Calendar> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        return OPDSAvailabilityHeldReady.get(end_date, revoke);
      }

      if ("reserved".equals(status)) {
        final OptionType<Calendar> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        final OptionType<Calendar> start_date =
          OPDSXML.getAttributeRFC3339Optional(available, "since");
        OptionType<Integer> queue = Option.none();
        if (holds_opt.isSome()) {
          final Some<Element> holds_some = (Some<Element>) holds_opt;
          queue = OPDSXML.getAttributeIntegerOptional(holds_some.get(), "position");
        }
        return OPDSAvailabilityHeld.get(start_date, queue, end_date, revoke);
      }

      if ("available".equals(status)) {
        final OptionType<Calendar> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        final OptionType<Calendar> start_date =
          OPDSXML.getAttributeRFC3339Optional(available, "since");
        final String rel = NullCheck.notNull(e.getAttribute("rel"));
        if (Type.ACQUISITION_BORROW.getURI().toString().equals(rel)) {
          return OPDSAvailabilityLoanable.get();
        }
        else if (Type.ACQUISITION_GENERIC.getURI().toString().equals(rel)) {
          return OPDSAvailabilityLoaned.get(start_date, end_date, revoke);
        }
      }
    }

    /**
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

  @Override public OPDSAcquisitionFeedEntry parseEntry(
    final Element e)
    throws OPDSParseException
  {
    NullCheck.notNull(e);

    try {
      return OPDSAcquisitionFeedEntryParser.parseAcquisitionEntry(e);
    } catch (final ParseException ex) {
      throw new OPDSParseException(ex);
    } catch (final URISyntaxException ex) {
      throw new OPDSParseException(ex);
    }
  }

  @Override public OPDSAcquisitionFeedEntry parseEntryStream(
    final InputStream s)
    throws OPDSParseException
  {
    NullCheck.notNull(s);

    try {
      final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document d = NullCheck.notNull(db.parse(s));
      final Element e = NullCheck.notNull(d.getDocumentElement());
      return this.parseEntry(e);
    } catch (final ParserConfigurationException ex) {
      throw new OPDSParseException(ex);
    } catch (final SAXException ex) {
      throw new OPDSParseException(ex);
    } catch (final IOException ex) {
      throw new OPDSParseException(ex);
    }
  }
}
