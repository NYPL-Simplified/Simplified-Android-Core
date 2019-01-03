package org.nypl.simplified.books.book_registry;

/**
 * Status matcher.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface BookStatusDownloadingMatcherType<A, E extends Exception>
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

  A onBookStatusDownloadFailed(
    BookStatusDownloadFailed s)
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

  A onBookStatusDownloadInProgress(
    BookStatusDownloadInProgress s)
    throws E;
}
