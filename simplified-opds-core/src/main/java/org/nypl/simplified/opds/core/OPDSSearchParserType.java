package org.nypl.simplified.opds.core;

import java.io.InputStream;
import java.net.URI;

/**
 * <p>
 * The type of parsers that consume {@link InputStream} values and produce
 * search descriptions.
 * </p>
 * <p>
 * Implementations are required to be able to accept requests from any number
 * of threads simultaneously.
 * </p>
 */

public interface OPDSSearchParserType
{
  /**
   * Parse the search description associated with the given stream
   * <code>s</code>. The description is assumed to exist at <code>uri</code>.
   *
   * @param uri
   *          The URI of the description
   * @param s
   *          The input stream
   * @return A parsed description
   * @throws OPDSParseException
   *           On errors
   */

  OPDSOpenSearch1_1 parse(
    final URI uri,
    final InputStream s)
    throws OPDSParseException;
}
