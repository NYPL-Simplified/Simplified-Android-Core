package org.nypl.simplified.books.core;

import org.nypl.simplified.downloader.core.DownloadSnapshot;

public interface BookStatusWithSnapshotType extends BookStatusType
{
  DownloadSnapshot getSnapshot();
}
