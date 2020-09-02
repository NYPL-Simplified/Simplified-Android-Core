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

import org.joda.time.DateTime;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import one.irradia.mime.api.MIMEType;
import one.irradia.mime.vanilla.MIMEParser;

/**
 * The default implementation of the {@link OPDSJSONParserType} interface.
 */

public final class OPDSJSONParser implements OPDSJSONParserType {

  /**
   * The name of the field used for indirect acquisitions.
   */

  public static final String INDIRECT_ACQUISITIONS_FIELD = "indirect_acquisitions";
  public static final String CONTENT_TYPE_FIELD = "content_type";

  private OPDSJSONParser() {
    // Nothing
  }

  /**
   * @return A new JSON parser
   */

  public static OPDSJSONParserType newParser() {
    return new OPDSJSONParser();
  }

  private static OPDSAcquisition parseAcquisition(
    final ObjectNode o)
    throws OPDSParseException {
    try {

      /*
       * XXX: COMPATIBILITY: this field is called "type" when it should really be called "relation".
       * The name is preserved in order to allow new versions of the application to open old
       * versions of the on-disk data.
       */

      final Relation relation =
        Relation.valueOf(JSONParserUtilities.getString(o, "type"));

      final URI uri =
        JSONParserUtilities.getURI(o, "uri");

      final List<OPDSIndirectAcquisition> indirects;
      if (o.has(INDIRECT_ACQUISITIONS_FIELD)) {
        indirects = parseIndirectAcquisitions(
          JSONParserUtilities.getArray(o, INDIRECT_ACQUISITIONS_FIELD));
      } else {
        indirects = Collections.emptyList();
      }

      /*
       * XXX: COMPATIBILITY: The content type field will not be present for old versions of the
       * book database. Luckily, old book databases can only contain epub files.
       */

      MIMEType type;
      if (o.has(CONTENT_TYPE_FIELD)) {
        type = MIMEParser.Companion.parseRaisingException(
          JSONParserUtilities.getString(o, CONTENT_TYPE_FIELD)
        );
      } else {
        type = MIMEParser.Companion.parseRaisingException("application/epub+zip");
      }

      return new OPDSAcquisition(relation, uri, type, indirects);
    } catch (final Exception e) {
      throw new OPDSParseException(e);
    }
  }

  private static OPDSIndirectAcquisition parseIndirectAcquisition(
    final JsonNode jnode)
    throws OPDSParseException {
    NullCheck.notNull(jnode, "JSON node");

    try {
      final ObjectNode obj =
        JSONParserUtilities.checkObject(null, jnode);
      final MIMEType type =
        MIMEParser.Companion.parseRaisingException(JSONParserUtilities.getString(obj, "type"));
      final ArrayNode indirects =
        JSONParserUtilities.getArray(obj, INDIRECT_ACQUISITIONS_FIELD);
      return new OPDSIndirectAcquisition(type, parseIndirectAcquisitions(indirects));
    } catch (final Exception e) {
      throw new OPDSParseException(e);
    }
  }

  private static List<OPDSIndirectAcquisition> parseIndirectAcquisitions(
    final ArrayNode indirects)
    throws OPDSParseException {
    NullCheck.notNull(indirects, "Array node");

    final List<OPDSIndirectAcquisition> results = new ArrayList<>(indirects.size());
    for (int index = 0; index < indirects.size(); ++index) {
      results.add(parseIndirectAcquisition(indirects.get(index)));
    }
    return results;
  }

