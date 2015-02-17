package org.nypl.simplified.app;

import java.io.Serializable;
import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings("synthetic-access") public final class CatalogActivity extends
  PartActivity
{
  private static final class StateAcquisition implements
    Serializable,
    StateType
  {
    private static final long         serialVersionUID = 12398712937189238L;
    private final ViewGroup           content_area;
    private final OPDSAcquisitionFeed feed;
    private final LinearLayout        layout;
    private final ViewGroup           loading;
    private final boolean             root;
    private final URI                 uri;

    private StateAcquisition(
      final ViewGroup in_content_area,
      final ViewGroup in_loading,
      final LinearLayout in_layout,
      final boolean in_root,
      final OPDSAcquisitionFeed in_feed,
      final URI in_uri)
    {
      this.content_area = in_content_area;
      this.loading = in_loading;
      this.layout = in_layout;
      this.root = in_root;
      this.feed = in_feed;
      this.uri = in_uri;
    }

    @Override public ViewGroup getContentArea()
    {
      return this.content_area;
    }

    @Override public LinearLayout getLayout()
    {
      return this.layout;
    }

    @Override public ViewGroup getProgressBar()
    {
      return this.loading;
    }

    @Override public void matchState(
      final StateTypeMatcherType m)
    {
      m.acquisition(this);
    }
  }

  private static final class StateError implements Serializable, StateType
  {
    private static final long  serialVersionUID = 12398712937189238L;
    private final ViewGroup    content_area;
    private final Exception    error;
    private final LinearLayout layout;
    private final ViewGroup    loading;
    private final boolean      root;
    private final URI          uri;

    private StateError(
      final ViewGroup in_content_area,
      final ViewGroup in_loading,
      final LinearLayout in_layout,
      final boolean in_root,
      final Exception in_error,
      final URI in_uri)
    {
      this.content_area = in_content_area;
      this.loading = in_loading;
      this.layout = in_layout;
      this.root = in_root;
      this.error = in_error;
      this.uri = in_uri;
    }

    @Override public ViewGroup getContentArea()
    {
      return this.content_area;
    }

    @Override public LinearLayout getLayout()
    {
      return this.layout;
    }

    @Override public ViewGroup getProgressBar()
    {
      return this.loading;
    }

    @Override public void matchState(
      final StateTypeMatcherType m)
    {
      m.error(this);
    }
  }

  private static final class StateNavigation implements
    Serializable,
    StateType
  {
    private static final long        serialVersionUID = 1567589930716465962L;
    private final ViewGroup          content_area;
    private final OPDSNavigationFeed feed;
    private final LinearLayout       layout;
    private final ListView           list;
    private final ViewGroup          loading;
    private final boolean            root;
    private final URI                uri;

    private StateNavigation(
      final ViewGroup in_content_area,
      final ViewGroup in_loading,
      final LinearLayout in_layout,
      final ListView in_list,
      final boolean in_root,
      final OPDSNavigationFeed in_feed,
      final URI in_uri)
    {
      this.content_area = in_content_area;
      this.loading = in_loading;
      this.layout = in_layout;
      this.list = in_list;
      this.root = in_root;
      this.feed = in_feed;
      this.uri = in_uri;
    }

    @Override public ViewGroup getContentArea()
    {
      return this.content_area;
    }

    @Override public LinearLayout getLayout()
    {
      return this.layout;
    }

    @Override public ViewGroup getProgressBar()
    {
      return this.loading;
    }

    @Override public void matchState(
      final StateTypeMatcherType m)
    {
      m.navigation(this);
    }
  }

  private static interface StateType
  {
    ViewGroup getContentArea();

    LinearLayout getLayout();

    ViewGroup getProgressBar();

    void matchState(
      final StateTypeMatcherType m);
  }

  private static interface StateTypeMatcherType
  {
    void acquisition(
      StateAcquisition s);

    void error(
      StateError s);

    void navigation(
      StateNavigation s);
  }

  private static final int    FADE_TIME = 100;
  private static final String TAG       = "CatalogActivity";

  private static void doFadeIn(
    final View v)
  {
    v.setAlpha(0.0f);
    v.animate().setDuration(CatalogActivity.FADE_TIME).alpha(1.0f);
  }

  private static void doFadeOutWithRunnable(
    final View v,
    final Runnable r)
  {
    v
      .animate()
      .alpha(0.0f)
      .setDuration(CatalogActivity.FADE_TIME)
      .withEndAction(r);
  }

  private static void onShowLayout(
    final StateType s)
  {
    final ViewGroup loading = s.getProgressBar();
    final LinearLayout layout = s.getLayout();
    final ViewGroup content_area = s.getContentArea();

    /**
     * If the progress bar is visible, then the layout has never been faded
     * in. Add the layout to the content area and fade it in whilst fading out
     * the progress bar.
     *
     * Otherwise, if the progress bar isn't visible, then the content area
     * should be faded back in.
     */

    if (loading.getVisibility() == View.VISIBLE) {
      CatalogActivity.doFadeOutWithRunnable(layout, new Runnable() {
        @Override public void run()
        {
          content_area.removeView(loading);
          content_area.requestLayout();
          loading.setVisibility(View.GONE);

          content_area.addView(layout);
          content_area.requestLayout();
          CatalogActivity.doFadeIn(layout);
        }
      });
    } else {
      CatalogActivity.doFadeIn(layout);
    }
  }

  private static void requestedPop()
  {
    final Simplified app = Simplified.get();
    final URI u = app.catalogFeedsPop();
    Log.d(CatalogActivity.TAG, String.format("Popped: %s", u));
  }

  private @Nullable StateType current_state;

  @Override public <A, E extends Exception> A matchPartActivity(
    final PartActivityMatcherType<A, E> m)
    throws E
  {
    return m.catalog(this);
  }

  @Override public void onBackPressed()
  {
    super.onBackPressed();
    CatalogActivity.requestedPop();
    this.finish();
    this.overridePendingTransition(0, 0);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(CatalogActivity.TAG, "onCreate");

    final Simplified app = Simplified.get();
    final OPDSFeedLoaderType loader = app.getFeedLoader();
    final URI feed_uri = app.catalogFeedsPeek();

    final LayoutInflater inflater =
      NullCheck.notNull((LayoutInflater) this
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    final ViewGroup content_area =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.content_area));
    final ViewGroup loading =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_loading,
        content_area,
        false));

    content_area.addView(loading);
    content_area.requestLayout();
    CatalogActivity.doFadeIn(loading);

    final ActionBar bar = this.getActionBar();
    if (app.catalogFeedsCount() > 0) {
      bar.setDisplayHomeAsUpEnabled(true);
    } else {
      bar.setDisplayHomeAsUpEnabled(false);
    }

    loader.fromURI(feed_uri, new OPDSFeedLoadListenerType() {
      @Override public void onFailure(
        final Exception e)
      {
        CatalogActivity.this.runOnUiThread(new Runnable() {
          @Override public void run()
          {
            CatalogActivity.this.onReceiveFeedError(
              inflater,
              content_area,
              loading,
              e,
              feed_uri);
          }
        });
      }

      @Override public void onSuccess(
        final OPDSFeedType f)
      {
        f
          .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
            @Override public Unit acquisition(
              final OPDSAcquisitionFeed af)
            {
              CatalogActivity.this.runOnUiThread(new Runnable() {
                @Override public void run()
                {
                  CatalogActivity.this.onReceiveAcquisitionFeed(
                    inflater,
                    content_area,
                    loading,
                    af,
                    feed_uri);
                }
              });
              return Unit.unit();
            }

            @Override public Unit navigation(
              final OPDSNavigationFeed nf)
            {
              CatalogActivity.this.runOnUiThread(new Runnable() {
                @Override public void run()
                {
                  CatalogActivity.this.onReceiveNavigationFeed(
                    inflater,
                    content_area,
                    loading,
                    nf,
                    feed_uri);
                }
              });
              return Unit.unit();
            }
          });
      }
    });
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item)
  {
    assert item != null;

    switch (item.getItemId()) {
      case android.R.id.home:
      {
        CatalogActivity.requestedPop();
        this.finish();
        this.overridePendingTransition(0, 0);
        return true;
      }
      default:
      {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  private void onReceiveAcquisitionFeed(
    final LayoutInflater inflater,
    final ViewGroup content_area,
    final ViewGroup loading,
    final OPDSAcquisitionFeed af,
    final URI feed_uri)
  {
    Log.d(CatalogActivity.TAG, "Got acquisition feed: " + af);

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_acquisition_feed,
        content_area,
        false));
    layout.setAlpha(0.0f);

    final StateAcquisition sa =
      new StateAcquisition(content_area, loading, layout, false, af, feed_uri);
    this.current_state = sa;
    CatalogActivity.onShowLayout(sa);
  }

  private void onReceiveFeedError(
    final LayoutInflater inflater,
    final ViewGroup content_area,
    final ViewGroup loading,
    final Exception e,
    final URI feed_uri)
  {
    Log.e(CatalogActivity.TAG, "Failed to get feed: " + e, e);

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_loading_error,
        content_area,
        false));
    layout.setAlpha(0.0f);

    final StateError se =
      new StateError(content_area, loading, layout, false, e, feed_uri);
    this.current_state = se;
    CatalogActivity.onShowLayout(se);
  }

  private void onReceiveNavigationFeed(
    final LayoutInflater inflater,
    final ViewGroup content_area,
    final ViewGroup loading,
    final OPDSNavigationFeed nf,
    final URI feed_uri)
  {
    Log.d(CatalogActivity.TAG, "Got navigation feed: " + nf);

    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_navigation_feed,
        content_area,
        false));
    layout.setAlpha(0.0f);

    final ListView list_view =
      NullCheck.notNull((ListView) layout
        .findViewById(R.id.catalog_nav_feed_list));

    list_view.setVerticalScrollBarEnabled(false);
    list_view.setDividerHeight(0);

    /**
     * Construct a listener that will open the target feed in a new activity
     * whenever a user clicks on the title of a feed entry.
     */

    final Simplified app = Simplified.get();
    final CatalogNavigationFeedTitleClickListener listener =
      new CatalogNavigationFeedTitleClickListener() {
        @Override public void onClick(
          final OPDSNavigationFeedEntry e)
        {
          /**
           * Fade out layout.
           */

          CatalogActivity.doFadeOutWithRunnable(layout, new Runnable() {
            @Override public void run()
            {
              final Intent i = new Intent();
              app.catalogFeedsPush(e.getTargetURI());
              i.setClass(CatalogActivity.this, CatalogActivity.class);

              int flags = 0;
              flags |= Intent.FLAG_ACTIVITY_NO_ANIMATION;

              i.setFlags(flags);
              CatalogActivity.this.startActivity(i);
            }
          });
        }
      };

    /**
     * Construct a list adapter that ties everything together.
     */

    final CatalogNavigationFeedAdapter adapter =
      new CatalogNavigationFeedAdapter(
        this,
        list_view,
        nf.getFeedEntries(),
        listener);
    list_view.setAdapter(adapter);

    final StateNavigation sn =
      new StateNavigation(
        content_area,
        loading,
        layout,
        list_view,
        false,
        nf,
        feed_uri);
    this.current_state = sn;
    CatalogActivity.onShowLayout(sn);
  }

  @Override protected void onResume()
  {
    super.onResume();
    Log.d(CatalogActivity.TAG, "onResume");

    /**
     * If the current state is unset, then it means that the activity is
     * "resuming" having not yet actually received a feed.
     */

    final StateType s = this.current_state;
    if (s != null) {
      CatalogActivity.onShowLayout(s);
    }
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append("[CatalogActivity@");
    b.append(this.hashCode());
    b.append(" ");
    final StateType s = this.current_state;
    if (s != null) {
      s.matchState(new StateTypeMatcherType() {
        @Override public void acquisition(
          final StateAcquisition ss)
        {
          b.append(" (Acquisition ");
          b.append(ss.uri);
          b.append(")");
        }

        @Override public void error(
          final StateError se)
        {
          b.append(" (Error ");
          b.append(se.uri);
          b.append(")");
        }

        @Override public void navigation(
          final StateNavigation sn)
        {
          b.append(" (Navigation ");
          b.append(sn.uri);
          b.append(")");
        }
      });
    }
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
