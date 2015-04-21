package org.nypl.simplified.books.core;

/**
 * The type of listeners for book snapshot operations.
 */

public interface BookSnapshotListenerType
{
  /**
   * Producing a snapshot failed.
   */

  void onBookSnapshotFailure(
    Throwable x);

  /**
   * Producing a snapshot succeeded.
   */

  void onBookSnapshotSuccess(
    BookID id,
    BookSnapshot snap);
}
