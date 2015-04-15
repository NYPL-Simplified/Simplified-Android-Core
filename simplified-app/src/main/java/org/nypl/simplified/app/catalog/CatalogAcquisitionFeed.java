package org.nypl.simplified.app.catalog;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.app.ExpensiveStoppableType;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

public final class CatalogAcquisitionFeed implements
  ListAdapter,
  ExpensiveStoppableType,
  OnScrollListener,
  OPDSFeedLoadListenerType,
  OPDSFeedMatcherType<Unit, UnreachableCodeException>
{
  private static final String TAG;

  static {
    TAG = "CAF";
  }

  private static boolean shouldLoadNext(
    final int first_visible_item,
    final int total_count)
  {
    return (total_count - first_visible_item) <= 50;
  }

  private final Activity                                    activity;
  private final ArrayAdapter<OPDSAcquisitionFeedEntry>      adapter;
  private final BooksType                                   books;
  private final Map<String, Unit>                           entries_received;
  private final CatalogAcquisitionFeedListenerType          listener;
  private final OPDSFeedLoaderType                          loader;
  private volatile @Nullable ListenableFuture<OPDSFeedType> loading;
  private final AtomicReference<OptionType<URI>>            uri_next;

  public CatalogAcquisitionFeed(
    final Context in_context,
    final OPDSAcquisitionFeed in_feed,
    final Activity in_activity,
    final CatalogAcquisitionFeedListenerType in_listener,
    final OPDSFeedLoaderType in_feed_loader,
    final BooksType in_books)
  {
    NullCheck.notNull(in_context);
    NullCheck.notNull(in_feed);

    this.activity = NullCheck.notNull(in_activity);
    this.listener = NullCheck.notNull(in_listener);
    this.books = NullCheck.notNull(in_books);

    final ArrayAdapter<OPDSAcquisitionFeedEntry> in_adapter =
      new ArrayAdapter<OPDSAcquisitionFeedEntry>(
        in_context,
        0,
        new ArrayList<OPDSAcquisitionFeedEntry>());
    in_adapter.addAll(in_feed.getFeedEntries());

    this.entries_received = new ConcurrentHashMap<String, Unit>();
    this.adapter = in_adapter;
    this.loader = NullCheck.notNull(in_feed_loader);
    this.uri_next = new AtomicReference<OptionType<URI>>(in_feed.getNext());
    this.loading = this.loadNext(this.uri_next);
  }

  @Override public boolean areAllItemsEnabled()
  {
    return NullCheck.notNull(this.adapter).areAllItemsEnabled();
  }

  @Override public void expensiveStop()
  {
    final ListenableFuture<OPDSFeedType> l = this.loading;
    if (l != null) {
      l.cancel(true);
      this.loading = null;
    }
  }

  @Override public int getCount()
  {
    return NullCheck.notNull(this.adapter).getCount();
  }

  @Override public Object getItem(
    final int position)
  {
    return NullCheck.notNull(NullCheck
      .notNull(this.adapter)
      .getItem(position));
  }

  @Override public long getItemId(
    final int position)
  {
    return NullCheck.notNull(this.adapter).getItemId(position);
  }

  @Override public int getItemViewType(
    final int position)
  {
    return NullCheck.notNull(this.adapter).getItemViewType(position);
  }

  @Override public View getView(
    final int position,
    final @Nullable View reused,
    final @Nullable ViewGroup parent)
  {
    final OPDSAcquisitionFeedEntry e =
      NullCheck.notNull(this.adapter.getItem(position));

    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();
    final CatalogAcquisitionCellView cv;
    if (reused != null) {
      cv = (CatalogAcquisitionCellView) reused;
    } else {
      cv =
        new CatalogAcquisitionCellView(
          this.activity,
          app.getCoverProvider(),
          this.books);
    }

    cv.viewConfigure(e, this.listener);
    return cv;
  }

  @Override public int getViewTypeCount()
  {
    return NullCheck.notNull(this.adapter).getViewTypeCount();
  }

  @Override public boolean hasStableIds()
  {
    return NullCheck.notNull(this.adapter).hasStableIds();
  }

  @Override public boolean isEmpty()
  {
    return NullCheck.notNull(this.adapter).isEmpty();
  }

  @Override public boolean isEnabled(
    final int position)
  {
    return NullCheck.notNull(this.adapter).isEnabled(position);
  }

  private @Nullable ListenableFuture<OPDSFeedType> loadNext(
    final AtomicReference<OptionType<URI>> next_ref)
  {
    final OptionType<URI> next_opt = next_ref.get();
    if (next_opt.isSome()) {
      final Some<URI> next_some = (Some<URI>) next_opt;
      final URI next = next_some.get();

      Log.d(
        CatalogAcquisitionFeed.TAG,
        String.format("loading next feed (%s)", next));

      return this.loader.fromURI(next, this);
    }

    return null;
  }

  @Override public Unit onAcquisitionFeed(
    final OPDSAcquisitionFeed af)
    throws UnreachableCodeException
  {
    Log.d(
      CatalogAcquisitionFeed.TAG,
      String.format("received %s", af.getFeedID()));

    this.uri_next.set(af.getNext());

    final Map<String, Unit> entries = this.entries_received;
    final ArrayAdapter<OPDSAcquisitionFeedEntry> a = this.adapter;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        for (final OPDSAcquisitionFeedEntry e : af.getFeedEntries()) {
          if (entries.containsKey(e.getID())) {
            continue;
          }
          entries.put(e.getID(), Unit.unit());
          a.add(e);
        }

        a.notifyDataSetChanged();
      }
    });

    return Unit.unit();
  }

  @Override public void onFeedLoadingFailure(
    final Throwable e)
  {
    this.loading = null;

    if (e instanceof CancellationException) {
      return;
    }

    Log.e(CatalogAcquisitionFeed.TAG, e.getMessage(), e);
  }

  @Override public void onFeedLoadingSuccess(
    final OPDSFeedType f)
  {
    this.loading = null;
    f.matchFeedType(this);
  }

  @Override public Unit onNavigationFeed(
    final OPDSNavigationFeed nf)
    throws UnreachableCodeException
  {
    Log.e(CatalogAcquisitionFeed.TAG, String.format(
      "received navigation feed instead of acquisition feed (%s)",
      nf.getFeedID()));

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
     * If a feed is already loading, do not try to load it again.
     */

    if (CatalogAcquisitionFeed
      .shouldLoadNext(first_visible_item, total_count)) {
      final ListenableFuture<OPDSFeedType> l = this.loading;
      if (l == null) {
        this.loading = this.loadNext(this.uri_next);
      }
    }
  }

  @Override public void onScrollStateChanged(
    final @Nullable AbsListView view,
    final int state)
  {
    // Nothing
  }

  @Override public void registerDataSetObserver(
    final @Nullable DataSetObserver observer)
  {
    NullCheck.notNull(this.adapter).registerDataSetObserver(observer);
  }

  @Override public void unregisterDataSetObserver(
    final @Nullable DataSetObserver observer)
  {
    NullCheck.notNull(this.adapter).unregisterDataSetObserver(observer);
  }
}
