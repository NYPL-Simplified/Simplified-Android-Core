package org.nypl.simplified.books.core;

import java.io.File;

import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.DownloadStatus;
import org.nypl.simplified.downloader.core.DownloaderType;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * Utility functions for producing and comparing values of the
 * {@link BookStatusType} types.
 */

@SuppressWarnings("synthetic-access") public final class BookStatus
{
  public static enum RelativeImportance
  {
    IMPORTANCE_LESS_THAN,
    IMPORTANCE_MORE_THAN,
    IMPORTANCE_SAME
  }

  public static RelativeImportance compareImportance(
    final BookStatusType x,
    final BookStatusType y)
  {
    NullCheck.notNull(x);
    NullCheck.notNull(y);

    return x
      .matchBookStatus(new BookStatusMatcherType<RelativeImportance, UnreachableCodeException>() {

        @Override public RelativeImportance onBookStatusLoanedType(
          final BookStatusLoanedType x_loaned)
        {
          return BookStatus.compareImportanceLoaned(x_loaned, y);
        }

        @Override public RelativeImportance onBookStatusRequestingLoan(
          final BookStatusRequestingLoan x_req)
        {
          return y
            .matchBookStatus(new BookStatusMatcherType<RelativeImportance, UnreachableCodeException>() {
              @Override public RelativeImportance onBookStatusLoanedType(
                final BookStatusLoanedType y_loaned)
              {
                return RelativeImportance.IMPORTANCE_LESS_THAN;
              }

              @Override public RelativeImportance onBookStatusRequestingLoan(
                final BookStatusRequestingLoan y_req)
              {
                return RelativeImportance.IMPORTANCE_SAME;
              }
            });
        }
      });
  }

