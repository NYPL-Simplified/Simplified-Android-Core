package org.nypl.simplified.books.core;

public interface BookSnapshotListenerType
{
  void onBookSnapshotFailure(
    Throwable x);

  void onBookSnapshotSuccess(
    BookID id,
    BookSnapshot snap);
}
