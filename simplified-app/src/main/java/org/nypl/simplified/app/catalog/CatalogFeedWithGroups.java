package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.BookCoverProviderType;
import org.nypl.simplified.app.ScreenSizeControllerType;
import org.nypl.simplified.books.core.FeedGroup;
import org.nypl.simplified.books.core.FeedWithGroups;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogFeedWithGroups implements
  ListAdapter,
  OnScrollListener
{
  private final Activity                    activity;
  private final ArrayAdapter<FeedGroup>     adapter;
  private final BookCoverProviderType       book_cover_provider;
  private final FeedWithGroups              feed;
  private final CatalogFeedLaneListenerType lane_listener;
  private final ScreenSizeControllerType    screen;

  public CatalogFeedWithGroups(
    final Activity in_activity,
    final ScreenSizeControllerType in_screen,
    final BookCoverProviderType in_book_cover_provider,
    final CatalogFeedLaneListenerType in_lane_listener,
    final FeedWithGroups in_feed)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.book_cover_provider = NullCheck.notNull(in_book_cover_provider);
    this.lane_listener = NullCheck.notNull(in_lane_listener);
    this.feed = NullCheck.notNull(in_feed);
    this.screen = NullCheck.notNull(in_screen);
    this.adapter = new ArrayAdapter<FeedGroup>(this.activity, 0, this.feed);
  }

  @Override public boolean areAllItemsEnabled()
  {
    return this.adapter.areAllItemsEnabled();
  }

  @Override public int getCount()
  {
    return this.adapter.getCount();
  }

  @Override public FeedGroup getItem(
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
    final FeedGroup group = this.feed.get(position);

    CatalogFeedLane view;
    if (reused != null) {
      view = (CatalogFeedLane) reused;
    } else {
      view =
        new CatalogFeedLane(
          this.activity,
          this.book_cover_provider,
          this.screen,
          this.lane_listener);
    }

    view.configureForGroup(group);
    return view;
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

  @Override public void onScroll(
    final @Nullable AbsListView view,
    final int first_visible_item,
    final int visible_count,
    final int total_count)
  {
    // Nothing
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
}
