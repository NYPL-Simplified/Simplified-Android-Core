package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

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

public final class CatalogAcquisitionFeed implements
  ListAdapter,
  RecyclerListener
{
  private final Activity                               activity;
  private final ArrayAdapter<OPDSAcquisitionFeedEntry> adapter;
  private final OPDSAcquisitionFeed                    feed;
  private final ScreenSizeControllerType               screen;

  public CatalogAcquisitionFeed(
    final Context in_context,
    final ArrayAdapter<OPDSAcquisitionFeedEntry> in_adapter,
    final ScreenSizeControllerType in_screen,
    final OPDSAcquisitionFeed in_feed,
    final Activity in_activity)
  {
    NullCheck.notNull(in_context);

    this.adapter = NullCheck.notNull(in_adapter);
    NullCheck.notNull(in_screen);
    this.feed = NullCheck.notNull(in_feed);
    this.activity = NullCheck.notNull(in_activity);
    this.screen = NullCheck.notNull(in_screen);
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
    final int row_height =
      CatalogImageSizeEstimates
        .acquisitionFeedLargeThumbnailHeight(this.screen);
    final OPDSAcquisitionFeedEntry e =
      NullCheck.notNull(this.feed.getFeedEntries().get(position));

    final CatalogAcquisitionCellView cv =
      new CatalogAcquisitionCellView(
        this.activity,
        this.screen,
        e,
        row_height);

    cv.expensiveRequestDisplay();
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

  @Override public void onMovedToScrapHeap(
    final @Nullable View view)
  {
    final ExpensiveDisplayableType lv =
      NullCheck.notNull((ExpensiveDisplayableType) view);
    lv.expensiveRequestStopDisplaying();
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
