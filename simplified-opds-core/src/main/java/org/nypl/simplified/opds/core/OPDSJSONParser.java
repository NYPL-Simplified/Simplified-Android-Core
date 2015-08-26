package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSAcquisition.Type;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

/**
 * The default implementation of the {@link OPDSJSONParserType} interface.
 */

public final class OPDSJSONParser implements OPDSJSONParserType
{
  private OPDSJSONParser()
  {
    // Nothing
  }

  /**
   * @return A new JSON parser
   */

  public static OPDSJSONParserType newParser()
  {
    return new OPDSJSONParser();
  }

  private static OPDSAcquisition parseAcquisition(
    final ObjectNode o)
    throws OPDSParseException
  {
    try {
      final Type in_type = Type.valueOf(
        OPDSJSONParserUtilities.getString(o, "type"));
      final URI in_uri = new URI(OPDSJSONParserUtilities.getString(o, "uri"));
      return new OPDSAcquisition(in_type, in_uri);
    } catch (final URISyntaxException e) {
      throw new OPDSParseException(e);
    }
  }

  private static OPDSAvailabilityType parseAvailability(
    final ObjectNode node)
    throws OPDSParseException
  {
    if (node.has("loanable")) {
      return OPDSAvailabilityLoanable.get();
    }
    if (node.has("holdable")) {
      return OPDSAvailabilityHoldable.get();
    }

    if (node.has("loaned")) {
      final ObjectNode n = OPDSJSONParserUtilities.getObject(node, "loaned");
      final Calendar in_start_date =
        OPDSJSONParserUtilities.getTimestamp(n, "start_date");
      final OptionType<Calendar> in_end_date =
        OPDSJSONParserUtilities.getTimestampOptional(n, "end_date");
      final OptionType<URI> in_revoke =
        OPDSJSONParserUtilities.getURIOptional(n, "revoke");
      return OPDSAvailabilityLoaned.get(in_start_date, in_end_date, in_revoke);
    }

    if (node.has("held")) {
      final ObjectNode n = OPDSJSONParserUtilities.getObject(node, "held");
      final Calendar in_start_date =
        OPDSJSONParserUtilities.getTimestamp(n, "start_date");
      final OptionType<Integer> in_position =
        OPDSJSONParserUtilities.getIntegerOptional(n, "position");
      final OptionType<Calendar> in_end_date =
        OPDSJSONParserUtilities.getTimestampOptional(n, "end_date");
      final OptionType<URI> in_revoke =
        OPDSJSONParserUtilities.getURIOptional(n, "revoke");
      return OPDSAvailabilityHeld.get(
        in_start_date, in_position, in_end_date, in_revoke);
    }

    if (node.has("held_ready")) {
      final ObjectNode n =
        OPDSJSONParserUtilities.getObject(node, "held_ready");
      final OptionType<Calendar> in_end_date =
        OPDSJSONParserUtilities.getTimestampOptional(n, "end_date");
      final OptionType<URI> in_revoke =
        OPDSJSONParserUtilities.getURIOptional(n, "revoke");
      return OPDSAvailabilityHeldReady.get(in_end_date, in_revoke);
    }

    if (node.has("open_access")) {
      final ObjectNode n = OPDSJSONParserUtilities.getObject(
        node, "open_access");
      final OptionType<URI> in_revoke =
        OPDSJSONParserUtilities.getURIOptional(n, "revoke");
      return OPDSAvailabilityOpenAccess.get(in_revoke);
    }

    throw new OPDSParseException("Expected availability information");
  }

  private static OPDSCategory parseCategory(
    final JsonNode jn)
    throws OPDSParseException
  {
    NullCheck.notNull(jn);
    final ObjectNode o = OPDSJSONParserUtilities.checkObject(null, jn);
    final String in_term = OPDSJSONParserUtilities.getString(o, "term");
    final String in_scheme = OPDSJSONParserUtilities.getString(o, "scheme");
    final OptionType<String> in_label =
      OPDSJSONParserUtilities.getStringOptional(o, "label");
    return new OPDSCategory(in_term, in_scheme, in_label);
  }

