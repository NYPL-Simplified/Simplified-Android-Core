package org.nypl.simplified.app;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

public final class CatalogFeedActivity extends CatalogActivity implements
  CatalogLaneViewListenerType
{
  private static final String CATALOG_URI;
  private static final String TAG;

  static {
    TAG = "CAct";
    CATALOG_URI = "org.nypl.simplified.app.CatalogFeedActivity.uri";
  }

  public static void setActivityArguments(
    final Bundle b,
    final boolean drawer_open,
    final ImmutableList<URI> up_stack,
    final URI uri)
  {
    NullCheck.notNull(b);
    SimplifiedActivity.setActivityArguments(b, drawer_open);
    b
      .putSerializable(
        CatalogFeedActivity.CATALOG_URI,
        NullCheck.notNull(uri));
    CatalogActivity.setActivityArguments(b, up_stack);
  }

  public static void startNewActivity(
    final Activity from,
    final ImmutableList<URI> up_stack,
    final URI target)
  {
    final Bundle b = new Bundle();
    CatalogFeedActivity.setActivityArguments(b, false, up_stack, target);
    final Intent i = new Intent(from, CatalogFeedActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
  }

  private @Nullable ListenableFuture<OPDSFeedType> loading;
  private @Nullable ViewGroup                      progress_layout;

  private void configureUpButton(
    final List<URI> up_stack)
  {
    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(true);
    } else {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(false);
    }
  }

  private ListenableFuture<OPDSFeedType> getFeed(
    final URI uri,
    final OPDSFeedLoaderType loader)
  {
    return loader.fromURI(uri, new OPDSFeedLoadListenerType() {
      @Override public void onFailure(
        final Throwable e)
      {
        CatalogFeedActivity.this.runOnUiThread(new Runnable() {
          @Override public void run()
          {
            CatalogFeedActivity.this.onReceiveFeedError(e, uri);
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
              CatalogFeedActivity.this.runOnUiThread(new Runnable() {
                @Override public void run()
                {
                  CatalogFeedActivity.this.onReceiveFeedAcquisition(af);
                }
              });
              return Unit.unit();
            }

            @Override public Unit navigation(
              final OPDSNavigationFeed nf)
            {
              CatalogFeedActivity.this.runOnUiThread(new Runnable() {
                @Override public void run()
                {
                  CatalogFeedActivity.this.onReceiveFeedNavigation(nf);
                }
              });
              return Unit.unit();
            }
          });
      }
    });
  }

  private URI getURI()
  {
    final Simplified app = Simplified.get();
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = i.getExtras();
    if (a != null) {
      return NullCheck.notNull((URI) a
        .getSerializable(CatalogFeedActivity.CATALOG_URI));
    }
    return app.getFeedInitialURI();
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(CatalogFeedActivity.TAG, "onCreate: " + this);
    this.configureUpButton(this.getUpStack());
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    Log.d(CatalogFeedActivity.TAG, "onDestroy: " + this);
    this.stopDownloading();
  }

  private void onReceiveFeedAcquisition(
    final OPDSAcquisitionFeed af)
  {
    Log.d(CatalogFeedActivity.TAG, "received acquisition feed: " + af);
    final ViewGroup pl = NullCheck.notNull(this.progress_layout);
    pl.setVisibility(View.GONE);
  }

  private void onReceiveFeedError(
    final Throwable e,
    final URI feed_uri)
  {
    if (e instanceof CancellationException) {
      Log.d(CatalogFeedActivity.TAG, "Cancelled feed");
      return;
    }

    Log.e(CatalogFeedActivity.TAG, "Failed to get feed: " + e, e);

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final LinearLayout error =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_loading_error,
        content_area,
        false));
    content_area.addView(error);
    content_area.requestLayout();
  }

  private void onReceiveFeedNavigation(
    final OPDSNavigationFeed nf)
  {
    Log.d(CatalogFeedActivity.TAG, "received navigation feed: " + nf);

    final Simplified app = Simplified.get();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_navigation_feed,
        content_area,
        false));

    final ListView list_view =
      NullCheck.notNull((ListView) layout
        .findViewById(R.id.catalog_nav_feed_list));
    list_view.setVerticalScrollBarEnabled(false);
    list_view.setDividerHeight(0);

    final ArrayAdapter<OPDSNavigationFeedEntry> in_adapter =
      new ArrayAdapter<OPDSNavigationFeedEntry>(this, 0, nf.getFeedEntries());

    final List<URI> up_stack = this.getUpStack();
    final Builder<URI> new_up_stack_b = ImmutableList.builder();
    new_up_stack_b.addAll(up_stack);
    new_up_stack_b.add(nf.getFeedURI());
    final ImmutableList<URI> new_up_stack =
      NullCheck.notNull(new_up_stack_b.build());

    final CatalogLaneViewListenerType lane_view_listener =
      new CatalogLaneViewListenerType() {
        @Override public void onSelectBook(
          final CatalogLaneView v,
          final OPDSAcquisitionFeedEntry e)
        {
          Log.d(CatalogFeedActivity.TAG, "onSelectBook: " + this);

          if (app.screenIsLarge()) {
            final CatalogBookDialog df = CatalogBookDialog.newDialog(e);
            final FragmentManager fm =
              CatalogFeedActivity.this.getFragmentManager();
            df.show(fm, "book-detail");
          } else {
            CatalogBookDetailActivity.startNewActivity(
              CatalogFeedActivity.this,
              new_up_stack,
              e);
          }
        }

        @Override public void onSelectFeed(
          final CatalogLaneView v,
          final OPDSNavigationFeedEntry feed)
        {
          Log.d(CatalogFeedActivity.TAG, "onSelectFeed: " + this);
          CatalogFeedActivity.startNewActivity(
            CatalogFeedActivity.this,
            new_up_stack,
            feed.getTargetURI());
        }
      };

    final CatalogNavigationFeed f =
      new CatalogNavigationFeed(
        this,
        in_adapter,
        nf,
        this,
        lane_view_listener);

    list_view.setAdapter(f);
    list_view.setRecyclerListener(f);

    content_area.addView(layout);
    content_area.requestLayout();
  }

  @Override protected void onResume()
  {
    super.onResume();
    Log.d(CatalogFeedActivity.TAG, "onResume: " + this);

    final LayoutInflater inflater = this.getLayoutInflater();
    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup layout =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_loading,
        content_area,
        false));
    content_area.addView(layout);
    content_area.requestLayout();
    this.progress_layout = layout;

    final Simplified app = Simplified.get();
    final OPDSFeedLoaderType loader = app.getFeedLoader();
    this.loading = this.getFeed(this.getURI(), loader);
  }

  @Override public void onSelectBook(
    final CatalogLaneView v,
    final OPDSAcquisitionFeedEntry e)
  {
    Log.d(CatalogFeedActivity.TAG, "onSelectBook: " + e);
  }

  @Override public void onSelectFeed(
    final CatalogLaneView v,
    final OPDSNavigationFeedEntry feed)
  {
    Log.d(CatalogFeedActivity.TAG, "onSelectFeed: " + feed);
  }

  @Override protected void onStop()
  {
    super.onStop();
    Log.d(CatalogFeedActivity.TAG, "onStop: " + this);
    this.stopDownloading();

    final FrameLayout content_area = this.getContentFrame();
    content_area.removeAllViews();
  }

  private void stopDownloading()
  {
    final ListenableFuture<OPDSFeedType> l = this.loading;
    if (l != null) {
      if (l.isDone() == false) {
        l.cancel(true);
      }
    }
  }
}