  private static OPDSAvailabilityType parseAvailability(
    final ObjectNode node)
    throws OPDSParseException {
    try {
      if (node.has("loanable")) {
        return OPDSAvailabilityLoanable.get();
      }
      if (node.has("holdable")) {
        return OPDSAvailabilityHoldable.get();
      }

      if (node.has("loaned")) {
        final ObjectNode n = JSONParserUtilities.getObject(node, "loaned");
        final OptionType<DateTime> in_start_date =
          JSONParserUtilities.getTimestampOptional(n, "start_date");
        final OptionType<DateTime> in_end_date =
          JSONParserUtilities.getTimestampOptional(n, "end_date");
        final OptionType<URI> in_revoke =
          JSONParserUtilities.getURIOptional(n, "revoke");
        return OPDSAvailabilityLoaned.get(in_start_date, in_end_date, in_revoke);
      }

      if (node.has("held")) {
        final ObjectNode n = JSONParserUtilities.getObject(node, "held");
        final OptionType<DateTime> in_start_date =
          JSONParserUtilities.getTimestampOptional(n, "start_date");
        final OptionType<Integer> in_position =
          JSONParserUtilities.getIntegerOptional(n, "position");
        final OptionType<DateTime> in_end_date =
          JSONParserUtilities.getTimestampOptional(n, "end_date");
        final OptionType<URI> in_revoke =
          JSONParserUtilities.getURIOptional(n, "revoke");
        return OPDSAvailabilityHeld.get(in_start_date, in_position, in_end_date, in_revoke);
      }

      if (node.has("held_ready")) {
        final ObjectNode n = JSONParserUtilities.getObject(node, "held_ready");
        final OptionType<DateTime> in_end_date =
          JSONParserUtilities.getTimestampOptional(n, "end_date");
        final OptionType<URI> in_revoke =
          JSONParserUtilities.getURIOptional(n, "revoke");
        return OPDSAvailabilityHeldReady.get(in_end_date, in_revoke);
      }

      if (node.has("open_access")) {
        final ObjectNode n = JSONParserUtilities.getObject(
          node, "open_access");
        final OptionType<URI> in_revoke =
          JSONParserUtilities.getURIOptional(n, "revoke");
        return OPDSAvailabilityOpenAccess.get(in_revoke);
      }

      if (node.has("revoked")) {
        final ObjectNode n = JSONParserUtilities.getObject(
          node, "revoked");
        final URI in_revoke = JSONParserUtilities.getURI(n, "revoke");
        return OPDSAvailabilityRevoked.get(in_revoke);
      }

      throw new OPDSParseException("Expected availability information");
    } catch (final JSONParseException e) {
      throw new OPDSParseException(e);
    }
  }

  private static OPDSCategory parseCategory(
    final JsonNode jn)
    throws OPDSParseException {
    NullCheck.notNull(jn);
    try {
      final ObjectNode o = JSONParserUtilities.checkObject(null, jn);
      final String in_term = JSONParserUtilities.getString(o, "term");
      final String in_scheme = JSONParserUtilities.getString(o, "scheme");
      final OptionType<String> in_label =
        JSONParserUtilities.getStringOptional(o, "label");
      return new OPDSCategory(in_term, in_scheme, in_label);
    } catch (final JSONParseException e) {
      throw new OPDSParseException(e);
    }
  }

  private static DRMLicensor parseLicensor(
    final JsonNode jn)
    throws OPDSParseException {
    NullCheck.notNull(jn);
    try {
      final ObjectNode o = JSONParserUtilities.checkObject(null, jn);
      final String in_vendor = JSONParserUtilities.getString(o, "vendor");
      final String in_client_token = JSONParserUtilities.getString(o, "clientToken");
      final OptionType<String> in_device_manager =
        JSONParserUtilities.getStringOptional(o, "deviceManager");

      return new DRMLicensor(in_vendor, in_client_token, in_device_manager);
    } catch (final JSONParseException e) {
      throw new OPDSParseException(e);
    }
  }


