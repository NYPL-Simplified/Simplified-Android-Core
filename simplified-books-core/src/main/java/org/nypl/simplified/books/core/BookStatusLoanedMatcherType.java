package org.nypl.simplified.books.core;

/**
 * Status matcher.
 *
 * @param <A>
 *          The type of returned values
 * @param <E>
 *          The type of raised exceptions
 */

public interface BookStatusLoanedMatcherType<A, E extends Exception>
{
  A onBookStatusDownloaded(
    BookStatusDownloaded d)
    throws E;

  A onBookStatusDownloading(
    BookStatusDownloadingType o)
    throws E;

  A onBookStatusLoaned(
    BookStatusLoaned o)
    throws E;

  A onBookStatusRequestingDownload(
    BookStatusRequestingDownload d)
    throws E;
}
