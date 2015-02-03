package org.nypl.simplified.opds.core;

import java.net.URI;

/**
 * The type of asynchronous OPDS feed loaders.
 */

public interface OPDSFeedLoaderType
{
  /**
   * Load the feed at <code>uri</code>, passing the resulting feed to
   * <code>p</code>.
   *
   * @param uri
   *          The URI
   * @param p
   *          The listener to receive the feed
   */

  void fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p);
}
