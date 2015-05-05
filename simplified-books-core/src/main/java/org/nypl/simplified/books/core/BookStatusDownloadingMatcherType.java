package org.nypl.simplified.books.core;

/**
 * Status matcher.
 *
 * @param <A>
 *          The type of returned values
 * @param <E>
 *          The type of raised exceptions
 */

public interface BookStatusDownloadingMatcherType<A, E extends Exception>
{
  A onBookStatusDownloadCancelled(
    BookStatusDownloadCancelled c)
    throws E;

  A onBookStatusDownloaded(
    BookStatusDownloaded d)
    throws E;

  A onBookStatusDownloadFailed(
    BookStatusDownloadFailed f)
    throws E;

  A onBookStatusDownloadingPaused(
    BookStatusDownloadingPaused p)
    throws E;

  A onBookStatusDownloadInProgress(
    BookStatusDownloadInProgress d)
    throws E;
}
