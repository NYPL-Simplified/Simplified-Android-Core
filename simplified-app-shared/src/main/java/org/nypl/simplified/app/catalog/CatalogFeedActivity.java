package org.nypl.simplified.app.catalog;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.app.LoginActivity;
import org.nypl.simplified.app.LoginDialog;
import org.nypl.simplified.app.LoginListenerType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.ThemeMatcher;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.AccountsControllerType;
import org.nypl.simplified.books.core.BookFeedListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.BooksFeedSelection;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.DeviceActivationListenerType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.EULAType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.FeedFacetMatcherType;
import org.nypl.simplified.books.core.FeedFacetOPDS;
import org.nypl.simplified.books.core.FeedFacetPseudo;
import org.nypl.simplified.books.core.FeedFacetPseudo.FacetType;
import org.nypl.simplified.books.core.FeedFacetType;
import org.nypl.simplified.books.core.FeedGroup;
import org.nypl.simplified.books.core.FeedLoaderAuthenticationListenerType;
import org.nypl.simplified.books.core.FeedLoaderListenerType;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.books.core.FeedMatcherType;
import org.nypl.simplified.books.core.FeedSearchLocal;
import org.nypl.simplified.books.core.FeedSearchMatcherType;
import org.nypl.simplified.books.core.FeedSearchOpen1_1;
import org.nypl.simplified.books.core.FeedSearchType;
import org.nypl.simplified.books.core.FeedType;
import org.nypl.simplified.books.core.FeedWithGroups;
import org.nypl.simplified.books.core.FeedWithoutGroups;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSFacet;
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;
import org.nypl.simplified.stack.ImmutableStack;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * The main catalog feed activity, responsible for displaying different types of
 * feeds.
 */

