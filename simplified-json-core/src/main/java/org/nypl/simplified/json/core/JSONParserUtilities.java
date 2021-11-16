package org.nypl.simplified.json.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * <p>Utility functions for deserializing elements from JSON.</p>
 * <p>
 * <p>The functions take a strict approach: Types are checked upon key retrieval
 * and exceptions are raised if the type is not exactly as expected.</p>
 */

public final class JSONParserUtilities {
  private JSONParserUtilities() {
    throw new UnreachableCodeException();
  }

  /**
   * Check that {@code n} is an object.
   *
   * @param key An optional advisory key to be used in error messages
   * @param n   A node
   * @return {@code n} as an {@link ObjectNode}
   * @throws JSONParseException On type errors
   */

  public static ObjectNode checkObject(
    final @Nullable String key,
    final JsonNode n)
    throws JSONParseException {

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
        throw new JSONParseException(m);
      }
      case OBJECT: {
        return (ObjectNode) n;
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * Check that {@code n} is an array.
   *
   * @param key An optional advisory key to be used in error messages
   * @param n   A node
   * @return {@code n} as an {@link ObjectNode}
   * @throws JSONParseException On type errors
   */

  public static ArrayNode checkArray(
    final @Nullable String key,
    final JsonNode n)
    throws JSONParseException {

    NullCheck.notNull(n);

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
      case OBJECT:
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
        throw new JSONParseException(m);
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * Check that {@code n} is a string.
   *
   * @param n A node
   * @return {@code n} as a String
   * @throws JSONParseException On type errors
   */

  public static String checkString(
    final JsonNode n)
    throws JSONParseException {

    NullCheck.notNull(n);

    switch (n.getNodeType()) {
      case STRING: {
        return n.asText();
      }
      case ARRAY:
      case BINARY:
      case BOOLEAN:
      case MISSING:
      case NULL:
      case NUMBER:
      case POJO:
      case OBJECT: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Expected: A value of type String\n");
        sb.append("Got: A value of type ");
        sb.append(n.getNodeType());
        sb.append("\n");
        throw new JSONParseException(NullCheck.notNull(sb.toString()));
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   * @return An array from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static ArrayNode getArray(
    final ObjectNode s,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(s);
    NullCheck.notNull(key);

    final JsonNode n = JSONParserUtilities.getNode(s, key);
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
        throw new JSONParseException(m);
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   * @return An array from key {@code key}, or null if the key is not present
   * @throws JSONParseException On type errors
   */

  public static ArrayNode getArrayOrNull(
    final ObjectNode s,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(s);
    NullCheck.notNull(key);

    if (s.has(key)) {
      return getArray(s, key);
    }
    return null;
  }

  /**
   * @param key A key assumed to be holding a value
   * @param o   A node
   * @return A boolean value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static boolean getBoolean(
    final ObjectNode o,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(o);
    NullCheck.notNull(key);

    final JsonNode v = JSONParserUtilities.getNode(o, key);
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
        throw new JSONParseException(m);
      }
      case BOOLEAN: {
        return v.asBoolean();
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return An integer value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static int getInteger(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    final JsonNode v = JSONParserUtilities.getNode(n, key);
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
        throw new JSONParseException(m);
      }
      case NUMBER: {
        return v.asInt();
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A double value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static double getDouble(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    final JsonNode v = JSONParserUtilities.getNode(n, key);
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
        sb.append("' with a value of type Double\n");
        sb.append("Got: A value of type ");
        sb.append(v.getNodeType());
        sb.append("\n");
        final String m = NullCheck.notNull(sb.toString());
        throw new JSONParseException(m);
      }
      case NUMBER: {
        return v.asDouble();
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   * @return An arbitrary json node from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static JsonNode getNode(
    final ObjectNode s,
    final String key)
    throws JSONParseException {

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
    throw new JSONParseException(m);
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   * @return An object value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static ObjectNode getObject(
    final ObjectNode s,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(s);
    NullCheck.notNull(key);

    final JsonNode n = JSONParserUtilities.getNode(s, key);
    return JSONParserUtilities.checkObject(key, n);
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   * @return An object value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static OptionType<ObjectNode> getObjectOptional(
    final ObjectNode s,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(s);
    NullCheck.notNull(key);

    if (s.has(key)) {
      return Option.some(JSONParserUtilities.getObject(s, key));
    }
    return Option.none();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   * @return An object value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static ObjectNode getObjectOrNull(
    final ObjectNode s,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(s);
    NullCheck.notNull(key);

    if (s.has(key)) {
      return JSONParserUtilities.getObject(s, key);
    }
    return null;
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   * @return A string value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static String getString(
    final ObjectNode s,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(s);
    NullCheck.notNull(key);

    final JsonNode v = JSONParserUtilities.getNode(s, key);
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
        throw new JSONParseException(m);
      }
      case STRING: {
        return NullCheck.notNull(v.asText());
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return An integer value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static OptionType<Integer> getIntegerOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(JSONParserUtilities.getInteger(n, key));
    }
    return Option.none();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return An integer value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static Integer getIntegerOrNull(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return JSONParserUtilities.getInteger(n, key);
    }
    return null;
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A string value from key {@code key}, if the key exists, or {@code default_value} otherwise.
   * @throws JSONParseException On type errors
   */

  public static int getIntegerDefault(
    final ObjectNode n,
    final String key,
    final int default_value)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    return getIntegerOptional(n, key).accept(
      new OptionVisitorType<Integer, Integer>() {
        @Override
        public Integer none(final None<Integer> none) {
          return default_value;
        }

        @Override
        public Integer some(final Some<Integer> some) {
          return some.get();
        }
      }).intValue();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return An double value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static OptionType<Double> getDoubleOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(JSONParserUtilities.getDouble(n, key));
    }
    return Option.none();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return An double value from key {@code key}, if the key exists, or {@code default_value} otherwise.
   * @throws JSONParseException On type errors
   */

  public static double getDoubleDefault(
    final ObjectNode n,
    final String key,
    final double default_value)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    return getDoubleOptional(n, key).accept(
      new OptionVisitorType<Double, Double>() {
        @Override
        public Double none(final None<Double> none) {
          return default_value;
        }

        @Override
        public Double some(final Some<Double> some) {
          return some.get();
        }
      });
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A string value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static OptionType<String> getStringOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      if (n.get(key).isNull()) {
        return Option.none();
      }
      return Option.some(JSONParserUtilities.getString(n, key));
    }
    return Option.none();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A string value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static String getStringOrNull(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      if (n.get(key).isNull()) {
        return null;
      }
      return JSONParserUtilities.getString(n, key);
    }
    return null;
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A string value from key {@code key}, if the key exists, or {@code default_value} otherwise.
   * @throws JSONParseException On type errors
   */

  public static String getStringDefault(
    final ObjectNode n,
    final String key,
    final String default_value)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    return getStringOptional(n, key).accept(
      new OptionVisitorType<String, String>() {
        @Override
        public String none(final None<String> none) {
          return default_value;
        }

        @Override
        public String some(final Some<String> some) {
          return some.get();
        }
      });
  }

  /**
   * @param s   A node
   * @param key A key assumed to be holding a value
   * @return A timestamp value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static DateTime getTimestamp(
    final ObjectNode s,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(s);
    NullCheck.notNull(key);

    try {
      return ISODateTimeFormat.dateTimeParser()
        .withZoneUTC()
        .parseDateTime(JSONParserUtilities.getString(s, key));
    } catch (final IllegalArgumentException e) {
      throw new JSONParseException(
        String.format("Could not parse RFC3999 date for key '%s'", key), e);
    }
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A timestamp value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static OptionType<DateTime> getTimestampOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(JSONParserUtilities.getTimestamp(n, key));
    }
    return Option.none();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A URI value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static OptionType<URI> getURIOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    return JSONParserUtilities.getStringOptional(n, key).mapPartial(
      x -> {
        try {
          return new URI(x);
        } catch (final URISyntaxException e) {
          throw new JSONParseException(e);
        }
      });
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A URI value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static URI getURIOrNull(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    OptionType<URI> opt = getURIOptional(n, key);
    if (opt.isSome()) {
      return ((Some<URI>) opt).get();
    } else {
      return null;
    }
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A URI value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static URI getURI(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    try {
      return new URI(JSONParserUtilities.getString(n, key).trim());
    } catch (final URISyntaxException e) {
      throw new JSONParseException(e);
    }
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A URI value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static URI getURIDefault(
    final ObjectNode n,
    final String key,
    final URI default_value)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);
    Objects.requireNonNull(default_value, "Default");

    return getURIOptional(n, key).accept(new OptionVisitorType<URI, URI>() {
      @Override
      public URI none(None<URI> n) {
        return default_value;
      }

      @Override
      public URI some(Some<URI> s) {
        return s.get();
      }
    });
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @param v   A default value
   * @return A boolean from key {@code key}, or {@code v} if the key does not
   * exist
   * @throws JSONParseException On type errors
   */

  public static boolean getBooleanDefault(
    final ObjectNode n,
    final String key,
    final boolean v)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return JSONParserUtilities.getBoolean(n, key);
    }
    return v;
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A big integer value from key {@code key}, if the key exists
   * @throws JSONParseException On type errors
   */

  public static OptionType<BigInteger> getBigIntegerOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(JSONParserUtilities.getBigInteger(n, key));
    }
    return Option.none();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @return A big integer value from key {@code key}
   * @throws JSONParseException On type errors
   */

  public static BigInteger getBigInteger(
    final ObjectNode n,
    final String key)
    throws JSONParseException {

    NullCheck.notNull(n);
    NullCheck.notNull(key);

    final JsonNode v = JSONParserUtilities.getNode(n, key);
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
        throw new JSONParseException(m);
      }
      case NUMBER: {
        try {
          return new BigInteger(v.asText());
        } catch (final NumberFormatException e) {
          throw new JSONParseException(e);
        }
      }
    }

    throw new UnreachableCodeException();
  }
}
