package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;

import com.aferdita.urms.DITAURMS;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.reader.ReaderActivity;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot;
import org.nypl.simplified.books.core.BookDatabaseReadableType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.circanalytics.CirculationAnalytics;
import org.nypl.simplified.opds.core.DRMLicensor;
import org.nypl.simplified.opds.core.OPDSIndirectAcquisition;
import org.nypl.simplified.prefs.Prefs;
import org.slf4j.Logger;

import java.io.File;

/**
 * A controller that opens a given book for reading.
 */

public final class CatalogBookRead implements OnClickListener
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookRead.class);
  }

  private final Activity activity;
  private final BookID   id;
  private final FeedEntryOPDS entry;
  private final CatalogBookReadButton catalog_book_read_button;
  private       AccountCredentials                credentials;

  /**
   * The parent activity.
   * @param in_catalog_book_read_button  read button view
   * @param in_activity   The activity
   * @param in_id         The book ID
   * @param in_entry      The OPDS feed entry
   */

  public CatalogBookRead(
    final CatalogBookReadButton in_catalog_book_read_button,
    final Activity in_activity,
    final BookID in_id,
    final FeedEntryOPDS in_entry) {
    this.activity = NullCheck.notNull(in_activity);
    this.id = NullCheck.notNull(in_id);
    this.entry = NullCheck.notNull(in_entry);
    this.catalog_book_read_button = in_catalog_book_read_button;
  }

  @Override public void onClick(
    final @Nullable View v)
  {

    final Prefs prefs = Simplified.getSharedPrefs();
    prefs.putBoolean("post_last_read", false);
    LOG.debug("CurrentPage prefs {}", prefs.getBoolean("post_last_read"));

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    books.accountGetCachedLoginDetails(
      new AccountGetCachedCredentialsListenerType()
      {
        @Override public void onAccountIsNotLoggedIn()
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountIsLoggedIn(
          final AccountCredentials creds) {

          CirculationAnalytics.postEvent(creds, CatalogBookRead.this.activity, CatalogBookRead.this.entry, "open_book");

        }
      }
    );

    final BookDatabaseReadableType db = books.bookGetDatabase();
    final Activity a = this.activity;

    final OptionType<BookDatabaseEntrySnapshot> snap_opt =
      db.databaseGetEntrySnapshot(this.id);

    if (snap_opt.isSome()) {
      final Some<BookDatabaseEntrySnapshot> some_snap =
        (Some<BookDatabaseEntrySnapshot>) snap_opt;
      final BookDatabaseEntrySnapshot snap = some_snap.get();
      final OptionType<File> book_opt = snap.getBook();
      if (book_opt.isSome()) {
        final Some<File> some_book = (Some<File>) book_opt;
        final File book = some_book.get();

        books.accountGetCachedLoginDetails(
          new AccountGetCachedCredentialsListenerType()
          {
            @Override public void onAccountIsNotLoggedIn()
            {
              throw new UnreachableCodeException();
            }

            @Override public void onAccountIsLoggedIn(
              final AccountCredentials creds) {

              CatalogBookRead.this.credentials = creds;

            }
          }
        );

        if (this.entry.getFeedEntry().getIndirectAcquisition().isSome()) {
          final OPDSIndirectAcquisition ind = ((Some<OPDSIndirectAcquisition>) this.entry.getFeedEntry().getIndirectAcquisition()).get();
          if (ind.getCcid().isSome()) {
            final String ccid = ((Some<String>) ind.getCcid()).get();
            if (this.credentials != null) {
              if (this.credentials.getDrmLicensor().isSome()) {
                final DRMLicensor l = ((Some<DRMLicensor>) this.credentials.getDrmLicensor()).get();
                if (l.getClientTokenUrl().isSome()) {
                  final String url = ((Some<String>) l.getClientTokenUrl()).get();
                  // enable activity indicator / progress
                  final ProgressBar progress = new ProgressBar(a);
                  if (CatalogBookRead.this.catalog_book_read_button != null) {
                    CatalogBookRead.this.catalog_book_read_button.setGravity(Gravity.CENTER_VERTICAL);
                    CatalogBookRead.this.catalog_book_read_button.addView(progress);
                    CatalogBookRead.this.catalog_book_read_button.setEnabled(false);
                  }
                  DITAURMS.evaluateBook(a,
                    ccid,
                    book,
                    Simplified.getCurrentAccount().getAbbreviation(),
                    this.credentials.getBarcode().toString(),
                    this.credentials.getPin().toString(),
                    url,
                    true,
                    new DITAURMS.CallbackType() {
                    @Override
                    public void onSuccess() {
                      LOG.debug("DITAURMSCallbackSuccess ");
                      ReaderActivity.startActivity(a, CatalogBookRead.this.id, book, CatalogBookRead.this.entry);
                      CatalogBookRead.this.catalog_book_read_button.removeView(progress);
                      CatalogBookRead.this.catalog_book_read_button.setEnabled(true);
                    }
                    @Override
                    public void onFailed() {
                      LOG.error("DITAURMSCallbackFailure ");
                      CatalogBookRead.this.catalog_book_read_button.removeView(progress);
                      CatalogBookRead.this.catalog_book_read_button.setEnabled(true);
                      ErrorDialogUtilities.showAlert(
                        a, CatalogBookRead.LOG, "An error occurred while evaluating book license!", "Please try again later or make sure you have permission to read this book.", null);
                    }
                  });
                }
              }
            }
          }
          else {
            ReaderActivity.startActivity(a, this.id, book, this.entry);
          }
        } else {
          // open access , no drm, no login
          ReaderActivity.startActivity(a, this.id, book, this.entry);
        }
      } else {
        ErrorDialogUtilities.showError(
          a,
          CatalogBookRead.LOG,
          "Bug: book claimed to be downloaded but no book file exists in "
          + "storage",
          null);
      }
    } else {
      ErrorDialogUtilities.showError(
        a, CatalogBookRead.LOG, "Book no longer exists!", null);
    }
  }
}