  @Override
  public OPDSAcquisitionFeed parseAcquisitionFeed(
    final ObjectNode s)
    throws OPDSParseException {
    NullCheck.notNull(s);

    try {
      final URI in_uri = new URI(JSONParserUtilities.getString(s, "uri"));
      final String in_id = JSONParserUtilities.getString(s, "id");
      final DateTime in_updated = JSONParserUtilities.getTimestamp(
        s, "updated");
      final String in_title = JSONParserUtilities.getString(s, "title");

      final OPDSAcquisitionFeedBuilderType fb =
        OPDSAcquisitionFeed.newBuilder(in_uri, in_id, in_updated, in_title);

      fb.setNextOption(
        JSONParserUtilities.getStringOptional(s, "next").mapPartial(
          new PartialFunctionType<String, URI, URISyntaxException>() {
            @Override
            public URI call(
              final String u)
              throws URISyntaxException {
              return new URI(u);
            }
          }));

      fb.setSearchOption(
        JSONParserUtilities.getObjectOptional(s, "search").mapPartial(
          new PartialFunctionType<ObjectNode, OPDSSearchLink,
            JSONParseException>() {
            @Override
            public OPDSSearchLink call(
              final ObjectNode o)
              throws JSONParseException {
              final String in_search_type =
                JSONParserUtilities.getString(o, "type");
              final URI in_search_uri = JSONParserUtilities.getURI(o, "uri");
              return new OPDSSearchLink(in_search_type, in_search_uri);
            }
          }));

      {
        final ArrayNode fs = JSONParserUtilities.getArray(s, "facets");
        for (int index = 0; index < fs.size(); ++index) {
          final ObjectNode o = JSONParserUtilities.checkObject(null, fs.get(index));
          final boolean in_facet_active =
            JSONParserUtilities.getBoolean(o, "active");
          final URI in_facet_uri = new URI(
            JSONParserUtilities.getString(o, "uri"));
          final String in_facet_group =
            JSONParserUtilities.getString(o, "group");
          final OptionType<String> in_facet_group_type =
            JSONParserUtilities.getStringOptional(o, "group_type");
          final String in_facet_title =
            JSONParserUtilities.getString(o, "title");
          fb.addFacet(new OPDSFacet(
            in_facet_active,
            in_facet_uri,
            in_facet_group,
            in_facet_title,
            in_facet_group_type));
        }
      }

      {
        final ArrayNode fs = JSONParserUtilities.getArray(s, "entries");
        for (int index = 0; index < fs.size(); ++index) {
          final ObjectNode o = JSONParserUtilities.checkObject(
            null, fs.get(index));
          fb.addEntry(this.parseAcquisitionFeedEntry(o));
        }
      }

      return fb.build();
    } catch (final URISyntaxException e) {
      throw new OPDSParseException(e);
    } catch (final JSONParseException e) {
      throw new OPDSParseException(e);
    }
  }

