package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

public final class CatalogLoadingFragment extends NavigableFragment
{
  static final String         CATALOG_LOADING_FRAGMENT_URI_ID;
  private static final String TAG = "CatalogLoadingFragment";

  static {
    CATALOG_LOADING_FRAGMENT_URI_ID =
      "org.nypl.simplified.app.CatalogLoadingFragment.uri";
  }

  /**
   * Construct a new instance without a parent.
   *
   * @param feed_uri
   *          The URI of the feed
   * @param container
   *          The container
   * @return A new instance
   */

  public static NavigableFragment newInstanceWithoutParent(
    final URI feed_uri,
    final int container)
  {
    NullCheck.notNull(feed_uri);

    final CatalogLoadingFragment f = new CatalogLoadingFragment();
    NavigableFragment.setFragmentArguments(f, null, container);
    final Bundle a = f.getArguments();
    a.putSerializable(
      CatalogLoadingFragment.CATALOG_LOADING_FRAGMENT_URI_ID,
      feed_uri);
    return f;
  }

  /**
   * Construct a new instance with a parent.
   *
   * @param feed_uri
   *          The URI of the feed
   * @param container
   *          The container
   * @return A new instance
   */

  public static NavigableFragment newInstanceWithParent(
    final URI feed_uri,
    final NavigableFragment parent,
    final int container)
  {
    NullCheck.notNull(feed_uri);

    final CatalogLoadingFragment f = new CatalogLoadingFragment();
    NavigableFragment.setFragmentArguments(
      f,
      NullCheck.notNull(parent),
      container);
    final Bundle a = f.getArguments();
    a.putSerializable(
      CatalogLoadingFragment.CATALOG_LOADING_FRAGMENT_URI_ID,
      feed_uri);
    return f;
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
  }

  @Override public void onResume()
  {
    super.onResume();
    Log.d(CatalogLoadingFragment.TAG, "onResume: " + this);
    this.loadFeed();
  }

  private void loadFeed()
  {
    Log.d(CatalogLoadingFragment.TAG, "id: " + this.getNavigableID());

    final Bundle a = NullCheck.notNull(this.getArguments());
    final URI u =
      (URI) NullCheck
        .notNull(a
          .getSerializable(CatalogLoadingFragment.CATALOG_LOADING_FRAGMENT_URI_ID));

    final FragmentManager fm = this.getFragmentManager();
    final Activity act = this.getActivity();
    final Simplified app = (Simplified) act.getApplication();
    final OPDSFeedLoaderType loader = app.getFeedLoader();
    final int cid = this.getNavigableContainerID();

    /**
     * Asynchronously download and parse the feed at the given URI, displaying
     * a message if something goes wrong.
     */

    loader.fromURI(u, new OPDSFeedLoadListenerType() {
      @Override public void onFailure(
        final Exception e)
      {
        Log.e(CatalogLoadingFragment.TAG, "Error loading feed: " + u, e);

        final NavigableFragment fn =
          CatalogLoadingErrorFragment.newInstance(u, cid);

        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(cid, fn, "Error");
        ft.commit();
      }

      @Override public void onSuccess(
        final OPDSFeedType f)
      {
        /**
         * A feed was received, create a new fragment based on the type of
         * feed and display it.
         */

        Log.d(CatalogLoadingFragment.TAG, "Loaded feed: " + u);

        f
          .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
            @Override public Unit acquisition(
              final OPDSAcquisitionFeed af)
            {
              Log.d(CatalogLoadingFragment.TAG, "Feed "
                + u
                + " is acquisition feed");

              final NavigableFragment ff =
                CatalogAcquisitionFeedFragment.newInstance(af, cid);
              final FragmentTransaction ft = fm.beginTransaction();

              ft.replace(cid, ff, af.getFeedTitle());
              ft.commit();
              return Unit.unit();
            }

            @Override public Unit navigation(
              final OPDSNavigationFeed nf)
            {
              Log.d(CatalogLoadingFragment.TAG, "Feed "
                + u
                + " is navigation feed");

              final NavigableFragment ff =
                CatalogNavigationFeedFragment.newInstance(nf, cid);
              final FragmentTransaction ft = fm.beginTransaction();

              ft.replace(cid, ff, nf.getFeedTitle());
              ft.commit();
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
    final View view =
      NullCheck.notNull(inflater.inflate(
        R.layout.catalog_loading,
        container,
        false));
    return view;
  }
}
