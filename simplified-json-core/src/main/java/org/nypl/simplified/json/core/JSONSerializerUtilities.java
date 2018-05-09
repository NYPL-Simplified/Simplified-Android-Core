package org.nypl.simplified.json.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Utilities for implementing JSON serializers.
 */

public final class JSONSerializerUtilities
{
  private JSONSerializerUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Serialize the given object node to the given stream.
   *
   * @param d  The node
   * @param os The output stream
   *
   * @throws IOException On I/O errors
   */

  public static void serialize(
    final ObjectNode d,
    final OutputStream os)
    throws IOException
  {
    NullCheck.notNull(d);
    NullCheck.notNull(os);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectWriter jw = jom.writerWithDefaultPrettyPrinter();
    jw.writeValue(os, d);
  }

  /**
   * Serialize the given object node to a string.
   *
   * @param d The node
   *
   * @return Pretty-printed JSON
   *
   * @throws IOException On I/O errors
   */

  public static String serializeToString(
    final ObjectNode d)
    throws IOException
  {
    NullCheck.notNull(d);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectWriter jw = jom.writerWithDefaultPrettyPrinter();
    return jw.writeValueAsString(d);
  }
}
