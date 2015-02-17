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
  /**
   * @return The number of URIs on the stack. Will be 0 iff no URIs have been
   *         pushed prior to calling.
   */

  int catalogFeedsCount();

  /**
   * @return The URI at the top of the stack.
   */

  URI catalogFeedsPeek();

  /**
   * Pop a URI from the stack.
   *
   * @return The URI removed from the stack.
   */

  URI catalogFeedsPop();

  /**
   * Push a URI onto the stack.
   */

  void catalogFeedsPush(
    URI uri);
}
