package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSNavigationFeed;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogNavigationFeedFragment extends NavigableFragment
{
  private static final String NAVIGATION_FEED_ID;

  static {
    NAVIGATION_FEED_ID =
      "org.nypl.simplified.app.CatalogNavigationFeedFragment.feed";
  }

  /**
   * Construct a new instance without a parent.
   *
   * @param feed
   *          The navigation feed
   * @param container
   *          The container
   * @return A new instance
   */

  public static NavigableFragment newInstance(
    final OPDSNavigationFeed feed,
    final int container)
  {
    NullCheck.notNull(feed);

    final CatalogNavigationFeedFragment f =
      new CatalogNavigationFeedFragment();
    NavigableFragment.setFragmentArguments(f, null, container);
    final Bundle b = f.getArguments();
    b.putSerializable(CatalogNavigationFeedFragment.NAVIGATION_FEED_ID, feed);
    return f;
  }

  private @Nullable OPDSNavigationFeed feed;

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final Bundle a = NullCheck.notNull(this.getArguments());
    this.feed =
      (OPDSNavigationFeed) NullCheck.notNull(a
        .getSerializable(CatalogNavigationFeedFragment.NAVIGATION_FEED_ID));
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;
    final View view =
      NullCheck.notNull(inflater.inflate(
        R.layout.catalog_navigation_feed,
        container,
        false));

    final OPDSNavigationFeed f = NullCheck.notNull(this.feed);
    final Activity act = NullCheck.notNull(this.getActivity());

    final ListView lv =
      (ListView) view.findViewById(R.id.catalog_nav_feed_list);
    lv.setVerticalScrollBarEnabled(false);
    lv.setDividerHeight(0);

    final CatalogNavigationFeedAdapter adapter =
      new CatalogNavigationFeedAdapter(act, lv, f.getFeedEntries());

    lv.setAdapter(adapter);
    return view;
  }
}
