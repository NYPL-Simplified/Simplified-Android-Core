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
import android.content.res.Resources;
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

public final class CatalogActivity extends PartActivity
{
  private static final String FEED_URI_ID;
  private static final String TAG = "CatalogActivity";

  static {
    FEED_URI_ID = "org.nypl.simplified.app.CatalogActivity.uri";
  }

  private static boolean getActivityIsRoot(
    final Bundle ex)
  {
    return ex.get(CatalogActivity.FEED_URI_ID) == null;
  }

  private static URI getArgumentsURI(
    final Resources rr,
    final Bundle ex)
  {
    final URI u = (URI) ex.get(CatalogActivity.FEED_URI_ID);
    if (u != null) {
      return u;
    }
    return NullCheck.notNull(URI.create(rr
      .getString(R.string.catalog_start_uri)));
  }

  private @Nullable ViewGroup content_area;
  private @Nullable ViewGroup loading;
  private boolean             root;
  private @Nullable URI       uri;

  private void goUp()
  {
    if (this.root) {
      this.finish();
    } else {
      this.requestedPop();
      final Intent i = new Intent(this, CatalogActivity.class);
      int flags = 0;
      flags |= Intent.FLAG_ACTIVITY_CLEAR_TOP;
      flags |= Intent.FLAG_ACTIVITY_NO_HISTORY;
      flags |= Intent.FLAG_ACTIVITY_NO_ANIMATION;
      i.setFlags(flags);
      this.startActivity(i);
      this.finish();
      this.overridePendingTransition(0, 0);
    }
  }

  private ViewGroup hideProgressAndGetContentArea()
  {
    final ViewGroup ca = NullCheck.notNull(this.content_area);
    final ViewGroup la = NullCheck.notNull(this.loading);
    ca.removeView(la);
    ca.requestLayout();
    la.setVisibility(View.GONE);
    return ca;
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

    final ViewGroup ca =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.content_area));
    final ViewGroup la =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_loading,
        ca,
        false));

    ca.addView(la);
    ca.requestLayout();

    final ActionBar bar = this.getActionBar();
    if (this.root) {
      bar.setDisplayHomeAsUpEnabled(false);
    } else {
      bar.setDisplayHomeAsUpEnabled(true);
    }

    this.content_area = ca;
    this.loading = la;
    this.uri = feed_uri;
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

    final ViewGroup ca = this.hideProgressAndGetContentArea();

    final LayoutInflater inflater =
      (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    final LinearLayout ll =
      (LinearLayout) inflater.inflate(
        R.layout.catalog_acquisition_feed,
        ca,
        false);

    ca.addView(ll);
    ca.requestLayout();
  }

  protected void onReceiveFeedError(
    final Exception e)
  {
    Log.e(CatalogActivity.TAG, "Failed to get feed: " + e, e);

    final ViewGroup ca = this.hideProgressAndGetContentArea();

    final LayoutInflater inflater =
      (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    final LinearLayout ll =
      (LinearLayout) inflater.inflate(
        R.layout.catalog_loading_error,
        ca,
        false);

    ca.addView(ll);
    ca.requestLayout();
  }

  private void onReceiveNavigationFeed(
    final OPDSNavigationFeed nf)
  {
    Log.d(CatalogActivity.TAG, "Got navigation feed: " + nf);

    final ViewGroup ca = this.hideProgressAndGetContentArea();

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
          final Intent i = new Intent();
          app.catalogFeedsPush(e.getTargetURI());
          i.setClass(CatalogActivity.this, CatalogActivity.class);
          i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
          CatalogActivity.this.startActivity(i);
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

    ca.addView(ll);
    ca.requestLayout();
  }

  private void requestedPop()
  {
    final Simplified app = Simplified.get();
    final URI u = app.catalogFeedsPop();
    Log.d(CatalogActivity.TAG, String.format("Popped: %s", u));
  }
}
