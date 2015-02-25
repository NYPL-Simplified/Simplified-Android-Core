package org.nypl.simplified.app;

import java.net.URI;
import java.util.List;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.app.Activity;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogNavigationFeedFragment extends CatalogFragment implements
  CatalogLaneViewListenerType,
  ListAdapter,
  RecyclerListener
{
  private static final String FEED_ID;
  private static final String TAG;

  static {
    TAG = "CNavFeed";
    FEED_ID = "org.nypl.simplified.app.CatalogNavigationFeedFragment.feed";
  }

  public static CatalogNavigationFeedFragment newInstance(
    final OPDSNavigationFeed feed,
    final ImmutableList<URI> up_stack)
  {
    final Bundle b = new Bundle();

    b.putSerializable(
      CatalogNavigationFeedFragment.FEED_ID,
      NullCheck.notNull(feed));
    b.putSerializable(
      CatalogFragment.FEED_UP_STACK,
      NullCheck.notNull(up_stack));

    final CatalogNavigationFeedFragment f =
      new CatalogNavigationFeedFragment();
    f.setArguments(b);
    return f;
  }

  private static ImmutableList<URI> stackPlus(
    final ImmutableList<URI> current_up,
    final URI u)
  {
    final Builder<URI> b = ImmutableList.builder();
    b.addAll(current_up);
    b.add(u);
    return NullCheck.notNull(b.build());
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

    this.up_stack =
      NullCheck.notNull((ImmutableList<URI>) args
        .getSerializable(CatalogFragment.FEED_UP_STACK));
    this.debugShowUpStack();

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

    /**
     * Push the URI of the current feed onto a new up stack for the new
     * fragment.
     */

    final OPDSNavigationFeed f = NullCheck.notNull(this.feed);
    final ImmutableList<URI> current_up = NullCheck.notNull(this.up_stack);
    final ImmutableList<URI> new_up =
      CatalogNavigationFeedFragment.stackPlus(current_up, f.getFeedURI());

    /**
     * Create a new fragment or dialog based on the current device screen
     * size.
     */

    final MainActivityType act = (MainActivityType) this.getActivity();
    if (act.hasLargeScreen()) {
      final CatalogBookDialog df = new CatalogBookDialog();
      act.fragControllerSetAndShowDialog(df);
    } else {
      final CatalogBookDetailFragment df =
        CatalogBookDetailFragment.newInstance(in_book, new_up);
      act.fragControllerSetContentFragmentWithBackReturn(this, df);
    }
  }

  @Override public void onSelectFeed(
    final CatalogLaneView in_view,
    final OPDSNavigationFeedEntry in_feed)
  {
    NullCheck.notNull(in_view);
    NullCheck.notNull(in_feed);
    Log.d(CatalogNavigationFeedFragment.TAG, "onSelectFeed: " + in_feed);

    final MainActivityType act = (MainActivityType) this.getActivity();

    /**
     * Push the URI of the current feed onto a new up stack for the new
     * fragment.
     */

    final OPDSNavigationFeed f = NullCheck.notNull(this.feed);
    final ImmutableList<URI> current_up = NullCheck.notNull(this.up_stack);
    final ImmutableList<URI> new_up =
      CatalogNavigationFeedFragment.stackPlus(current_up, f.getFeedURI());

    final CatalogLoadingFragment nf =
      CatalogLoadingFragment.newInstance(in_feed.getTargetURI(), new_up);
    act.fragControllerSetContentFragmentWithBackReturn(this, nf);
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
