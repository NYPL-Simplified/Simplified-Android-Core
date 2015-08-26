package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Utilities for implementing JSON serializers.
 */

public final class OPDSJSONSerializerUtilities
{
  private OPDSJSONSerializerUtilities()
  {
    throw new UnreachableCodeException();
  }

  static void serialize(
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
}
