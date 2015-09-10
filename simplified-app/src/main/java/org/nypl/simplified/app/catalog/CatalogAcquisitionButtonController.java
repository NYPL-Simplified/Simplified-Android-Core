package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.app.LoginDialog;
import org.nypl.simplified.app.LoginListenerType;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

/**
 * A controller for an acquisition button.
 *
 * This is responsible for logging in, if necessary, and then starting the
 * borrow of a given book.
 */

public final class CatalogAcquisitionButtonController
  implements OnClickListener, LoginListenerType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogAcquisitionButtonController.class);
  }

  private final OPDSAcquisition acq;
  private final Activity        activity;
  private final BooksType       books;
  private final FeedEntryOPDS   entry;
  private final BookID          id;

  /**
   * Construct a button controller.
   *
   * @param in_activity The host activity
   * @param in_books    The books database
   * @param in_id       The book ID
   * @param in_acq      The acquisition
   * @param in_entry    The associated feed entry
   */

  public CatalogAcquisitionButtonController(
    final Activity in_activity,
    final BooksType in_books,
    final BookID in_id,
    final OPDSAcquisition in_acq,
    final FeedEntryOPDS in_entry)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.acq = NullCheck.notNull(in_acq);
    this.id = NullCheck.notNull(in_id);
    this.books = NullCheck.notNull(in_books);
    this.entry = NullCheck.notNull(in_entry);
  }

  @Override public void onClick(
    final @Nullable View v)
  {
    if (this.books.accountIsLoggedIn()) {
      this.books.accountGetCachedLoginDetails(
        new AccountGetCachedCredentialsListenerType()
        {
          @Override public void onAccountIsNotLoggedIn()
          {
            throw new UnreachableCodeException();
          }

          @Override public void onAccountIsLoggedIn(
            final AccountCredentials creds)
          {
            CatalogAcquisitionButtonController.this.onLoginSuccess(creds);
          }
        });
    } else {
      this.tryLogin();
    }
  }

  private void tryLogin()
  {
    final AccountBarcode barcode = new AccountBarcode("");
    final AccountPIN pin = new AccountPIN("");

    final LoginDialog df =
      LoginDialog.newDialog("Login required", barcode, pin);
    df.setLoginListener(this);

    final FragmentManager fm = this.activity.getFragmentManager();
    df.show(fm, "login-dialog");
  }

  @Override public void onLoginAborted()
  {
    // Nothing
  }

  @Override public void onLoginFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    // Nothing
  }

  @Override public void onLoginSuccess(
    final AccountCredentials creds)
  {
    CatalogAcquisitionButtonController.LOG.debug("login succeeded");
    CatalogAcquisitionButtonController.LOG.debug(
      "attempting borrow of {} acquisition", this.acq.getType());

    switch (this.acq.getType()) {
      case ACQUISITION_BORROW:
      case ACQUISITION_GENERIC:
      case ACQUISITION_OPEN_ACCESS: {
        final OPDSAcquisitionFeedEntry eo = this.entry.getFeedEntry();
        this.books.bookBorrow(this.id, this.acq, eo);
        break;
      }
      case ACQUISITION_BUY:
      case ACQUISITION_SAMPLE:
      case ACQUISITION_SUBSCRIBE: {
        throw new UnimplementedCodeException();
      }
    }
  }
}
