package org.nypl.simplified.app.catalog;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.SimplifiedPart;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.BookFeedListenerType;
import org.nypl.simplified.books.core.BooksController;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.FeedFacetMatcherType;
import org.nypl.simplified.books.core.FeedFacetOPDS;
import org.nypl.simplified.books.core.FeedFacetPseudo;
import org.nypl.simplified.books.core.FeedFacetPseudo.FacetType;
import org.nypl.simplified.books.core.FeedFacetPseudoTitleProviderType;
import org.nypl.simplified.books.core.FeedFacetType;
import org.nypl.simplified.books.core.FeedGroup;
import org.nypl.simplified.books.core.FeedLoaderListenerType;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.books.core.FeedMatcherType;
import org.nypl.simplified.books.core.FeedType;
import org.nypl.simplified.books.core.FeedWithGroups;
import org.nypl.simplified.books.core.FeedWithoutGroups;
import org.nypl.simplified.http.core.URIQueryBuilder;
import org.nypl.simplified.opds.core.OPDSFacet;
import org.nypl.simplified.opds.core.OPDSSearchLink;
import org.nypl.simplified.stack.ImmutableStack;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings({ "boxing", "synthetic-access" }) public class CatalogFeedActivity extends
  CatalogActivity implements
  BookFeedListenerType,
  FeedMatcherType<Unit, UnreachableCodeException>,
  FeedLoaderListenerType
{
  /**
   * A handler for local book searches.
   */

  private final class BooksLocalSearchQueryHandler implements
    OnQueryTextListener
  {
    private final CatalogFeedArgumentsType  args;
    private final FeedFacetPseudo.FacetType facet_active;
    private final Resources                 resources;

    BooksLocalSearchQueryHandler(
      final Resources in_resources,
      final CatalogFeedArgumentsType in_args,
      final FeedFacetPseudo.FacetType in_facet_active)
    {
      this.resources = NullCheck.notNull(in_resources);
      this.args = NullCheck.notNull(in_args);
      this.facet_active = NullCheck.notNull(in_facet_active);
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

      final CatalogFeedActivity cfa = CatalogFeedActivity.this;
      final ImmutableStack<CatalogFeedArgumentsType> us =
        cfa.newUpStack(this.args);

      final String title =
        this.resources.getString(R.string.catalog_search) + ": " + qnn;

      final CatalogFeedArgumentsLocalBooks new_args =
        new CatalogFeedArgumentsLocalBooks(
          us,
          title,
          this.facet_active,
          Option.some(qnn));

      CatalogFeedActivity.startNewActivity(cfa, new_args);
      return true;
    }
  }

  /**
   * A handler for OpenSearch 1.1 searches.
   */

  private final class OpenSearchQueryHandler implements OnQueryTextListener
  {
    private final CatalogFeedArgumentsType args;
    private final URI                      base;
    private final Resources                resources;

    OpenSearchQueryHandler(
      final Resources in_resources,
      final CatalogFeedArgumentsType in_args,
      final URI in_base)
    {
      this.resources = NullCheck.notNull(in_resources);
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
      final ImmutableStack<CatalogFeedArgumentsType> us =
        cfa.newUpStack(this.args);

      final String title =
        this.resources.getString(R.string.catalog_search) + ": " + qnn;

      final CatalogFeedArgumentsRemote new_args =
        new CatalogFeedArgumentsRemote(false, us, title, target);
      CatalogFeedActivity.startNewActivity(cfa, new_args);
      return true;
    }
  }

  private static final String CATALOG_ARGS;
  private static final String LIST_STATE_ID;
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogFeedActivity.class);
  }

  static {
    CATALOG_ARGS = "org.nypl.simplified.app.CatalogFeedActivity.arguments";
    LIST_STATE_ID =
      "org.nypl.simplified.app.CatalogFeedActivity.list_view_state";
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
          CatalogActivity.setActivityArguments(b, c.getUpStack());
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
   * @param in_args
   *          The feed arguments
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

  /**
   * Start a new catalog feed activity, assuming that the user came from
   * <tt>from</tt>, with up stack <tt>up_stack</tt>, attempting to load the
   * feed at <tt>target</tt>. The new activity "replaces" the current activity
   * by calling <tt>finish()</tt> on the existing activity.
   *
   * @param from
   *          The previous activity
   * @param in_args
   *          The feed arguments
   */

  public static void startNewActivityReplacing(
    final Activity from,
    final CatalogFeedArgumentsType in_args)
  {
    final Bundle b = new Bundle();
    CatalogFeedActivity.setActivityArguments(b, in_args);
    final Intent i = new Intent(from, CatalogFeedActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
    from.finish();
    from.overridePendingTransition(0, 0);
  }

  private @Nullable FeedType     feed;
  private @Nullable AbsListView  list_view;
  private @Nullable Future<Unit> loading;
  private @Nullable ViewGroup    progress_layout;
  private int                    saved_scroll_pos;

  private void configureFacets(
    final FeedWithoutGroups f,
    final ViewGroup layout,
    final SimplifiedCatalogAppServicesType app,
    final Resources rr)
  {
    final ViewGroup facets_view =
      NullCheck.notNull((ViewGroup) layout
        .findViewById(R.id.catalog_feed_nogroups_facets));
    final View facet_divider =
      NullCheck.notNull(layout
        .findViewById(R.id.catalog_feed_nogroups_facet_divider));

    final Map<String, List<FeedFacetType>> facet_groups =
      f.getFeedFacetsByGroup();

    if (facet_groups.isEmpty()) {
      facets_view.setVisibility(View.GONE);
      facet_divider.setVisibility(View.GONE);
    } else {
      for (final String group_name : facet_groups.keySet()) {
        final List<FeedFacetType> group =
          NullCheck.notNull(facet_groups.get(group_name));
        final ArrayList<FeedFacetType> group_copy =
          new ArrayList<FeedFacetType>(group);

        final LinearLayout.LayoutParams tvp =
          new LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        tvp.rightMargin = (int) app.screenDPToPixels(8);

        final TextView tv = new TextView(this);
        tv.setTextColor(rr.getColor(R.color.normal_text_major));
        tv.setTextSize(12.0f);
        tv.setText(group_name + ":");
        tv.setLayoutParams(tvp);
        facets_view.addView(tv);

        final OptionType<String> search_terms;
        final CatalogFeedArgumentsType current_args = this.getArguments();
        if (current_args instanceof CatalogFeedArgumentsLocalBooks) {
          final CatalogFeedArgumentsLocalBooks locals =
            (CatalogFeedArgumentsLocalBooks) current_args;
          search_terms = locals.getSearchTerms();
        } else {
          search_terms = Option.none();
        }

        final FeedFacetMatcherType<Unit, UnreachableCodeException> facet_feed_listener =
          new FeedFacetMatcherType<Unit, UnreachableCodeException>() {
            @Override public Unit onFeedFacetOPDS(
              final FeedFacetOPDS feed_opds)
            {
              final OPDSFacet o = feed_opds.getOPDSFacet();
              final CatalogFeedArgumentsRemote args =
                new CatalogFeedArgumentsRemote(
                  false,
                  CatalogFeedActivity.this.getUpStack(),
                  f.getFeedTitle(),
                  o.getURI());
              CatalogFeedActivity.startNewActivityReplacing(
                CatalogFeedActivity.this,
                args);
              return Unit.unit();
            }

            @Override public Unit onFeedFacetPseudo(
              final FeedFacetPseudo fp)
            {
              final String facet_title =
                NullCheck.notNull(rr.getString(R.string.books_sort_by));
              final CatalogFeedArgumentsLocalBooks args =
                new CatalogFeedArgumentsLocalBooks(
                  CatalogFeedActivity.this.getUpStack(),
                  facet_title,
                  fp.getType(),
                  search_terms);
              CatalogFeedActivity.startNewActivityReplacing(
                CatalogFeedActivity.this,
                args);
              return Unit.unit();
            }
          };

        final CatalogFacetSelectionListenerType facet_listener =
          new CatalogFacetSelectionListenerType() {
            @Override public void onFacetSelected(
              final FeedFacetType in_selected)
            {
              in_selected.matchFeedFacet(facet_feed_listener);
            }
          };

        final CatalogFacetButton fb =
          new CatalogFacetButton(
            this,
            NullCheck.notNull(group_name),
            group_copy,
            facet_listener);

        fb.setLayoutParams(tvp);
        facets_view.addView(fb);
      }
    }
  }

  private void configureUpButton(
    final ImmutableStack<CatalogFeedArgumentsType> up_stack,
    final String title)
  {
    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(true);
      bar.setTitle(title);
    }
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
      final CatalogFeedArgumentsType args =
        (CatalogFeedArgumentsType) a
          .getSerializable(CatalogFeedActivity.CATALOG_ARGS);
      if (args != null) {
        return args;
      }
    }

    /**
     * If there were no arguments (because, for example, this activity is the
     * initial one started for the app), synthesize some.
     */

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final boolean in_drawer_open = true;
    final ImmutableStack<CatalogFeedArgumentsType> empty =
      ImmutableStack.empty();
    final String in_title =
      NullCheck.notNull(rr.getString(R.string.app_name));
    final URI in_uri = app.getFeedInitialURI();

    return new CatalogFeedArgumentsRemote(
      in_drawer_open,
      NullCheck.notNull(empty),
      in_title,
      in_uri);
  }

  private void loadFeed(
    final FeedLoaderType feed_loader,
    final URI u)
  {
    CatalogFeedActivity.LOG.debug("loading feed: {}", u);
    this.loading = feed_loader.fromURI(u, this);
  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return SimplifiedPart.PART_CATALOG;
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return this.getUpStack().isEmpty();
  }

  private ImmutableStack<CatalogFeedArgumentsType> newUpStack(
    final CatalogFeedArgumentsType args)
  {
    final ImmutableStack<CatalogFeedArgumentsType> up_stack =
      this.getUpStack();
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack =
      up_stack.push(args);
    return new_up_stack;
  }

  @Override public void onBookFeedFailure(
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

  @Override public void onBookFeedSuccess(
    final FeedWithoutGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received locally generated feed: {}",
      f.getFeedID());

    this.feed = f;
    this.onFeedWithoutGroups(f);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    final CatalogFeedArgumentsType args = this.getArguments();
    final ImmutableStack<CatalogFeedArgumentsType> stack = this.getUpStack();
    this.configureUpButton(stack, args.getTitle());

    if (state != null) {
      CatalogFeedActivity.LOG.debug("received state");
      this.saved_scroll_pos = state.getInt(CatalogFeedActivity.LIST_STATE_ID);
    } else {
      this.saved_scroll_pos = 0;
    }

    /**
     * If this is the root of the catalog, attempt the initial load/login/sync
     * of books.
     */

    if (stack.isEmpty()) {
      final SimplifiedCatalogAppServicesType app =
        Simplified.getCatalogAppServices();
      app.syncInitial();
    }

    final LayoutInflater inflater = this.getLayoutInflater();
    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup in_progress_layout =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_loading,
        content_area,
        false));

    content_area.addView(in_progress_layout);
    content_area.requestLayout();
    this.progress_layout = in_progress_layout;

    final Resources rr = NullCheck.notNull(this.getResources());
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final FeedLoaderType feed_loader = app.getFeedLoader();

    final FeedFacetPseudoTitleProviderType facet_title_provider =
      new CatalogFacetPseudoTitleProvider(rr);

    final CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException> matcher =
      new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>() {
        @Override public Unit onFeedArgumentsLocalBooks(
          final CatalogFeedArgumentsLocalBooks c)
        {
          final BooksType books = app.getBooks();
          final Calendar now = NullCheck.notNull(Calendar.getInstance());
          final URI dummy_uri = NullCheck.notNull(URI.create("Books"));
          final String dummy_id =
            NullCheck.notNull(rr.getString(R.string.books));
          final String title =
            NullCheck.notNull(rr.getString(R.string.books));
          final String facet_group =
            NullCheck.notNull(rr.getString(R.string.books_sort_by));

          books.booksGetFeed(
            dummy_uri,
            dummy_id,
            now,
            title,
            c.getFacetType(),
            facet_group,
            facet_title_provider,
            c.getSearchTerms(),
            CatalogFeedActivity.this);
          return Unit.unit();
        }

        @Override public Unit onFeedArgumentsRemote(
          final CatalogFeedArgumentsRemote c)
        {
          CatalogFeedActivity.this.loadFeed(feed_loader, c.getURI());
          return Unit.unit();
        }
      };

    args.matchArguments(matcher);
  }

  @Override public boolean onCreateOptionsMenu(
    final @Nullable Menu in_menu)
  {
    final Menu menu_nn = NullCheck.notNull(in_menu);

    CatalogFeedActivity.LOG.debug("inflating menu");
    final MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.catalog, menu_nn);

    if (this.feed == null) {
      CatalogFeedActivity.LOG
        .debug("menu creation requested but feed is not yet present");
      return true;
    }

    CatalogFeedActivity.LOG
      .debug("menu creation requested and feed is present");

    this.onCreateOptionsMenuSearchItem(menu_nn);
    this.onCreateOptionsMenuRefreshItem(menu_nn);
    return true;
  }

  @SuppressWarnings("static-method") private
    void
    onCreateOptionsMenuRefreshItem(
      final Menu menu_nn)
  {
    final MenuItem refresh_item =
      menu_nn.findItem(R.id.catalog_action_refresh);

    refresh_item.setEnabled(true);
    refresh_item.setVisible(true);
  }

  private void onCreateOptionsMenuSearchItem(
    final Menu menu_nn)
  {
    final MenuItem search_item = menu_nn.findItem(R.id.catalog_action_search);

    /**
     * If the feed actually has a search URI, then show the search field.
     * Otherwise, disable and hide it.
     */

    final FeedType feed_actual = NullCheck.notNull(this.feed);
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

      final Resources rr = NullCheck.notNull(this.getResources());
      final OPDSSearchLink search = search_some.get();
      if ("application/opensearchdescription+xml".equals(search.getType())) {
        sv.setOnQueryTextListener(new OpenSearchQueryHandler(rr, args, search
          .getURI()));
        search_ok = true;
      } else if (BooksController.LOCAL_SEARCH_TYPE.equals(search.getType())) {
        final FacetType active_facet = FacetType.SORT_BY_TITLE;
        sv.setOnQueryTextListener(new BooksLocalSearchQueryHandler(
          rr,
          args,
          active_facet));
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

    if (search_ok) {
      search_item.setEnabled(true);
      search_item.setVisible(true);
    }
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    CatalogFeedActivity.LOG.debug("onDestroy");

    final Future<Unit> future = this.loading;
    if (future != null) {
      future.cancel(true);
    }
  }

  @Override public void onFeedLoadFailure(
    final URI u,
    final Throwable x)
  {
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedActivity.this.onFeedLoadingFailureUI(x);
      }
    });
  }

  private void onFeedLoadingFailureUI(
    final Throwable e)
  {
    UIThread.checkIsUIThread();

    CatalogFeedActivity.LOG.error("Failed to get feed: ", e);
    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup error =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_loading_error,
        content_area,
        false));
    content_area.addView(error);
    content_area.requestLayout();

    final Button retry =
      NullCheck
        .notNull((Button) error.findViewById(R.id.catalog_error_retry));
    retry.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        CatalogFeedActivity.this.retryFeed();
      }
    });
  }

  @Override public void onFeedLoadSuccess(
    final URI u,
    final FeedType f)
  {
    CatalogFeedActivity.LOG.debug("received feed for {}", u);
    this.feed = f;

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedActivity.this.configureUpButton(
          CatalogFeedActivity.this.getUpStack(),
          f.getFeedTitle());
      }
    });

    f.matchFeed(this);
  }

  @Override public Unit onFeedWithGroups(
    final FeedWithGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed with blocks: {}",
      f.getFeedURI());

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedActivity.this.onFeedWithGroupsUI(f);
      }
    });

    return Unit.unit();
  }

  private void onFeedWithGroupsUI(
    final FeedWithGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed with blocks: {}",
      f.getFeedURI());

    UIThread.checkIsUIThread();

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_feed_groups_list,
        content_area,
        false));

    content_area.addView(layout);
    content_area.requestLayout();

    CatalogFeedActivity.LOG.debug(
      "restoring scroll position: {}",
      this.saved_scroll_pos);

    final ListView list =
      NullCheck.notNull((ListView) layout
        .findViewById(R.id.catalog_feed_blocks_list));
    list.post(new Runnable() {
      @Override public void run()
      {
        list.setSelection(CatalogFeedActivity.this.saved_scroll_pos);
      }
    });
    list.setDividerHeight(0);
    this.list_view = list;

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();

    final CatalogFeedArgumentsType args = this.getArguments();
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack =
      this.newUpStack(args);

    final CatalogFeedLaneListenerType in_lane_listener =
      new CatalogFeedLaneListenerType() {
        @Override public void onSelectBook(
          final FeedEntryOPDS e)
        {
          CatalogFeedActivity.this.onSelectedBook(app, new_up_stack, e);
        }

        @Override public void onSelectFeed(
          final FeedGroup in_block)
        {
          CatalogFeedActivity.this
            .onSelectedFeedGroup(new_up_stack, in_block);
        }
      };

    final CatalogFeedWithGroups cfl =
      new CatalogFeedWithGroups(
        this,
        app,
        app.getCoverProvider(),
        in_lane_listener,
        f);

    list.setAdapter(cfl);
    list.setOnScrollListener(cfl);
  }

  @Override public Unit onFeedWithoutGroups(
    final FeedWithoutGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed without blocks: {}",
      f.getFeedURI());

    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        CatalogFeedActivity.this.onFeedWithoutGroupsUI(f);
      }
    });
    return Unit.unit();
  }

  private void onFeedWithoutGroupsEmptyUI(
    final FeedWithoutGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed without blocks (empty): {}",
      f.getFeedURI());

    UIThread.checkIsUIThread();
    Assertions.checkPrecondition(f.isEmpty(), "Feed is empty");

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_feed_nogroups_empty,
        content_area,
        false));

    content_area.addView(layout);
    content_area.requestLayout();
  }

  private void onFeedWithoutGroupsNonEmptyUI(
    final FeedWithoutGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed without blocks (non-empty): {}",
      f.getFeedURI());

    UIThread.checkIsUIThread();
    Assertions.checkPrecondition(f.isEmpty() == false, "Feed is non-empty");

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.catalog_feed_nogroups,
        content_area,
        false));

    content_area.addView(layout);
    content_area.requestLayout();

    CatalogFeedActivity.LOG.debug(
      "restoring scroll position: {}",
      this.saved_scroll_pos);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final Resources rr = NullCheck.notNull(this.getResources());

    final GridView grid_view =
      NullCheck.notNull((GridView) layout
        .findViewById(R.id.catalog_feed_nogroups_grid));

    this.configureFacets(f, layout, app, rr);

    grid_view.post(new Runnable() {
      @Override public void run()
      {
        grid_view.setSelection(CatalogFeedActivity.this.saved_scroll_pos);
      }
    });
    this.list_view = grid_view;

    final CatalogFeedArgumentsType args = this.getArguments();
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack =
      this.newUpStack(args);

    final CatalogBookSelectionListenerType book_select_listener =
      new CatalogBookSelectionListenerType() {
        @Override public void onSelectBook(
          final CatalogFeedBookCellView v,
          final FeedEntryOPDS e)
        {
          CatalogFeedActivity.this.onSelectedBook(app, new_up_stack, e);
        }
      };

    final CatalogFeedWithoutGroups without =
      new CatalogFeedWithoutGroups(
        this,
        app.getCoverProvider(),
        book_select_listener,
        app.getBooks(),
        app.getFeedLoader(),
        f);
    grid_view.setAdapter(without);
    grid_view.setOnScrollListener(without);
  }

  private void onFeedWithoutGroupsUI(
    final FeedWithoutGroups f)
  {
    UIThread.checkIsUIThread();

    if (f.isEmpty()) {
      this.onFeedWithoutGroupsEmptyUI(f);
      return;
    }

    this.onFeedWithoutGroupsNonEmptyUI(f);
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item)
  {
    final MenuItem item_nn = NullCheck.notNull(item);
    switch (item_nn.getItemId()) {

    /**
     * The menu option to refresh feeds. Essentially, the feed is invalidated
     * in the cache and a new activity is started that loads the same feed
     * again (replacing the current activity).
     */

      case R.id.catalog_action_refresh:
      {
        CatalogFeedActivity.LOG.debug("refreshing feed");
        this.retryFeed();
        return true;
      }
    }

    return super.onOptionsItemSelected(item_nn);
  }

  @Override protected void onSaveInstanceState(
    final @Nullable Bundle state)
  {
    super.onSaveInstanceState(state);

    CatalogFeedActivity.LOG.debug("saving state");

    final Bundle nn_state = NullCheck.notNull(state);
    final AbsListView lv = this.list_view;
    if (lv != null) {
      final int position = lv.getFirstVisiblePosition();
      CatalogFeedActivity.LOG
        .debug("saving list view position: {}", position);
      nn_state.putInt(CatalogFeedActivity.LIST_STATE_ID, position);
    }
  }

  private void onSelectedBook(
    final SimplifiedCatalogAppServicesType app,
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack,
    final FeedEntryOPDS e)
  {
    CatalogFeedActivity.LOG.debug("onSelectedBook: {}", this);

    if (app.screenIsLarge()) {
      final CatalogBookDialog df = CatalogBookDialog.newDialog(e);
      final FragmentManager fm =
        CatalogFeedActivity.this.getFragmentManager();
      df.show(fm, "book-detail");
    } else {
      CatalogBookDetailActivity.startNewActivity(
        CatalogFeedActivity.this,
        new_up_stack,
        this.navigationDrawerGetPart(),
        e);
    }
  }

  private void onSelectedFeedGroup(
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack,
    final FeedGroup f)
  {
    CatalogFeedActivity.LOG.debug("onSelectFeed: {}", this);

    final CatalogFeedArgumentsRemote remote =
      new CatalogFeedArgumentsRemote(
        false,
        new_up_stack,
        f.getGroupTitle(),
        f.getGroupURI());
    CatalogFeedActivity.startNewActivity(this, remote);
  }

  private void retryFeed()
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final FeedLoaderType loader = app.getFeedLoader();
    final CatalogFeedArgumentsType args = this.getArguments();
    args
      .matchArguments(new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>() {
        @Override public Unit onFeedArgumentsLocalBooks(
          final CatalogFeedArgumentsLocalBooks c)
        {
          // Nothing to refresh for local books. This shouldn't even be
          // reachable.
          throw new UnreachableCodeException();
        }

        @Override public Unit onFeedArgumentsRemote(
          final CatalogFeedArgumentsRemote c)
        {
          loader.invalidate(c.getURI());
          CatalogFeedActivity.startNewActivityReplacing(
            CatalogFeedActivity.this,
            args);
          return Unit.unit();
        }
      });
  }
}
