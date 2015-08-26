package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;

/**
 * Utility functions for deserializing elements from JSON.
 */

public final class OPDSJSONParserUtilities
{
  private OPDSJSONParserUtilities()
  {
    throw new UnreachableCodeException();
  }

  static ObjectNode checkObject(
    final @Nullable String key,
    final JsonNode n)
    throws OPDSParseException
  {
    NullCheck.notNull(n);

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

  static ArrayNode getArray(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(s);
    NullCheck.notNull(key);

    final JsonNode n = OPDSJSONParserUtilities.getNode(s, key);
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

  static boolean getBoolean(
    final ObjectNode o,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(o);
    NullCheck.notNull(key);

    final JsonNode v = OPDSJSONParserUtilities.getNode(o, key);
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

  static int getInteger(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    final JsonNode v = OPDSJSONParserUtilities.getNode(n, key);
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

  static JsonNode getNode(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(s);
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

  static ObjectNode getObject(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(s);
    NullCheck.notNull(key);

    final JsonNode n = OPDSJSONParserUtilities.getNode(s, key);
    return OPDSJSONParserUtilities.checkObject(key, n);
  }

  static OptionType<ObjectNode> getObjectOptional(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(s);
    NullCheck.notNull(key);

    if (s.has(key)) {
      return Option.some(OPDSJSONParserUtilities.getObject(s, key));
    }
    return Option.none();
  }

  static String getString(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(s);
    NullCheck.notNull(key);

    final JsonNode v = OPDSJSONParserUtilities.getNode(s, key);
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

  static OptionType<Integer> getIntegerOptional(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(OPDSJSONParserUtilities.getInteger(n, key));
    }
    return Option.none();
  }

  static OptionType<String> getStringOptional(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(OPDSJSONParserUtilities.getString(n, key));
    }
    return Option.none();
  }

  static Calendar getTimestamp(
    final ObjectNode s,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(s);
    NullCheck.notNull(key);

    try {
      return OPDSRFC3339Formatter.parseRFC3339Date(
        OPDSJSONParserUtilities.getString(
          s, key));
    } catch (final ParseException e) {
      final String m = NullCheck.notNull(
        String.format(
          "Could not parse RFC3999 date for key '%s'", key));
      throw new OPDSParseException(m, e);
    }
  }

  static OptionType<Calendar> getTimestampOptional(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(OPDSJSONParserUtilities.getTimestamp(n, key));
    }
    return Option.none();
  }

  static OptionType<URI> getURIOptional(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    return OPDSJSONParserUtilities.getStringOptional(n, key).mapPartial(
      new PartialFunctionType<String, URI, OPDSParseException>()
      {
        @Override public URI call(final String x)
          throws OPDSParseException
        {
          try {
            return new URI(x);
          } catch (final URISyntaxException e) {
            throw new OPDSParseException(e);
          }
        }
      });
  }

  static URI getURI(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    try {
      return new URI(OPDSJSONParserUtilities.getString(n, key));
    } catch (final URISyntaxException e) {
      throw new OPDSParseException(e);
    }
  }

  static boolean getBooleanDefault(
    final ObjectNode n,
    final String key,
    final boolean v)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return OPDSJSONParserUtilities.getBoolean(n, key);
    }
    return v;
  }

  static OptionType<BigInteger> getBigIntegerOptional(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(OPDSJSONParserUtilities.getBigInteger(n, key));
    }
    return Option.none();
  }

  static BigInteger getBigInteger(
    final ObjectNode n,
    final String key)
    throws OPDSParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    final JsonNode v = OPDSJSONParserUtilities.getNode(n, key);
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
        try {
          return new BigInteger(v.asText());
        } catch (final NumberFormatException e) {
          throw new OPDSParseException(e);
        }
      }
    }

    throw new UnreachableCodeException();
  }
}
