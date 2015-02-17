package org.nypl.simplified.app;

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

  private static void requestedPop()
  {
    final Simplified app = Simplified.get();
    final URI u = app.catalogFeedsPop();
    Log.d(CatalogActivity.TAG, String.format("Popped: %s", u));
  }

  private @Nullable ViewGroup content_area;
  private @Nullable ViewGroup loading;

  private boolean             root;

  private void goUp()
  {
    if (this.root) {
      this.finish();
    } else {
      CatalogActivity.requestedPop();

      final Intent i = new Intent(this, CatalogActivity.class);
      int flags = 0;
      flags |= Intent.FLAG_ACTIVITY_CLEAR_TOP;
      flags |= Intent.FLAG_ACTIVITY_NO_HISTORY;
      flags |= Intent.FLAG_ACTIVITY_NO_ANIMATION;
      i.setFlags(flags);

      final CatalogActivity a = this;
      final ViewGroup ca = NullCheck.notNull(this.content_area);
      final View ll = NullCheck.notNull(ca.getChildAt(0));

      CatalogActivity.doFadeOutWithRunnable(ll, new Runnable() {
        @Override public void run()
        {
          a.startActivity(i);
          a.finish();
          a.overridePendingTransition(0, 0);
        }
      });
    }
  }

  @Override public <A, E extends Exception> A matchPartActivity(
    final PartActivityMatcherType<A, E> m)
    throws E
  {
    return m.catalog(this);
  }

  @Override public void onBackPressed()
  {
    super.onBackPressed();
    this.goUp();
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(CatalogActivity.TAG, "onCreate");

    final Simplified app = Simplified.get();
    final OPDSFeedLoaderType loader = app.getFeedLoader();

    this.root = app.catalogFeedsCount() == 0;
    final URI feed_uri = app.catalogFeedsPeek();

    final LayoutInflater inflater =
      (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final ViewGroup ca =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.content_area));
    final ViewGroup la =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_loading,
        ca,
        false));

    ca.addView(la);
    ca.requestLayout();
    CatalogActivity.doFadeIn(la);

    final ActionBar bar = this.getActionBar();
    if (this.root) {
      bar.setDisplayHomeAsUpEnabled(false);
    } else {
      bar.setDisplayHomeAsUpEnabled(true);
    }

    this.content_area = NullCheck.notNull(ca);
    this.loading = NullCheck.notNull(la);

    loader.fromURI(feed_uri, new OPDSFeedLoadListenerType() {
      @Override public void onFailure(
        final Exception e)
      {
        CatalogActivity.this.runOnUiThread(new Runnable() {
          @Override public void run()
          {
            CatalogActivity.this.onReceiveFeedError(e);
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
              throws UnreachableCodeException
            {
              CatalogActivity.this.runOnUiThread(new Runnable() {
                @Override public void run()
                {
                  CatalogActivity.this.onReceiveAcquisitionFeed(af);
                }
              });
              return Unit.unit();
            }

            @Override public Unit navigation(
              final OPDSNavigationFeed nf)
              throws UnreachableCodeException
            {
              CatalogActivity.this.runOnUiThread(new Runnable() {
                @Override public void run()
                {
                  CatalogActivity.this.onReceiveNavigationFeed(nf);
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
        this.goUp();
        return true;
      }
      default:
      {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  private void onReceiveAcquisitionFeed(
    final OPDSAcquisitionFeed af)
  {
    Log.d(CatalogActivity.TAG, "Got acquisition feed: " + af);

    final ViewGroup ca = NullCheck.notNull(this.content_area);
    final ViewGroup la = NullCheck.notNull(this.loading);

    final LayoutInflater inflater =
      (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    final LinearLayout ll =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_acquisition_feed,
        ca,
        false));

    /**
     * Fade out progress, fade in layout.
     */

    CatalogActivity.doFadeOutWithRunnable(la, new Runnable() {
      @Override public void run()
      {
        ca.removeView(la);
        ca.requestLayout();
        la.setVisibility(View.GONE);

        ca.addView(ll);
        ca.requestLayout();
        CatalogActivity.doFadeIn(ll);
      }
    });
  }

  protected void onReceiveFeedError(
    final Exception e)
  {
    Log.e(CatalogActivity.TAG, "Failed to get feed: " + e, e);

    final ViewGroup ca = NullCheck.notNull(this.content_area);
    final ViewGroup la = NullCheck.notNull(this.loading);

    final LayoutInflater inflater =
      (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    final LinearLayout ll =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_loading_error,
        ca,
        false));

    /**
     * Fade out progress, fade in layout.
     */

    CatalogActivity.doFadeOutWithRunnable(la, new Runnable() {
      @Override public void run()
      {
        ca.removeView(la);
        ca.requestLayout();
        la.setVisibility(View.GONE);

        ca.addView(ll);
        ca.requestLayout();
        CatalogActivity.doFadeIn(ll);
      }
    });
  }

  private void onReceiveNavigationFeed(
    final OPDSNavigationFeed nf)
  {
    Log.d(CatalogActivity.TAG, "Got navigation feed: " + nf);

    final ViewGroup ca = NullCheck.notNull(this.content_area);
    final ViewGroup la = NullCheck.notNull(this.loading);

    final LayoutInflater inflater =
      (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    final LinearLayout ll =
      (LinearLayout) inflater.inflate(
        R.layout.catalog_navigation_feed,
        ca,
        false);

    final ListView lv =
      (ListView) ll.findViewById(R.id.catalog_nav_feed_list);
    lv.setVerticalScrollBarEnabled(false);
    lv.setDividerHeight(0);

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

          CatalogActivity.doFadeOutWithRunnable(ll, new Runnable() {
            @Override public void run()
            {
              final Intent i = new Intent();
              app.catalogFeedsPush(e.getTargetURI());
              i.setClass(CatalogActivity.this, CatalogActivity.class);

              int flags = 0;
              flags |= Intent.FLAG_ACTIVITY_CLEAR_TOP;
              flags |= Intent.FLAG_ACTIVITY_NO_HISTORY;
              flags |= Intent.FLAG_ACTIVITY_NO_ANIMATION;
              flags |= Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;

              i.setFlags(flags);
              CatalogActivity.this.startActivity(i);
              CatalogActivity.this.overridePendingTransition(0, 0);
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
        lv,
        nf.getFeedEntries(),
        listener);

    lv.setAdapter(adapter);

    /**
     * Fade out progress, fade in layout.
     */

    CatalogActivity.doFadeOutWithRunnable(la, new Runnable() {
      @Override public void run()
      {
        ca.removeView(la);
        ca.requestLayout();
        la.setVisibility(View.GONE);

        ca.addView(ll);
        ca.requestLayout();
        CatalogActivity.doFadeIn(ll);
      }
    });
  }
}
