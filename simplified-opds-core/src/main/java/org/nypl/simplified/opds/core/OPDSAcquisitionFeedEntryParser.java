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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

public final class OPDSAcquisitionFeedEntryParser implements
  OPDSAcquisitionFeedEntryParserType
{
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
      OPDSAcquisitionFeedEntry.newBuilder(id, title, updated);

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

    return eb.build();
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
