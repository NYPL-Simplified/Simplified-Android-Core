package org.nypl.simplified.opds.core;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.nypl.simplified.opds.core.OPDSAcquisition.Type;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The default implementation of the
 * {@link OPDSAcquisitionFeedEntrySerializerType} interface.
 */

@SuppressWarnings("synthetic-access") public final class OPDSAcquisitionFeedEntrySerializer implements
  OPDSAcquisitionFeedEntrySerializerType
{
  public static OPDSAcquisitionFeedEntrySerializerType newSerializer()
  {
    return new OPDSAcquisitionFeedEntrySerializer();
  }

  private OPDSAcquisitionFeedEntrySerializer()
  {

  }

  @Override public Document serializeFeedEntry(
    final OPDSAcquisitionFeedEntry e)
    throws OPDSFeedSerializationException
  {
    NullCheck.notNull(e);

    try {
      final DocumentBuilderFactory dbf =
        NullCheck.notNull(DocumentBuilderFactory.newInstance());
      final DocumentBuilder db = NullCheck.notNull(dbf.newDocumentBuilder());
      final Document d = NullCheck.notNull(db.newDocument());
      this.serializeFeedEntryForDocument(d, e);
      return d;
    } catch (final ParserConfigurationException ex) {
      throw new OPDSFeedSerializationException(ex);
    }
  }

  private static Element createLink(
    final Document d,
    final String href,
    final @Nullable String type,
    final @Nullable String rel)
  {
    final Element e =
      d.createElementNS(OPDSFeedConstants.ATOM_URI_TEXT, "link");
    e.setAttribute("href", href);
    if (type != null) {
      e.setAttribute("type", type);
    }
    if (rel != null) {
      e.setAttribute("rel", rel);
    }
    return e;
  }

  @Override public Element serializeFeedEntryForDocument(
    final Document d,
    final OPDSAcquisitionFeedEntry e)
    throws OPDSFeedSerializationException
  {
    NullCheck.notNull(d);
    NullCheck.notNull(e);

    final SimpleDateFormat fmt = OPDSRFC3339Formatter.newDateFormatter();

    final Element ee =
      NullCheck.notNull(d.createElementNS(
        OPDSFeedConstants.ATOM_URI_TEXT,
        "entry"));

    {
      final Element ee_id =
        NullCheck.notNull(d.createElementNS(
          OPDSFeedConstants.ATOM_URI_TEXT,
          "id"));
      ee_id.appendChild(d.createTextNode(e.getID()));
      ee.appendChild(ee_id);
    }

    {
      final Element ee_title =
        NullCheck.notNull(d.createElementNS(
          OPDSFeedConstants.ATOM_URI_TEXT,
          "title"));
      ee_title.appendChild(d.createTextNode(e.getTitle()));
      ee.appendChild(ee_title);
    }

    {
      for (final String a : e.getAuthors()) {
        final Element ee_author =
          NullCheck.notNull(d.createElementNS(
            OPDSFeedConstants.ATOM_URI_TEXT,
            "author"));
        final Element ee_author_name =
          NullCheck.notNull(d.createElementNS(
            OPDSFeedConstants.ATOM_URI_TEXT,
            "name"));
        ee_author_name.appendChild(d.createTextNode(a));
        ee_author.appendChild(ee_author_name);
        ee.appendChild(ee_author);
      }
    }

    {
      final Element ee_summary =
        NullCheck.notNull(d.createElementNS(
          OPDSFeedConstants.ATOM_URI_TEXT,
          "summary"));
      final Attr ta = d.createAttribute("type");
      ta.setTextContent("html");
      ee_summary.setAttributeNode(ta);
      ee_summary.appendChild(d.createTextNode(e.getSummary()));
      ee.appendChild(ee_summary);
    }

    {
      final Element ee_updated =
        NullCheck.notNull(d.createElementNS(
          OPDSFeedConstants.ATOM_URI_TEXT,
          "updated"));

      final Calendar updated = e.getUpdated();
      ee_updated.appendChild(d.createTextNode(fmt.format(updated.getTime())));
      ee.appendChild(ee_updated);
    }

    {
      final OptionType<URI> cover_opt = e.getCover();
      cover_opt
        .mapPartial(new PartialFunctionType<URI, Unit, OPDSFeedSerializationException>() {
          @Override public Unit call(
            final URI u)
            throws OPDSFeedSerializationException
          {
            ee.appendChild(OPDSAcquisitionFeedEntrySerializer.createLink(
              d,
              NullCheck.notNull(u.toString()),
              null,
              OPDSFeedConstants.IMAGE_URI_TEXT));
            return Unit.unit();
          }
        });
    }

    {
      final OptionType<URI> thumb_opt = e.getThumbnail();
      thumb_opt
        .mapPartial(new PartialFunctionType<URI, Unit, OPDSFeedSerializationException>() {
          @Override public Unit call(
            final URI u)
            throws OPDSFeedSerializationException
          {
            ee.appendChild(OPDSAcquisitionFeedEntrySerializer.createLink(
              d,
              NullCheck.notNull(u.toString()),
              null,
              OPDSFeedConstants.THUMBNAIL_URI_TEXT));
            return Unit.unit();
          }
        });
    }

    {
      final OptionType<String> pub_opt = e.getPublisher();
      pub_opt
        .mapPartial(new PartialFunctionType<String, Unit, OPDSFeedSerializationException>() {
          @Override public Unit call(
            final String publisher)
            throws OPDSFeedSerializationException
          {
            final Element ee_publisher =
              NullCheck.notNull(d.createElementNS(
                OPDSFeedConstants.DUBLIN_CORE_TERMS_URI_TEXT,
                "dcterms:publisher"));

            ee_publisher.appendChild(d.createTextNode(publisher));
            ee.appendChild(ee_publisher);
            return Unit.unit();
          }
        });
    }

    {
      final OptionType<Calendar> pub_opt = e.getPublished();
      pub_opt
        .mapPartial(new PartialFunctionType<Calendar, Unit, OPDSFeedSerializationException>() {
          @Override public Unit call(
            final Calendar published)
            throws OPDSFeedSerializationException
          {
            final Element ee_published =
              NullCheck.notNull(d.createElementNS(
                OPDSFeedConstants.ATOM_URI_TEXT,
                "published"));

            final String text = fmt.format(published.getTime());
            ee_published.appendChild(d.createTextNode(text));
            ee.appendChild(ee_published);
            return Unit.unit();
          }
        });
    }

    {
      for (final OPDSCategory c : e.getCategories()) {
        final Element ee_category =
          NullCheck.notNull(d.createElementNS(
            OPDSFeedConstants.ATOM_URI_TEXT,
            "category"));
        ee_category.setAttribute("scheme", c.getScheme());
        ee_category.setAttribute("term", c.getTerm());
        ee.appendChild(ee_category);
      }
    }

    {
      for (final OPDSAcquisition a : e.getAcquisitions()) {
        final URI uri = a.getURI();
        final Type type = a.getType();
        final URI type_uri = type.getURI();
        ee.appendChild(OPDSAcquisitionFeedEntrySerializer.createLink(
          d,
          NullCheck.notNull(uri.toString()),
          null,
          NullCheck.notNull(type_uri.toString())));
      }
    }

    d.appendChild(ee);
    return ee;
  }
}
