package org.nypl.simplified.books.core;

/**
 * The type of listeners for book snapshot operations.
 */

public interface BookSnapshotListenerType
{
  /**
   * Producing a snapshot failed.
   *
   * @param x The exception raised
   */

  void onBookSnapshotFailure(
    Throwable x);

  /**
   * Producing a snapshot succeeded.
   *
   * @param id   The book ID
   * @param snap The book snapshot
   */

  void onBookSnapshotSuccess(
    BookID id,
    BookSnapshot snap);
}
