package org.nypl.simplified.books.core;

public interface BookSnapshotListenerType
{
  void onBookSnapshotSuccess(
    BookID id,
    BookSnapshot snap);

  void onBookSnapshotFailure(
    Throwable x);
}
