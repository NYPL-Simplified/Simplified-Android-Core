package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p> The type of serializers that produce OPDS authentication documents.</p>
 */

public interface OPDSAuthenticationDocumentSerializerType
{
  /**
   * Serialize the given feed to JSON.
   *
   * @param e The feed
   *
   * @return JSON
   *
   * @throws OPDSSerializationException On serialization errors
   */

  ObjectNode serializeDocument(
    OPDSAuthenticationDocument e)
    throws OPDSSerializationException;


  /**
   * Serialize the given JSON to the given output stream.
   *
   * @param d  The JSON
   * @param os The output stream
   *
   * @throws IOException On I/O errors
   */

  void serializeToStream(
    ObjectNode d,
    OutputStream os)
    throws IOException;
}
