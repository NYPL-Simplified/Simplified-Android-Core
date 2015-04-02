package org.nypl.simplified.books.core;

public interface BookStatusMatcherType<A, E extends Exception>
{
  A onBookStatusCancelled(
    BookStatusCancelled c)
    throws E;

  A onBookStatusDone(
    BookStatusDone d)
    throws E;

  A onBookStatusDownloading(
    BookStatusDownloading d)
    throws E;

  A onBookStatusFailed(
    BookStatusFailed f)
    throws E;

  A onBookStatusOwned(
    BookStatusOwned o)
    throws E;

  A onBookStatusPaused(
    BookStatusPaused p)
    throws E;
}
