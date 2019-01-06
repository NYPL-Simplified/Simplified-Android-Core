package org.nypl.simplified.app.catalog;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.app.ApplicationColorScheme;
import org.nypl.simplified.app.NavigationDrawerActivity;
import org.nypl.simplified.app.NetworkConnectivityType;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.ScreenSizeInformationType;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.ThemeMatcher;
import org.nypl.simplified.app.login.LoginActivity;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.book_registry.BookStatusEvent;
import org.nypl.simplified.books.controller.ProfileFeedRequest;
import org.nypl.simplified.books.document_store.DocumentStoreType;
import org.nypl.simplified.books.eula.EULAType;
import org.nypl.simplified.books.feeds.FeedBooksSelection;
import org.nypl.simplified.books.feeds.FeedEntryOPDS;
import org.nypl.simplified.books.feeds.FeedFacetMatcherType;
import org.nypl.simplified.books.feeds.FeedFacetOPDS;
import org.nypl.simplified.books.feeds.FeedFacetPseudo;
import org.nypl.simplified.books.feeds.FeedFacetType;
import org.nypl.simplified.books.feeds.FeedFacets;
import org.nypl.simplified.books.feeds.FeedGroup;
import org.nypl.simplified.books.feeds.FeedLoaderAuthenticationListenerType;
import org.nypl.simplified.books.feeds.FeedLoaderListenerType;
import org.nypl.simplified.books.feeds.FeedLoaderType;
import org.nypl.simplified.books.feeds.FeedMatcherType;
import org.nypl.simplified.books.feeds.FeedSearchLocal;
import org.nypl.simplified.books.feeds.FeedSearchMatcherType;
import org.nypl.simplified.books.feeds.FeedSearchOpen1_1;
import org.nypl.simplified.books.feeds.FeedSearchType;
import org.nypl.simplified.books.feeds.FeedType;
import org.nypl.simplified.books.feeds.FeedWithGroups;
import org.nypl.simplified.books.feeds.FeedWithoutGroups;
import org.nypl.simplified.books.profiles.ProfileAccountSelectEvent;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.nypl.simplified.opds.core.OPDSFacet;
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;
import org.nypl.simplified.stack.ImmutableStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static org.nypl.simplified.books.feeds.FeedFacetPseudo.FacetType;
import static org.nypl.simplified.books.feeds.FeedFacetPseudo.FacetType.SORT_BY_TITLE;

/**
 * The main catalog feed activity, responsible for displaying different types of
 * feeds.
 */

