package org.nypl.simplified.opds.core;

import java.io.InputStream;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * <p>
 * The type of parsers that consume simple JSON in a private format.
 * </p>
 */

public interface OPDSJSONParserType
{
  OPDSAcquisitionFeed parseAcquisitionFeed(
    ObjectNode s)
    throws OPDSParseException;

  OPDSAcquisitionFeedEntry parseAcquisitionFeedEntry(
    ObjectNode s)
    throws OPDSParseException;

  OPDSAcquisitionFeedEntry parseAcquisitionFeedEntryFromStream(
    InputStream s)
    throws OPDSParseException;

  OPDSAcquisitionFeed parseAcquisitionFeedFromStream(
    InputStream s)
    throws OPDSParseException;
}