public abstract class CatalogFeedActivity extends CatalogActivity implements
  BookFeedListenerType,
  FeedMatcherType<Unit, UnreachableCodeException>,
  FeedLoaderListenerType
{
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

  private @Nullable FeedType     feed;
  private @Nullable AbsListView  list_view;
  private @Nullable SwipeRefreshLayout swipe_refresh_layout;
  private @Nullable Future<Unit> loading;
  private @Nullable ViewGroup    progress_layout;
  private           int          saved_scroll_pos;
  private           boolean      previously_paused;
  private           SearchView   search_view;

  /**
   * Construct an activity.
   */

  public CatalogFeedActivity()
  {

  }

  /**
   * Set the arguments of the activity to be created.
   * Modifies Bundle based on attributes and type (from local or remote)
   * before being given to Intent in the calling method.
   *
   * @param b       The argument bundle
   * @param in_args The feed arguments
   */

  public static void setActivityArguments(
    final Bundle b,
    final CatalogFeedArgumentsType in_args)
  {
    NullCheck.notNull(b);
    NullCheck.notNull(in_args);

    b.putSerializable(CatalogFeedActivity.CATALOG_ARGS, in_args);

    in_args.matchArguments(
      new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>()
      {
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
   * On the (possible) receipt of a link to the feed's EULA, update the URI for
   * the document if one has actually been defined for the application.
   *
   * @param latest The (possible) link
   *
   * @see {@link EULAType}
   */

  private static void onPossiblyReceivedEULALink(final OptionType<URI> latest)
  {
    latest.map_(
      new ProcedureType<URI>()
      {
        @Override public void call(final URI latest_actual)
        {
          final SimplifiedCatalogAppServicesType app =
            Simplified.getCatalogAppServices();
          final DocumentStoreType docs = app.getDocumentStore();

          docs.getEULA().map_(
            new ProcedureType<EULAType>()
            {
              @Override public void call(final EULAType eula)
              {
                try {
                  eula.documentSetLatestURL(latest_actual.toURL());
                } catch (final MalformedURLException e) {
                  CatalogFeedActivity.LOG.error(
                    "could not use latest EULA link: ", e);
                }
              }
            });
        }
      });
  }
  @Override public void onBackPressed() {

    this.invalidateOptionsMenu();
    super.onBackPressed();
  }
  @Override protected void onResume()
  {
    super.onResume();

    /**
     * If the activity was previously paused, this means that the user
     * navigated away from the activity and is now coming back to it. If the
     * user went into a book detail view and revoked a book, then the feed
     * should be completely reloaded when the user comes back, to ensure that
     * the book no longer shows up in the list.
     *
     * This obviously only applies to local feeds.
     */

    if (this.search_view != null)
    {
      this.search_view.setQuery("", false);
      this.search_view.clearFocus();
    }

    boolean did_retry = false;
    final Bundle extras = getIntent().getExtras();
    if (extras != null) {
      final boolean reload = extras.getBoolean("reload");
      if (reload == true)
      {
        did_retry = true;
        CatalogFeedActivity.this.retryFeed();
        extras.putBoolean("reload", false);
      }
    }

    if (this.previously_paused == true && did_retry == false) {
      final CatalogFeedArgumentsType args = this.getArguments();
      if (args.isLocallyGenerated()) {
        this.retryFeed();
      }
    }
  }

  /**
   * Configure the facets layout. This is what causes facets to be shown or not
   * shown at the top of the screen when rendering a feed.
   *
   * @param f      The feed
   * @param layout The view group that will contain facet elements
   * @param app    The app services
   * @param rr     The app resources
   */

  private void configureFacets(
    final FeedWithoutGroups f,
    final ViewGroup layout,
    final SimplifiedCatalogAppServicesType app,
    final Resources rr)
  {
    final ViewGroup facets_view = NullCheck.notNull(
      (ViewGroup) layout.findViewById(
        R.id.catalog_feed_nogroups_facets));
    final View facet_divider = NullCheck.notNull(
      layout.findViewById(
        R.id.catalog_feed_nogroups_facet_divider));

    final Map<String, List<FeedFacetType>> facet_groups =
      f.getFeedFacetsByGroup();

    /**
     * If the facet groups are empty, then no facet bar should be displayed.
     */

    if (facet_groups.isEmpty()) {
      facets_view.setVisibility(View.GONE);
      facet_divider.setVisibility(View.GONE);
    } else {

      /**
       * Otherwise, for each facet group, show a drop-down menu allowing
       * the selection of individual facets.
       */

      for (final String group_name : facet_groups.keySet()) {
        final List<FeedFacetType> group =
          NullCheck.notNull(facet_groups.get(group_name));
        final ArrayList<FeedFacetType> group_copy =
          new ArrayList<FeedFacetType>(group);

        final LinearLayout.LayoutParams tvp = new LinearLayout.LayoutParams(
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

        final FeedFacetMatcherType<Unit, UnreachableCodeException>
          facet_feed_listener =
          new FeedFacetMatcherType<Unit, UnreachableCodeException>()
          {
            @Override public Unit onFeedFacetOPDS(
              final FeedFacetOPDS feed_opds)
            {
              final OPDSFacet o = feed_opds.getOPDSFacet();
              final CatalogFeedArgumentsRemote args =
                new CatalogFeedArgumentsRemote(
                  false,
                  CatalogFeedActivity.this.getUpStack(),
                  f.getFeedTitle(),
                  o.getURI(),
                  false);

              CatalogFeedActivity.this.catalogActivityForkNewReplacing(args);
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
                  search_terms,
                  CatalogFeedActivity.this.getLocalFeedTypeSelection());

              CatalogFeedActivity.this.catalogActivityForkNewReplacing(args);
              return Unit.unit();
            }
          };

        final CatalogFacetSelectionListenerType facet_listener =
          new CatalogFacetSelectionListenerType()
          {
            @Override public void onFacetSelected(
              final FeedFacetType in_selected)
            {
              in_selected.matchFeedFacet(facet_feed_listener);
            }
          };

        final CatalogFacetButton fb = new CatalogFacetButton(
          this, NullCheck.notNull(group_name), group_copy, facet_listener);

        fb.setLayoutParams(tvp);
        facets_view.addView(fb);
      }
    }
  }

  /**
   * If this activity is being used in a part of the application that generates
   * local feeds, then return the type of feed that should be generated.
   *
   * @return The type of feed that should be generated.
   */

  protected abstract BooksFeedSelection getLocalFeedTypeSelection();

  @Override public void onFeedRequiresAuthentication(
    final URI u,
    final int attempts,
    final FeedLoaderAuthenticationListenerType listener)
  {
    /**
     * The feed requires authentication. If an attempt hasn't been made
     * to fetch it with the current cached credentials (if any), then try
     * to authenticate with those credentials.
     */

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final AccountsControllerType accounts = app.getBooks();

    /**
     * An adapter that will receive cached credentials and forward them
     * on to the listener.
     */

    CatalogFeedActivity.LOG.trace("feed auth: attempt {}", attempts);
    if (attempts == 0) {
      if (accounts.accountIsLoggedIn()) {
        CatalogFeedActivity.LOG.trace("feed auth: using cached credentials");

        accounts.accountGetCachedLoginDetails(
          new AccountGetCachedCredentialsListenerType()
          {
            @Override public void onAccountIsNotLoggedIn()
            {
              throw new UnreachableCodeException();
            }

            @Override public void onAccountIsLoggedIn(
              final AccountCredentials creds)
            {
              listener.onAuthenticationProvided(creds);
            }
          });
      }
    }

    if (attempts > 0 || accounts.accountIsLoggedIn() == false) {
      CatalogFeedActivity.LOG.trace("feed auth: login required");

      /**
       * Otherwise, this is a new attempt and the current credentials
       * are assumed to be stale. Ask the user for new ones.
       */

      final LoginListenerType login_listener = new LoginListenerType()
      {
        @Override public void onLoginAborted()
        {
          CatalogFeedActivity.LOG.trace("feed auth: aborted login");
          listener.onAuthenticationNotProvided();
        }

        @Override public void onLoginFailure(
          final OptionType<Throwable> error,
          final String message)
        {
          LogUtilities.errorWithOptionalException(
            CatalogFeedActivity.LOG, "failed login", error);
          listener.onAuthenticationError(error, message);
        }

        @Override public void onLoginSuccess(
          final AccountCredentials creds)
        {
          CatalogFeedActivity.LOG.trace(
            "feed auth: login supplied new credentials");
          listener.onAuthenticationProvided(creds);
        }
      };

      // replace with login activity
      UIThread.runOnUIThread(
        new Runnable()
        {
          @Override public void run()
          {

            final Intent i = new Intent(CatalogFeedActivity.this, LoginActivity.class);
            CatalogFeedActivity.this.startActivity(i);
            CatalogFeedActivity.this.overridePendingTransition(0, 0);
            CatalogFeedActivity.this.finish();

          }
        });
    }
  }

  private void configureUpButton(
    final ImmutableStack<CatalogFeedArgumentsType> up_stack,
    final String title)
  {
    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setTitle(title);
    }
  }

  private CatalogFeedArgumentsType getArguments()
  {
    /*
     * FIXME: When real navigation support comes into the app to support age-gated
     * collections, like the SimplyE Collection, remove this hack.
     */
    final Resources res = NullCheck.notNull(this.getResources());
    final String lib_title = NullCheck.notNull(res.getString(R.string.feature_app_name));
    final int libraryID = Simplified.getCurrentAccount().getId();
    final ImmutableStack<CatalogFeedArgumentsType> upStack = this.getUpStack();
    if (upStack.isEmpty() && libraryID == 2 && this.getClass() == MainCatalogActivity.class) {
      if (Simplified.getSharedPrefs().contains("age13") == false) {
        //Show Age Verification and load <13 to be safe
        showAgeCheckAlert();
      }
      final boolean over13 = Simplified.getSharedPrefs().getBoolean("age13");
      final URI ageURI;
      try {
        if (over13) {
          ageURI = new URI(Simplified.getCurrentAccount().getCatalogUrl13AndOver());
        } else {
          ageURI = new URI(Simplified.getCurrentAccount().getCatalogUrlUnder13());
        }
        CatalogFeedActivity.LOG.debug("Hardcoding SimplyE Collection URI: {}", ageURI);
        return new CatalogFeedArgumentsRemote(
          false,
          ImmutableStack.empty(),
          lib_title,
          ageURI,
          false
        );
      } catch (Exception e) {
        CatalogFeedActivity.LOG.error(
          "error constructing SimplyE collection uri: {}", e.getMessage(), e);
      }
    }
    /*
     * End of hack..
     */


    /**
     * Attempt to fetch arguments.
     */

    final Resources rr = NullCheck.notNull(this.getResources());
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = i.getExtras();
    if (a != null) {
      final CatalogFeedArgumentsType args =
        (CatalogFeedArgumentsType) a.getSerializable(
          CatalogFeedActivity.CATALOG_ARGS);
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
    final BooksType books = app.getBooks();
    final BooksControllerConfigurationType books_config =
      books.booksGetConfiguration();

    final boolean in_drawer_open = true;
    final ImmutableStack<CatalogFeedArgumentsType> empty =
      ImmutableStack.empty();
    final String in_title =
      NullCheck.notNull(rr.getString(R.string.feature_app_name));
    final URI in_uri = books_config.getCurrentRootFeedURI();

    return new CatalogFeedArgumentsRemote(
      in_drawer_open, NullCheck.notNull(empty), in_title, in_uri, false);
  }

  private void loadFeed(
    final FeedLoaderType feed_loader,
    final URI u)
  {
    CatalogFeedActivity.LOG.debug("loading feed: {}", u);
    final OptionType<HTTPAuthType> none = Option.none();
    this.loading = feed_loader.fromURIWithDatabaseEntries(u, none, this);
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return this.getUpStack().isEmpty();
  }

  private ImmutableStack<CatalogFeedArgumentsType> newUpStack(
    final CatalogFeedArgumentsType args)
  {
    final ImmutableStack<CatalogFeedArgumentsType> up_stack = this.getUpStack();
    return up_stack.push(args);
  }

  @Override public void onBookFeedFailure(
    final Throwable e)
  {
    if (e instanceof CancellationException) {
      CatalogFeedActivity.LOG.debug("Cancelled feed");
      return;
    }

    UIThread.runOnUIThread(
      new Runnable()
      {
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
      "received locally generated feed: {}", f.getFeedID());

    this.feed = f;
    this.onFeedWithoutGroups(f);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    this.navigationDrawerSetActionBarTitle();

    final CatalogFeedArgumentsType args = this.getArguments();
    final ImmutableStack<CatalogFeedArgumentsType> stack = this.getUpStack();
    this.configureUpButton(stack, args.getTitle());

    final Resources rr = NullCheck.notNull(this.getResources());
    setTitle(args.getTitle().equals(NullCheck.notNull(rr.getString(R.string.feature_app_name))) ? rr.getString(R.string.catalog) : args.getTitle());
    /**
     * Attempt to restore the saved scroll position, if there is one.
     */

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
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();

    if (stack.isEmpty()) {
      app.syncInitial();
    }

    /**
     * Display a progress bar until the feed is either loaded or fails.
     */

    final LayoutInflater inflater = this.getLayoutInflater();
    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup in_progress_layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_loading, content_area, false));

    content_area.addView(in_progress_layout);
    content_area.requestLayout();
    this.progress_layout = in_progress_layout;

    /**
     * If the feed is not locally generated, and the network is not
     * available, then fail fast and display an error message.
     */


    if (args.isLocallyGenerated() == false) {
      if (app.isNetworkAvailable() == false) {
        this.onNetworkUnavailable();
        return;
      }
    }

    /**
     * Create a dispatching function that will load a feed based on the given
     * arguments, and execute it.
     */

    args.matchArguments(
      new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>()
      {
        @Override public Unit onFeedArgumentsLocalBooks(
          final CatalogFeedArgumentsLocalBooks c)
        {
          CatalogFeedActivity.this.doLoadLocalFeed(c);
          return Unit.unit();
        }

        @Override public Unit onFeedArgumentsRemote(
          final CatalogFeedArgumentsRemote c)
        {
          CatalogFeedActivity.this.doLoadRemoteFeed(c);
          return Unit.unit();
        }
      });
  }

  @Override public boolean onCreateOptionsMenu(
    final @Nullable Menu in_menu)
  {
    final Menu menu_nn = NullCheck.notNull(in_menu);

    CatalogFeedActivity.LOG.debug("inflating menu");
    final MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.catalog, menu_nn);

    if (this.feed == null) {
      CatalogFeedActivity.LOG.debug(
        "menu creation requested but feed is not yet present");
      return true;
    }

    CatalogFeedActivity.LOG.debug(
      "menu creation requested and feed is " + "present");

    this.onCreateOptionsMenuSearchItem(menu_nn);
    return true;
  }



  /**
   * If the feed actually has a search URI, then show the search field.
   * Otherwise, disable and hide it.
   */
  private void onCreateOptionsMenuSearchItem(final Menu menu_nn) {
    final MenuItem search_item = menu_nn.findItem(R.id.catalog_action_search);

    final FeedType feed_actual = NullCheck.notNull(this.feed);

    final OptionType<FeedSearchType> search_opt = feed_actual.getFeedSearch();

    // Set some placeholder text
    final CatalogFeedArgumentsType args = this.getArguments();

    boolean search_ok = false;
    if (search_opt.isSome()) {
      final Some<FeedSearchType> search_some = (Some<FeedSearchType>) search_opt;

      this.search_view = (SearchView) search_item.getActionView();

      // Check that the search URI is of an understood type.
      final Resources rr = NullCheck.notNull(this.getResources());
      final FeedSearchType search = search_some.get();
      search_ok = search.matchSearch(
        new FeedSearchMatcherType<Boolean, UnreachableCodeException>() {
          @Override
          public Boolean onFeedSearchOpen1_1(
            final FeedSearchOpen1_1 fs) {
            CatalogFeedActivity.this.search_view.setOnQueryTextListener(
              new OpenSearchQueryHandler(
                rr, args, fs.getSearch()));
            return NullCheck.notNull(Boolean.TRUE);
          }

          @Override
          public Boolean onFeedSearchLocal(
            final FeedSearchLocal f) {
            CatalogFeedActivity.this.search_view.setOnQueryTextListener(
              new BooksLocalSearchQueryHandler(
                rr, args, FacetType.SORT_BY_TITLE));
            return NullCheck.notNull(Boolean.TRUE);
          }
        });

    } else {
      CatalogFeedActivity.LOG.debug("Feed has no search opts.");
    }

    if (search_ok) {
      this.search_view.setSubmitButtonEnabled(true);
      this.search_view.setIconifiedByDefault(false);
      search_item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
      search_item.expandActionView();

      // display either the category title or the previously searched keywords 
      this.search_view.setQueryHint(String.format(getString(R.string.search_hint_format), Objects.toString(this.feed.getFeedTitle(), getString(R.string.search_hint_feed_title_default))));
      if (args.getTitle().startsWith(getString(R.string.search_hint_prefix))) {
        this.search_view.setQueryHint(args.getTitle());
      }

      search_item.setEnabled(true);
      search_item.setVisible(true);
    } else {
      search_item.setEnabled(false);
      search_item.collapseActionView();
      search_item.setVisible(false);
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
    UIThread.runOnUIThread(
      new Runnable()
      {
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

    CatalogFeedActivity.LOG.info("Failed to get feed: ", e);
    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup error = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_loading_error, content_area, false));
    content_area.addView(error);
    content_area.requestLayout();

    final Button retry =
      NullCheck.notNull((Button) error.findViewById(R.id.catalog_error_retry));
    retry.setOnClickListener(
      new OnClickListener()
      {
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

    final CatalogFeedActivity cfa = this;
    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          cfa.configureUpButton(cfa.getUpStack(), f.getFeedTitle());
        }
      });

    f.matchFeed(this);
  }

  @Override public Unit onFeedWithGroups(
    final FeedWithGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed with blocks: {}", f.getFeedURI());

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          CatalogFeedActivity.this.onFeedWithGroupsUI(f);
        }
      });

    CatalogFeedActivity.onPossiblyReceivedEULALink(f.getFeedTermsOfService());
    return Unit.unit();
  }

  private void onFeedWithGroupsUI(
    final FeedWithGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed with blocks: {}", f.getFeedURI());

    UIThread.checkIsUIThread();

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_feed_groups_list, content_area, false));

    content_area.addView(layout);
    content_area.requestLayout();

    CatalogFeedActivity.LOG.debug(
      "restoring scroll position: {}", Integer.valueOf(this.saved_scroll_pos));

    final ListView list = NullCheck.notNull(
      (ListView) layout.findViewById(
        R.id.catalog_feed_blocks_list));

    this.swipe_refresh_layout = NullCheck.notNull((SwipeRefreshLayout) layout.findViewById(R.id.swipe_refresh_layout));

    this.swipe_refresh_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {

        CatalogFeedActivity.this.retryFeed();

      }
    });

    list.post(
      new Runnable()
      {
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
      new CatalogFeedLaneListenerType()
      {
        @Override public void onSelectBook(
          final FeedEntryOPDS e)
        {
          CatalogFeedActivity.this.onSelectedBook(app, new_up_stack, e);
        }

        @Override public void onSelectFeed(
          final FeedGroup in_group)
        {
          CatalogFeedActivity.this.onSelectedFeedGroup(new_up_stack, in_group);
        }
      };

    final CatalogFeedWithGroups cfl = new CatalogFeedWithGroups(
      this, app, app.getCoverProvider(), in_lane_listener, f);

    list.setAdapter(cfl);
    list.setOnScrollListener(cfl);
  }

  @Override public Unit onFeedWithoutGroups(
    final FeedWithoutGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed without blocks: {}", f.getFeedURI());

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          CatalogFeedActivity.this.onFeedWithoutGroupsUI(f);
        }
      });

    CatalogFeedActivity.onPossiblyReceivedEULALink(f.getFeedTermsOfService());
    return Unit.unit();
  }

  public void showAgeCheckAlert() {
    final AlertDialog.Builder builder = new AlertDialog.Builder(CatalogFeedActivity.this);

    builder.setTitle(R.string.age_verification_title);
    builder.setMessage(R.string.age_verification_question);

      // Under 13
      builder.setNeutralButton(R.string.age_verification_13_younger, (dialog, which) -> {
        Simplified.getSharedPrefs().putBoolean("age13", false);
        CatalogFeedActivity.this.reloadCatalogActivity(true);
      });

      // 13 or Over
      builder.setPositiveButton(R.string.age_verification_13_older, (dialog, which) -> {
        Simplified.getSharedPrefs().putBoolean("age13", true);
        CatalogFeedActivity.this.reloadCatalogActivity(false);
      });

    if(!this.isFinishing()) {
      AlertDialog alert = builder.show();
      final int resID = ThemeMatcher.Companion.color(Simplified.getCurrentAccount().getMainColor());
      final int mainTextColor = ContextCompat.getColor(this, resID);
      alert.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(mainTextColor);
      alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mainTextColor);
    }
  }

  private void reloadCatalogActivity(final boolean delete_books)
  {
    Simplified.getCatalogAppServices().reloadCatalog(delete_books, Simplified.getCurrentAccount());
    final Intent i = new Intent(CatalogFeedActivity.this, MainCatalogActivity.class);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    final Bundle b = new Bundle();
    SimplifiedActivity.setActivityArguments(b, false);
    i.putExtras(b);
    startActivity(i);
    overridePendingTransition(0, 0);
  }

  private void onFeedWithoutGroupsEmptyUI(
    final FeedWithoutGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed without blocks (empty): {}", f.getFeedURI());

    UIThread.checkIsUIThread();
    Assertions.checkPrecondition(f.isEmpty(), "Feed is empty");

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_feed_nogroups_empty, content_area, false));

    final TextView empty_text = NullCheck.notNull(
      (TextView) layout.findViewById(
        R.id.catalog_feed_nogroups_empty_text));

    if (this.getArguments().isSearching()) {
      final Resources resources = this.getResources();
      empty_text.setText(resources.getText(R.string.catalog_empty_feed));
    } else {
      empty_text.setText(this.catalogFeedGetEmptyText());
    }

    content_area.addView(layout);
    content_area.requestLayout();
  }

  private void onFeedWithoutGroupsNonEmptyUI(
    final FeedWithoutGroups f)
  {
    CatalogFeedActivity.LOG.debug(
      "received feed without blocks (non-empty): {}", f.getFeedURI());

    UIThread.checkIsUIThread();
    Assertions.checkPrecondition(!f.isEmpty(), "Feed is non-empty");

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_feed_nogroups, content_area, false));

    content_area.addView(layout);
    content_area.requestLayout();

    CatalogFeedActivity.LOG.debug(
      "restoring scroll position: {}", this.saved_scroll_pos);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final Resources rr = NullCheck.notNull(this.getResources());

    final GridView grid_view = NullCheck.notNull(
      (GridView) layout.findViewById(
        R.id.catalog_feed_nogroups_grid));

    this.swipe_refresh_layout = NullCheck.notNull((SwipeRefreshLayout) layout.findViewById(R.id.swipe_refresh_layout));

    this.swipe_refresh_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {

        final SimplifiedCatalogAppServicesType app =
          Simplified.getCatalogAppServices();
        final BooksType books = app.getBooks();

        books.accountSync(new SyncListener(), new DeviceActivationListenerType() {
          @Override
          public void onDeviceActivationFailure(final String message) {

          }

          @Override
          public void onDeviceActivationSuccess() {

          }
        });
        CatalogFeedActivity.this.retryFeed();

      }
    });

    this.configureFacets(f, layout, app, rr);

    grid_view.post(
      new Runnable()
      {
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
      new CatalogBookSelectionListenerType()
      {
        @Override public void onSelectBook(
          final CatalogFeedBookCellView v,
          final FeedEntryOPDS e)
        {
          CatalogFeedActivity.this.onSelectedBook(app, new_up_stack, e);
        }
      };

    final CatalogFeedWithoutGroups without = new CatalogFeedWithoutGroups(
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

  /**
   * The network is unavailable. Simply display a message and a button to allow
   * the user to retry loading when they have fixed their connection.
   */

  private void onNetworkUnavailable()
  {
    UIThread.checkIsUIThread();

    CatalogFeedActivity.LOG.debug("network is unavailable");

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup error = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_loading_not_connected, content_area, false));
    content_area.addView(error);
    content_area.requestLayout();

    final Button retry =
      NullCheck.notNull((Button) error.findViewById(R.id.catalog_error_retry));
    retry.setOnClickListener(
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
          CatalogFeedActivity.this.retryFeed();
        }
      });
  }

  @Override protected void onPause()
  {
    super.onPause();
    this.previously_paused = true;
  }

  @Override protected void onSaveInstanceState(
    final @Nullable Bundle state)
  {
    super.onSaveInstanceState(state);

    CatalogFeedActivity.LOG.debug("saving state");

    /**
     * Save the scroll position in the hope that it can be restored later.
     */

    final Bundle nn_state = NullCheck.notNull(state);
    final AbsListView lv = this.list_view;
    if (lv != null) {
      final int position = lv.getFirstVisiblePosition();
      CatalogFeedActivity.LOG.debug(
        "saving list view position: {}", Integer.valueOf(position));
      nn_state.putInt(CatalogFeedActivity.LIST_STATE_ID, position);
    }
  }

  private void onSelectedBook(
    final SimplifiedCatalogAppServicesType app,
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack,
    final FeedEntryOPDS e)
  {
    CatalogFeedActivity.LOG.debug("onSelectedBook: {}", this);
    CatalogBookDetailActivity.startNewActivity(
      CatalogFeedActivity.this,
      new_up_stack,
      this.navigationDrawerGetPart(),
      e);
  }

  private void onSelectedFeedGroup(
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack,
    final FeedGroup f)
  {
    CatalogFeedActivity.LOG.debug("onSelectFeed: {}", this);

    final CatalogFeedArgumentsRemote remote = new CatalogFeedArgumentsRemote(
      false, new_up_stack, f.getGroupTitle(), f.getGroupURI(), false);
    this.catalogActivityForkNew(remote);
  }

  /**
   * Retry the current feed.
   */

  protected final void retryFeed()
  {
    final CatalogFeedArgumentsType args = this.getArguments();
    CatalogFeedActivity.LOG.debug("retrying feed {}", args);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final FeedLoaderType loader = app.getFeedLoader();

    args.matchArguments(
      new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>()
      {
        @Override public Unit onFeedArgumentsLocalBooks(
          final CatalogFeedArgumentsLocalBooks c)
        {
          CatalogFeedActivity.this.catalogActivityForkNewReplacing(args);
          if (CatalogFeedActivity.this.swipe_refresh_layout != null)
          {
            CatalogFeedActivity.this.swipe_refresh_layout.setRefreshing(false);
          }
          return Unit.unit();
        }

        @Override public Unit onFeedArgumentsRemote(
          final CatalogFeedArgumentsRemote c)
        {
          loader.invalidate(c.getURI());
          CatalogFeedActivity.this.catalogActivityForkNewReplacing(args);
          if (CatalogFeedActivity.this.swipe_refresh_layout != null)
          {
            CatalogFeedActivity.this.swipe_refresh_layout.setRefreshing(false);
          }
          return Unit.unit();
        }
      });
  }

  /**
   * @return The text to display when a feed is empty.
   */

  protected abstract String catalogFeedGetEmptyText();

  /**
   * Unconditionally load a locally-generated feed.
   *
   * @param c The feed arguments
   */

  private void doLoadLocalFeed(final CatalogFeedArgumentsLocalBooks c)
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final Resources resources = CatalogFeedActivity.this.getResources();

    final BooksType books = app.getBooks();
    final Calendar now = NullCheck.notNull(Calendar.getInstance());
    final URI dummy_uri = NullCheck.notNull(URI.create("Books"));
    final String dummy_id = NullCheck.notNull(
      resources.getString(R.string.books));
    final String title = NullCheck.notNull(
      resources.getString(R.string.books));
    final String facet_group = NullCheck.notNull(
      resources.getString(R.string.books_sort_by));
    final BooksFeedSelection selection = c.getSelection();

    books.booksGetFeed(
      dummy_uri,
      dummy_id,
      now,
      title,
      c.getFacetType(),
      facet_group,
      new CatalogFacetPseudoTitleProvider(resources),
      c.getSearchTerms(),
      selection,
      CatalogFeedActivity.this);
  }

  /**
   * Unconditionally load a remote feed.
   *
   * @param c The feed arguments
   */

  private void doLoadRemoteFeed(final CatalogFeedArgumentsRemote c)
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final FeedLoaderType feed_loader = app.getFeedLoader();
    CatalogFeedActivity.this.loadFeed(feed_loader, c.getURI());
  }

  /**
   * A handler for local book searches.
   */

  private final class BooksLocalSearchQueryHandler
    implements OnQueryTextListener
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
        ImmutableStack.empty();


      final String title =
        this.resources.getString(R.string.catalog_search) + ": " + qnn;

      final CatalogFeedArgumentsLocalBooks new_args =
        new CatalogFeedArgumentsLocalBooks(
          us,
          title,
          this.facet_active,
          Option.some(qnn),
          cfa.getLocalFeedTypeSelection());
      if ("Search".equals(CatalogFeedActivity.this.feed.getFeedTitle())) {
        cfa.catalogActivityForkNewReplacing(new_args);
      }
      else
      {
        cfa.catalogActivityForkNew(new_args);
      }


      return true;
    }
  }

  /**
   * A handler for OpenSearch 1.1 searches.
   */

  private final class OpenSearchQueryHandler implements OnQueryTextListener
  {
    private final CatalogFeedArgumentsType args;
    private final OPDSOpenSearch1_1        search;
    private final Resources                resources;

    OpenSearchQueryHandler(
      final Resources in_resources,
      final CatalogFeedArgumentsType in_args,
      final OPDSOpenSearch1_1 in_search)
    {
      this.resources = NullCheck.notNull(in_resources);
      this.args = NullCheck.notNull(in_args);
      this.search = NullCheck.notNull(in_search);
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
      final URI target = this.search.getQueryURIForTerms(qnn);

      final CatalogFeedActivity cfa = CatalogFeedActivity.this;
      final ImmutableStack<CatalogFeedArgumentsType> us = cfa.newUpStack(this.args);

      final String title =
        this.resources.getString(R.string.catalog_search) + ": " + qnn;

      final CatalogFeedArgumentsRemote new_args =
        new CatalogFeedArgumentsRemote(false, us, title, target, true);

      if ("Search".equals(CatalogFeedActivity.this.feed.getFeedTitle())) {
        cfa.catalogActivityForkNewReplacing(new_args);
      }
      else
      {
        cfa.catalogActivityForkNew(new_args);
      }
      return true;
    }
  }

  private final class SyncListener implements AccountSyncListenerType
  {
    SyncListener()
    {

    }

    @Override
    public void onAccountSyncAuthenticationFailure(final String message)
    {
      CatalogFeedActivity.LOG.debug("account syncing failed: {}", message);

      final boolean clever_enabled = CatalogFeedActivity.this.getResources().getBoolean(R.bool.feature_auth_provider_clever);

      if (clever_enabled) {

        final Intent account =
          new Intent(CatalogFeedActivity.this, LoginActivity.class);

        CatalogFeedActivity.this.startActivityForResult(account, 1);

        CatalogFeedActivity.this.overridePendingTransition(0, 0);

      } else {

        final AccountBarcode barcode = new AccountBarcode("");
        final AccountPIN pin = new AccountPIN("");

        final LoginDialog df =
          LoginDialog.newDialog("Login required", barcode, pin);

        final FragmentManager fm = CatalogFeedActivity.this.getFragmentManager();
        df.show(fm, "login-dialog");
      }
    }

    @Override public void onAccountSyncBook(final BookID book)
    {
      CatalogFeedActivity.LOG.debug("synced: {}", book);
    }

    @Override public void onAccountSyncFailure(
      final OptionType<Throwable> error,
      final String message)
    {
      LogUtilities.errorWithOptionalException(
        CatalogFeedActivity.LOG, message, error);
    }

    @Override public void onAccountSyncSuccess()
    {
      CatalogFeedActivity.LOG.debug("account syncing finished");
    }

    @Override public void onAccountSyncBookDeleted(final BookID book)
    {
      CatalogFeedActivity.LOG.debug("book deleted: {}", book);
    }
  }
}
