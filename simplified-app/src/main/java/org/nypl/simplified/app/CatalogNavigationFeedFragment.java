package org.nypl.simplified.app;

import java.util.List;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.app.Activity;
import android.app.Fragment;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogNavigationFeedFragment extends Fragment implements
  CatalogLaneViewListenerType,
  ListAdapter,
  RecyclerListener
{
  private static final String FEED_ID;
  private static final String TAG;

  static {
    TAG = "CatalogNavigationFeedFragment";
    FEED_ID = "org.nypl.simplified.app.CatalogNavigationFeedFragment.feed";
  }

  public static CatalogNavigationFeedFragment newInstance(
    final OPDSNavigationFeed feed)
  {
    final Bundle b = new Bundle();
    b.putSerializable(
      CatalogNavigationFeedFragment.FEED_ID,
      NullCheck.notNull(feed));

    final CatalogNavigationFeedFragment f =
      new CatalogNavigationFeedFragment();
    f.setArguments(b);
    return f;
  }

  private @Nullable ArrayAdapter<OPDSNavigationFeedEntry> adapter;
  private @Nullable OPDSNavigationFeed                    feed;

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

    final Activity act = NullCheck.notNull(this.getActivity());
    final CatalogLaneView v = new CatalogLaneView(act);
    v.setLaneViewFeedAndListener(e, this);
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

  @Override public void onAttach(
    final @Nullable Activity activity)
  {
    super.onAttach(activity);
    Log.d(CatalogNavigationFeedFragment.TAG, "onAttach: " + this);
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(CatalogNavigationFeedFragment.TAG, "onCreate: " + this);

    final Bundle args = this.getArguments();

    final OPDSNavigationFeed in_feed =
      NullCheck.notNull((OPDSNavigationFeed) args
        .getSerializable(CatalogNavigationFeedFragment.FEED_ID));
    final Activity act = NullCheck.notNull(this.getActivity());
    final ArrayAdapter<OPDSNavigationFeedEntry> in_adapter =
      new ArrayAdapter<OPDSNavigationFeedEntry>(
        act,
        0,
        in_feed.getFeedEntries());

    this.feed = in_feed;
    this.adapter = in_adapter;
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    Log.d(CatalogNavigationFeedFragment.TAG, "onCreateView: " + this);

    assert inflater != null;

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_navigation_feed,
        container,
        false));

    final ListView lv =
      NullCheck.notNull((ListView) layout
        .findViewById(R.id.catalog_nav_feed_list));
    lv.setAdapter(this);
    lv.setRecyclerListener(this);
    return layout;
  }

  @Override public void onDestroy()
  {
    super.onDestroy();
    Log.d(CatalogNavigationFeedFragment.TAG, "onDestroy: " + this);
  }

  @Override public void onDestroyView()
  {
    super.onDestroyView();
    Log.d(CatalogNavigationFeedFragment.TAG, "onDestroyView: " + this);
  }

  @Override public void onDetach()
  {
    super.onDetach();
    Log.d(CatalogNavigationFeedFragment.TAG, "onDetach: " + this);
  }

  @Override public void onMovedToScrapHeap(
    final @Nullable View view)
  {
    final CatalogLaneView lv = NullCheck.notNull((CatalogLaneView) view);
    lv.cancel();
  }

  @Override public void onResume()
  {
    super.onResume();
    Log.d(CatalogNavigationFeedFragment.TAG, "onResume: " + this);
  }

  @Override public void onSelectBook(
    final CatalogLaneView in_view,
    final OPDSAcquisitionFeedEntry in_book)
  {
    NullCheck.notNull(in_view);
    NullCheck.notNull(in_book);
    Log.d(CatalogNavigationFeedFragment.TAG, "onSelectBook: " + in_book);
  }

  @Override public void onSelectFeed(
    final CatalogLaneView in_view,
    final OPDSNavigationFeedEntry in_feed)
  {
    NullCheck.notNull(in_view);
    NullCheck.notNull(in_feed);
    Log.d(CatalogNavigationFeedFragment.TAG, "onSelectFeed: " + in_feed);

    final MainActivity act = (MainActivity) this.getActivity();
    final CatalogLoadingFragment f =
      CatalogLoadingFragment.newInstance(in_feed.getTargetURI());
    act.replaceFragmentWithBackstack(f);
  }

  @Override public void onStop()
  {
    super.onStop();
    Log.d(CatalogNavigationFeedFragment.TAG, "onStop: " + this);
  }

  @Override public void onViewStateRestored(
    final @Nullable Bundle state)
  {
    super.onViewStateRestored(state);
    Log.d(CatalogNavigationFeedFragment.TAG, "onViewStateRestored: " + this);
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
