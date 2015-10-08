package org.nypl.simplified.app.catalog;

import android.view.Menu;
import android.view.MenuItem;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

/**
 * An abstract activity implementing the common feed refreshing code for Books
 * and Holds activities.
 */

abstract class MainLocalFeedActivity extends CatalogFeedActivity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainLocalFeedActivity.class);
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
    final int id = item_nn.getItemId();

    if (id == R.id.catalog_action_sync_books) {
      final SimplifiedCatalogAppServicesType app =
        Simplified.getCatalogAppServices();
      final BooksType books = app.getBooks();

      item_nn.setEnabled(false);
      books.accountSync(new SyncListener());
      MainLocalFeedActivity.this.retryFeed();
      return true;
    }

    return super.onOptionsItemSelected(item_nn);
  }

  private static final class SyncListener implements AccountSyncListenerType
  {
    SyncListener()
    {

    }

    @Override
    public void onAccountSyncAuthenticationFailure(final String message)
    {
      MainLocalFeedActivity.LOG.debug("account syncing failed: {}", message);
    }

    @Override public void onAccountSyncBook(final BookID book)
    {
      MainLocalFeedActivity.LOG.debug("synced: {}", book);
    }

    @Override public void onAccountSyncFailure(
      final OptionType<Throwable> error,
      final String message)
    {
      LogUtilities.errorWithOptionalException(
        MainLocalFeedActivity.LOG, message, error);
    }

    @Override public void onAccountSyncSuccess()
    {
      MainLocalFeedActivity.LOG.debug("account syncing finished");
    }

    @Override public void onAccountSyncBookDeleted(final BookID book)
    {
      MainLocalFeedActivity.LOG.debug("book deleted: {}", book);
    }
  }
}
