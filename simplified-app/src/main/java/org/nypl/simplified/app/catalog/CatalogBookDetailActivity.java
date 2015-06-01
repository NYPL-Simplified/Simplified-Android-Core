package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.stack.ImmutableStack;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * An activity showing a full-screen book detail page.
 */

public final class CatalogBookDetailActivity extends CatalogActivity
{
  private static final String CATALOG_BOOK_DETAIL_FEED_ENTRY_ID;

  static {
    CATALOG_BOOK_DETAIL_FEED_ENTRY_ID =
      "org.nypl.simplified.app.CatalogBookDetailActivity.feed_entry";
  }

  public static void setActivityArguments(
    final Bundle b,
    final boolean drawer_open,
    final ImmutableStack<CatalogFeedArgumentsType> up_stack,
    final FeedEntryOPDS e)
  {
    NullCheck.notNull(b);
    SimplifiedActivity.setActivityArguments(b, drawer_open);
    CatalogActivity.setActivityArguments(b, up_stack);
    b.putSerializable(
      CatalogBookDetailActivity.CATALOG_BOOK_DETAIL_FEED_ENTRY_ID,
      NullCheck.notNull(e));
  }

  public static void startNewActivity(
    final Activity from,
    final ImmutableStack<CatalogFeedArgumentsType> up_stack,
    final FeedEntryOPDS e)
  {
    final Bundle b = new Bundle();
    CatalogBookDetailActivity.setActivityArguments(b, false, up_stack, e);
    final Intent i = new Intent(from, CatalogBookDetailActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
  }

  private @Nullable CatalogBookDetailView view;

  private FeedEntryOPDS getFeedEntry()
  {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(i.getExtras());
    return NullCheck
      .notNull((FeedEntryOPDS) a
        .getSerializable(CatalogBookDetailActivity.CATALOG_BOOK_DETAIL_FEED_ENTRY_ID));
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();

    final LayoutInflater inflater =
      NullCheck.notNull(this.getLayoutInflater());

    final CatalogBookDetailView detail_view =
      new CatalogBookDetailView(this, inflater, this.getFeedEntry());
    this.view = detail_view;

    final FrameLayout content_area = this.getContentFrame();
    content_area.removeAllViews();
    content_area.addView(detail_view.getScrollView());
    content_area.requestLayout();

    books.booksObservableAddObserver(detail_view);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    books.booksObservableDeleteObserver(NullCheck.notNull(this.view));
  }

  @Override protected boolean shouldShowNavigationDrawerIndicator()
  {
    return false;
  }
}