  private static RelativeImportance compareImportanceLoaned(
    final BookStatusLoanedType x_loaned,
    final BookStatusType y)
  {
    /**
     * Proceed by induction on x and y...
     */

    return x_loaned
      .matchBookLoanedStatus(new BookStatusLoanedMatcherType<RelativeImportance, UnreachableCodeException>() {
        @Override public RelativeImportance onBookStatusDownloaded(
          final BookStatusDownloaded x_downloaded)
        {
          return y
            .matchBookStatus(new BookStatusMatcherType<RelativeImportance, UnreachableCodeException>() {
              @Override public RelativeImportance onBookStatusLoanedType(
                final BookStatusLoanedType y_loaned)
              {
                return y_loaned
                  .matchBookLoanedStatus(new BookStatusLoanedMatcherType<RelativeImportance, UnreachableCodeException>() {

                    @Override public
                      RelativeImportance
                      onBookStatusDownloaded(
                        final BookStatusDownloaded y_downloaded)
                    {
                      return RelativeImportance.IMPORTANCE_SAME;
                    }

                    @Override public
                      RelativeImportance
                      onBookStatusDownloading(
                        final BookStatusDownloadingType y_downloading)
                    {
                      return RelativeImportance.IMPORTANCE_MORE_THAN;
                    }

                    @Override public RelativeImportance onBookStatusLoaned(
                      final BookStatusLoaned y_loaned_actual)
                    {
                      return RelativeImportance.IMPORTANCE_MORE_THAN;
                    }

                    @Override public
                      RelativeImportance
                      onBookStatusRequestingDownload(
                        final BookStatusRequestingDownload y_requesting)
                    {
                      return RelativeImportance.IMPORTANCE_MORE_THAN;
                    }
                  });
              }

              @Override public RelativeImportance onBookStatusRequestingLoan(
                final BookStatusRequestingLoan s)
              {
                return RelativeImportance.IMPORTANCE_MORE_THAN;
              }
            });
        }

        @Override public RelativeImportance onBookStatusDownloading(
          final BookStatusDownloadingType x_downloading)
        {
          return y
            .matchBookStatus(new BookStatusMatcherType<RelativeImportance, UnreachableCodeException>() {
              @Override public RelativeImportance onBookStatusLoanedType(
                final BookStatusLoanedType y_loaned)
              {
                return y_loaned
                  .matchBookLoanedStatus(new BookStatusLoanedMatcherType<RelativeImportance, UnreachableCodeException>() {

                    @Override public
                      RelativeImportance
                      onBookStatusDownloaded(
                        final BookStatusDownloaded y_downloaded)
                    {
                      return RelativeImportance.IMPORTANCE_LESS_THAN;
                    }

                    @Override public
                      RelativeImportance
                      onBookStatusDownloading(
                        final BookStatusDownloadingType y_downloading)
                    {
                      return RelativeImportance.IMPORTANCE_SAME;
                    }

                    @Override public RelativeImportance onBookStatusLoaned(
                      final BookStatusLoaned y_loaned_actual)
                    {
                      return RelativeImportance.IMPORTANCE_MORE_THAN;
                    }

                    @Override public
                      RelativeImportance
                      onBookStatusRequestingDownload(
                        final BookStatusRequestingDownload y_requesting)
                    {
                      return RelativeImportance.IMPORTANCE_MORE_THAN;
                    }
                  });
              }

              @Override public RelativeImportance onBookStatusRequestingLoan(
                final BookStatusRequestingLoan s)
              {
                return RelativeImportance.IMPORTANCE_MORE_THAN;
              }
            });
        }

        @Override public RelativeImportance onBookStatusLoaned(
          final BookStatusLoaned x_loaned_x)
        {
          return y
            .matchBookStatus(new BookStatusMatcherType<RelativeImportance, UnreachableCodeException>() {
              @Override public RelativeImportance onBookStatusLoanedType(
                final BookStatusLoanedType y_loaned)
              {
                return y_loaned
                  .matchBookLoanedStatus(new BookStatusLoanedMatcherType<RelativeImportance, UnreachableCodeException>() {

                    @Override public
                      RelativeImportance
                      onBookStatusDownloaded(
                        final BookStatusDownloaded y_downloaded)
                    {
                      return RelativeImportance.IMPORTANCE_LESS_THAN;
                    }

                    @Override public
                      RelativeImportance
                      onBookStatusDownloading(
                        final BookStatusDownloadingType y_downloading)
                    {
                      return RelativeImportance.IMPORTANCE_LESS_THAN;
                    }

                    @Override public RelativeImportance onBookStatusLoaned(
                      final BookStatusLoaned y_loaned_actual)
                    {
                      return RelativeImportance.IMPORTANCE_SAME;
                    }

                    @Override public
                      RelativeImportance
                      onBookStatusRequestingDownload(
                        final BookStatusRequestingDownload y_requesting)
                    {
                      return RelativeImportance.IMPORTANCE_LESS_THAN;
                    }
                  });
              }

              @Override public RelativeImportance onBookStatusRequestingLoan(
                final BookStatusRequestingLoan s)
              {
                return RelativeImportance.IMPORTANCE_MORE_THAN;
              }
            });
        }

        @Override public RelativeImportance onBookStatusRequestingDownload(
          final BookStatusRequestingDownload x_requesting)
        {
          return y
            .matchBookStatus(new BookStatusMatcherType<RelativeImportance, UnreachableCodeException>() {
              @Override public RelativeImportance onBookStatusLoanedType(
                final BookStatusLoanedType y_loaned)
              {
                return y_loaned
                  .matchBookLoanedStatus(new BookStatusLoanedMatcherType<RelativeImportance, UnreachableCodeException>() {

                    @Override public
                      RelativeImportance
                      onBookStatusDownloaded(
                        final BookStatusDownloaded y_downloaded)
                    {
                      return RelativeImportance.IMPORTANCE_LESS_THAN;
                    }

                    @Override public
                      RelativeImportance
                      onBookStatusDownloading(
                        final BookStatusDownloadingType y_downloading)
                    {
                      return RelativeImportance.IMPORTANCE_LESS_THAN;
                    }

                    @Override public RelativeImportance onBookStatusLoaned(
                      final BookStatusLoaned y_loaned_actual)
                    {
                      return RelativeImportance.IMPORTANCE_LESS_THAN;
                    }

                    @Override public
                      RelativeImportance
                      onBookStatusRequestingDownload(
                        final BookStatusRequestingDownload y_requesting)
                    {
                      return RelativeImportance.IMPORTANCE_SAME;
                    }
                  });
              }

              @Override public RelativeImportance onBookStatusRequestingLoan(
                final BookStatusRequestingLoan s)
              {
                return RelativeImportance.IMPORTANCE_MORE_THAN;
              }
            });
        }
      });
  }

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
      return new BookStatusDownloaded(id);
    }

    /**
     * Otherwise, if there's a recorded download ID, the book is being
     * downloaded (or possibly in a paused, cancelled, etc, state). Query the
     * downloader to find out which.
     */

    final OptionType<Long> did_opt = snap.getDownloadID();
    if (did_opt.isSome()) {
      final Long did = ((Some<Long>) did_opt).get();
      final OptionType<DownloadSnapshot> d_snap_opt =
        downloader.downloadStatusSnapshot(did.longValue());
      if (d_snap_opt.isSome()) {
        final DownloadSnapshot d_snap =
          ((Some<DownloadSnapshot>) d_snap_opt).get();

        final DownloadStatus d_stat = d_snap.statusGet();
        switch (d_stat) {
          case STATUS_CANCELLED:
          {
            return new BookStatusDownloadCancelled(id, d_snap);
          }
          case STATUS_COMPLETED_NOT_TAKEN:
          {
            /**
             * If the data isn't yet taken, then behave as if it's still in
             * the process of downloading: The data will be taken at any
             * moment.
             */
            return new BookStatusDownloadInProgress(id, d_snap);
          }
          case STATUS_FAILED:
            return new BookStatusDownloadFailed(id, d_snap, d_snap.getError());
          case STATUS_IN_PROGRESS:
            return new BookStatusDownloadInProgress(id, d_snap);
          case STATUS_IN_PROGRESS_RESUMED:
            return new BookStatusDownloadInProgress(id, d_snap);
          case STATUS_PAUSED:
            return new BookStatusDownloadingPaused(id, d_snap);
          case STATUS_COMPLETED_TAKEN:
          {
            /**
             * The data was taken, but the book isn't there.
             */
          }
        }
      }
    }

    /**
     * If there was no book, no valid download, then the book is simply
     * "loaned".
     */

    return new BookStatusLoaned(id);
  }

  private BookStatus()
  {
    throw new UnreachableCodeException();
  }
}
