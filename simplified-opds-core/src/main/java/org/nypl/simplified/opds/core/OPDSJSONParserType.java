package org.nypl.simplified.opds.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;

/**
 * <p> The type of parsers that consume simple JSON in a private format. </p>
 */

public interface OPDSJSONParserType
{
  /**
   * Parse an acquisition feed from the given JSON.
   *
   * @param s The JSON
   *
   * @return An acquisition feed
   *
   * @throws OPDSParseException If the given JSON does not represent a feed
   */

  OPDSAcquisitionFeed parseAcquisitionFeed(
    ObjectNode s)
    throws OPDSParseException;

  /**
   * Parse an acquisition feed entry from the given JSON.
   *
   * @param s The JSON
   *
   * @return An acquisition feed entry
   *
   * @throws OPDSParseException If the given JSON does not represent a feed
   *                            entry
   */

  OPDSAcquisitionFeedEntry parseAcquisitionFeedEntry(
    ObjectNode s)
    throws OPDSParseException;

  /**
   * Parse an acquisition feed entry from the JSON data on given stream
   *
   * @param s The stream
   *
   * @return An acquisition feed entry
   *
   * @throws OPDSParseException If the given JSON stream does not represent a
   *                            feed entry
   */

  OPDSAcquisitionFeedEntry parseAcquisitionFeedEntryFromStream(
    InputStream s)
    throws OPDSParseException;

  /**
   * Parse an acquisition feed  from the JSON data on given stream
   *
   * @param s The stream
   *
   * @return An acquisition feed
   *
   * @throws OPDSParseException If the given JSON stream does not represent a
   *                            feed
   */

  OPDSAcquisitionFeed parseAcquisitionFeedFromStream(
    InputStream s)
    throws OPDSParseException;
}
