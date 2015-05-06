package org.nypl.simplified.app.catalog;

import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;

import org.nypl.simplified.app.ExpensiveStoppableType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.BookAcquisitionFeedListenerType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.http.core.URIQueryBuilder;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;
import org.nypl.simplified.opds.core.OPDSSearchLink;
import org.slf4j.Logger;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
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

@SuppressWarnings("synthetic-access") public class CatalogFeedActivity extends
  CatalogActivity implements
  OPDSFeedLoadListenerType,
  OPDSFeedMatcherType<Unit, UnreachableCodeException>,
  BookAcquisitionFeedListenerType
{
  /**
   * A handler for OpenSearch 1.1 searches.
   */

  private final class OpenSearchQueryHandler implements OnQueryTextListener
  {
    private final CatalogFeedArgumentsType args;
    private final URI                      base;

    OpenSearchQueryHandler(
      final CatalogFeedArgumentsType in_args,
      final URI in_base)
    {
      this.args = NullCheck.notNull(in_args);
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
      final String qnn = NullCheck.notNull(query);

      final SortedMap<String, String> parameters =
        new TreeMap<String, String>();
      parameters.put("q", qnn);
      final URI target = URIQueryBuilder.encodeQuery(this.base, parameters);

      final CatalogFeedActivity cfa = CatalogFeedActivity.this;
      final OPDSFeedType f = NullCheck.notNull(cfa.feed);

      final ImmutableList<CatalogUpStackEntry> us =
        cfa.newUpStack(f.getFeedURI(), this.args.getTitle());

      final CatalogFeedArgumentsRemote new_args =
        new CatalogFeedArgumentsRemote(false, us, "Search", target);
      CatalogFeedActivity.startNewActivity(cfa, new_args);
      return true;
    }
  }

  private static final String CATALOG_ARGS;

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogFeedActivity.class);
  }

  static {
    CATALOG_ARGS = "org.nypl.simplified.app.CatalogFeedActivity.arguments";
  }

  public static void setActivityArguments(
    final Bundle b,
    final CatalogFeedArgumentsType in_args)
  {
    NullCheck.notNull(b);
    NullCheck.notNull(in_args);

    b.putSerializable(CatalogFeedActivity.CATALOG_ARGS, in_args);

    in_args
      .matchArguments(new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>() {
        @Override public Unit onFeedArgumentsLocalBooks(
          final CatalogFeedArgumentsLocalBooks c)
        {
          SimplifiedActivity.setActivityArguments(b, false);
          final ImmutableList<CatalogUpStackEntry> empty = ImmutableList.of();
          CatalogActivity.setActivityArguments(b, NullCheck.notNull(empty));
          return Unit.unit();
        }

        @Override public Unit onFeedArgumentsRemote(
          final CatalogFeedArgumentsRemote c)
        {
          SimplifiedActivity.setActivityArguments(b, c.isDrawerOpen());
          CatalogActivity.setActivityArguments(b, c.getUpStack());
          return Unit.unit();
        }
      });
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
   * @param title
   *          The title of the feed
   * @param target
   *          The URI of the feed
   */

  public static void startNewActivity(
    final Activity from,
    final CatalogFeedArgumentsType in_args)
  {
    final Bundle b = new Bundle();
    CatalogFeedActivity.setActivityArguments(b, in_args);
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
    final List<CatalogUpStackEntry> up_stack,
    final String title)
  {
    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(true);
    } else {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(false);
    }

    bar.setTitle(title);
  }

  private CatalogFeedArgumentsType getArguments()
  {
    /**
     * Attempt to fetch arguments.
     */

    final Resources rr = NullCheck.notNull(this.getResources());
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = i.getExtras();
    if (a != null) {
      return NullCheck.notNull((CatalogFeedArgumentsType) a
        .getSerializable(CatalogFeedActivity.CATALOG_ARGS));
    }

    /**
     * If there were no arguments (because, for example, this activity is the
     * initial one started for the app), synthesize some.
     */

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final boolean in_drawer_open = true;
    final ImmutableList<CatalogUpStackEntry> empty = ImmutableList.of();
    final String in_title =
      NullCheck.notNull(rr.getString(R.string.app_name));
    final URI in_uri = app.getFeedInitialURI();

    return new CatalogFeedArgumentsRemote(
      in_drawer_open,
      NullCheck.notNull(empty),
      in_title,
      in_uri);
  }

  private ImmutableList<CatalogUpStackEntry> newUpStack(
    final URI feed_uri,
    final String feed_title)
  {
    final List<CatalogUpStackEntry> up_stack = this.getUpStack();
    final Builder<CatalogUpStackEntry> new_up_stack_b =
      ImmutableList.builder();
    new_up_stack_b.addAll(up_stack);
    new_up_stack_b.add(new CatalogUpStackEntry(feed_uri, feed_title));
    final ImmutableList<CatalogUpStackEntry> new_up_stack =
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
    CatalogFeedActivity.LOG.debug("received acquisition feed: {}", af);

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    /**
     * If the feed is empty, show a simple message instead of a list.
     */

    final boolean empty_feed = af.getFeedEntries().isEmpty();
    final int layout_id;
    if (empty_feed) {
      layout_id = R.layout.catalog_acquisition_feed_empty;
    } else {
      layout_id = R.layout.catalog_acquisition_feed;
    }

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        layout_id,
        content_area,
        false));

    content_area.addView(layout);
    content_area.requestLayout();

    if (empty_feed) {
      return;
    }

    this.onAcquisitionFeedUINonEmpty(af, layout);
  }

  private void onAcquisitionFeedUINonEmpty(
    final OPDSAcquisitionFeed af,
    final ViewGroup layout)
  {
    final GridView grid_view =
      NullCheck.notNull((GridView) layout
        .findViewById(R.id.catalog_acq_feed_grid));

    final CatalogFeedArgumentsType args = this.getArguments();
    final URI feed_uri = af.getFeedURI();
    final ImmutableList<CatalogUpStackEntry> new_up_stack =
      this.newUpStack(feed_uri, args.getTitle());
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();

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
        app.getFeedLoader(),
        app.getBooks());

    grid_view.setAdapter(f);
    grid_view.setOnScrollListener(f);

    this.cancellable = f;
  }

  @Override public void onBookAcquisitionFeedFailure(
    final Throwable e)
  {
    if (e instanceof CancellationException) {
      CatalogFeedActivity.LOG.debug("Cancelled feed");
      return;
    }

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedActivity.this.onFeedLoadingFailureUI(e);
      }
    });
  }

  @Override public void onBookAcquisitionFeedSuccess(
    final OPDSAcquisitionFeed f)
  {
    this.onAcquisitionFeed(f);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    final CatalogFeedArgumentsType args = this.getArguments();
    final List<CatalogUpStackEntry> stack = this.getUpStack();
    this.configureUpButton(stack, args.getTitle());

    /**
     * If this is the root of the catalog, attempt the initial load/login/sync
     * of books.
     */

    if (stack.isEmpty()) {
      final SimplifiedCatalogAppServicesType app =
        Simplified.getCatalogAppServices();
      app.syncInitial();
    }
  }

  @Override public boolean onCreateOptionsMenu(
    final @Nullable Menu in_menu)
  {
    final Menu menu_nn = NullCheck.notNull(in_menu);

    if (this.feed == null) {
      CatalogFeedActivity.LOG
        .debug("menu creation requested but feed is not present");
      return true;
    }

    CatalogFeedActivity.LOG
      .debug("menu creation requested and feed is present");

    final MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.catalog, menu_nn);

    final MenuItem search_item = menu_nn.findItem(R.id.catalog_action_search);

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
       * Set some placeholder text
       */

      final CatalogFeedArgumentsType args = this.getArguments();
      if (this.getUpStack().isEmpty()) {
        sv.setQueryHint("Search");
      } else {
        sv.setQueryHint("Search " + args.getTitle());
      }

      /**
       * Check that the search URI is of an understood type.
       */

      final OPDSSearchLink search = search_some.get();
      if ("application/opensearchdescription+xml".equals(search.getType())) {
        sv.setOnQueryTextListener(new OpenSearchQueryHandler(args, search
          .getURI()));
        search_ok = true;
      } else {

        /**
         * The application doesn't understand the search type.
         */

        CatalogFeedActivity.LOG.error(
          "unknown search type: {}",
          search.getType());
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
    this.stopDownloading();
  }

  @Override public void onFeedLoadingFailure(
    final Throwable e)
  {
    if (e instanceof CancellationException) {
      CatalogFeedActivity.LOG.debug("Cancelled feed");
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
    CatalogFeedActivity.LOG
      .error("Failed to get feed: {}", e.getMessage(), e);

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
    CatalogFeedActivity.LOG.debug("received navigation feed: {}", nf);

    this.invalidateOptionsMenu();

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();

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

    final CatalogFeedArgumentsType args = this.getArguments();
    final URI feed_uri = nf.getFeedURI();
    final ImmutableList<CatalogUpStackEntry> new_up_stack =
      this.newUpStack(feed_uri, args.getTitle());

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

    final Resources rr = NullCheck.notNull(this.getResources());
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final CatalogFeedArgumentsType args = this.getArguments();
    args
      .matchArguments(new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>() {
        @Override public Unit onFeedArgumentsLocalBooks(
          final CatalogFeedArgumentsLocalBooks c)
        {
          final BooksType books = app.getBooks();
          final URI dummy_uri = NullCheck.notNull(URI.create("Books"));
          final String dummy_id =
            NullCheck.notNull(rr.getString(R.string.books));
          final Calendar now = Calendar.getInstance();
          final String title =
            NullCheck.notNull(rr.getString(R.string.books));
          books.booksGetAcquisitionFeed(
            dummy_uri,
            dummy_id,
            now,
            title,
            CatalogFeedActivity.this);
          return Unit.unit();
        }

        @Override public Unit onFeedArgumentsRemote(
          final CatalogFeedArgumentsRemote c)
        {
          final OPDSFeedLoaderType loader = app.getFeedLoader();
          CatalogFeedActivity.this.loading =
            loader.fromURI(c.getURI(), CatalogFeedActivity.this);
          return Unit.unit();
        }
      });
  }

  private void onSelectedBook(
    final SimplifiedCatalogAppServicesType app,
    final ImmutableList<CatalogUpStackEntry> new_up_stack,
    final OPDSAcquisitionFeedEntry e)
  {
    CatalogFeedActivity.LOG.debug("onSelectBook: {}", this);

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
    final ImmutableList<CatalogUpStackEntry> new_up_stack,
    final OPDSNavigationFeedEntry f)
  {
    CatalogFeedActivity.LOG.debug("onSelectFeed: {}", this);

    final CatalogFeedArgumentsRemote remote =
      new CatalogFeedArgumentsRemote(
        false,
        new_up_stack,
        f.getTitle(),
        f.getTargetURI());
    CatalogFeedActivity.startNewActivity(this, remote);
  }

  @Override protected void onStop()
  {
    super.onStop();
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
