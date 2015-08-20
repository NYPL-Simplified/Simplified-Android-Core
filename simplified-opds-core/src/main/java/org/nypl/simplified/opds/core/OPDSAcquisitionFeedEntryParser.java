package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.assertions.Assertions;
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
          OPDSFeedConstants.ACQUISITION_URI_PREFIX_TEXT)) {

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
            eb.setAvailability(OPDSAvailabilityOpenAccess.get());
          } else {
            OPDSAcquisitionFeedEntryParser.tryAvailability(eb, e_link);
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
    eb.setPublishedOption(OPDSAtom.findPublished(e));
    eb.setSummaryOption(
      OPDSXML.getFirstChildElementTextWithNameOptional(
        e, OPDSFeedConstants.ATOM_URI, "summary"));

    return eb.build();
  }

  private static void tryAvailability(
    final OPDSAcquisitionFeedEntryBuilderType eb,
    final Element e)
    throws OPDSParseException, ParseException
  {
    final OptionType<Element> copies_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
        e, OPDSFeedConstants.OPDS_URI, "copies");
    final OptionType<Element> holds_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
        e, OPDSFeedConstants.OPDS_URI, "holds");

    if (copies_opt.isSome()) {
      Assertions.checkPrecondition(
        holds_opt.isSome(), "If opds:copies exists, opds:holds must exist");

      final Some<Element> copies_some = (Some<Element>) copies_opt;
      final Some<Element> holds_some = (Some<Element>) holds_opt;
      final Element copies = copies_some.get();
      final Element holds = holds_some.get();
      eb.setAvailability(
        OPDSAcquisitionFeedEntryParser.inferAvailability(e, copies, holds));
    }
  }

  private static OPDSAvailabilityType inferAvailability(
    final Element e,
    final Element copies,
    final Element holds)
    throws OPDSParseException
  {
    /**
     * If there is an "available" element, then the user has interacted
     * with the book in some manner, such as attempting to borrow it or
     * place a hold on it.
     */

    final OptionType<Element> available_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
        e, OPDSFeedConstants.OPDS_URI, "availability");

    if (available_opt.isSome()) {
      final Some<Element> available_some = (Some<Element>) available_opt;
      final Element available = available_some.get();
      final String status = available.getAttribute("status");

      if ("reserved".equals(status)) {
        final OptionType<Calendar> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        return OPDSAvailabilityReserved.get(end_date);
      }

      if ("unavailable".equals(status)) {
        final OptionType<Calendar> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        final Calendar start_date =
          OPDSXML.getAttributeRFC3339(available, "since");
        final OptionType<Integer> queue =
          OPDSXML.getAttributeIntegerOptional(holds, "position");
        return OPDSAvailabilityHeld.get(start_date, queue, end_date);
      }

      if ("available".equals(status)) {
        final OptionType<Calendar> end_date =
          OPDSXML.getAttributeRFC3339Optional(available, "until");
        final Calendar start_date =
          OPDSXML.getAttributeRFC3339(available, "since");
        return OPDSAvailabilityLoaned.get(start_date, end_date);
      }
    }

    /**
     * Otherwise, the user has never seen the book before, and the book
     * is either holdable or loanable. If there are available copies, the
     * book is loanable. Otherwise, all that can be done is to place a hold.
     */

    final int copies_available =
      OPDSXML.getAttributeInteger(copies, "available");

    if (copies_available > 0) {
      return OPDSAvailabilityLoanable.get();
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
