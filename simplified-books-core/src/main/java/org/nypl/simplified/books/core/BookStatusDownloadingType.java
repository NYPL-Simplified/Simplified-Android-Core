package org.nypl.simplified.books.core;

import org.nypl.simplified.downloader.core.DownloadSnapshot;

/**
 * The given book is owned/loaned.
 */

public interface BookStatusDownloadingType extends BookStatusLoanedType
{
  /**
   * @return The most recently recorded download snapshot.
   */

  DownloadSnapshot getDownloadSnapshot();

  /**
   * Match on the type of status.
   *
   * @param m
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <tt>E</tt>
   */

  <A, E extends Exception> A matchBookDownloadingStatus(
    final BookStatusDownloadingMatcherType<A, E> m)
    throws E;
}
