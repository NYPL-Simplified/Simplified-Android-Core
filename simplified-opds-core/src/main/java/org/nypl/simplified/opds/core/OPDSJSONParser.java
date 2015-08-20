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
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.opds.core.OPDSAcquisition.Type;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
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

  private static ObjectNode checkObject(
    final @Nullable String key,
    final JsonNode n)
    throws OPDSParseException
  {
    switch (n.getNodeType()) {
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case NUMBER:
      case POJO:
      case STRING: {
        final StringBuilder sb = new StringBuilder(128);
        if (key != null) {
          sb.append("Expected: A key '");
          sb.append(key);
          sb.append("' with a value of type Object\n");
          sb.append("Got: A value of type ");
          sb.append(n.getNodeType());
          sb.append("\n");
        } else {
          sb.append("Expected: A value of type Object\n");
          sb.append("Got: A value of type ");
          sb.append(n.getNodeType());
          sb.append("\n");
        }

        final String m = NullCheck.notNull(sb.toString());
        throw new OPDSParseException(m);
      }
      case OBJECT: {
        return (ObjectNode) n;
      }
    }

    throw new UnreachableCodeException();
  }

  private static ArrayNode getArray(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    final JsonNode n = OPDSJSONParser.getNode(s, key);
    switch (n.getNodeType()) {
      case ARRAY: {
        return (ArrayNode) n;
      }
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case NUMBER:
      case POJO:
      case STRING:
      case OBJECT: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type Array\n");
        sb.append("Got: A value of type ");
        sb.append(n.getNodeType());
        sb.append("\n");
        final String m = NullCheck.notNull(sb.toString());
        throw new OPDSParseException(m);
      }
    }

    throw new UnreachableCodeException();
  }

  private static boolean getBoolean(
    final ObjectNode o,
    final String key)
    throws OPDSParseException
  {
    final JsonNode v = OPDSJSONParser.getNode(o, key);
    switch (v.getNodeType()) {
      case ARRAY:
      case BINARY:
      case MISSING:
      case NULL:
      case OBJECT:
      case POJO:
      case STRING:
      case NUMBER: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type Boolean\n");
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append("\n");
        final String m = NullCheck.notNull(sb.toString());
        throw new OPDSParseException(m);
      }
      case BOOLEAN: {
        return v.asBoolean();
      }
    }

    throw new UnreachableCodeException();
  }

  private static int getInteger(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    final JsonNode v = OPDSJSONParser.getNode(n, key);
    switch (v.getNodeType()) {
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case OBJECT:
      case POJO:
      case STRING: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type Integer\n");
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append("\n");
        final String m = NullCheck.notNull(sb.toString());
        throw new OPDSParseException(m);
      }
      case NUMBER: {
        return v.asInt();
      }
    }

    throw new UnreachableCodeException();
  }

  private static JsonNode getNode(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(key);

    if (s.has(key)) {
      return NullCheck.notNull(s.get(key));
    }

    final StringBuilder sb = new StringBuilder(128);
    sb.append("Expected: A key '");
    sb.append(key);
    sb.append("'\n");
    sb.append("Got: nothing\n");
    final String m = NullCheck.notNull(sb.toString());
    throw new OPDSParseException(m);
  }

  private static ObjectNode getObject(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    final JsonNode n = OPDSJSONParser.getNode(s, key);
    return OPDSJSONParser.checkObject(key, n);
  }

  private static OptionType<ObjectNode> getObjectOptional(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    if (s.has(key)) {
      return Option.some(OPDSJSONParser.getObject(s, key));
    }
    return Option.none();
  }

  private static String getString(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    final JsonNode v = OPDSJSONParser.getNode(s, key);
    switch (v.getNodeType()) {
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case NUMBER:
      case OBJECT:
      case POJO: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A key '");
        sb.append(key);
        sb.append("' with a value of type String\n");
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append("\n");
        final String m = NullCheck.notNull(sb.toString());
        throw new OPDSParseException(m);
      }
      case STRING: {
        return NullCheck.notNull(v.asText());
      }
    }

    throw new UnreachableCodeException();
  }

  private static OptionType<Integer> getIntegerOptional(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    if (n.has(key)) {
      return Option.some(OPDSJSONParser.getInteger(n, key));
    }
    return Option.none();
  }

  private static OptionType<String> getStringOptional(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    if (n.has(key)) {
      return Option.some(OPDSJSONParser.getString(n, key));
    }
    return Option.none();
  }

  private static Calendar getTimestamp(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    try {
      return OPDSRFC3339Formatter.parseRFC3339Date(
        OPDSJSONParser.getString(
          s, key));
    } catch (final ParseException e) {
      final String m = NullCheck.notNull(
        String.format(
          "Could not parse RFC3999 date for key '%s'", key));
      throw new OPDSParseException(m, e);
    }
  }

  private static OptionType<Calendar> getTimestampOptional(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    if (n.has(key)) {
      return Option.some(OPDSJSONParser.getTimestamp(n, key));
    }
    return Option.none();
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
      final Type in_type = Type.valueOf(OPDSJSONParser.getString(o, "type"));
      final URI in_uri = new URI(OPDSJSONParser.getString(o, "uri"));
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
      final ObjectNode n = OPDSJSONParser.getObject(node, "loaned");
      final Calendar in_start_date =
        OPDSJSONParser.getTimestamp(n, "start_date");
      final OptionType<Calendar> in_end_date =
        OPDSJSONParser.getTimestampOptional(n, "end_date");
      return OPDSAvailabilityLoaned.get(in_start_date, in_end_date);
    }

    if (node.has("held")) {
      final ObjectNode n = OPDSJSONParser.getObject(node, "held");
      final Calendar in_start_date =
        OPDSJSONParser.getTimestamp(n, "start_date");
      final OptionType<Integer> in_position =
        OPDSJSONParser.getIntegerOptional(n, "position");
      final OptionType<Calendar> in_end_date =
        OPDSJSONParser.getTimestampOptional(n, "end_date");
      return OPDSAvailabilityHeld.get(in_start_date, in_position, in_end_date);
    }

    if (node.has("reserved")) {
      final ObjectNode n = OPDSJSONParser.getObject(node, "reserved");
      final OptionType<Calendar> in_end_date =
        OPDSJSONParser.getTimestampOptional(n, "end_date");
      return OPDSAvailabilityReserved.get(in_end_date);
    }

    if (node.has("open_access")) {
      return OPDSAvailabilityOpenAccess.get();
    }

    throw new OPDSParseException("Expected availability information");
  }

  private static OPDSCategory parseCategory(
    final JsonNode jn)
    throws OPDSParseException
  {
    NullCheck.notNull(jn);
    final ObjectNode o = OPDSJSONParser.checkObject(null, jn);
    final String in_term = OPDSJSONParser.getString(o, "term");
    final String in_scheme = OPDSJSONParser.getString(o, "scheme");
    final OptionType<String> in_label =
      OPDSJSONParser.getStringOptional(o, "label");
    return new OPDSCategory(in_term, in_scheme, in_label);
  }

  @Override public OPDSAcquisitionFeed parseAcquisitionFeed(
    final ObjectNode s)
    throws OPDSParseException
  {
    NullCheck.notNull(s);

    try {
      final URI in_uri = new URI(OPDSJSONParser.getString(s, "uri"));
      final String in_id = OPDSJSONParser.getString(s, "id");
      final Calendar in_updated = OPDSJSONParser.getTimestamp(s, "updated");
      final String in_title = OPDSJSONParser.getString(s, "title");

      final OPDSAcquisitionFeedBuilderType fb =
        OPDSAcquisitionFeed.newBuilder(in_uri, in_id, in_updated, in_title);

      fb.setNextOption(
        OPDSJSONParser.getStringOptional(s, "next").mapPartial(
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
        OPDSJSONParser.getObjectOptional(s, "search").mapPartial(
          new PartialFunctionType<ObjectNode, OPDSSearchLink,
            OPDSParseException>()
          {
            @Override public OPDSSearchLink call(
              final ObjectNode o)
              throws OPDSParseException
            {
              try {
                final String in_search_type =
                  OPDSJSONParser.getString(o, "type");
                final URI in_search_uri =
                  new URI(OPDSJSONParser.getString(o, "uri"));
                return new OPDSSearchLink(in_search_type, in_search_uri);
              } catch (final URISyntaxException e) {
                throw new OPDSParseException(e);
              }
            }
          }));

      {
        final ArrayNode fs = OPDSJSONParser.getArray(s, "facets");
        for (int index = 0; index < fs.size(); ++index) {
          final ObjectNode o = OPDSJSONParser.checkObject(null, fs.get(index));
          final boolean in_facet_active =
            OPDSJSONParser.getBoolean(o, "active");
          final URI in_facet_uri = new URI(OPDSJSONParser.getString(o, "uri"));
          final String in_facet_group = OPDSJSONParser.getString(o, "group");
          final String in_facet_title = OPDSJSONParser.getString(o, "title");
          fb.addFacet(
            new OPDSFacet(
              in_facet_active, in_facet_uri, in_facet_group, in_facet_title));
        }
      }

      {
        final ArrayNode fs = OPDSJSONParser.getArray(s, "entries");
        for (int index = 0; index < fs.size(); ++index) {
          final ObjectNode o = OPDSJSONParser.checkObject(null, fs.get(index));
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

    final String in_id = OPDSJSONParser.getString(s, "id");
    final String in_title = OPDSJSONParser.getString(s, "title");
    final Calendar in_updated = OPDSJSONParser.getTimestamp(s, "updated");

    final OPDSAvailabilityType in_availability =
      OPDSJSONParser.parseAvailability(
        OPDSJSONParser.getObject(
          s, "availability"));

    final OPDSAcquisitionFeedEntryBuilderType fb =
      OPDSAcquisitionFeedEntry.newBuilder(
        in_id, in_title, in_updated, in_availability);

    {
      final ArrayNode a = OPDSJSONParser.getArray(s, "authors");
      for (int index = 0; index < a.size(); ++index) {
        fb.addAuthor(a.get(index).asText());
      }
    }

    {
      final ArrayNode a = OPDSJSONParser.getArray(s, "acquisitions");
      for (int index = 0; index < a.size(); ++index) {
        fb.addAcquisition(
          OPDSJSONParser.parseAcquisition(
            OPDSJSONParser.checkObject(null, a.get(index))));
      }
    }

    {
      final ArrayNode a = OPDSJSONParser.getArray(s, "categories");
      for (int index = 0; index < a.size(); ++index) {
        fb.addCategory(OPDSJSONParser.parseCategory(a.get(index)));
      }
    }

    {
      final ArrayNode a = OPDSJSONParser.getArray(s, "groups");
      for (int index = 0; index < a.size(); ++index) {
        try {
          final ObjectNode jo = OPDSJSONParser.checkObject(null, a.get(index));
          final URI in_uri = new URI(OPDSJSONParser.getString(jo, "uri"));
          final String in_name = OPDSJSONParser.getString(jo, "name");
          fb.addGroup(in_uri, in_name);
        } catch (final URISyntaxException e) {
          throw new OPDSParseException(e);
        }
      }
    }

    {
      final OptionType<String> o = OPDSJSONParser.getStringOptional(s, "cover");
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
        OPDSJSONParser.getStringOptional(s, "thumbnail");
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

    fb.setPublishedOption(OPDSJSONParser.getTimestampOptional(s, "published"));
    fb.setPublisherOption(OPDSJSONParser.getStringOptional(s, "publisher"));
    fb.setSummaryOption(OPDSJSONParser.getStringOptional(s, "summary"));
    return fb.build();
  }

  @Override public OPDSAcquisitionFeedEntry parseAcquisitionFeedEntryFromStream(
    final InputStream s)
    throws OPDSParseException
  {
    try {
      final ObjectMapper jom = new ObjectMapper();
      return this.parseAcquisitionFeedEntry(
        OPDSJSONParser.checkObject(
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
        OPDSJSONParser.checkObject(
          null, jom.readTree(s)));
    } catch (final JsonProcessingException e) {
      throw new OPDSParseException(e);
    } catch (final IOException e) {
      throw new OPDSParseException(e);
    }
  }
}