  @Override public OPDSAcquisitionFeed parseAcquisitionFeed(
    final ObjectNode s)
    throws OPDSParseException
  {
    NullCheck.notNull(s);

    try {
      final URI in_uri = new URI(OPDSJSONParserUtilities.getString(s, "uri"));
      final String in_id = OPDSJSONParserUtilities.getString(s, "id");
      final Calendar in_updated = OPDSJSONParserUtilities.getTimestamp(
        s, "updated");
      final String in_title = OPDSJSONParserUtilities.getString(s, "title");

      final OPDSAcquisitionFeedBuilderType fb =
        OPDSAcquisitionFeed.newBuilder(in_uri, in_id, in_updated, in_title);

      fb.setNextOption(
        OPDSJSONParserUtilities.getStringOptional(s, "next").mapPartial(
          new PartialFunctionType<String, URI, URISyntaxException>()
          {
            @Override public URI call(
              final String u)
              throws URISyntaxException
            {
              return new URI(u);
            }
          }));

      fb.setSearchOption(
        OPDSJSONParserUtilities.getObjectOptional(s, "search").mapPartial(
          new PartialFunctionType<ObjectNode, OPDSSearchLink,
            OPDSParseException>()
          {
            @Override public OPDSSearchLink call(
              final ObjectNode o)
              throws OPDSParseException
            {
              try {
                final String in_search_type =
                  OPDSJSONParserUtilities.getString(o, "type");
                final URI in_search_uri =
                  new URI(OPDSJSONParserUtilities.getString(o, "uri"));
                return new OPDSSearchLink(in_search_type, in_search_uri);
              } catch (final URISyntaxException e) {
                throw new OPDSParseException(e);
              }
            }
          }));

      {
        final ArrayNode fs = OPDSJSONParserUtilities.getArray(s, "facets");
        for (int index = 0; index < fs.size(); ++index) {
          final ObjectNode o = OPDSJSONParserUtilities.checkObject(
            null, fs.get(index));
          final boolean in_facet_active =
            OPDSJSONParserUtilities.getBoolean(o, "active");
          final URI in_facet_uri = new URI(
            OPDSJSONParserUtilities.getString(
              o, "uri"));
          final String in_facet_group = OPDSJSONParserUtilities.getString(
            o, "group");
          final String in_facet_title = OPDSJSONParserUtilities.getString(
            o, "title");
          fb.addFacet(
            new OPDSFacet(
              in_facet_active, in_facet_uri, in_facet_group, in_facet_title));
        }
      }

      {
        final ArrayNode fs = OPDSJSONParserUtilities.getArray(s, "entries");
        for (int index = 0; index < fs.size(); ++index) {
          final ObjectNode o = OPDSJSONParserUtilities.checkObject(
            null, fs.get(index));
          fb.addEntry(this.parseAcquisitionFeedEntry(o));
        }
      }

      return fb.build();
    } catch (final URISyntaxException e) {
      throw new OPDSParseException(e);
    }
  }

