package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.common.collect.ImmutableList;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogAcquisitionFeedFragment extends CatalogFragment
{
  private static final String FEED_ID;
  private static final String TAG;

  static {
    TAG = "CatalogAcquisitionFeedFragment";
    FEED_ID = "org.nypl.simplified.app.CatalogAcquisitionFeedFragment.feed";
  }

  public static CatalogAcquisitionFeedFragment newInstance(
    final OPDSAcquisitionFeed feed,
    final ImmutableList<URI> up_stack)
  {
    final Bundle b = new Bundle();

    b.putSerializable(
      CatalogAcquisitionFeedFragment.FEED_ID,
      NullCheck.notNull(feed));
    b.putSerializable(
      CatalogFragment.FEED_UP_STACK,
      NullCheck.notNull(up_stack));

    final CatalogAcquisitionFeedFragment f =
      new CatalogAcquisitionFeedFragment();
    f.setArguments(b);
    return f;
  }

  private @Nullable OPDSAcquisitionFeed feed;

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(CatalogAcquisitionFeedFragment.TAG, "onCreate: " + this);

    final Bundle args = this.getArguments();

    final OPDSAcquisitionFeed in_feed =
      NullCheck.notNull((OPDSAcquisitionFeed) args
        .getSerializable(CatalogAcquisitionFeedFragment.FEED_ID));
    final ImmutableList<URI> us =
      NullCheck.notNull((ImmutableList<URI>) args
        .getSerializable(CatalogFragment.FEED_UP_STACK));
    this.debugShowUpStack();

    this.feed = in_feed;
    this.setUpStack(us);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_acquisition_feed,
        container,
        false));

    return layout;
  }

  @Override public void onStop()
  {
    super.onStop();
    Log.d(CatalogAcquisitionFeedFragment.TAG, "onStop: " + this);
  }

  @Override public void onViewStateRestored(
    final @Nullable Bundle state)
  {
    super.onViewStateRestored(state);
    Log.d(CatalogAcquisitionFeedFragment.TAG, "onViewStateRestored: " + this);
  }
}
