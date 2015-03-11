package org.nypl.simplified.app;

import java.util.ArrayList;
import java.util.List;

import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A controller for a navigation feed.
 */

public final class CatalogNavigationFeed implements
  ListAdapter,
  RecyclerListener
{
  private final ArrayAdapter<OPDSNavigationFeedEntry> adapter;
  private final List<CatalogNavigationLaneView>       lanes;

  public CatalogNavigationFeed(
    final Context in_context,
    final ArrayAdapter<OPDSNavigationFeedEntry> in_adapter,
    final OPDSNavigationFeed in_feed,
    final Activity in_activity,
    final CatalogNavigationLaneViewListenerType in_listener)
  {
    NullCheck.notNull(in_context);

    this.adapter = NullCheck.notNull(in_adapter);
    NullCheck.notNull(in_feed);
    NullCheck.notNull(in_activity);
    NullCheck.notNull(in_listener);

    final List<OPDSNavigationFeedEntry> entries = in_feed.getFeedEntries();
    final int size = entries.size();
    this.lanes = new ArrayList<CatalogNavigationLaneView>(size);

    for (int index = 0; index < size; ++index) {
      final OPDSNavigationFeedEntry e = NullCheck.notNull(entries.get(index));
      final CatalogNavigationLaneView cv =
        new CatalogNavigationLaneView(in_context, e, in_listener);
      this.lanes.add(cv);
    }
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
    final CatalogNavigationLaneView v =
      NullCheck.notNull(this.lanes.get(position));
    v.expensiveStart();
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
    final ExpensiveStoppableType lv = NullCheck.notNull((ExpensiveType) view);
    lv.expensiveStop();
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