  @Override public OPDSAcquisitionFeedEntry parseAcquisitionFeedEntry(
    final ObjectNode s)
    throws OPDSParseException
  {
    NullCheck.notNull(s);

    final String in_id = OPDSJSONParserUtilities.getString(s, "id");
    final String in_title = OPDSJSONParserUtilities.getString(s, "title");
    final Calendar in_updated = OPDSJSONParserUtilities.getTimestamp(
      s, "updated");

    final OPDSAvailabilityType in_availability =
      OPDSJSONParser.parseAvailability(
        OPDSJSONParserUtilities.getObject(s, "availability"));

    final OPDSAcquisitionFeedEntryBuilderType fb =
      OPDSAcquisitionFeedEntry.newBuilder(
        in_id, in_title, in_updated, in_availability);

    {
      final ArrayNode a = OPDSJSONParserUtilities.getArray(s, "authors");
      for (int index = 0; index < a.size(); ++index) {
        fb.addAuthor(a.get(index).asText());
      }
    }

    {
      final ArrayNode a = OPDSJSONParserUtilities.getArray(s, "acquisitions");
      for (int index = 0; index < a.size(); ++index) {
        fb.addAcquisition(
          OPDSJSONParser.parseAcquisition(
            OPDSJSONParserUtilities.checkObject(null, a.get(index))));
      }
    }

    {
      final ArrayNode a = OPDSJSONParserUtilities.getArray(s, "categories");
      for (int index = 0; index < a.size(); ++index) {
        fb.addCategory(OPDSJSONParser.parseCategory(a.get(index)));
      }
    }

    {
      final ArrayNode a = OPDSJSONParserUtilities.getArray(s, "groups");
      for (int index = 0; index < a.size(); ++index) {
        try {
          final ObjectNode jo = OPDSJSONParserUtilities.checkObject(
            null, a.get(index));
          final URI in_uri = new URI(
            OPDSJSONParserUtilities.getString(
              jo, "uri"));
          final String in_name = OPDSJSONParserUtilities.getString(jo, "name");
          fb.addGroup(in_uri, in_name);
        } catch (final URISyntaxException e) {
          throw new OPDSParseException(e);
        }
      }
    }

    {
      final OptionType<String> o = OPDSJSONParserUtilities.getStringOptional(
        s, "cover");
      o.mapPartial(
        new PartialFunctionType<String, Unit, OPDSParseException>()
        {
          @Override public Unit call(
            final String u)
            throws OPDSParseException
          {
            try {
              fb.setCoverOption(Option.some(new URI(u)));
              return Unit.unit();
            } catch (final URISyntaxException e) {
              throw new OPDSParseException(e);
            }
          }
        });
    }

    {
      final OptionType<String> o =
        OPDSJSONParserUtilities.getStringOptional(s, "thumbnail");
      o.mapPartial(
        new PartialFunctionType<String, Unit, OPDSParseException>()
        {
          @Override public Unit call(
            final String u)
            throws OPDSParseException
          {
            try {
              fb.setThumbnailOption(Option.some(new URI(u)));
              return Unit.unit();
            } catch (final URISyntaxException e) {
              throw new OPDSParseException(e);
            }
          }
        });
    }

    fb.setPublishedOption(
      OPDSJSONParserUtilities.getTimestampOptional(s, "published"));
    fb.setPublisherOption(
      OPDSJSONParserUtilities.getStringOptional(s, "publisher"));
    fb.setSummaryOption(
      OPDSJSONParserUtilities.getStringOptional(s, "summary"));
    return fb.build();
  }

  @Override public OPDSAcquisitionFeedEntry parseAcquisitionFeedEntryFromStream(
    final InputStream s)
    throws OPDSParseException
  {
    try {
      final ObjectMapper jom = new ObjectMapper();
      return this.parseAcquisitionFeedEntry(
        OPDSJSONParserUtilities.checkObject(
          null, jom.readTree(s)));
    } catch (final JsonProcessingException e) {
      throw new OPDSParseException(e);
    } catch (final IOException e) {
      throw new OPDSParseException(e);
    }
  }

  @Override public OPDSAcquisitionFeed parseAcquisitionFeedFromStream(
    final InputStream s)
    throws OPDSParseException
  {
    try {
      final ObjectMapper jom = new ObjectMapper();
      return this.parseAcquisitionFeed(
        OPDSJSONParserUtilities.checkObject(
          null, jom.readTree(s)));
    } catch (final JsonProcessingException e) {
      throw new OPDSParseException(e);
    } catch (final IOException e) {
      throw new OPDSParseException(e);
    }
  }
}
