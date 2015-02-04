package org.nypl.simplified.opds.core;

import java.io.InputStream;

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
  OPDSFeedType parse(
    final InputStream s)
    throws OPDSFeedParseException;
}
