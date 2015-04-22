package org.nypl.simplified.app.reader;

import java.net.URI;

/**
 * The type of Simplified function dispatchers.
 *
 * A dispatcher expects non-hierarchical URIs that use a <tt>simplified</tt>
 * scheme.
 */

public interface ReaderSimplifiedFeedbackDispatcherType
{
  /**
   * Dispatch the URI to the given listener.
   */

  void dispatch(
    URI uri,
    ReaderSimplifiedFeedbackListenerType l);
}
