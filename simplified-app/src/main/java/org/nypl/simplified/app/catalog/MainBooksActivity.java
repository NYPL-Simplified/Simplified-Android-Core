package org.nypl.simplified.app.catalog;

import android.view.Menu;
import android.view.MenuItem;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.SimplifiedPart;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksFeedSelection;
import org.nypl.simplified.books.core.BooksType;
import org.slf4j.Logger;

/**
 * The activity that displays the currently loaned and/or downloaded books.
 */

public final class MainBooksActivity extends CatalogFeedActivity
  implements AccountGetCachedCredentialsListenerType, AccountSyncListenerType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainBooksActivity.class);
  }

  /**
   * Construct an activity.
   */

  public MainBooksActivity()
  {

  }

  @Override protected BooksFeedSelection getLocalFeedTypeSelection()
  {
    return BooksFeedSelection.BOOKS_FEED_LOANED;
  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return SimplifiedPart.PART_BOOKS;
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return true;
  }

  @Override protected String catalogFeedGetEmptyText()
  {
    return this.getResources().getString(R.string.catalog_empty_feed);
  }

  @Override public void onAccountIsLoggedIn(
    final AccountBarcode barcode,
    final AccountPIN pin)
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.accountSync(this);
  }

  @Override public void onAccountIsNotLoggedIn()
  {
    MainBooksActivity.LOG.debug("account is not logged in");
  }

  @Override public void onAccountSyncAuthenticationFailure(
    final String message)
  {
    // Nothing
  }

  @Override public void onAccountSyncBook(
    final BookID book)
  {
    // Nothing
  }

  @Override public void onAccountSyncFailure(
    final OptionType<Throwable> error,
    final String message)
  {
    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          MainBooksActivity.this.invalidateOptionsMenu();
        }
      });
  }

  @Override public void onAccountSyncSuccess()
  {
    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          MainBooksActivity.this.invalidateOptionsMenu();
        }
      });
  }

  @Override public boolean onCreateOptionsMenu(
    final @Nullable Menu in_menu)
  {
    super.onCreateOptionsMenu(in_menu);

    final Menu menu_nn = NullCheck.notNull(in_menu);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final MenuItem sync_item =
      NullCheck.notNull(menu_nn.findItem(R.id.catalog_action_sync_books));
    final MenuItem refresh_item =
      NullCheck.notNull(menu_nn.findItem(R.id.catalog_action_refresh));

    if (books.accountIsLoggedIn()) {
      sync_item.setEnabled(true);
      sync_item.setVisible(true);
    }

    refresh_item.setEnabled(false);
    refresh_item.setVisible(false);
    return true;
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item)
  {
    final MenuItem item_nn = NullCheck.notNull(item);
    switch (item_nn.getItemId()) {
      case R.id.catalog_action_sync_books: {
        final SimplifiedCatalogAppServicesType app =
          Simplified.getCatalogAppServices();
        final BooksType books = app.getBooks();

        item_nn.setEnabled(false);
        books.accountGetCachedLoginDetails(this);
        return true;
      }
    }

    return super.onOptionsItemSelected(item_nn);
  }
}
