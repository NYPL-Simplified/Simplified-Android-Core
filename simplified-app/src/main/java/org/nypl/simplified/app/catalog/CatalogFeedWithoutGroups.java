package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.app.BookCoverProviderType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksStatusCacheType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryType;
import org.nypl.simplified.books.core.FeedLoaderListenerType;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.books.core.FeedMatcherType;
import org.nypl.simplified.books.core.FeedType;
import org.nypl.simplified.books.core.FeedWithGroups;
import org.nypl.simplified.books.core.FeedWithoutGroups;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A view that displays a catalog feed that does not contain any groups.
 */

public final class CatalogFeedWithoutGroups implements ListAdapter,
  OnScrollListener,
  FeedLoaderListenerType,
  FeedMatcherType<Unit, UnreachableCodeException>,
  Observer
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogFeedWithoutGroups.class);
  }

  private final Activity                                 activity;
  private final ArrayAdapter<FeedEntryType>              adapter;
  private final BookCoverProviderType                    book_cover_provider;
  private final CatalogBookSelectionListenerType         book_select_listener;
  private final BooksType                                books;
  private final FeedWithoutGroups                        feed;
  private final FeedLoaderType                           feed_loader;
  private final AtomicReference<Pair<Future<Unit>, URI>> loading;
  private final AtomicReference<OptionType<URI>>         uri_next;

  /**
   * Construct a view.
   *
   * @param in_activity                The host activity
   * @param in_book_cover_provider     A cover provider
   * @param in_book_selection_listener A book selection listener
   * @param in_books                   The books database
   * @param in_feed_loader             An asynchronous feed loader
   * @param in_feed                    The current feed
   */

  public CatalogFeedWithoutGroups(
    final Activity in_activity,
    final BookCoverProviderType in_book_cover_provider,
    final CatalogBookSelectionListenerType in_book_selection_listener,
    final BooksType in_books,
    final FeedLoaderType in_feed_loader,
    final FeedWithoutGroups in_feed)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.book_cover_provider = NullCheck.notNull(in_book_cover_provider);
    this.book_select_listener = NullCheck.notNull(in_book_selection_listener);
    this.books = NullCheck.notNull(in_books);
    this.feed = NullCheck.notNull(in_feed);
    this.feed_loader = NullCheck.notNull(in_feed_loader);
    this.uri_next = new AtomicReference<OptionType<URI>>(in_feed.getFeedNext());
    this.adapter = new ArrayAdapter<FeedEntryType>(this.activity, 0, this.feed);
    this.loading = new AtomicReference<Pair<Future<Unit>, URI>>();

    final BooksStatusCacheType status = this.books.bookGetStatusCache();
    status.booksObservableAddObserver(this);
  }

  private static boolean shouldLoadNext(
    final int first_visible_item,
    final int total_count)
  {
    CatalogFeedWithoutGroups.LOG.debug(
      "shouldLoadNext: {} - {} = {}",
      Integer.valueOf(total_count),
      Integer.valueOf(first_visible_item),
      Integer.valueOf(total_count - first_visible_item));
    return (total_count - first_visible_item) <= 50;
  }

  @Override public boolean areAllItemsEnabled()
  {
    return this.adapter.areAllItemsEnabled();
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

  @Override public int getItemViewType(
    final int position)
  {
    return this.adapter.getItemViewType(position);
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
      cv = new CatalogFeedBookCellView(
        this.activity, this.book_cover_provider, this.books);
    }

    cv.viewConfigure(e, this.book_select_listener);
    return cv;

  }

  @Override public int getViewTypeCount()
  {
    return this.adapter.getViewTypeCount();
  }

  @Override public boolean hasStableIds()
  {
    return this.adapter.hasStableIds();
  }

  @Override public boolean isEmpty()
  {
    return this.adapter.isEmpty();
  }

  @Override public boolean isEnabled(
    final int position)
  {
    return this.adapter.isEnabled(position);
  }

  /**
   * Attempt to load the next feed, if necessary. If the feed is already
   * loading, the feed will not be requested again.
   *
   * @param next_ref The next URI, if any
   *
   * @return A future representing the loading feed
   */

  private @Nullable Future<Unit> loadNext(
    final AtomicReference<OptionType<URI>> next_ref)
  {
    final OptionType<URI> next_opt = next_ref.get();
    if (next_opt.isSome()) {
      final Some<URI> next_some = (Some<URI>) next_opt;
      final URI next = next_some.get();

      final Pair<Future<Unit>, URI> in_loading = this.loading.get();
      if (in_loading == null) {
        CatalogFeedWithoutGroups.LOG.debug(
          "no feed currently loading; loading next feed: {}", next);
        return this.loadNextActual(next);
      }

      final URI loading_uri = in_loading.getRight();
      if (loading_uri.equals(next) == false) {
        CatalogFeedWithoutGroups.LOG.debug(
          "different feed currently loading; loading next feed: {}", next);
        return this.loadNextActual(next);
      }

      CatalogFeedWithoutGroups.LOG.debug(
        "already loading next feed, not loading again: {}", next);
    }

    return null;
  }

  private Future<Unit> loadNextActual(
    final URI next)
  {
    CatalogFeedWithoutGroups.LOG.debug("loading: {}", next);
    final OptionType<HTTPAuthType> none = Option.none();
    final Future<Unit> r = this.feed_loader.fromURI(next, none, this);
    this.loading.set(Pair.pair(r, next));
    return r;
  }

  @Override public void onFeedLoadFailure(
    final URI u,
    final Throwable e)
  {
    if (e instanceof CancellationException) {
      return;
    }

    CatalogFeedWithoutGroups.LOG.error("failed to load feed: ", e);
  }

  @Override public void onFeedLoadSuccess(
    final URI u,
    final FeedType f)
  {
    f.matchFeed(this);
  }

  @Override public Unit onFeedWithGroups(
    final FeedWithGroups f)
  {
    CatalogFeedWithoutGroups.LOG.error(
      "received feed with groups: {}", f.getFeedID());

    return Unit.unit();
  }

  @Override public Unit onFeedWithoutGroups(
    final FeedWithoutGroups f)
  {
    CatalogFeedWithoutGroups.LOG.debug(
      "received feed without groups: {}", f.getFeedID());

    this.feed.addAll(f);
    this.uri_next.set(f.getFeedNext());

    CatalogFeedWithoutGroups.LOG.debug(
      "current feed size: {}", Integer.valueOf(this.feed.size()));
    return Unit.unit();
  }

  @Override public void onScroll(
    final @Nullable AbsListView view,
    final int first_visible_item,
    final int visible_count,
    final int total_count)
  {
    /**
     * If the user is close enough to the end of the list, load the next feed.
     */

    if (CatalogFeedWithoutGroups.shouldLoadNext(
      first_visible_item, total_count)) {
      this.loadNext(this.uri_next);
    }
  }

  @Override public void onScrollStateChanged(
    final @Nullable AbsListView view,
    final int state)
  {
    switch (state) {
      case OnScrollListener.SCROLL_STATE_FLING:
      case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL: {
        this.book_cover_provider.loadingThumbailsPause();
        break;
      }
      case OnScrollListener.SCROLL_STATE_IDLE: {
        this.book_cover_provider.loadingThumbnailsContinue();
        break;
      }
    }
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

  @Override public void update(
    final Observable observable,
    final Object data)
  {
    Assertions.checkPrecondition(
      data instanceof BookID, "%s instanceof %s", data, BookID.class);

    CatalogFeedWithoutGroups.LOG.debug("update: {}", data);

    final BookID update_id = (BookID) data;
    if (this.feed.containsID(update_id)) {
      final BooksStatusCacheType status = this.books.bookGetStatusCache();
      final OptionType<FeedEntryType> e = status.booksFeedEntryGet(update_id);
      if (e.isSome()) {
        final FeedEntryType ee = ((Some<FeedEntryType>) e).get();
        this.feed.updateEntry(ee);

        UIThread.runOnUIThread(
          new Runnable()
          {
            @Override public void run()
            {
              CatalogFeedWithoutGroups.this.adapter.notifyDataSetChanged();
            }
          });
      }
    }
  }
}
