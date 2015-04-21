package org.nypl.simplified.books.core;

import org.nypl.simplified.downloader.core.DownloadSnapshot;

/**
 * The subset of status types that have an associated download snapshot.
 */

public interface BookStatusWithSnapshotType extends BookStatusLoanedType
{
  /**
   * @return The latest download status snapshot
   */

  DownloadSnapshot getSnapshot();
}
