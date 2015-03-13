package org.nypl.simplified.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.apache.http.client.utils.URIUtils;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;
import org.nypl.simplified.opds.core.OPDSSearchLink;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings("synthetic-access") public final class CatalogFeedActivity extends
  CatalogActivity implements
  OPDSFeedLoadListenerType,
  OPDSFeedMatcherType<Unit, UnreachableCodeException>
{
  /**
   * A handler for OpenSearch 1.1 searches.
   */

  private final class OpenSearchQueryHandler implements OnQueryTextListener
  {
    private final URI base;

    OpenSearchQueryHandler(
      final URI in_base)
    {
      this.base = NullCheck.notNull(in_base);
    }

    @Override public boolean onQueryTextChange(
      final @Nullable String s)
    {
      return true;
    }

    @Override public boolean onQueryTextSubmit(
      final @Nullable String query)
    {
      try {
        final String qnn = NullCheck.notNull(query);

        final URI target =
          NullCheck.notNull(URIUtils.createURI(
            this.base.getScheme(),
            this.base.getHost(),
            this.base.getPort(),
            this.base.getPath(),
            "q=" + Uri.encode(qnn),
            null));

        final OPDSFeedType f =
          NullCheck.notNull(CatalogFeedActivity.this.feed);
        final ImmutableList<URI> us =
          CatalogFeedActivity.this.newUpStack(f.getFeedURI());

        CatalogFeedActivity.startNewActivity(
          CatalogFeedActivity.this,
          us,
          target);

      } catch (final URISyntaxException e) {
        Log.e(CatalogFeedActivity.TAG, e.getMessage(), e);
      }

      return true;
    }
  }

  private static final String CATALOG_URI;
  private static final String TAG;

  static {
    TAG = "CFA";
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

  /**
   * Start a new catalog feed activity, assuming that the user came from
   * <tt>from</tt>, with up stack <tt>up_stack</tt>, attempting to load the
   * feed at <tt>target</tt>.
   * 
   * @param from
   *          The previous activity
   * @param up_stack
   *          The up stack for the new activity
   * @param target
   *          The URI of the feed
   */

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

  private @Nullable ExpensiveStoppableType         cancellable;
  private @Nullable OPDSFeedType                   feed;
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

  private void configureUpButtonTitle(
    final String title,
    final List<URI> up_stack)
  {
    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setTitle(title);
    }
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

  private ImmutableList<URI> newUpStack(
    final URI feed_uri)
  {
    final List<URI> up_stack = this.getUpStack();
    final Builder<URI> new_up_stack_b = ImmutableList.builder();
    new_up_stack_b.addAll(up_stack);
    new_up_stack_b.add(feed_uri);
    final ImmutableList<URI> new_up_stack =
      NullCheck.notNull(new_up_stack_b.build());
    return new_up_stack;
  }

  @Override public Unit onAcquisitionFeed(
    final OPDSAcquisitionFeed f)
  {
    CatalogFeedActivity.this.runOnUiThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedActivity.this.onAcquisitionFeedUI(f);
      }
    });

    return Unit.unit();
  }

  private void onAcquisitionFeedUI(
    final OPDSAcquisitionFeed af)
  {
    Log.d(CatalogFeedActivity.TAG, "received acquisition feed: " + af);

    this.configureUpButtonTitle(af.getFeedTitle(), this.getUpStack());
    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final LinearLayout layout =
      NullCheck.notNull((LinearLayout) inflater.inflate(
        R.layout.catalog_acquisition_feed,
        content_area,
        false));

    final GridView grid_view =
      NullCheck.notNull((GridView) layout
        .findViewById(R.id.catalog_acq_feed_grid));

    final URI feed_uri = af.getFeedURI();
    final ImmutableList<URI> new_up_stack = this.newUpStack(feed_uri);
    final Simplified app = Simplified.get();

    final CatalogAcquisitionFeedListenerType listener =
      new CatalogAcquisitionFeedListenerType() {
        @Override public void onSelectBook(
          final CatalogAcquisitionCellView v,
          final OPDSAcquisitionFeedEntry e)
        {
          CatalogFeedActivity.this.onSelectedBook(app, new_up_stack, e);
        }
      };

    final CatalogAcquisitionFeed f =
      new CatalogAcquisitionFeed(
        this,
        af,
        this,
        listener,
        app.getFeedLoader());

    grid_view.setAdapter(f);
    grid_view.setOnScrollListener(f);

    content_area.addView(layout);
    content_area.requestLayout();

    this.cancellable = f;
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final URI u = this.getURI();
    Log.d(
      CatalogFeedActivity.TAG,
      String.format("onCreate: %s (%s)", this, u));

    this.configureUpButton(this.getUpStack());
  }

  @Override public boolean onCreateOptionsMenu(
    final @Nullable Menu in_menu)
  {
    assert in_menu != null;

    if (this.feed == null) {
      Log.d(
        CatalogFeedActivity.TAG,
        "menu creation requested but feed is not present");
      return true;
    }

    Log.d(
      CatalogFeedActivity.TAG,
      "menu creation requested and feed is present");

    final MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.catalog, in_menu);

    final MenuItem search_item = in_menu.findItem(R.id.catalog_action_search);

    /**
     * If the feed actually has a search URI, then show the search field.
     * Otherwise, disable and hide it.
     */

    final OPDSFeedType feed_actual = NullCheck.notNull(this.feed);
    final OptionType<OPDSSearchLink> search_opt =
      feed_actual.getFeedSearchURI();

    boolean search_ok = false;
    if (search_opt.isSome()) {
      final Some<OPDSSearchLink> search_some =
        (Some<OPDSSearchLink>) search_opt;

      final SearchView sv = (SearchView) search_item.getActionView();
      sv.setSubmitButtonEnabled(true);

      /**
       * Check that the search URI is of an understood type.
       */

      final OPDSSearchLink search = search_some.get();
      if ("application/opensearchdescription+xml".equals(search.getType())) {
        sv
          .setOnQueryTextListener(new OpenSearchQueryHandler(search.getURI()));
        search_ok = true;
      } else {

        /**
         * The application doesn't understand the search type.
         */

        Log.e(
          CatalogFeedActivity.TAG,
          String.format("unknown search type '%s'", search.getType()));
      }
    }

    if (search_ok == false) {
      search_item.setEnabled(false);
      search_item.setVisible(false);
    }

    return true;
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();

    final URI u = this.getURI();
    Log.d(
      CatalogFeedActivity.TAG,
      String.format("onDestroy: %s (%s)", this, u));

    this.stopDownloading();
  }

  @Override public void onFeedLoadingFailure(
    final Throwable e)
  {
    if (e instanceof CancellationException) {
      Log.d(CatalogFeedActivity.TAG, "Cancelled feed");
      return;
    }

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedActivity.this.onFeedLoadingFailureUI(e);
      }
    });
  }

  private void onFeedLoadingFailureUI(
    final Throwable e)
  {
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

  @Override public void onFeedLoadingSuccess(
    final OPDSFeedType f)
  {
    this.feed = f;
    f.matchFeedType(this);
  }

  @Override public Unit onNavigationFeed(
    final OPDSNavigationFeed f)
  {
    CatalogFeedActivity.this.runOnUiThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedActivity.this.onNavigationFeedUI(f);
      }
    });

    return Unit.unit();
  }

  private void onNavigationFeedUI(
    final OPDSNavigationFeed nf)
  {
    Log.d(CatalogFeedActivity.TAG, "received navigation feed: " + nf);

    this.configureUpButtonTitle(nf.getFeedTitle(), this.getUpStack());
    this.invalidateOptionsMenu();

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

    final URI feed_uri = nf.getFeedURI();
    final ImmutableList<URI> new_up_stack = this.newUpStack(feed_uri);

    final CatalogNavigationLaneViewListenerType lane_view_listener =
      new CatalogNavigationLaneViewListenerType() {
        @Override public void onSelectBook(
          final CatalogNavigationLaneView v,
          final OPDSAcquisitionFeedEntry e)
        {
          CatalogFeedActivity.this.onSelectedBook(app, new_up_stack, e);
        }

        @Override public void onSelectFeed(
          final CatalogNavigationLaneView v,
          final OPDSNavigationFeedEntry f)
        {
          CatalogFeedActivity.this.onSelectedFeed(new_up_stack, f);
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

    final URI u = this.getURI();
    Log.d(
      CatalogFeedActivity.TAG,
      String.format("onResume: %s (%s)", this, u));

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
    this.loading = loader.fromURI(u, this);
  }

  private void onSelectedBook(
    final Simplified app,
    final ImmutableList<URI> new_up_stack,
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

  private void onSelectedFeed(
    final ImmutableList<URI> new_up_stack,
    final OPDSNavigationFeedEntry f)
  {
    Log.d(CatalogFeedActivity.TAG, "onSelectFeed: " + this);
    CatalogFeedActivity.startNewActivity(
      CatalogFeedActivity.this,
      new_up_stack,
      f.getTargetURI());
  }

  @Override protected void onStop()
  {
    super.onStop();

    final URI u = this.getURI();
    Log.d(CatalogFeedActivity.TAG, String.format("onStop: %s (%s)", this, u));

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

    final ExpensiveStoppableType c = this.cancellable;
    if (c != null) {
      c.expensiveStop();
    }
  }
}
