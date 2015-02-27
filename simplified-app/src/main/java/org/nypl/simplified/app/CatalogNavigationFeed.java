package org.nypl.simplified.app;

import java.util.List;

import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogNavigationFeed implements
  ListAdapter,
  RecyclerListener
{
  private final Activity                              activity;
  private final ArrayAdapter<OPDSNavigationFeedEntry> adapter;
  private final OPDSNavigationFeed                    feed;
  private final CatalogLaneViewListenerType           listener;

  public CatalogNavigationFeed(
    final ArrayAdapter<OPDSNavigationFeedEntry> in_adapter,
    final OPDSNavigationFeed in_feed,
    final Activity in_activity,
    final CatalogLaneViewListenerType in_listener)
  {
    this.adapter = NullCheck.notNull(in_adapter);
    this.feed = NullCheck.notNull(in_feed);
    this.activity = NullCheck.notNull(in_activity);
    this.listener = NullCheck.notNull(in_listener);
  }

  @Override public boolean areAllItemsEnabled()
  {
    return NullCheck.notNull(this.adapter).areAllItemsEnabled();
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
    final @Nullable View convertView,
    final @Nullable ViewGroup parent)
  {
    final OPDSNavigationFeed f = NullCheck.notNull(this.feed);
    final List<OPDSNavigationFeedEntry> es =
      NullCheck.notNull(f.getFeedEntries());
    final OPDSNavigationFeedEntry e = NullCheck.notNull(es.get(position));

    final CatalogLaneView v = new CatalogLaneView(this.activity);
    v.setLaneViewFeedAndListener(e, this.listener);
    return v;
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

  @Override public void onMovedToScrapHeap(
    final @Nullable View view)
  {
    final CatalogLaneView lv = NullCheck.notNull((CatalogLaneView) view);
    lv.cancel();
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
