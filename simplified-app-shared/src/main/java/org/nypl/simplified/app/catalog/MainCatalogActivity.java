package org.nypl.simplified.app.catalog;

import android.app.AlertDialog;
import android.content.res.Resources;
import android.os.Bundle;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookStatusDownloaded;
import org.nypl.simplified.books.book_registry.BookStatusDownloadingType;
import org.nypl.simplified.books.book_registry.BookStatusHeld;
import org.nypl.simplified.books.book_registry.BookStatusHeldReady;
import org.nypl.simplified.books.book_registry.BookStatusHoldable;
import org.nypl.simplified.books.book_registry.BookStatusLoanable;
import org.nypl.simplified.books.book_registry.BookStatusLoaned;
import org.nypl.simplified.books.book_registry.BookStatusLoanedMatcherType;
import org.nypl.simplified.books.book_registry.BookStatusLoanedType;
import org.nypl.simplified.books.book_registry.BookStatusMatcherType;
import org.nypl.simplified.books.book_registry.BookStatusRequestingDownload;
import org.nypl.simplified.books.book_registry.BookStatusRequestingLoan;
import org.nypl.simplified.books.book_registry.BookStatusRequestingRevoke;
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed;
import org.nypl.simplified.books.book_registry.BookStatusRevoked;
import org.nypl.simplified.books.book_registry.BookStatusType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.core.BookAcquisitionSelection;
import org.nypl.simplified.books.feeds.FeedBooksSelection;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;

/**
 * The main catalog feed activity, responsible for displaying different types of
 * feeds.
 */

public final class MainCatalogActivity extends CatalogFeedActivity
{
  private static final Logger LOG = LoggerFactory.getLogger(MainCatalogActivity.class);

  @Override
  protected Logger log() {
    return LOG;
  }

  /**
   * Construct an activity.
   */

  public MainCatalogActivity()
  {

  }

  @Override
  protected String navigationDrawerGetActivityTitle(final Resources resources) {
    return resources.getString(R.string.catalog);
  }

  @Override protected FeedBooksSelection localFeedTypeSelection()
  {
    /*
     * This activity does not display local feeds. To ask it to do so is an
     * error!
     */

    throw new UnreachableCodeException();
  }

  @Override protected String catalogFeedGetEmptyText()
  {
    return this.getResources().getString(R.string.catalog_empty_feed);
  }

  private boolean bookStatusIsAwatingDownload(final BookStatusType status) {
    final boolean[] isAwaiting = {false};

    status.matchBookStatus(new BookStatusMatcherType<Unit, UnreachableCodeException>() {
      @Override
      public Unit onBookStatusHoldable(BookStatusHoldable s) {
        return Unit.unit();
      }

      @Override
      public Unit onBookStatusHeld(BookStatusHeld s) {
        return Unit.unit();
      }

      @Override
      public Unit onBookStatusHeldReady(BookStatusHeldReady s) {
        return Unit.unit();
      }

      @Override
      public Unit onBookStatusLoanedType(BookStatusLoanedType s) {
        s.matchBookLoanedStatus(new BookStatusLoanedMatcherType<Unit, UnreachableCodeException>() {
          @Override
          public Unit onBookStatusDownloaded(BookStatusDownloaded s) {
            return Unit.unit();
          }

          @Override
          public Unit onBookStatusDownloading(BookStatusDownloadingType s) {
            return Unit.unit();
          }

          @Override
          public Unit onBookStatusLoaned(BookStatusLoaned s) {
            isAwaiting[0] = true;
            return Unit.unit();
          }

          @Override
          public Unit onBookStatusRequestingDownload(BookStatusRequestingDownload s) {
            return Unit.unit();
          }
        });

        return Unit.unit();
      }

      @Override
      public Unit onBookStatusRequestingLoan(BookStatusRequestingLoan s) {
        return Unit.unit();
      }

      @Override
      public Unit onBookStatusRequestingRevoke(BookStatusRequestingRevoke s) {
        return Unit.unit();
      }

      @Override
      public Unit onBookStatusLoanable(BookStatusLoanable s) {
        return Unit.unit();
      }

      @Override
      public Unit onBookStatusRevokeFailed(BookStatusRevokeFailed s) {
        return Unit.unit();
      }

      @Override
      public Unit onBookStatusRevoked(BookStatusRevoked s) {
        return Unit.unit();
      }
    });

    return isAwaiting[0];
  }

  private boolean isAwaitingDownload() {
    for (final BookWithStatus bookWithStatus : Simplified.getBooksRegistry().books().values()) {
      if (this.bookStatusIsAwatingDownload(bookWithStatus.status())) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);

    final boolean connected = Simplified.getNetworkConnectivity().isWifiAvailable();
    if (connected && this.isAwaitingDownload()) {
      final AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("Downloads Waiting");
      builder.setMessage(
        "You have books waiting to be downloaded. Do you want to download them now?");
      builder.setPositiveButton("Download Now", (dialogInterface, i) -> {
        try {
          final AccountType account = Simplified.getProfilesController().profileAccountCurrent();
          final SortedMap<BookID, BookWithStatus> books = Simplified.getBooksRegistry().books();
          for (BookWithStatus bookWithStatus : books.values()) {
            if (this.bookStatusIsAwatingDownload(bookWithStatus.status())) {
              final OPDSAcquisitionFeedEntry entry = bookWithStatus.book().getEntry();

              final OptionType<OPDSAcquisition> acquisition =
                BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitions());

              acquisition.map_((someAcquisition) -> {
                Simplified.getBooksController()
                  .bookBorrow(account, bookWithStatus.book().getId(), someAcquisition, entry);
              });
            }
          }
        } catch (ProfileNoneCurrentException e) {
          throw new IllegalStateException();
        }
      });
      builder.setNegativeButton("Later", (dialogInterface, i) -> {});
      builder.show();
    }
  }
}
