package org.nypl.simplified.books.core;

/**
 * A matcher for values of {@link BookStatusLoanedType}.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface BookStatusLoanedMatcherType<A, E extends Exception>
{
  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusDownloaded(
    BookStatusDownloaded s)
    throws E;

  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusDownloading(
    BookStatusDownloadingType s)
    throws E;

  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusLoaned(
    BookStatusLoaned s)
    throws E;

  /**
   * Match a status value.
   *
   * @param s The status value
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onBookStatusRequestingDownload(
    BookStatusRequestingDownload s)
    throws E;
}
