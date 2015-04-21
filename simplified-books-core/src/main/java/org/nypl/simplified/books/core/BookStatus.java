package org.nypl.simplified.books.core;

import java.io.File;

import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.DownloaderType;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Utility functions for producing values of the {@link BookStatusType} types.
 */

public final class BookStatus
{
  public static BookStatusLoanedType fromBookSnapshot(
    final DownloaderType downloader,
    final BookID id,
    final BookSnapshot snap)
  {
    /**
     * If there is completed book data, the book is "done".
     */

    final OptionType<File> book_opt = snap.getBook();
    if (book_opt.isSome()) {
      return new BookStatusDone(id);
    }

    /**
     * Otherwise, if there's a recorded download ID, the book is being
     * downloaded (or possibly in a paused, cancelled, etc, state). Query the
     * downloader to find out which.
     */

    final OptionType<Long> did_opt = snap.getDownloadID();
    if (did_opt.isSome()) {
      final Long did = ((Some<Long>) did_opt).get();
      final OptionType<DownloadSnapshot> dstat_opt =
        downloader.downloadStatusSnapshot(did.longValue());
      if (dstat_opt.isSome()) {
        final DownloadSnapshot dstat =
          ((Some<DownloadSnapshot>) dstat_opt).get();
        return BookStatus.fromDownloadStatus(id, dstat);
      }
    }

    /**
     * If there was no book, no valid download, then the book is simply
     * "loaned".
     */

    return new BookStatusLoaned(id);
  }

  public static BookStatusLoanedType fromDownloadStatus(
    final BookID id,
    final DownloadSnapshot status)
  {
    switch (status.statusGet()) {
      case STATUS_CANCELLED:
        return new BookStatusCancelled(id, status);
      case STATUS_COMPLETED:
        return new BookStatusDone(id);
      case STATUS_FAILED:
        return new BookStatusFailed(id, status, status.getError());
      case STATUS_IN_PROGRESS:
        return new BookStatusDownloading(id, status);
      case STATUS_IN_PROGRESS_RESUMED:
        return new BookStatusDownloading(id, status);
      case STATUS_PAUSED:
        return new BookStatusPaused(id, status);
    }

    throw new UnreachableCodeException();
  }

  private BookStatus()
  {
    throw new UnreachableCodeException();
  }
}
