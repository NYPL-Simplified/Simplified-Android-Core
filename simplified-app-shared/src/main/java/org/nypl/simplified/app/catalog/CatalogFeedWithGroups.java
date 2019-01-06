package org.nypl.simplified.app.catalog;

import android.database.DataSetObserver;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.ApplicationColorScheme;
import org.nypl.simplified.app.ScreenSizeInformationType;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.covers.BookCoverProviderType;
import org.nypl.simplified.books.feeds.FeedGroup;
import org.nypl.simplified.books.feeds.FeedWithGroups;

/**
 * A view that displays a catalog feed that contains groups.
 */

public final class CatalogFeedWithGroups implements ListAdapter, OnScrollListener {

  private final AppCompatActivity activity;
  private final ArrayAdapter<FeedGroup> adapter;
  private final BookCoverProviderType book_cover_provider;
  private final FeedWithGroups feed;
  private final CatalogFeedLaneListenerType lane_listener;
  private final ScreenSizeInformationType screen;
  private final AccountType account;
  private final ApplicationColorScheme colorScheme;

  /**
   * Construct a view.
   */

  CatalogFeedWithGroups(
    final AppCompatActivity in_activity,
    final AccountType in_account,
    final ScreenSizeInformationType in_screen,
    final BookCoverProviderType in_book_cover_provider,
    final CatalogFeedLaneListenerType in_lane_listener,
    final FeedWithGroups in_feed,
    final ApplicationColorScheme colorScheme) {

    this.activity = NullCheck.notNull(in_activity);
    this.account = NullCheck.notNull(in_account, "Account");
    this.book_cover_provider = NullCheck.notNull(in_book_cover_provider);
    this.lane_listener = NullCheck.notNull(in_lane_listener);
    this.feed = NullCheck.notNull(in_feed);
    this.screen = NullCheck.notNull(in_screen);
    this.colorScheme = NullCheck.notNull(colorScheme);
    this.adapter = new ArrayAdapter<>(this.activity, 0, this.feed);
  }

  @Override
  public boolean areAllItemsEnabled() {
    return this.adapter.areAllItemsEnabled();
  }

  @Override
  public int getCount() {
    return this.adapter.getCount();
  }

  @Override
  public FeedGroup getItem(final int position) {
    return NullCheck.notNull(this.adapter.getItem(position));
  }

  @Override
  public long getItemId(
    final int position) {
    return this.adapter.getItemId(position);
  }

  @Override
  public int getItemViewType(
    final int position) {
    return this.adapter.getItemViewType(position);
  }

  @Override
  public View getView(
    final int position,
    final @Nullable View reused,
    final @Nullable ViewGroup parent) {
    final FeedGroup group = this.feed.get(position);

    final CatalogFeedLane view;
    if (reused != null) {
      view = (CatalogFeedLane) reused;
    } else {
      view = new CatalogFeedLane(
        this.activity,
        this.book_cover_provider,
        this.screen,
        this.lane_listener,
        this.colorScheme);
    }

    view.configureForGroup(group);
    return view;
  }

  @Override
  public int getViewTypeCount() {
    return this.adapter.getViewTypeCount();
  }

  @Override
  public boolean hasStableIds() {
    return this.adapter.hasStableIds();
  }

  @Override
  public boolean isEmpty() {
    return this.adapter.isEmpty();
  }

  @Override
  public boolean isEnabled(
    final int position) {
    return this.adapter.isEnabled(position);
  }

  @Override
  public void onScroll(
    final @Nullable AbsListView view,
    final int first_visible_item,
    final int visible_count,
    final int total_count) {
    // Nothing
  }

  @Override
  public void onScrollStateChanged(
    final @Nullable AbsListView view,
    final int state) {
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

  @Override
  public void registerDataSetObserver(
    final @Nullable DataSetObserver observer) {
    this.adapter.registerDataSetObserver(observer);
  }

  @Override
  public void unregisterDataSetObserver(
    final @Nullable DataSetObserver observer) {
    this.adapter.unregisterDataSetObserver(observer);
  }
}
