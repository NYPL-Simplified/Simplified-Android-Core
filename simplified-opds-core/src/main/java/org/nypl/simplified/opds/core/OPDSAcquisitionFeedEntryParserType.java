package org.nypl.simplified.opds.core;

import org.w3c.dom.Element;

import java.io.InputStream;
import java.net.URI;

/**
 * The type of feed entry parsers.
 */

public interface OPDSAcquisitionFeedEntryParserType {

  /**
   * Parse the feed entry represented by the XML element {@code e}.
   *
   * @param source The source URI
   * @param e      The XML element
   * @return A parsed feed entry
   * @throws OPDSParseException On errors
   */

  OPDSAcquisitionFeedEntry parseEntry(
    final URI source,
    final Element e)
    throws OPDSParseException;

  /**
   * Parse the feed entry represented by the given stream {@code s}.
   *
   * @param source The source URI
   * @param s      The entry stream
   * @return A parsed feed entry
   * @throws OPDSParseException On errors
   */

  OPDSAcquisitionFeedEntry parseEntryStream(
    final URI source,
    final InputStream s)
    throws OPDSParseException;
}
