package org.nypl.simplified.opds.core;

import java.net.URI;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * <p>
 * The type of asynchronous OPDS feed loaders.
 * </p>
 * <p>
 * Implementations are required to be able to accept requests from any number
 * of threads simultaneously.
 * </p>
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

  ListenableFuture<OPDSFeedType> fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p);
}
