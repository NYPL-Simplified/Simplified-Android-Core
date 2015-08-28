package org.nypl.simplified.json.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.rfc3339.core.RFC3339Formatter;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Calendar;

/**
 * <p>Utility functions for deserializing elements from JSON.</p>
 *
 * <p>The functions take a strict approach: Types are checked upon key retrieval
 * and exceptions are raised if the type is not exactly as expected.</p>
 */

public final class JSONParserUtilities
{
  private JSONParserUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Check that {@code n} is an object.
   *
   * @param key An optional advisory key to be used in error messages
   * @param n   A node
   *
   * @return {@code n} as an {@link ObjectNode}
   *
   * @throws JSONParseException On type errors
   */

  public static ObjectNode checkObject(
    final @Nullable String key,
    final JsonNode n)
    throws JSONParseException
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
        throw new JSONParseException(m);
      }
      case OBJECT: {
        return (ObjectNode) n;
      }
    }

    throw new UnreachableCodeException();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return An array from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static ArrayNode getArray(
    final ObjectNode s,
    final String key)
    throws JSONParseException
  {
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
   * @param o   A node
   *
   * @return A boolean value from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static boolean getBoolean(
    final ObjectNode o,
    final String key)
    throws JSONParseException
  {
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
   *
   * @return An integer value from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static int getInteger(
    final ObjectNode n,
    final String key)
    throws JSONParseException
  {
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
   * @param s   A node
   *
   * @return An arbitrary json node from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static JsonNode getNode(
    final ObjectNode s,
    final String key)
    throws JSONParseException
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
    throw new JSONParseException(m);
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return An object value from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static ObjectNode getObject(
    final ObjectNode s,
    final String key)
    throws JSONParseException
  {
    NullCheck.notNull(s);
    NullCheck.notNull(key);

    final JsonNode n = JSONParserUtilities.getNode(s, key);
    return JSONParserUtilities.checkObject(key, n);
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return An object value from key {@code key}, if the key exists
   *
   * @throws JSONParseException On type errors
   */

  public static OptionType<ObjectNode> getObjectOptional(
    final ObjectNode s,
    final String key)
    throws JSONParseException
  {
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
   *
   * @return A string value from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static String getString(
    final ObjectNode s,
    final String key)
    throws JSONParseException
  {
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
   *
   * @return An integer value from key {@code key}, if the key exists
   *
   * @throws JSONParseException On type errors
   */

  public static OptionType<Integer> getIntegerOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException
  {
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
   *
   * @return A string value from key {@code key}, if the key exists
   *
   * @throws JSONParseException On type errors
   */

  public static OptionType<String> getStringOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    if (n.has(key)) {
      return Option.some(JSONParserUtilities.getString(n, key));
    }
    return Option.none();
  }

  /**
   * @param key A key assumed to be holding a value
   * @param s   A node
   *
   * @return A timestamp value from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static Calendar getTimestamp(
    final ObjectNode s,
    final String key)
    throws JSONParseException
  {
    NullCheck.notNull(s);
    NullCheck.notNull(key);

    try {
      return RFC3339Formatter.parseRFC3339Date(
        JSONParserUtilities.getString(s, key));
    } catch (final ParseException e) {
      final String m = NullCheck.notNull(
        String.format("Could not parse RFC3999 date for key '%s'", key));
      throw new JSONParseException(m, e);
    }
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   *
   * @return A timestamp value from key {@code key}, if the key exists
   *
   * @throws JSONParseException On type errors
   */

  public static OptionType<Calendar> getTimestampOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException
  {
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
   *
   * @return A URI value from key {@code key}, if the key exists
   *
   * @throws JSONParseException On type errors
   */

  public static OptionType<URI> getURIOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    return JSONParserUtilities.getStringOptional(n, key).mapPartial(
      new PartialFunctionType<String, URI, JSONParseException>()
      {
        @Override public URI call(final String x)
          throws JSONParseException
        {
          try {
            return new URI(x);
          } catch (final URISyntaxException e) {
            throw new JSONParseException(e);
          }
        }
      });
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   *
   * @return A URI value from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static URI getURI(
    final ObjectNode n,
    final String key)
    throws JSONParseException
  {
    NullCheck.notNull(n);
    NullCheck.notNull(key);

    try {
      return new URI(JSONParserUtilities.getString(n, key));
    } catch (final URISyntaxException e) {
      throw new JSONParseException(e);
    }
  }

  /**
   * @param key A key assumed to be holding a value
   * @param n   A node
   * @param v   A default value
   *
   * @return A boolean from key {@code key}, or {@code v} if the key does not
   * exist
   *
   * @throws JSONParseException On type errors
   */

  public static boolean getBooleanDefault(
    final ObjectNode n,
    final String key,
    final boolean v)
    throws JSONParseException
  {
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
   *
   * @return A big integer value from key {@code key}, if the key exists
   *
   * @throws JSONParseException On type errors
   */

  public static OptionType<BigInteger> getBigIntegerOptional(
    final ObjectNode n,
    final String key)
    throws JSONParseException
  {
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
   *
   * @return A big integer value from key {@code key}
   *
   * @throws JSONParseException On type errors
   */

  public static BigInteger getBigInteger(
    final ObjectNode n,
    final String key)
    throws JSONParseException
  {
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
