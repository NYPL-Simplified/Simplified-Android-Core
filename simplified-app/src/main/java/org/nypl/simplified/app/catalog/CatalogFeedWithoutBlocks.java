package org.nypl.simplified.app.catalog;

import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.app.BookCoverProviderType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryType;
import org.nypl.simplified.books.core.FeedLoaderListenerType;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.books.core.FeedMatcherType;
import org.nypl.simplified.books.core.FeedType;
import org.nypl.simplified.books.core.FeedWithBlocks;
import org.nypl.simplified.books.core.FeedWithoutBlocks;
import org.slf4j.Logger;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

public final class CatalogFeedWithoutBlocks implements
  ListAdapter,
  OnScrollListener,
  FeedLoaderListenerType,
  FeedMatcherType<Unit, UnreachableCodeException>
{
  private final ArrayAdapter<FeedEntryType>      adapter;
  private final Activity                         activity;
  private final CatalogBookSelectionListenerType book_select_listener;
  private final BookCoverProviderType            book_cover_provider;
  private final BooksType                        books;
  private final FeedWithoutBlocks                feed;
  private final AtomicReference<OptionType<URI>> uri_next;
  private final FeedLoaderType                   feed_loader;

  @Override public boolean hasStableIds()
  {
    return this.adapter.hasStableIds();
  }

  @Override public void registerDataSetObserver(
    final @Nullable DataSetObserver observer)
  {
    this.adapter.registerDataSetObserver(observer);
  }

  @Override public void unregisterDataSetObserver(
    final @Nullable DataSetObserver observer)
  {
    this.adapter.unregisterDataSetObserver(observer);
  }

  @Override public boolean areAllItemsEnabled()
  {
    return this.adapter.areAllItemsEnabled();
  }

  @Override public boolean isEnabled(
    final int position)
  {
    return this.adapter.isEnabled(position);
  }

  @Override public int getItemViewType(
    final int position)
  {
    return this.adapter.getItemViewType(position);
  }

  @Override public int getViewTypeCount()
  {
    return this.adapter.getViewTypeCount();
  }

  @Override public boolean isEmpty()
  {
    return this.adapter.isEmpty();
  }

  @Override public int getCount()
  {
    return this.adapter.getCount();
  }

  @Override public FeedEntryType getItem(
    final int position)
  {
    return NullCheck.notNull(this.adapter.getItem(position));
  }

  @Override public long getItemId(
    final int position)
  {
    return this.adapter.getItemId(position);
  }

  public CatalogFeedWithoutBlocks(
    final Activity in_activity,
    final BookCoverProviderType in_book_cover_provider,
    final CatalogBookSelectionListenerType in_book_selection_listener,
    final BooksType in_books,
    final FeedLoaderType in_feed_loader,
    final FeedWithoutBlocks in_feed)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.book_cover_provider = NullCheck.notNull(in_book_cover_provider);
    this.book_select_listener = NullCheck.notNull(in_book_selection_listener);
    this.books = NullCheck.notNull(in_books);
    this.feed = NullCheck.notNull(in_feed);
    this.feed_loader = NullCheck.notNull(in_feed_loader);
    this.uri_next =
      new AtomicReference<OptionType<URI>>(in_feed.getFeedNext());
    this.adapter =
      new ArrayAdapter<FeedEntryType>(this.activity, 0, this.feed);
  }

  @Override public View getView(
    final int position,
    final @Nullable View reused,
    final @Nullable ViewGroup parent)
  {
    final FeedEntryType e = NullCheck.notNull(this.adapter.getItem(position));

    final CatalogFeedBookCellView cv;
    if (reused != null) {
      cv = (CatalogFeedBookCellView) reused;
    } else {
      cv =
        new CatalogFeedBookCellView(
          this.activity,
          this.book_cover_provider,
          this.books);
    }

    cv.viewConfigure(e, this.book_select_listener);
    return cv;

  }

  @Override public void onScroll(
    final @Nullable AbsListView view,
    final int first_visible_item,
    final int visible_count,
    final int total_count)
  {
    /**
     * If the user is close enough to the end of the list, load the next feed.
     * If a feed is already loading, do not try to load it again.
     */

    if (CatalogFeedWithoutBlocks.shouldLoadNext(
      first_visible_item,
      total_count)) {
      this.loadNext(this.uri_next);
    }
  }

  private @Nullable Future<Unit> loadNext(
    final AtomicReference<OptionType<URI>> next_ref)
  {
    final OptionType<URI> next_opt = next_ref.get();
    if (next_opt.isSome()) {
      final Some<URI> next_some = (Some<URI>) next_opt;
      final URI next = next_some.get();

      CatalogFeedWithoutBlocks.LOG.debug("loading next feed: %s", next);
      return this.feed_loader.fromURI(next, this);
    }

    return null;
  }

  @Override public void onScrollStateChanged(
    final @Nullable AbsListView view,
    final int state)
  {
    switch (state) {
      case OnScrollListener.SCROLL_STATE_FLING:
      case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
      {
        this.book_cover_provider.loadingThumbailsPause();
        break;
      }
      case OnScrollListener.SCROLL_STATE_IDLE:
      {
        this.book_cover_provider.loadingThumbnailsContinue();
        break;
      }
    }
  }

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogFeedWithoutBlocks.class);
  }

  private static boolean shouldLoadNext(
    final int first_visible_item,
    final int total_count)
  {
    return (total_count - first_visible_item) <= 50;
  }

  @Override public void onFeedLoadSuccess(
    final URI u,
    final FeedType f)
  {
    f.matchFeed(this);
  }

  @Override public void onFeedLoadFailure(
    final URI u,
    final Throwable e)
  {
    if (e instanceof CancellationException) {
      return;
    }

    CatalogFeedWithoutBlocks.LOG.error("failed to load feed: ", e);
  }

  @Override public Unit onFeedWithBlocks(
    final FeedWithBlocks f)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  @Override public Unit onFeedWithoutBlocks(
    final FeedWithoutBlocks f)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }
}
