package org.nypl.simplified.opds.core;

import java.io.InputStream;
import java.net.URI;

/**
 * <p>
 * The type of parsers that consume {@link InputStream} values and produce
 * feeds.
 * </p>
 * <p>
 * Implementations are required to be able to accept requests from any number
 * of threads simultaneously.
 * </p>
 */

public interface OPDSFeedParserType
{
  /**
   * Parse the feed associated with the given stream <code>s</code>. The feed
   * is assumed to exist at <code>uri</code>.
   * 
   * @param uri
   *          The URI of the feed
   * @param s
   *          The input stream
   * @return A parsed feed
   * @throws OPDSFeedParseException
   *           On errors
   */

  OPDSFeedType parse(
    final URI uri,
    final InputStream s)
    throws OPDSFeedParseException;
}