  @Override
  public OPDSAcquisitionFeedEntry parseAcquisitionFeedEntry(
    final ObjectNode s)
    throws OPDSParseException {
    NullCheck.notNull(s);

    try {
      final String in_id = JSONParserUtilities.getString(s, "id");
      final String in_title = JSONParserUtilities.getString(s, "title");
      final DateTime in_updated = JSONParserUtilities.getTimestamp(s, "updated");

      final OPDSAvailabilityType in_availability =
        OPDSJSONParser.parseAvailability(
          JSONParserUtilities.getObject(s, "availability"));

      final OPDSAcquisitionFeedEntryBuilderType fb =
        OPDSAcquisitionFeedEntry.newBuilder(
          in_id, in_title, in_updated, in_availability);

      {
        final ArrayNode a = JSONParserUtilities.getArray(s, "authors");
        for (int index = 0; index < a.size(); ++index) {
          fb.addAuthor(a.get(index).asText());
        }
      }

      {
        final ArrayNode a = JSONParserUtilities.getArray(s, "acquisitions");
        for (int index = 0; index < a.size(); ++index) {
          fb.addAcquisition(
            OPDSJSONParser.parseAcquisition(
              JSONParserUtilities.checkObject(null, a.get(index))));
        }
      }

      {
        if (s.has("licensor")) {
          final JsonNode a = JSONParserUtilities.getNode(s, "licensor");
          fb.setLicensorOption(Option.some(OPDSJSONParser.parseLicensor(a)));
        }
      }

      {
        final ArrayNode a = JSONParserUtilities.getArray(s, "categories");
        for (int index = 0; index < a.size(); ++index) {
          fb.addCategory(OPDSJSONParser.parseCategory(a.get(index)));
        }
      }

      {
        final ArrayNode a = JSONParserUtilities.getArray(s, "groups");
        for (int index = 0; index < a.size(); ++index) {
          try {
            final ObjectNode jo = JSONParserUtilities.checkObject(
              null, a.get(index));
            final URI in_uri = new URI(
              JSONParserUtilities.getString(
                jo, "uri"));
            final String in_name = JSONParserUtilities.getString(jo, "name");
            fb.addGroup(in_uri, in_name);
          } catch (final URISyntaxException e) {
            throw new OPDSParseException(e);
          }
        }
      }

      {
        final OptionType<String> o = JSONParserUtilities.getStringOptional(
          s, "cover");
        o.mapPartial(
          new PartialFunctionType<String, Unit, OPDSParseException>() {
            @Override
            public Unit call(
              final String u)
              throws OPDSParseException {
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
          JSONParserUtilities.getStringOptional(s, "thumbnail");
        o.mapPartial(
          new PartialFunctionType<String, Unit, OPDSParseException>() {
            @Override
            public Unit call(
              final String u)
              throws OPDSParseException {
              try {
                fb.setThumbnailOption(Option.some(new URI(u)));
                return Unit.unit();
              } catch (final URISyntaxException e) {
                throw new OPDSParseException(e);
              }
            }
          });
      }


      {
        final OptionType<URI> o =
          JSONParserUtilities.getURIOptional(s, "alternate");
        o.mapPartial(
          new PartialFunctionType<URI, Unit, OPDSParseException>() {
            @Override
            public Unit call(
              final URI u)
              throws OPDSParseException {
              fb.setAlternateOption(Option.some(u));
              return Unit.unit();
            }
          });
      }

      {
        final OptionType<URI> o =
          JSONParserUtilities.getURIOptional(s, "analytics");
        o.mapPartial(
          new PartialFunctionType<URI, Unit, OPDSParseException>() {
            @Override
            public Unit call(
              final URI u)
              throws OPDSParseException {
              fb.setAnalyticsOption(Option.some(u));
              return Unit.unit();
            }
          });
      }

      {
        final OptionType<URI> o =
          JSONParserUtilities.getURIOptional(s, "annotations");
        o.mapPartial(
          new PartialFunctionType<URI, Unit, OPDSParseException>() {
            @Override
            public Unit call(
              final URI u)
              throws OPDSParseException {
              fb.setAnnotationsOption(Option.some(u));
              return Unit.unit();
            }
          });
      }

      fb.setPublishedOption(
        JSONParserUtilities.getTimestampOptional(s, "published"));
      fb.setPublisherOption(
        JSONParserUtilities.getStringOptional(s, "publisher"));
      fb.setDistribution(
        JSONParserUtilities.getString(s, "distribution"));
      fb.setSummaryOption(
        JSONParserUtilities.getStringOptional(s, "summary"));
      return fb.build();
    } catch (final JSONParseException e) {
      throw new OPDSParseException(e);
    }
  }

  @Override
  public OPDSAcquisitionFeedEntry parseAcquisitionFeedEntryFromStream(
    final InputStream s)
    throws OPDSParseException {
    try {
      final ObjectMapper jom = new ObjectMapper();
      return this.parseAcquisitionFeedEntry(
        JSONParserUtilities.checkObject(
          null, jom.readTree(s)));
    } catch (final JsonProcessingException e) {
      throw new OPDSParseException(e);
    } catch (final IOException e) {
      throw new OPDSParseException(e);
    }
  }

  @Override
  public OPDSAcquisitionFeed parseAcquisitionFeedFromStream(
    final InputStream s)
    throws OPDSParseException {
    try {
      final ObjectMapper jom = new ObjectMapper();
      return this.parseAcquisitionFeed(
        JSONParserUtilities.checkObject(
          null, jom.readTree(s)));
    } catch (final JsonProcessingException e) {
      throw new OPDSParseException(e);
    } catch (final IOException e) {
      throw new OPDSParseException(e);
    }
  }
}
