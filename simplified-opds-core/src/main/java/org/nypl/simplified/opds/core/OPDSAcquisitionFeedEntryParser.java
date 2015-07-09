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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

public final class OPDSAcquisitionFeedEntryParser implements
  OPDSAcquisitionFeedEntryParserType
{
  private static final Logger LOG;

  static {
    LOG =
      NullCheck.notNull(LoggerFactory
        .getLogger(OPDSAcquisitionFeedEntryParser.class));
  }

  private static void findAcquisitionAuthors(
    final Element e,
    final OPDSAcquisitionFeedEntryBuilderType eb)
    throws OPDSParseException
  {
    final List<Element> e_authors =
      OPDSXML.getChildElementsWithName(
        e,
        OPDSFeedConstants.ATOM_URI,
        "author");
    for (final Element ea : e_authors) {
      final String name =
        OPDSXML.getFirstChildElementTextWithName(
          NullCheck.notNull(ea),
          OPDSFeedConstants.ATOM_URI,
          "name");
      eb.addAuthor(name);
    }
  }

  private static OptionType<String> findPublisher(
    final Element e)
  {
    return OPDSXML.getFirstChildElementTextWithNameOptional(
      e,
      OPDSFeedConstants.DUBLIN_CORE_TERMS_URI,
      "publisher");
  }

  public static OPDSAcquisitionFeedEntryParserType newParser()
  {
    return new OPDSAcquisitionFeedEntryParser();
  }

  private static OPDSAcquisitionFeedEntry parseAcquisitionEntry(
    final Element e)
    throws OPDSParseException,
      ParseException,
      URISyntaxException
  {
    final String id = OPDSAtom.findID(e);
    final String title = OPDSAtom.findTitle(e);
    final Calendar updated = OPDSAtom.findUpdated(e);

    final OPDSAcquisitionFeedEntryBuilderType eb =
      OPDSAcquisitionFeedEntry.newBuilder(
        id,
        title,
        updated,
        OPDSAvailabilityLoanable.get());

    final List<Element> e_links =
      OPDSXML.getChildElementsWithNameNonEmpty(
        e,
        OPDSFeedConstants.ATOM_URI,
        "link");

    for (final Element e_link : e_links) {
      if (e_link.hasAttribute("rel")) {
        final String rel_text = NullCheck.notNull(e_link.getAttribute("rel"));

        /**
         * Block definition.
         */

        if (rel_text.equals(OPDSFeedConstants.GROUP_URI_TEXT)) {
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

        if (rel_text
          .startsWith(OPDSFeedConstants.ACQUISITION_URI_PREFIX_TEXT)) {
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

    final List<Element> e_categories =
      OPDSXML.getChildElementsWithName(
        e,
        OPDSFeedConstants.ATOM_URI,
        "category");

    for (final Element ce : e_categories) {
      final String term = NullCheck.notNull(ce.getAttribute("term"));
      final String scheme = NullCheck.notNull(ce.getAttribute("scheme"));
      eb.addCategory(new OPDSCategory(term, scheme));
    }

    OPDSAcquisitionFeedEntryParser.findAcquisitionAuthors(e, eb);
    eb.setPublisherOption(OPDSAcquisitionFeedEntryParser.findPublisher(e));
    eb.setPublishedOption(OPDSAtom.findPublished(e));
    eb.setSummaryOption(OPDSXML.getFirstChildElementTextWithNameOptional(
      e,
      OPDSFeedConstants.ATOM_URI,
      "summary"));

    eb.setAvailability(OPDSAcquisitionFeedEntryParser.determineAvailability(
      eb,
      e));
    return eb.build();
  }

  private static OPDSAvailabilityType determineAvailability(
    final OPDSAcquisitionFeedEntryBuilderType eb,
    final Element e)
    throws OPDSParseException,
      ParseException
  {
    final OptionType<Element> ee_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
        e,
        OPDSFeedConstants.SCHEMA_URI,
        "Event");

    /**
     * If there is a <tt>schema:Event</tt> element, then the book is either
     * already borrowed, or is on hold.
     */

    if (ee_opt.isSome()) {
      final Some<Element> ee_some = (Some<Element>) ee_opt;
      final Element ee = ee_some.get();
      final String ee_name =
        OPDSXML.getFirstChildElementTextWithName(
          ee,
          OPDSFeedConstants.SCHEMA_URI,
          "name");

      if ("loan".equals(ee_name)) {
        final Calendar start =
          OPDSRFC3339Formatter.parseRFC3339Date(OPDSXML
            .getFirstChildElementTextWithName(
              ee,
              OPDSFeedConstants.SCHEMA_URI,
              "startDate"));
        final OptionType<Calendar> end =
          OPDSXML.getFirstChildElementTextWithNameOptional(
            ee,
            OPDSFeedConstants.SCHEMA_URI,
            "endDate").mapPartial(
            new PartialFunctionType<String, Calendar, ParseException>() {
              @Override public Calendar call(
                final String s)
                throws ParseException
              {
                return OPDSRFC3339Formatter.parseRFC3339Date(s);
              }
            });

        return OPDSAvailabilityLoaned.get(start, end);
      }

      if ("hold".equals(ee_name)) {
        final Calendar start =
          OPDSRFC3339Formatter.parseRFC3339Date(OPDSXML
            .getFirstChildElementTextWithName(
              ee,
              OPDSFeedConstants.SCHEMA_URI,
              "startDate"));

        try {
          final int pos =
            Integer.valueOf(
              OPDSXML.getFirstChildElementTextWithName(
                ee,
                OPDSFeedConstants.SCHEMA_URI,
                "position")).intValue();

          return OPDSAvailabilityHeld.get(start, pos);
        } catch (final NumberFormatException x) {
          throw new OPDSParseException("Error parsing hold position", x);
        }
      }

      OPDSAcquisitionFeedEntryParser.LOG.error(
        "ignoring unrecognized event type: {}",
        ee_name);
    }

    /**
     * Otherwise, the availability must be inferred from the number of
     * licenses and the type of available acquisition links.
     */

    boolean borrow = false;
    final List<OPDSAcquisition> acqs = eb.getAcquisitions();
    for (int index = 0; index < acqs.size(); ++index) {
      final OPDSAcquisition acq = acqs.get(index);
      switch (acq.getType()) {
        case ACQUISITION_BORROW:
        case ACQUISITION_GENERIC:
        {
          borrow = true;
          break;
        }

        /**
         * If there was an acquisition link that implies open access, then the
         * book is available and there is nothing more to do.
         */

        case ACQUISITION_OPEN_ACCESS:
        {
          return OPDSAvailabilityOpenAccess.get();
        }
        case ACQUISITION_SAMPLE:
        case ACQUISITION_SUBSCRIBE:
        case ACQUISITION_BUY:
        {
          OPDSAcquisitionFeedEntryParser.LOG.error(
            "unimplemented acquisition type: {}",
            acq.getType());
          break;
        }
      }
    }

    /**
     * If there was an acquisition link that implies borrowing, then check to
     * see if there are available licenses. If there are no licenses, the book
     * is only available to put on hold.
     */

    if (borrow) {
      final OptionType<String> license_count_opt =
        OPDSXML.getFirstChildElementTextWithNameOptional(
          e,
          OPDSFeedConstants.SIMPLIFIED_URI,
          "available_licenses");

      if (license_count_opt.isSome()) {
        final Some<String> license_count_some =
          (Some<String>) license_count_opt;
        final String license_count_text = license_count_some.get();
        try {
          final Integer license_count = Integer.valueOf(license_count_text);
          if (license_count.intValue() == 0) {
            return OPDSAvailabilityHoldable.get();
          }
        } catch (final NumberFormatException x) {
          throw new OPDSParseException("Error parsing license count", x);
        }
      }
    }

    /**
     * If the number of licenses is not specified, or is specified and
     * non-zero, or the required information above is otherwise missing, the
     * book is assumed to be available for borrowing; there is not enough
     * information to make a more intelligent guess and no sane way to handle
     * the fact that there might be no acquisition links and nothing
     * whatsoever specified.
     */

    return OPDSAvailabilityLoanable.get();
  }

  private OPDSAcquisitionFeedEntryParser()
  {

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