public abstract class CatalogFeedActivity extends CatalogActivity implements
  FeedMatcherType<Unit, UnreachableCodeException>,
  FeedLoaderListenerType {

  private static final String CATALOG_ARGS;
  private static final String LIST_STATE_ID;
  private static final Logger LOG = LoggerFactory.getLogger(CatalogFeedActivity.class);

  static {
    CATALOG_ARGS =
      "org.nypl.simplified.app.CatalogFeedActivity.arguments";
    LIST_STATE_ID =
      "org.nypl.simplified.app.CatalogFeedActivity.list_view_state";
  }

  private FeedType feed;
  private AbsListView list_view;
  private SwipeRefreshLayout swipe_refresh_layout;
  private ListenableFuture<FeedType> loading;
  private ViewGroup progress_layout;
  private int saved_scroll_pos;
  private boolean previously_paused;
  private SearchView search_view;
  private ObservableSubscriptionType<ProfileEvent> profile_event_subscription;
  private ObservableSubscriptionType<BookStatusEvent> book_event_subscription;

  /**
   * @return The specific logger instance provided by subclasses
   */

  protected abstract Logger log();

  /**
   * Construct an activity.
   */

  public CatalogFeedActivity() {

  }

  /**
   * Set the arguments of the activity to be created.
   * Modifies Bundle based on attributes and type (from local or remote)
   * before being given to Intent in the calling method.
   *
   * @param b    The argument bundle
   * @param args The feed arguments
   */

  public static void setActivityArguments(
    final Bundle b,
    final CatalogFeedArgumentsType args) {
    NullCheck.notNull(b);
    NullCheck.notNull(args);

    b.putSerializable(CATALOG_ARGS, args);

    args.matchArguments(
      new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>() {
        @Override
        public Unit onFeedArgumentsLocalBooks(final CatalogFeedArgumentsLocalBooks c) {
          NavigationDrawerActivity.setActivityArguments(b, false);
          CatalogActivity.setActivityArguments(b, c.getUpStack());
          return Unit.unit();
        }

        @Override
        public Unit onFeedArgumentsRemote(final CatalogFeedArgumentsRemote c) {
          NavigationDrawerActivity.setActivityArguments(b, c.isDrawerOpen());
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
   * @see EULAType
   */

  private static void onPossiblyReceivedEULALink(final OptionType<URI> latest) {
    latest.map_(latest_actual -> {
      final DocumentStoreType docs = Simplified.getDocumentStore();

      docs.getEULA().map_(
        eula -> {
          try {
            eula.documentSetLatestURL(latest_actual.toURL());
          } catch (final MalformedURLException e) {
            LOG.error("could not use latest EULA link: ", e);
          }
        });
    });
  }

  @Override
  public void onBackPressed() {
    this.invalidateOptionsMenu();
    super.onBackPressed();
  }

  @Override
  protected void onResume() {
    super.onResume();

    /*
     * If the activity was previously paused, this means that the user
     * navigated away from the activity and is now coming back to it. If the
     * user went into a book detail view and revoked a book, then the feed
     * should be completely reloaded when the user comes back, to ensure that
     * the book no longer shows up in the list.
     *
     * This obviously only applies to local feeds.
     */

    if (this.search_view != null) {
      this.search_view.setQuery("", false);
      this.search_view.clearFocus();
    }

    boolean did_retry = false;
    final Bundle extras = getIntent().getExtras();
    if (extras != null) {
      final boolean reload = extras.getBoolean("reload");
      if (reload) {
        did_retry = true;
        CatalogFeedActivity.this.retryFeed();
        extras.putBoolean("reload", false);
      }
    }

    if (this.previously_paused && !did_retry) {
      final CatalogFeedArgumentsType args = this.getArguments();
      if (!args.requiresNetworkConnectivity()) {
        this.retryFeed();
      }
    }
  }

  /**
   * Configure the "entry point" facet layout. This causes the "entry point" buttons to appear
   * (or not) at the top of the screen based on the available facets.
   *
   * @param feed        The feed
   * @param layout      The view group that will contain facet elements
   * @param resources   The app resources
   * @param colorScheme The current app color scheme
   */

  private void configureFacetEntryPointButtons(
    final FeedType feed,
    final ViewGroup layout,
    final Resources resources,
    final ApplicationColorScheme colorScheme) {

    UIThread.checkIsUIThread();

    final RadioGroup facets_view =
      NullCheck.notNull(layout.findViewById(R.id.catalog_feed_facet_tabs));

    final OptionType<List<FeedFacetType>> facet_group_opt =
      FeedFacets.findEntryPointFacetGroupForFeed(feed);

    /*
     * If there isn't a group of facets representing an entry point, hide the space in which
     * they would have appeared.
     */

    if (facet_group_opt.isNone()) {
      facets_view.setVisibility(View.GONE);
      return;
    }

    /*
     * Add a set of radio buttons to the view.
     */

    final List<FeedFacetType> facet_group = ((Some<List<FeedFacetType>>) facet_group_opt).get();
    final ColorStateList text_color = createTextColorForRadioButton(resources, colorScheme);

    final int size = facet_group.size();

    for (int index = 0; index < size; ++index) {
      final FeedFacetType facet = facet_group.get(index);
      final RadioButton button = new RadioButton(this);

      final LinearLayout.LayoutParams button_layout =
        new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT, 1.0f / (float) size);

      button.setLayoutParams(button_layout);
      button.setTextColor(text_color);
      button.setGravity(Gravity.CENTER);

      /*
       * The buttons need unique IDs so that they can be addressed within the parent
       * radio group.
       */

      button.setId(View.generateViewId());

      if (index == 0) {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_left);
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_left);
      } else if (index == size - 1) {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_right);
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_right);
      } else {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_middle);
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_middle);
      }

      button.setText(facet.facetGetTitle());

      final CatalogFeedFacetLauncher launcher =
        new CatalogFeedFacetLauncher(this, feed, resources, this.getLocalSearchTerms());

      button.setOnClickListener(ignored -> {
        LOG.debug("selected entry point facet: {}", facet.facetGetTitle());
        facet.matchFeedFacet(launcher);
      });
      facets_view.addView(button);
    }

    /*
     * Uncheck all of the buttons, and then check the one that corresponds to the current
     * active facet.
     */

    facets_view.clearCheck();

    for (int index = 0; index < size; ++index) {
      final FeedFacetType facet = facet_group.get(index);
      final RadioButton button = (RadioButton) facets_view.getChildAt(index);

      if (facet.facetIsActive()) {
        LOG.debug("active entry point facet: {}", facet.facetGetTitle());
        facets_view.check(button.getId());
      }
    }
  }

  /**
   * Create a color state list that will return the correct text color for a radio button.
   */

  private static ColorStateList createTextColorForRadioButton(
    final Resources resources,
    final ApplicationColorScheme colorScheme) {

    final int mainColor = colorScheme.getColorRGBA();
    final int[][] states = new int[][]{
      new int[]{android.R.attr.state_checked}, // Checked
      new int[]{-android.R.attr.state_checked}, // Unchecked
    };

    final int[] colors = new int[]{
      resources.getColor(R.color.button_background),
      mainColor,
    };

    return new ColorStateList(states, colors);
  }

  /**
   * Configure the facets layout. This is what causes facets to be shown or not
   * shown at the top of the screen when rendering a feed.
   *
   * @param screen    A provider of screen size information
   * @param feed      The feed
   * @param layout    The view group that will contain facet elements
   * @param resources The app resources
   */

  private void configureFacets(
    final ScreenSizeInformationType screen,
    final FeedWithoutGroups feed,
    final ViewGroup layout,
    final Resources resources) {

    UIThread.checkIsUIThread();

    final ViewGroup facets_view =
      NullCheck.notNull(layout.findViewById(R.id.catalog_feed_nogroups_facets));
    final View facet_divider =
      NullCheck.notNull(layout.findViewById(R.id.catalog_feed_nogroups_facet_divider));

    final Map<String, List<FeedFacetType>> facet_groups = feed.getFeedFacetsByGroup();

    /*
     * If the facet groups are empty, or the only available facets are "entry point" facets,
     * then no facet bar should be displayed.
     */

    if (facet_groups.isEmpty() || FeedFacets.facetGroupsAreAllEntryPoints(facet_groups)) {
      facets_view.setVisibility(View.GONE);
      facet_divider.setVisibility(View.GONE);
      return;
    }

    /*
     * Otherwise, for each facet group, show a drop-down menu allowing
     * the selection of individual facets.
     */

    for (final String group_name : facet_groups.keySet()) {
      final List<FeedFacetType> group = NullCheck.notNull(facet_groups.get(group_name));
      final ArrayList<FeedFacetType> group_copy = new ArrayList<FeedFacetType>(group);

      final LinearLayout.LayoutParams tvp =
        new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
      tvp.rightMargin = (int) screen.screenDPToPixels(8);

      final TextView tv = new TextView(this);
      tv.setTextColor(resources.getColor(R.color.normal_text_major));
      tv.setTextSize(12.0f);
      tv.setText(group_name + ":");
      tv.setLayoutParams(tvp);
      facets_view.addView(tv);

      final OptionType<String> search_terms = this.getLocalSearchTerms();

      final CatalogFeedFacetLauncher facet_feed_listener =
        new CatalogFeedFacetLauncher(this, feed, resources, search_terms);

      final CatalogFacetSelectionListenerType facet_listener =
        selected -> selected.matchFeedFacet(facet_feed_listener);

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

  private OptionType<String> getLocalSearchTerms() {
    final OptionType<String> search_terms;
    final CatalogFeedArgumentsType current_args = this.getArguments();
    if (current_args instanceof CatalogFeedArgumentsLocalBooks) {
      final CatalogFeedArgumentsLocalBooks locals = (CatalogFeedArgumentsLocalBooks) current_args;
      search_terms = locals.getSearchTerms();
    } else {
      search_terms = Option.none();
    }
    return search_terms;
  }

  /**
   * If this activity is being used in a part of the application that generates
   * local feeds, then return the type of feed that should be generated.
   *
   * @return The type of feed that should be generated.
   */

  protected abstract FeedBooksSelection getLocalFeedTypeSelection();

  @Override
  public void onFeedRequiresAuthentication(
    final URI u,
    final int attempts,
    final FeedLoaderAuthenticationListenerType listener) {

    /*
     * Redirect the user to a login screen.
     */

    UIThread.runOnUIThread(() -> {
      final Intent i = new Intent(CatalogFeedActivity.this, LoginActivity.class);
      this.startActivity(i);
      this.finish();
    });
  }

  private void configureUpButton(
    final ImmutableStack<CatalogFeedArgumentsType> up_stack,
    final String title) {
    final ActionBar bar = this.getActionBar();
    if (!up_stack.isEmpty()) {
      bar.setTitle(title);
    }
  }

  private CatalogFeedArgumentsType getArguments() {
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
        LOG.debug("Hardcoding SimplyE Collection URI: {}", ageURI);
        return new CatalogFeedArgumentsRemote(
          false,
          ImmutableStack.empty(),
          lib_title,
          ageURI,
          false
        );
      } catch (Exception e) {
        LOG.error(
          "error constructing SimplyE collection uri: {}", e.getMessage(), e);
      }
    }
    /*
     * End of hack..
     */


    /*
     * Attempt to fetch arguments.
     */

    final Resources rr = NullCheck.notNull(this.getResources());
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = i.getExtras();
    if (a != null) {
      final CatalogFeedArgumentsType args =
        (CatalogFeedArgumentsType) a.getSerializable(
          CATALOG_ARGS);
      if (args != null) {
        return args;
      }
    }

    /*
     * If there were no arguments (because, for example, this activity is the
     * initial one started for the app), synthesize some.
     */

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    final BooksControllerConfigurationType books_config =
      books.booksGetConfiguration();

    final boolean in_drawer_open = true;
    final ImmutableStack<CatalogFeedArgumentsType> empty = ImmutableStack.empty();
    final String in_title = NullCheck.notNull(rr.getString(R.string.feature_app_name));
    final URI in_uri = books_config.getCurrentRootFeedURI();

    return new CatalogFeedArgumentsRemote(
      in_drawer_open, NullCheck.notNull(empty), in_title, in_uri, false);
  }

  private void loadFeed(
    final FeedLoaderType feed_loader,
    final URI u) {
    LOG.debug("loading feed: {}", u);
    final OptionType<HTTPAuthType> none = Option.none();
    this.loading = feed_loader.fromURIWithBookRegistryEntries(u, none, this);
  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return this.getUpStack().isEmpty();
  }

  private ImmutableStack<CatalogFeedArgumentsType> newUpStack(
    final CatalogFeedArgumentsType args) {
    final ImmutableStack<CatalogFeedArgumentsType> up_stack = this.getUpStack();
    return up_stack.push(args);
  }

  private void onProfileEvent(final ProfileEvent event) {

    /*
     * If the current profile changed accounts, start a new catalog feed activity. The
     * new activity will automatically navigate to the root of the new account's catalog.
     */

    if (event instanceof ProfileAccountSelectEvent.ProfileAccountSelectSucceeded) {
      UIThread.runOnUIThread(() -> {
        final Intent i = new Intent(CatalogFeedActivity.this, MainCatalogActivity.class);
        final Bundle b = new Bundle();
        NavigationDrawerActivity.setActivityArguments(b, false);
        i.putExtras(b);
        this.startActivity(i);
        this.finish();
      });
    }
  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    super.onCreate(state);

    final CatalogFeedArgumentsType args = this.getArguments();
    final ImmutableStack<CatalogFeedArgumentsType> stack = this.getUpStack();
    this.configureUpButton(stack, args.getTitle());

    final Resources rr = NullCheck.notNull(this.getResources());
    setTitle(args.getTitle().equals(NullCheck.notNull(rr.getString(R.string.feature_app_name))) ? rr.getString(R.string.catalog) : args.getTitle());

    /*
     * Attempt to restore the saved scroll position, if there is one.
     */

    if (state != null) {
      LOG.debug("received state");
      this.saved_scroll_pos = state.getInt(LIST_STATE_ID);
    } else {
      this.saved_scroll_pos = 0;
    }

    /*
     * Display a progress bar until the feed is either loaded or fails.
     */

    final LayoutInflater inflater = this.getLayoutInflater();
    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup in_progress_layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(R.layout.catalog_loading, content_area, false));

    content_area.addView(in_progress_layout);
    content_area.requestLayout();
    this.progress_layout = in_progress_layout;

    /*
     * If the feed requires network connectivity, and the network is not
     * available, then fail fast and display an error message.
     */

    final NetworkConnectivityType net = Simplified.getNetworkConnectivity();
    if (args.requiresNetworkConnectivity()) {
      if (!net.isNetworkAvailable()) {
        this.onNetworkUnavailable();
        return;
      }
    }

    /*
     * Create a dispatching function that will load a feed based on the given
     * arguments, and execute it.
     */

    args.matchArguments(
      new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>() {
        @Override
        public Unit onFeedArgumentsLocalBooks(final CatalogFeedArgumentsLocalBooks c) {
          CatalogFeedActivity.this.doLoadLocalFeed(c);
          return Unit.unit();
        }

        @Override
        public Unit onFeedArgumentsRemote(final CatalogFeedArgumentsRemote c) {
          CatalogFeedActivity.this.doLoadRemoteFeed(c);
          return Unit.unit();
        }
      });

    /*
     * Subscribe to profile change events.
     */

    this.profile_event_subscription =
      Simplified.getProfilesController()
        .profileEvents()
        .subscribe(this::onProfileEvent);
  }

  @Override
  public boolean onCreateOptionsMenu(final @Nullable Menu in_menu) {
    final Menu menu_nn = NullCheck.notNull(in_menu);

    LOG.debug("inflating menu");
    final MenuInflater inflater = this.getMenuInflater();
    inflater.inflate(R.menu.catalog, menu_nn);

    if (this.feed == null) {
      LOG.debug("menu creation requested but feed is not yet present");
      return true;
    }

    LOG.debug("menu creation requested and feed is present");
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
              new BooksLocalSearchQueryHandler(rr, args, SORT_BY_TITLE));
            return NullCheck.notNull(Boolean.TRUE);
          }
        });

    } else {
      LOG.debug("Feed has no search opts.");
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

  @Override
  protected void onDestroy() {
    super.onDestroy();
    LOG.debug("onDestroy");

    final ListenableFuture<FeedType> future = this.loading;
    if (future != null) {
      future.cancel(true);
    }

    final ObservableSubscriptionType<ProfileEvent> profile_sub = this.profile_event_subscription;
    if (profile_sub != null) {
      profile_sub.unsubscribe();
    }

    final ObservableSubscriptionType<BookStatusEvent> book_sub = this.book_event_subscription;
    if (book_sub != null) {
      book_sub.unsubscribe();
    }
  }

  @Override
  public void onFeedLoadFailure(
    final URI u,
    final Throwable x) {
    UIThread.runOnUIThread(() -> this.onFeedLoadingFailureUI(x));
  }

  private void onFeedLoadingFailureUI(final Throwable e) {
    UIThread.checkIsUIThread();

    LOG.info("Failed to get feed: ", e);
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

    final Button retry = NullCheck.notNull(error.findViewById(R.id.catalog_error_retry));
    retry.setOnClickListener(v -> CatalogFeedActivity.this.retryFeed());
  }

  @Override
  public void onFeedLoadSuccess(
    final URI u,
    final FeedType f) {
    LOG.debug("received feed for {}", u);
    this.feed = f;

    UIThread.runOnUIThread(() -> this.configureUpButton(this.getUpStack(), f.getFeedTitle()));
    f.matchFeed(this);
  }

  @Override
  public Unit onFeedWithGroups(final FeedWithGroups feed) {
    LOG.debug("received feed with groups: {}", feed.getFeedURI());

    UIThread.runOnUIThread(() -> this.onFeedWithGroupsUI(feed));
    onPossiblyReceivedEULALink(feed.getFeedTermsOfService());
    return Unit.unit();
  }

  private void onFeedWithGroupsUI(final FeedWithGroups feed) {
    LOG.debug("received feed with groups: {}", feed.getFeedURI());

    UIThread.checkIsUIThread();

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(R.layout.catalog_feed_groups_list, content_area, false));

    content_area.addView(layout);
    content_area.requestLayout();

    LOG.debug("restoring scroll position: {}", this.saved_scroll_pos);

    final ListView list = NullCheck.notNull(layout.findViewById(R.id.catalog_feed_groups_list));

    this.swipe_refresh_layout = NullCheck.notNull(layout.findViewById(R.id.swipe_refresh_layout));
    this.swipe_refresh_layout.setOnRefreshListener(this::retryFeed);

    list.post(() -> list.setSelection(this.saved_scroll_pos));
    list.setDividerHeight(0);
    this.list_view = list;

    this.configureFacetEntryPointButtons(
      this.feed,
      layout,
      this.getResources(),
      Simplified.getMainColorScheme());

    final CatalogFeedArgumentsType args = this.getArguments();
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack =
      this.newUpStack(args);

    final CatalogFeedLaneListenerType lane_listener =
      new CatalogFeedLaneListenerType() {
        @Override
        public void onSelectFeed(FeedGroup in_group) {
          CatalogFeedActivity.this.onSelectedFeedGroup(new_up_stack, in_group);
        }

        @Override
        public void onSelectBook(final FeedEntryOPDS e) {
          CatalogFeedActivity.this.onSelectedBook(new_up_stack, e);
        }
      };

    final CatalogFeedWithGroups cfl;
    try {
      cfl = new CatalogFeedWithGroups(
        this,
        Simplified.getProfilesController().profileAccountCurrent(),
        Simplified.getScreenSizeInformation(),
        Simplified.getCoverProvider(),
        lane_listener,
        feed,
        Simplified.getMainColorScheme());
    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }

    list.setAdapter(cfl);
    list.setOnScrollListener(cfl);
  }

  @Override
  public Unit onFeedWithoutGroups(final FeedWithoutGroups feed) {
    LOG.debug("received feed without blocks: {}", feed.getFeedURI());

    UIThread.runOnUIThread(() -> this.onFeedWithoutGroupsUI(feed));

    onPossiblyReceivedEULALink(feed.getFeedTermsOfService());
    return Unit.unit();
  }

  private void onFeedWithoutGroupsEmptyUI(final FeedWithoutGroups feed) {
    LOG.debug("received feed without blocks (empty): {}", feed.getFeedURI());

    UIThread.checkIsUIThread();
    Assertions.checkPrecondition(feed.isEmpty(), "Feed is empty");

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.catalog_feed_nogroups_empty, content_area, false));

    final TextView empty_text =
      NullCheck.notNull(layout.findViewById(R.id.catalog_feed_nogroups_empty_text));

    if (this.getArguments().isSearching()) {
      final Resources resources = this.getResources();
      empty_text.setText(resources.getText(R.string.catalog_empty_feed));
    } else {
      empty_text.setText(this.catalogFeedGetEmptyText());
    }

    content_area.addView(layout);
    content_area.requestLayout();
  }

  private void onFeedWithoutGroupsNonEmptyUI(final FeedWithoutGroups feed_without_groups) {
    LOG.debug("received feed without blocks (non-empty): {}", feed_without_groups.getFeedURI());

    UIThread.checkIsUIThread();
    Assertions.checkPrecondition(!feed_without_groups.isEmpty(), "Feed is non-empty");

    this.invalidateOptionsMenu();

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup progress = NullCheck.notNull(this.progress_layout);
    progress.setVisibility(View.GONE);
    content_area.removeAllViews();

    final LayoutInflater inflater = this.getLayoutInflater();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(R.layout.catalog_feed_nogroups, content_area, false));

    content_area.addView(layout);
    content_area.requestLayout();

    LOG.debug("restoring scroll position: {}", this.saved_scroll_pos);

    final Resources rr =
      NullCheck.notNull(this.getResources());
    final GridView grid_view =
      NullCheck.notNull(layout.findViewById(R.id.catalog_feed_nogroups_grid));

    this.swipe_refresh_layout = NullCheck.notNull(layout.findViewById(R.id.swipe_refresh_layout));
    this.swipe_refresh_layout.setOnRefreshListener(this::retryFeed);

    this.configureFacetEntryPointButtons(
      feed_without_groups, layout, rr, Simplified.getMainColorScheme());
    this.configureFacets(
      Simplified.getScreenSizeInformation(), feed_without_groups, layout, rr);

    grid_view.post(() -> grid_view.setSelection(this.saved_scroll_pos));
    this.list_view = grid_view;

    final CatalogFeedArgumentsType args = this.getArguments();
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack =
      this.newUpStack(args);

    final CatalogBookSelectionListenerType book_select_listener =
      (v, e) -> this.onSelectedBook(new_up_stack, e);

    final CatalogFeedWithoutGroups without;
    try {
      without = new CatalogFeedWithoutGroups(
        this,
        Simplified.getProfilesController().profileAccountCurrent(),
        Simplified.getCoverProvider(),
        book_select_listener,
        Simplified.getBooksRegistry(),
        Simplified.getBooksController(),
        Simplified.getProfilesController(),
        Simplified.getFeedLoader(),
        feed_without_groups,
        Simplified.getDocumentStore(),
        Simplified.getNetworkConnectivity(),
        Simplified.getBackgroundTaskExecutor(),
        Simplified.getMainColorScheme());
    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }

    grid_view.setAdapter(without);
    grid_view.setOnScrollListener(without);

    /*
     * Subscribe the grid view to book events. This will allow individual cells to be
     * updated whenever the status of a book changes.
     */

    this.book_event_subscription =
      Simplified.getBooksRegistry()
        .bookEvents()
        .subscribe(without::onBookEvent);
  }

  private void onFeedWithoutGroupsUI(final FeedWithoutGroups feed) {
    UIThread.checkIsUIThread();

    if (feed.isEmpty()) {
      this.onFeedWithoutGroupsEmptyUI(feed);
      return;
    }

    this.onFeedWithoutGroupsNonEmptyUI(feed);
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

    if (!this.isFinishing()) {
      AlertDialog alert = builder.show();
      final int resID = ThemeMatcher.Companion.color(Simplified.getCurrentAccount().getMainColor());
      final int mainTextColor = ContextCompat.getColor(this, resID);
      alert.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(mainTextColor);
      alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mainTextColor);
    }
  }

  private void reloadCatalogActivity(final boolean delete_books) {
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

  /**
   * The network is unavailable. Simply display a message and a button to allow
   * the user to retry loading when they have fixed their connection.
   */

  private void onNetworkUnavailable() {
    UIThread.checkIsUIThread();

    LOG.debug("network is unavailable");

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

    final Button retry = NullCheck.notNull(error.findViewById(R.id.catalog_error_retry));
    retry.setOnClickListener(v -> CatalogFeedActivity.this.retryFeed());
  }

  @Override
  protected void onPause() {
    super.onPause();
    this.previously_paused = true;
  }

  @Override
  protected void onSaveInstanceState(
    final @Nullable Bundle state) {
    super.onSaveInstanceState(state);

    LOG.debug("saving state");

    /*
     * Save the scroll position in the hope that it can be restored later.
     */

    final Bundle nn_state = NullCheck.notNull(state);
    final AbsListView lv = this.list_view;
    if (lv != null) {
      final int position = lv.getFirstVisiblePosition();
      LOG.debug("saving list view position: {}", Integer.valueOf(position));
      nn_state.putInt(LIST_STATE_ID, position);
    }
  }

  private void onSelectedBook(
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack,
    final FeedEntryOPDS e) {
    LOG.debug("onSelectedBook: {}", this);
    CatalogBookDetailActivity.startNewActivity(this, new_up_stack, e);
  }

  private void onSelectedFeedGroup(
    final ImmutableStack<CatalogFeedArgumentsType> new_up_stack,
    final FeedGroup f) {
    LOG.debug("onSelectFeed: {}", this);

    final CatalogFeedArgumentsRemote remote =
      new CatalogFeedArgumentsRemote(
        false, new_up_stack, f.getGroupTitle(), f.getGroupURI(), false);
    this.catalogActivityForkNew(remote);
  }

  /**
   * Retry the current feed.
   */

  protected final void retryFeed() {
    final CatalogFeedArgumentsType args = this.getArguments();
    LOG.debug("retrying feed {}", args);

    final FeedLoaderType loader = Simplified.getFeedLoader();

    args.matchArguments(
      new CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException>() {
        @Override
        public Unit onFeedArgumentsLocalBooks(final CatalogFeedArgumentsLocalBooks c) {
          catalogActivityForkNewReplacing(args);
          if (swipe_refresh_layout != null) {
            swipe_refresh_layout.setRefreshing(false);
          }
          return Unit.unit();
        }

        @Override
        public Unit onFeedArgumentsRemote(final CatalogFeedArgumentsRemote c) {
          loader.invalidate(c.getURI());
          catalogActivityForkNewReplacing(args);
          if (swipe_refresh_layout != null) {
            swipe_refresh_layout.setRefreshing(false);
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

  private void doLoadLocalFeed(final CatalogFeedArgumentsLocalBooks c) {
    final Resources resources = getResources();

    final URI books_uri = URI.create("Books");

    final ProfileFeedRequest request =
      ProfileFeedRequest.builder(
        books_uri,
        resources.getString(R.string.books),
        resources.getString(R.string.books_sort_by),
        new CatalogFacetPseudoTitleProvider(resources))
        .setFeedSelection(c.getSelection())
        .setSearch(c.getSearchTerms())
        .setFacetActive(c.getFacetType())
        .build();

    try {
      final ListeningExecutorService exec = Simplified.getBackgroundTaskExecutor();
      FluentFuture
        .from(Simplified.getProfilesController().profileFeed(request))
        .transform(feed -> {
          this.onFeedLoadSuccess(books_uri, feed);
          return Unit.unit();
        }, exec)
        .catching(Exception.class, ex -> {
          this.onFeedLoadFailure(books_uri, ex);
          return Unit.unit();
        }, exec);
    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Unconditionally load a remote feed.
   *
   * @param c The feed arguments
   */

  private void doLoadRemoteFeed(final CatalogFeedArgumentsRemote c) {
    this.loadFeed(Simplified.getFeedLoader(), c.getURI());
  }

  /**
   * A handler for local book searches.
   */

  private final class BooksLocalSearchQueryHandler
    implements OnQueryTextListener {
    private final CatalogFeedArgumentsType args;
    private final FacetType facet_active;
    private final Resources resources;

    BooksLocalSearchQueryHandler(
      final Resources in_resources,
      final CatalogFeedArgumentsType in_args,
      final FacetType in_facet_active) {
      this.resources = NullCheck.notNull(in_resources);
      this.args = NullCheck.notNull(in_args);
      this.facet_active = NullCheck.notNull(in_facet_active);
    }

    @Override
    public boolean onQueryTextChange(
      final @Nullable String s) {
      return true;
    }

    @Override
    public boolean onQueryTextSubmit(
      final @Nullable String query) {
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
      } else {
        cfa.catalogActivityForkNew(new_args);
      }


      return true;
    }
  }

  /**
   * A handler for OpenSearch 1.1 searches.
   */

  private final class OpenSearchQueryHandler implements OnQueryTextListener {
    private final CatalogFeedArgumentsType args;
    private final OPDSOpenSearch1_1 search;
    private final Resources resources;

    OpenSearchQueryHandler(
      final Resources in_resources,
      final CatalogFeedArgumentsType in_args,
      final OPDSOpenSearch1_1 in_search) {
      this.resources = NullCheck.notNull(in_resources);
      this.args = NullCheck.notNull(in_args);
      this.search = NullCheck.notNull(in_search);
    }

    @Override
    public boolean onQueryTextChange(
      final @Nullable String s) {
      return true;
    }

    @Override
    public boolean onQueryTextSubmit(
      final @Nullable String query) {
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
      } else {
        cfa.catalogActivityForkNew(new_args);
      }
      return true;
    }
  }

  /**
   * A launcher that can create a new catalog activity for the given facet.
   */

  private static final class CatalogFeedFacetLauncher
    implements FeedFacetMatcherType<Unit, UnreachableCodeException> {

    private final FeedType feed;
    private final Resources resources;
    private final OptionType<String> search_terms;
    private final CatalogFeedActivity parent;

    CatalogFeedFacetLauncher(
      CatalogFeedActivity parent,
      FeedType feed,
      Resources resources,
      OptionType<String> search_terms) {
      this.parent = parent;
      this.feed = feed;
      this.resources = resources;
      this.search_terms = search_terms;
    }

    @Override
    public Unit onFeedFacetOPDS(final FeedFacetOPDS feed_opds) {

      final OPDSFacet o = feed_opds.getOPDSFacet();
      final CatalogFeedArgumentsRemote args =
        new CatalogFeedArgumentsRemote(
          false,
          this.parent.getUpStack(),
          this.feed.getFeedTitle(),
          o.getUri(),
          false);

      this.parent.catalogActivityForkNewReplacing(args);
      return Unit.unit();
    }

    @Override
    public Unit onFeedFacetPseudo(final FeedFacetPseudo fp) {

      final String facet_title =
        NullCheck.notNull(this.resources.getString(R.string.books_sort_by));

      final CatalogFeedArgumentsLocalBooks args =
        new CatalogFeedArgumentsLocalBooks(
          this.parent.getUpStack(),
          facet_title,
          fp.getType(),
          this.search_terms,
          this.parent.getLocalFeedTypeSelection());

      this.parent.catalogActivityForkNewReplacing(args);
      return Unit.unit();
    }
  }
}
