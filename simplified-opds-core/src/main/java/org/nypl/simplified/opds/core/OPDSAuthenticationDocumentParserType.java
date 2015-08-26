package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;

/**
 * The type of OPDS authentication document parsers.
 */

public interface OPDSAuthenticationDocumentParserType
{
  /**
   * Parse an authentication document from the given JSON.
   *
   * @param s The JSON
   *
   * @return An authentication document
   *
   * @throws OPDSParseException If the given JSON does not represent a valid
   *                            document
   */

  OPDSAuthenticationDocument parseFromNode(
    ObjectNode s)
    throws OPDSParseException;

  /**
   * Parse an authentication document from the given stream.
   *
   * @param s The stream
   *
   * @return An authentication document
   *
   * @throws OPDSParseException If the given stream does not represent a valid
   *                            document
   */

  OPDSAuthenticationDocument parseFromStream(
    InputStream s)
    throws OPDSParseException;
}
