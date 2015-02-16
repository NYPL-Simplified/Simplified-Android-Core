package org.nypl.simplified.app;

import java.net.URI;

/**
 * <p>
 * The type of the current stack of feed URIs leading to the current catalog
 * feed.
 * </p>
 * <p>
 * The stack is guaranteed non-empty; popping the last element and then
 * peeking at the stack returns the statically configured initial URI.
 * </p>
 */

public interface CatalogFeedStackType
{
  URI catalogFeedsPeek();

  void catalogFeedsPush(
    URI uri);

  URI catalogFeedsPop();

  int catalogFeedsCount();
}
