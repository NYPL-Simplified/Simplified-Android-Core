package org.nypl.simplified.opds.core;

import java.net.URI;
import java.util.concurrent.Future;

import com.io7m.jfunctional.Unit;

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
   * @return A future representing the loading in progress
   * @param uri
   *          The URI
   * @param p
   *          The listener to receive the feed
   */

  Future<Unit> fromURI(
    final URI uri,
    final OPDSFeedLoadListenerType p);
}
