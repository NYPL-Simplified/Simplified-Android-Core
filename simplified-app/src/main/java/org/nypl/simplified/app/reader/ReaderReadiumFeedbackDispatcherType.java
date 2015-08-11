package org.nypl.simplified.app.reader;

import java.net.URI;

/**
 * The type of Readium function dispatchers.
 *
 * A dispatcher expects non-hierarchical URIs that use a {@code readium}
 * scheme.
 */

public interface ReaderReadiumFeedbackDispatcherType
{
  /**
   * Dispatch the URI to the given listener.
   *
   * @param uri The URI
   * @param l   The listener
   */

  void dispatch(
    URI uri,
    ReaderReadiumFeedbackListenerType l);
}
