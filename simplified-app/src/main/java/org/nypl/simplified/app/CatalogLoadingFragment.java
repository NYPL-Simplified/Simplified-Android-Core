package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings("synthetic-access") public final class CatalogLoadingFragment extends
  CatalogFragment
{
  private static final String FEED_URI_ID;
  private static final String TAG;

  static {
    FEED_URI_ID = "org.nypl.simplified.app.CatalogLoadingFragment.feed";
    TAG = "CLoad";
  }

  public static CatalogLoadingFragment newInstance(
    final URI feed_uri,
    final ImmutableList<URI> up_stack)
  {
    final Bundle b = new Bundle();

    b.putSerializable(
      CatalogLoadingFragment.FEED_URI_ID,
      NullCheck.notNull(feed_uri));
    b.putSerializable(
      CatalogFragment.FEED_UP_STACK,
      NullCheck.notNull(up_stack));

    final CatalogLoadingFragment f = new CatalogLoadingFragment();
    f.setArguments(b);
    return f;
  }

  private @Nullable OPDSAcquisitionFeed            feed_acq;
  private @Nullable OPDSNavigationFeed             feed_nav;
  private @Nullable LinearLayout                   layout;
  private @Nullable ListenableFuture<OPDSFeedType> loading;

  private void onAcquisitionFeedReceived(
    final OPDSAcquisitionFeed af)
  {
    this.feed_acq = af;
    this.onAcquisitionFeedShow();
  }

  private void onAcquisitionFeedShow()
  {
    final FragmentControllerType act =
      (FragmentControllerType) this.getActivity();
    final OPDSAcquisitionFeed af = NullCheck.notNull(this.feed_acq);
    final ImmutableList<URI> us = NullCheck.notNull(this.up_stack);
    final CatalogAcquisitionFeedFragment f =
      CatalogAcquisitionFeedFragment.newInstance(af, us);
    act.fragControllerSetContentFragmentWithoutBack(f);
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(CatalogLoadingFragment.TAG, "onCreate: " + this);

    final Bundle a = NullCheck.notNull(this.getArguments());

    final URI uri =
      NullCheck.notNull((URI) a
        .getSerializable(CatalogLoadingFragment.FEED_URI_ID));
    this.up_stack =
      NullCheck.notNull((ImmutableList<URI>) a
        .getSerializable(CatalogFragment.FEED_UP_STACK));
    this.debugShowUpStack();

    final Simplified app = Simplified.get();
    final OPDSFeedLoaderType loader = app.getFeedLoader();

    this.loading = loader.fromURI(uri, new OPDSFeedLoadListenerType() {
      @Override public void onFailure(
        final Throwable ex)
      {
        CatalogLoadingFragment.this.onFeedFailure(ex);
      }

      @Override public void onSuccess(
        final OPDSFeedType f)
      {
        f
          .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
            @Override public Unit acquisition(
              final OPDSAcquisitionFeed af)
            {
              CatalogLoadingFragment.this.onAcquisitionFeedReceived(af);
              return Unit.unit();
            }

            @Override public Unit navigation(
              final OPDSNavigationFeed nf)
            {
              CatalogLoadingFragment.this.onNavigationFeedReceived(nf);
              return Unit.unit();
            }
          });
      }
    });
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    assert inflater != null;

    final LinearLayout in_layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_loading,
        container,
        false));

    this.layout = in_layout;
    return in_layout;
  }

  @Override public void onDestroy()
  {
    super.onDestroy();

    final ListenableFuture<OPDSFeedType> f = this.loading;
    if (f != null) {
      f.cancel(true);
    }
  }

  @Override public void onDetach()
  {
    super.onDetach();
    Log.d(CatalogLoadingFragment.TAG, "onDetach: " + this);
  }

  private void onFeedFailure(
    final Throwable ex)
  {
    // TODO Auto-generated method stub
    throw new UnimplementedCodeException();
  }

  private void onNavigationFeedReceived(
    final OPDSNavigationFeed nf)
  {
    this.feed_nav = nf;
    this.onNavigationFeedShow();
  }

  private void onNavigationFeedShow()
  {
    final FragmentControllerType act =
      (FragmentControllerType) this.getActivity();
    final OPDSNavigationFeed nf = NullCheck.notNull(this.feed_nav);
    final ImmutableList<URI> us = NullCheck.notNull(this.up_stack);
    final CatalogNavigationFeedFragment f =
      CatalogNavigationFeedFragment.newInstance(nf, us);
    act.fragControllerSetContentFragmentWithoutBack(f);
  }

  @Override public void onResume()
  {
    super.onResume();
    Log.d(CatalogLoadingFragment.TAG, "onResume: " + this);
  }

  @Override public void onStop()
  {
    super.onStop();
    Log.d(CatalogLoadingFragment.TAG, "onStop: " + this);
  }

  @Override public void onViewStateRestored(
    final @Nullable Bundle state)
  {
    super.onViewStateRestored(state);
    Log.d(CatalogLoadingFragment.TAG, "onViewStateRestored: " + this);
  }
}
