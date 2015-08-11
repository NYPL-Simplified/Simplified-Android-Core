package org.nypl.simplified.opds.core;

import org.w3c.dom.Element;

import java.io.InputStream;

/**
 * The type of feed entry parsers.
 */

public interface OPDSAcquisitionFeedEntryParserType
{
  /**
   * Parse the feed entry represented by the XML element <tt>e</tt>.
   *
   * @param e The XML element
   *
   * @return A parsed feed entry
   *
   * @throws OPDSParseException On errors
   */

  OPDSAcquisitionFeedEntry parseEntry(
    final Element e)
    throws OPDSParseException;

  /**
   * Parse the feed entry represented by the given stream <tt>s</tt>.
   *
   * @param s The entry stream
   *
   * @return A parsed feed entry
   *
   * @throws OPDSParseException On errors
   */

  OPDSAcquisitionFeedEntry parseEntryStream(
    final InputStream s)
    throws OPDSParseException;
}
