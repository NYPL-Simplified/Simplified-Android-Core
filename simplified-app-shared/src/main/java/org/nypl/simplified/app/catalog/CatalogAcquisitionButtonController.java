package org.nypl.simplified.app.catalog;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.NetworkConnectivityType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.login.LoginDialog;
import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryReadableType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.controller.BooksControllerType;
import org.nypl.simplified.books.controller.ProfilesControllerType;
import org.nypl.simplified.books.document_store.DocumentStoreType;
import org.nypl.simplified.books.feeds.FeedEntry;
import org.nypl.simplified.books.logging.LogUtilities;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.profiles.ProfileReadableType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A controller for an acquisition button.
 * <p>
 * This is responsible for logging in, if necessary, and then starting the
 * borrow of a given book.
 */

public final class CatalogAcquisitionButtonController implements OnClickListener {

  private static final Logger LOG =
    LoggerFactory.getLogger(CatalogAcquisitionButtonController.class);

  private final OPDSAcquisition acquisition;
  private final AppCompatActivity activity;
  private final BooksControllerType books;
  private final FeedEntry.FeedEntryOPDS entry;
  private final BookID id;
  private final ProfilesControllerType profiles;
  private final BookRegistryReadableType book_registry;
  private final NetworkConnectivityType network_connectivity;
  private final ListeningExecutorService executor;
  private final DocumentStoreType documents;

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
    final AppCompatActivity in_activity,
    final ProfilesControllerType in_profiles,
    final ListeningExecutorService in_executor,
    final DocumentStoreType in_documents,
    final BooksControllerType in_books,
    final BookRegistryReadableType in_book_registry,
    final NetworkConnectivityType in_network_connectivity,
    final BookID in_id,
    final OPDSAcquisition in_acq,
    final FeedEntry.FeedEntryOPDS in_entry) {

    this.activity =
      NullCheck.notNull(in_activity, "Activity");
    this.acquisition =
      NullCheck.notNull(in_acq, "OPDS Acquisition");
    this.executor =
      NullCheck.notNull(in_executor, "Executor");
    this.documents =
      NullCheck.notNull(in_documents, "Documents");
    this.id =
      NullCheck.notNull(in_id, "Book ID");
    this.profiles =
      NullCheck.notNull(in_profiles, "Profiles controller");
    this.books =
      NullCheck.notNull(in_books, "Books controller");
    this.book_registry =
      NullCheck.notNull(in_book_registry, "Book registry");
    this.network_connectivity =
      NullCheck.notNull(in_network_connectivity, "Network connectivity");
    this.entry =
      NullCheck.notNull(in_entry, "Feed entry");
  }

  @Override
  public void onClick(final @Nullable View v) {

    if (!this.network_connectivity.isNetworkAvailable()) {
      Toast.makeText(
        this.activity.getApplicationContext(),
        this.activity.getResources().getString(R.string.catalog_added_for_later_download),
        Toast.LENGTH_LONG)
        .show();
    }

    final AccountType account =
      accountForBook(this.profiles, this.book_registry, this.id);

    final boolean authentication_required =
      account.provider().authentication().isSome()
        && this.acquisition.getRelation() != OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS;

    final boolean authentication_provided = account.credentials().isSome();
    if (authentication_required && !authentication_provided) {
      this.tryLogin(account);
      return;
    }

    this.tryBorrow(account);
  }

  private void tryBorrow(final AccountType account) {
    LOG.debug("trying borrow of type {}", this.acquisition.getType());
    this.books.bookBorrow(account, this.id, this.acquisition, this.entry.getFeedEntry());
  }

  private void tryLogin(final AccountType account) {
    LOG.debug("trying login");

    // XXX: Untranslated string!
    LoginDialog dialog =
      LoginDialog.newDialog(
        this.profiles,
        this.executor,
        this.documents,
        "Login Required",
        account,
        creds -> this.onLoginSuccess(account),
        this::onLoginCancelled,
        this::onLoginFailure);

    dialog.show(this.activity.getSupportFragmentManager(), "login-dialog");
  }

  private void onLoginFailure(OptionType<Exception> error, final String message) {
    LogUtilities.errorWithOptionalException(LOG, message, error);
  }

  private void onLoginCancelled() {
    LOG.debug("login cancelled");
  }

  private void onLoginSuccess(
    final AccountType account) {

    LOG.debug("login succeeded");
    this.tryBorrow(account);
  }

  private static AccountType accountForBook(
    final ProfilesControllerType profiles,
    final BookRegistryReadableType book_registry,
    final BookID book_id) {

    return Option.of(book_registry.books().get(book_id)).accept(
      new OptionVisitorType<BookWithStatus, AccountType>() {
        @Override
        public AccountType none(final None<BookWithStatus> none) {
          try {
            return profiles.profileAccountCurrent();
          } catch (final ProfileNoneCurrentException e) {
            throw new IllegalStateException(e);
          }
        }

        @Override
        public AccountType some(final Some<BookWithStatus> some) {
          try {
            final ProfileReadableType profile = profiles.profileCurrent();
            final AccountID account_id = some.get().book().getAccount();
            return profile.account(account_id);
          } catch (ProfileNoneCurrentException | AccountsDatabaseNonexistentException e) {
            throw new IllegalStateException(e);
          }
        }
      });
  }
}
