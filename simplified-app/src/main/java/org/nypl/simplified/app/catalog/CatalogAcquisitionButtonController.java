package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.LoginController;
import org.nypl.simplified.app.LoginControllerListenerType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BookBorrowListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;

public final class CatalogAcquisitionButtonController implements
  OnClickListener,
  LoginControllerListenerType,
  BookBorrowListenerType
{
  private static final Logger            LOG;

  static {
    LOG = LogUtilities.getLog(CatalogAcquisitionButtonController.class);
  }

  private final OPDSAcquisition          acq;
  private final Activity                 activity;
  private final BooksType                books;
  private final OPDSAcquisitionFeedEntry entry;
  private final BookID                   id;
  private final LoginController          login_controller;

  public CatalogAcquisitionButtonController(
    final Activity in_activity,
    final BooksType in_books,
    final BookID in_id,
    final OPDSAcquisition in_acq,
    final OPDSAcquisitionFeedEntry in_entry)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.acq = NullCheck.notNull(in_acq);
    this.id = NullCheck.notNull(in_id);
    this.books = NullCheck.notNull(in_books);
    this.entry = NullCheck.notNull(in_entry);
    this.login_controller =
      new LoginController(this.activity, this.books, this);
  }

  @Override public void onBookBorrowFailure(
    final BookID in_id,
    final OptionType<Throwable> in_e)
  {
    LogUtilities.errorWithOptionalException(
      CatalogAcquisitionButtonController.LOG,
      "borrow failed",
      in_e);
  }

  @Override public void onBookBorrowSuccess(
    final BookID in_id)
  {
    CatalogAcquisitionButtonController.LOG.debug("borrow succeeded");
  }

  @Override public void onClick(
    final @Nullable View v)
  {
    this.login_controller.onClick(v);
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

  @Override public void onLoginSuccess()
  {
    CatalogAcquisitionButtonController.LOG.debug("login succeeded");

    switch (this.acq.getType()) {
      case ACQUISITION_BORROW:
      case ACQUISITION_GENERIC:
      {
        this.books.bookBorrow(this.id, this.acq, this.entry.getTitle(), this);
        this.books.bookUpdateMetadata(this.id, this.entry);
        break;
      }
      case ACQUISITION_BUY:
      case ACQUISITION_OPEN_ACCESS:
      case ACQUISITION_SAMPLE:
      case ACQUISITION_SUBSCRIBE:
      {
        throw new UnimplementedCodeException();
      }
    }
  }
}
