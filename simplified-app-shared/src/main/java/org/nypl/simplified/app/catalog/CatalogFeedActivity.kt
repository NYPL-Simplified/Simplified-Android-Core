package org.nypl.simplified.app.catalog

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AbsListView
import android.widget.Button
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.TextView
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.FutureCallback
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.ProcedureType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.jnull.Nullable
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.app.ApplicationColorScheme
import org.nypl.simplified.app.NavigationDrawerActivity
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.login.LoginActivity
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.controller.ProfileFeedRequest
import org.nypl.simplified.books.eula.EULAType
import org.nypl.simplified.books.feeds.Feed
import org.nypl.simplified.books.feeds.Feed.FeedWithGroups
import org.nypl.simplified.books.feeds.Feed.FeedWithoutGroups
import org.nypl.simplified.books.feeds.FeedBooksSelection
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.books.feeds.FeedFacetMatcherType
import org.nypl.simplified.books.feeds.FeedFacetOPDS
import org.nypl.simplified.books.feeds.FeedFacetPseudo
import org.nypl.simplified.books.feeds.FeedFacetPseudo.FacetType
import org.nypl.simplified.books.feeds.FeedFacetPseudo.FacetType.SORT_BY_TITLE
import org.nypl.simplified.books.feeds.FeedFacetType
import org.nypl.simplified.books.feeds.FeedFacets
import org.nypl.simplified.books.feeds.FeedGroup
import org.nypl.simplified.books.feeds.FeedLoaderResult
import org.nypl.simplified.books.feeds.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.books.feeds.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral
import org.nypl.simplified.books.feeds.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.books.feeds.FeedLoaderType
import org.nypl.simplified.books.feeds.FeedSearchLocal
import org.nypl.simplified.books.feeds.FeedSearchMatcherType
import org.nypl.simplified.books.feeds.FeedSearchOpen1_1
import org.nypl.simplified.books.profiles.ProfileAccountSelectEvent
import org.nypl.simplified.books.profiles.ProfileEvent
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1
import org.nypl.simplified.stack.ImmutableStack
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URI
import java.util.ArrayList
import java.util.Objects

/**
 * The main catalog feed activity, responsible for displaying different types of
 * feeds.
 */

/**
 * Construct an activity.
 */

abstract class CatalogFeedActivity : CatalogActivity() {

  private var feed: Feed? = null
  private var listView: AbsListView? = null
  private lateinit var swipeRefreshLayout: SwipeRefreshLayout
  private var loading: FluentFuture<FeedLoaderResult>? = null
  private lateinit var progressLayout: ViewGroup
  private var savedScrollPosition: Int = 0
  private var previouslyPaused: Boolean = false
  private var searchView: SearchView? = null
  private var profileEventSubscription: ObservableSubscriptionType<ProfileEvent>? = null
  private var bookEventSubscription: ObservableSubscriptionType<BookStatusEvent>? = null

  /**
   * @return The specific logger instance provided by subclasses
   */

  protected abstract fun log(): Logger

  override fun onBackPressed() {
    this.invalidateOptionsMenu()
    super.onBackPressed()
  }

  override fun onResume() {
    super.onResume()

    /*
     * If the activity was previously paused, this means that the user
     * navigated away from the activity and is now coming back to it. If the
     * user went into a book detail view and revoked a book, then the feed
     * should be completely reloaded when the user comes back, to ensure that
     * the book no longer shows up in the list.
     *
     * This obviously only applies to local feeds.
     */

    if (this.searchView != null) {
      this.searchView!!.setQuery("", false)
      this.searchView!!.clearFocus()
    }

    var didRetry = false
    val extras = this.intent.extras
    if (extras != null) {
      val reload = extras.getBoolean("reload")
      if (reload) {
        didRetry = true
        this@CatalogFeedActivity.retryFeed()
        extras.putBoolean("reload", false)
      }
    }

    if (this.previouslyPaused && !didRetry) {
      val args = this.retrieveArguments()
      if (!args.requiresNetworkConnectivity()) {
        this.retryFeed()
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

  private fun configureFacetEntryPointButtons(
    feed: Feed?,
    layout: ViewGroup,
    resources: Resources,
    colorScheme: ApplicationColorScheme) {

    UIThread.checkIsUIThread()

    val facetsView =
      layout.findViewById<RadioGroup>(R.id.catalog_feed_facet_tabs)
    val facetGroupOpt =
      FeedFacets.findEntryPointFacetGroupForFeed(feed!!)

    /*
     * If there isn't a group of facets representing an entry point, hide the space in which
     * they would have appeared.
     */

    if (facetGroupOpt.isNone) {
      facetsView.visibility = View.GONE
      return
    }

    /*
     * Add a set of radio buttons to the view.
     */

    val facetGroup = (facetGroupOpt as Some<List<FeedFacetType>>).get()
    val textColor = createTextColorForRadioButton(resources, colorScheme)

    val size = facetGroup.size
    for (index in 0 until size) {
      val facet = facetGroup[index]
      val button = RadioButton(this)
      val buttonLayout =
        LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT, 1.0f / size.toFloat())

      button.layoutParams = buttonLayout
      button.setTextColor(textColor)
      button.gravity = Gravity.CENTER

      /*
       * The buttons need unique IDs so that they can be addressed within the parent
       * radio group.
       */

      button.id = View.generateViewId()

      if (index == 0) {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_left)
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_left)
      } else if (index == size - 1) {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_right)
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_right)
      } else {
        button.setBackgroundResource(R.drawable.catalog_facet_tab_button_background_middle)
        button.setButtonDrawable(R.drawable.catalog_facet_tab_button_background_middle)
      }

      button.text = facet.facetGetTitle()

      val launcher =
        CatalogFeedFacetLauncher(this, feed, resources, this.retrieveLocalSearchTerms())

      button.setOnClickListener { ignored ->
        LOG.debug("selected entry point facet: {}", facet.facetGetTitle())
        facet.matchFeedFacet(launcher)
      }
      facetsView.addView(button)
    }

    /*
     * Uncheck all of the buttons, and then check the one that corresponds to the current
     * active facet.
     */

    facetsView.clearCheck()

    for (index in 0 until size) {
      val facet = facetGroup[index]
      val button = facetsView.getChildAt(index) as RadioButton

      if (facet.facetIsActive()) {
        LOG.debug("active entry point facet: {}", facet.facetGetTitle())
        facetsView.check(button.id)
      }
    }
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

  private fun configureFacets(
    screen: ScreenSizeInformationType,
    feed: FeedWithoutGroups,
    layout: ViewGroup,
    resources: Resources) {

    UIThread.checkIsUIThread()

    val facetsView =
      layout.findViewById<ViewGroup>(R.id.catalog_feed_nogroups_facets)
    val facetDivider =
      layout.findViewById<View>(R.id.catalog_feed_nogroups_facet_divider)

    /*
     * If the facet groups are empty, or the only available facets are "entry point" facets,
     * then no facet bar should be displayed.
     */

    if (feed.facetsByGroup.isEmpty() || FeedFacets.facetGroupsAreAllEntryPoints(feed.facetsByGroup)) {
      facetsView.visibility = View.GONE
      facetDivider.visibility = View.GONE
      return
    }

    /*
     * Otherwise, for each facet group, show a drop-down menu allowing
     * the selection of individual facets.
     */

    for (groupName in feed.facetsByGroup.keys) {
      val group = feed.facetsByGroup.get(groupName)!!
      val groupCopy = ArrayList(group)

      val tvp = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
      tvp.rightMargin = screen.screenDPToPixels(8).toInt()

      val tv = TextView(this)
      tv.setTextColor(resources.getColor(R.color.normal_text_major))
      tv.textSize = 12.0f
      tv.setText(groupName + ":")
      tv.layoutParams = tvp
      facetsView.addView(tv)

      val searchTerms =
        this.retrieveLocalSearchTerms()
      val facetFeedListener =
        CatalogFeedFacetLauncher(this, feed, resources, searchTerms)

      val facetListener =
        CatalogFacetSelectionListenerType { selected -> selected.matchFeedFacet(facetFeedListener) }
      val facetButton =
        CatalogFacetButton(this, groupName, groupCopy, facetListener)

      facetButton.layoutParams = tvp
      facetsView.addView(facetButton)
    }
  }

  private fun retrieveLocalSearchTerms(): OptionType<String> {
    val searchTerms: OptionType<String>
    val currentArgs = this.retrieveArguments()
    if (currentArgs is CatalogFeedArgumentsLocalBooks) {
      searchTerms = currentArgs.searchTerms
    } else {
      searchTerms = Option.none()
    }
    return searchTerms
  }

  /**
   * If this activity is being used in a part of the application that generates
   * local feeds, then return the type of feed that should be generated.
   *
   * @return The type of feed that should be generated.
   */

  protected abstract fun localFeedTypeSelection(): FeedBooksSelection

  private fun configureUpButton(
    upStack: ImmutableStack<CatalogFeedArgumentsType>,
    title: String) {
    val bar = this.supportActionBar
    if (!upStack.isEmpty) {
      bar!!.title = title
    }
  }

  /**
   * FIXME: When real navigation support comes into the app to support age-gated
   * collections, like the SimplyE Collection, remove this hack.
   */

  private fun retrieveArguments(): CatalogFeedArgumentsType {
//    val libTitle = this.resources.getString(R.string.feature_app_name)
//    val libraryID = Simplified.getCurrentAccount().getId()
//    val upStack = this.upStack
//
//    if (upStack.isEmpty && libraryID == 2 && this.javaClass == MainCatalogActivity::class.java) {
//      if (Simplified.getSharedPrefs().contains("age13") === false) {
//        //Show Age Verification and load <13 to be safe
//        this.showAgeCheckAlert()
//      }
//      val over13 = Simplified.getSharedPrefs().getBoolean("age13")
//      val ageURI: URI
//      try {
//        if (over13) {
//          ageURI = URI(Simplified.getCurrentAccount().getCatalogUrl13AndOver())
//        } else {
//          ageURI = URI(Simplified.getCurrentAccount().getCatalogUrlUnder13())
//        }
//        LOG.debug("Hardcoding SimplyE Collection URI: {}", ageURI)
//        return CatalogFeedArgumentsRemote(
//          false,
//          ImmutableStack.empty(),
//          libTitle,
//          ageURI,
//          false
//        )
//      } catch (e: Exception) {
//        LOG.error(
//          "error constructing SimplyE collection uri: {}", e.message, e)
//      }
//    }

    /*
     * Attempt to fetch arguments.
     */

    val a = this.intent.extras
    if (a != null) {
      val args =
        a.getSerializable(CATALOG_ARGS) as CatalogFeedArgumentsType?
      if (args != null) {
        return args
      }
    }

    /*
     * If there were no arguments (because, for example, this activity is the
     * initial one started for the app), synthesize some.
     */

    val inDrawerOpen = true
    val empty = ImmutableStack.empty<CatalogFeedArgumentsType>()
    val feedTitle = this.resources.getString(R.string.feature_app_name)
    val account =
      Simplified.getProfilesController()
        .profileAccountCurrent()

    val feedURI = account.provider().catalogURIForAge(100)
    return CatalogFeedArgumentsRemote(inDrawerOpen, empty, feedTitle, feedURI, false)
  }

  private fun loadFeed(
    feedLoader: FeedLoaderType,
    feedURI: URI) {
    LOG.debug("loading feed: {}", feedURI)

    // XXX: Why no credentials here? Use the correct credentials!
    val none =
      Option.none<HTTPAuthType>()

    val executor =
      Simplified.getBackgroundTaskExecutor()

    val future =
      feedLoader.fetchURIWithBookRegistryEntries(feedURI, none)

    future.addCallback(object : FutureCallback<FeedLoaderResult> {
      override fun onSuccess(result: FeedLoaderResult?) {
        onFeedResult(feedURI, result!!)
      }

      override fun onFailure(ex: Throwable) {
        LOG.error("error in feed result handler: ", ex)
        onFeedResultFailedException(feedURI, ex)
      }
    }, executor)

    this.loading = future
  }

  private fun onFeedResultFailedException(feedURI: URI, ex: Throwable) {
    UIThread.runOnUIThread { this.onFeedResultFailedExceptionUI(feedURI, ex) }
  }

  private fun onFeedResultFailedExceptionUI(feedURI: URI, e: Throwable) {
    UIThread.checkIsUIThread()

    LOG.info("Failed to get feed: ", e)
    this.invalidateOptionsMenu()

    this.progressLayout.visibility = View.GONE
    this.contentFrame.removeAllViews()

    val error =
      this.layoutInflater.inflate(
        R.layout.catalog_loading_error,
        this.contentFrame,
        false) as ViewGroup
    this.contentFrame.addView(error)
    this.contentFrame.requestLayout()

    val retry = error.findViewById<Button>(R.id.catalog_error_retry)
    retry.setOnClickListener { v -> this.retryFeed() }
  }

  private fun onFeedResult(feedURI: URI, result: FeedLoaderResult) {
    return when (result) {
      is FeedLoaderSuccess -> {
        this.onFeedResultSuccess(feedURI, result.feed)
      }
      is FeedLoaderFailedGeneral ->
        this.onFeedResultFailedException(feedURI, result.exception)
      is FeedLoaderFailedAuthentication ->
        this.onFeedResultFailedAuthentication(feedURI, result.exception)
    }
  }

  private fun onFeedResultFailedAuthentication(
    feedURI: URI,
    exception: java.lang.Exception) {

    /*
     * Redirect the user to a login screen.
     */

    UIThread.runOnUIThread {
      val i = Intent(this@CatalogFeedActivity, LoginActivity::class.java)
      this.startActivity(i)
      this.finish()
    }
  }

  private fun onFeedResultSuccess(uri: URI, feed: Feed) {
    LOG.debug("received feed for {}", uri)
    this.feed = feed

    UIThread.runOnUIThread { this.configureUpButton(this.upStack, feed.feedTitle) }
    return when (feed) {
      is FeedWithoutGroups -> {
        this.onFeedResultSuccessWithoutGroups(feed)
      }
      is FeedWithGroups -> {
        this.onFeedResultSuccessWithGroups(feed)
      }
    }
  }

  private fun onFeedResultSuccessWithGroups(feed: FeedWithGroups) {
    LOG.debug("received feed with groups: {}", feed.feedURI)

    UIThread.runOnUIThread { this.onFeedResultSuccessWithGroupsUI(feed) }
    onPossiblyReceivedEULALink(feed.feedTermsOfService)
  }

  private fun onFeedResultSuccessWithGroupsUI(feed: FeedWithGroups) {
    LOG.debug("received feed with groups: {}", feed.feedURI)

    UIThread.checkIsUIThread()

    this.invalidateOptionsMenu()

    this.progressLayout.visibility = View.GONE
    this.contentFrame.removeAllViews()

    val layout =
      this.layoutInflater.inflate(
        R.layout.catalog_feed_groups_list,
        this.contentFrame,
        false) as ViewGroup

    this.contentFrame.addView(layout)
    this.contentFrame.requestLayout()

    LOG.debug("restoring scroll position: {}", this.savedScrollPosition)

    val list =
      layout.findViewById<ListView>(R.id.catalog_feed_groups_list)

    this.swipeRefreshLayout = layout.findViewById(R.id.swipe_refresh_layout)
    this.swipeRefreshLayout.setOnRefreshListener({ this.retryFeed() })

    list.post { list.setSelection(this.savedScrollPosition) }
    list.dividerHeight = 0
    this.listView = list

    this.configureFacetEntryPointButtons(
      this.feed,
      layout,
      this.resources,
      Simplified.getMainColorScheme())

    val args = this.retrieveArguments()
    val newUpStack = this.newUpStack(args)

    val laneListener = object : CatalogFeedLaneListenerType {
      override fun onSelectFeed(inGroup: FeedGroup) {
        this@CatalogFeedActivity.onSelectedFeedGroup(newUpStack, inGroup)
      }

      override fun onSelectBook(e: FeedEntryOPDS) {
        this@CatalogFeedActivity.onSelectedBook(newUpStack, e)
      }
    }

    val cfl: CatalogFeedWithGroups
    try {
      cfl = CatalogFeedWithGroups(
        this,
        Simplified.getProfilesController().profileAccountCurrent(),
        Simplified.getScreenSizeInformation(),
        Simplified.getCoverProvider(),
        laneListener,
        feed,
        Simplified.getMainColorScheme())
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    }

    list.adapter = cfl
    list.setOnScrollListener(cfl)
  }

  private fun onFeedResultSuccessWithoutGroups(feed: FeedWithoutGroups) {
    LOG.debug("received feed without blocks: {}", feed.feedURI)

    UIThread.runOnUIThread { this.onFeedResultSuccessWithoutGroupsUI(feed) }
    onPossiblyReceivedEULALink(feed.feedTermsOfService)
  }

  private fun onFeedResultSuccessWithoutGroupsUI(feed: FeedWithoutGroups) {
    UIThread.checkIsUIThread()

    if (feed.entriesInOrder.isEmpty()) {
      this.onFeedResultSuccessWithoutGroupsEmptyUI(feed)
      return
    }

    this.onFeedResultSuccessWithoutGroupsNonEmptyUI(feed)
  }

  private fun onFeedResultSuccessWithoutGroupsEmptyUI(feed: FeedWithoutGroups) {
    LOG.debug("received feed without blocks (empty): {}", feed.feedURI)

    UIThread.checkIsUIThread()
    Preconditions.checkArgument(feed.entriesInOrder.isEmpty(), "Feed is empty")

    this.invalidateOptionsMenu()

    this.progressLayout.visibility = View.GONE
    this.contentFrame.removeAllViews()

    val layout =
      this.layoutInflater.inflate(
        R.layout.catalog_feed_nogroups_empty,
        this.contentFrame,
        false) as ViewGroup
    val emptyText =
      layout.findViewById<TextView>(R.id.catalog_feed_nogroups_empty_text)

    if (this.retrieveArguments().isSearching) {
      val resources = this.resources
      emptyText.text = resources.getText(R.string.catalog_empty_feed)
    } else {
      emptyText.text = this.catalogFeedGetEmptyText()
    }

    this.contentFrame.addView(layout)
    this.contentFrame.requestLayout()
  }

  private fun onFeedResultSuccessWithoutGroupsNonEmptyUI(feedWithoutGroups: FeedWithoutGroups) {
    LOG.debug("received feed without blocks (non-empty): {}", feedWithoutGroups.feedURI)

    UIThread.checkIsUIThread()
    Preconditions.checkArgument(
      !feedWithoutGroups.entriesInOrder.isEmpty(), "Feed is non-empty")

    this.invalidateOptionsMenu()

    this.progressLayout.visibility = View.GONE
    this.contentFrame.removeAllViews()

    val layout =
      this.layoutInflater.inflate(
        R.layout.catalog_feed_nogroups,
        this.contentFrame,
        false) as ViewGroup

    this.contentFrame.addView(layout)
    this.contentFrame.requestLayout()

    LOG.debug("restoring scroll position: {}", this.savedScrollPosition)

    val gridView = layout.findViewById<GridView>(R.id.catalog_feed_nogroups_grid)
    this.swipeRefreshLayout = layout.findViewById(R.id.swipe_refresh_layout)
    this.swipeRefreshLayout.setOnRefreshListener({ this.retryFeed() })

    this.configureFacetEntryPointButtons(
      feedWithoutGroups, layout, this.resources, Simplified.getMainColorScheme())
    this.configureFacets(
      Simplified.getScreenSizeInformation(), feedWithoutGroups, layout, this.resources)

    gridView.post { gridView.setSelection(this.savedScrollPosition) }
    this.listView = gridView

    val args = this.retrieveArguments()
    val newUpStack = this.newUpStack(args)

    val bookSelectListener =
      CatalogBookSelectionListenerType { v, e -> this.onSelectedBook(newUpStack, e) }

    val without: CatalogFeedWithoutGroups
    try {
      without = CatalogFeedWithoutGroups(
        this,
        Simplified.getProfilesController().profileAccountCurrent(),
        Simplified.getCoverProvider(),
        bookSelectListener,
        Simplified.getBooksRegistry(),
        Simplified.getBooksController(),
        Simplified.getProfilesController(),
        Simplified.getFeedLoader(),
        feedWithoutGroups,
        Simplified.getDocumentStore(),
        Simplified.getNetworkConnectivity(),
        Simplified.getBackgroundTaskExecutor(),
        Simplified.getMainColorScheme())
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    }

    gridView.adapter = without
    gridView.setOnScrollListener(without)

    /*
     * Subscribe the grid view to book events. This will allow individual cells to be
     * updated whenever the status of a book changes.
     */

    this.bookEventSubscription = Simplified.getBooksRegistry()
      .bookEvents()
      .subscribe({ event -> without.onBookEvent(event) })
  }

  override fun navigationDrawerShouldShowIndicator(): Boolean {
    return this.upStack.isEmpty
  }

  private fun newUpStack(
    args: CatalogFeedArgumentsType): ImmutableStack<CatalogFeedArgumentsType> {
    val upStack = this.upStack
    return upStack.push(args)
  }

  private fun onProfileEvent(event: ProfileEvent) {

    /*
     * If the current profile changed accounts, start a new catalog feed activity. The
     * new activity will automatically navigate to the root of the new account's catalog.
     */

    if (event is ProfileAccountSelectEvent.ProfileAccountSelectSucceeded) {
      UIThread.runOnUIThread {
        val i = Intent(this@CatalogFeedActivity, MainCatalogActivity::class.java)
        val b = Bundle()
        NavigationDrawerActivity.setActivityArguments(b, false)
        i.putExtras(b)
        this.startActivity(i)
        this.finish()
      }
    }
  }

  override fun onCreate(@Nullable state: Bundle?) {
    super.onCreate(state)

    val args = this.retrieveArguments()
    val stack = this.upStack
    this.configureUpButton(stack, args.title)

    this.title =
      if (args.title == this.resources.getString(R.string.feature_app_name))
        this.resources.getString(R.string.catalog)
      else
        args.title

    /*
     * Attempt to restore the saved scroll position, if there is one.
     */

    if (state != null) {
      LOG.debug("received state")
      this.savedScrollPosition = state.getInt(LIST_STATE_ID)
    } else {
      this.savedScrollPosition = 0
    }

    /*
     * Display a progress bar until the feed is either loaded or fails.
     */

    val newProgressLayout =
      this.layoutInflater.inflate(R.layout.catalog_loading, this.contentFrame, false) as ViewGroup

    this.contentFrame.addView(newProgressLayout)
    this.contentFrame.requestLayout()
    this.progressLayout = newProgressLayout

    /*
     * If the feed requires network connectivity, and the network is not
     * available, then fail fast and display an error message.
     */

    val net = Simplified.getNetworkConnectivity()
    if (args.requiresNetworkConnectivity()) {
      if (!net.isNetworkAvailable) {
        this.onNetworkUnavailable()
        return
      }
    }

    /*
     * Create a dispatching function that will load a feed based on the given
     * arguments, and execute it.
     */

    args.matchArguments(
      object : CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException> {
        override fun onFeedArgumentsLocalBooks(c: CatalogFeedArgumentsLocalBooks): Unit {
          this@CatalogFeedActivity.doLoadLocalFeed(c)
          return Unit.unit()
        }

        override fun onFeedArgumentsRemote(c: CatalogFeedArgumentsRemote): Unit {
          this@CatalogFeedActivity.doLoadRemoteFeed(c)
          return Unit.unit()
        }
      })

    /*
     * Subscribe to profile change events.
     */

    this.profileEventSubscription = Simplified.getProfilesController()
      .profileEvents()
      .subscribe(ProcedureType<ProfileEvent> { this.onProfileEvent(it) })
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    LOG.debug("inflating menu")
    this.menuInflater.inflate(R.menu.catalog, menu)

    if (this.feed == null) {
      LOG.debug("menu creation requested but feed is not yet present")
      return true
    }

    LOG.debug("menu creation requested and feed is present")
    this.onCreateOptionsMenuSearchItem(menu)
    return true
  }

  /**
   * If the feed actually has a search URI, then show the search field.
   * Otherwise, disable and hide it.
   */

  private fun onCreateOptionsMenuSearchItem(menuNn: Menu) {
    val searchItem = menuNn.findItem(R.id.catalog_action_search)
    val feedActual = this.feed!!
    val search = feedActual.feedSearch
    val args = this.retrieveArguments()
    var searchOk = false

    // XXX: Update to support library search item
    if (search != null && false) {
      this.searchView = searchItem.actionView as SearchView

      // Check that the search URI is of an understood type.
      searchOk = search.matchSearch(
        object : FeedSearchMatcherType<Boolean, UnreachableCodeException> {
          override fun onFeedSearchOpen1_1(
            fs: FeedSearchOpen1_1): Boolean? {
            this@CatalogFeedActivity.searchView!!.setOnQueryTextListener(
              this@CatalogFeedActivity.OpenSearchQueryHandler(
                this@CatalogFeedActivity.resources, args, fs.search))
            return java.lang.Boolean.TRUE
          }

          override fun onFeedSearchLocal(
            f: FeedSearchLocal): Boolean? {
            this@CatalogFeedActivity.searchView!!.setOnQueryTextListener(
              this@CatalogFeedActivity.BooksLocalSearchQueryHandler(
                this@CatalogFeedActivity.resources, args, SORT_BY_TITLE))
            return java.lang.Boolean.TRUE
          }
        })
    } else {
      LOG.debug("Feed has no search opts.")
    }

    if (searchOk) {
      this.searchView!!.isSubmitButtonEnabled = true
      this.searchView!!.setIconifiedByDefault(false)
      searchItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
      searchItem.expandActionView()

      // display either the category title or the previously searched keywords
      this.searchView!!.queryHint =
        String.format(this.getString(R.string.search_hint_format),
          Objects.toString(this.feed!!.feedTitle,
            this.getString(R.string.search_hint_feed_title_default)))
      if (args.title.startsWith(this.getString(R.string.search_hint_prefix))) {
        this.searchView!!.queryHint = args.title
      }

      searchItem.isEnabled = true
      searchItem.isVisible = true
    } else {
      searchItem.isEnabled = false
      searchItem.collapseActionView()
      searchItem.isVisible = false
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    LOG.debug("onDestroy")

    val future = this.loading
    future?.cancel(true)

    val profileSub = this.profileEventSubscription
    profileSub?.unsubscribe()

    val bookSub = this.bookEventSubscription
    bookSub?.unsubscribe()
  }

  fun showAgeCheckAlert() {
//    val builder = AlertDialog.Builder(this@CatalogFeedActivity)
//
//    builder.setTitle(R.string.age_verification_title)
//    builder.setMessage(R.string.age_verification_question)
//
//    // Under 13
//    builder.setNeutralButton(R.string.age_verification_13_younger) { dialog, which ->
//      Simplified.getSharedPrefs().putBoolean("age13", false)
//      this@CatalogFeedActivity.reloadCatalogActivity(true)
//    }
//
//    // 13 or Over
//    builder.setPositiveButton(R.string.age_verification_13_older) { dialog, which ->
//      Simplified.getSharedPrefs().putBoolean("age13", true)
//      this@CatalogFeedActivity.reloadCatalogActivity(false)
//    }
//
//    if (!this.isFinishing) {
//      val alert = builder.show()
//      val resID = ThemeMatcher.color(Simplified.getCurrentAccount().getMainColor())
//      val mainTextColor = ContextCompat.getColor(this, resID)
//      alert.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(mainTextColor)
//      alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mainTextColor)
//    }
  }

  private fun reloadCatalogActivity(deleteBooks: Boolean) {
//    Simplified.getCatalogAppServices().reloadCatalog(deleteBooks, Simplified.getCurrentAccount())
//    val i = Intent(this@CatalogFeedActivity, MainCatalogActivity::class.java)
//    i.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
//    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//    val b = Bundle()
//    SimplifiedActivity.setActivityArguments(b, false)
//    i.putExtras(b)
//    this.startActivity(i)
//    this.overridePendingTransition(0, 0)
  }

  /**
   * The network is unavailable. Simply display a message and a button to allow
   * the user to retry loading when they have fixed their connection.
   */

  private fun onNetworkUnavailable() {
    UIThread.checkIsUIThread()

    LOG.debug("network is unavailable")

    this.progressLayout.visibility = View.GONE
    this.contentFrame.removeAllViews()

    val error =
      this.layoutInflater.inflate(
        R.layout.catalog_loading_not_connected, this.contentFrame, false) as ViewGroup
    this.contentFrame.addView(error)
    this.contentFrame.requestLayout()

    val retry = error.findViewById<Button>(R.id.catalog_error_retry)
    retry.setOnClickListener { v -> this.retryFeed() }
  }

  override fun onPause() {
    super.onPause()
    this.previouslyPaused = true
  }

  override fun onSaveInstanceState(state: Bundle) {
    super.onSaveInstanceState(state)

    LOG.debug("saving state")

    /*
     * Save the scroll position in the hope that it can be restored later.
     */

    val lv = this.listView
    if (lv != null) {
      val position = lv.firstVisiblePosition
      LOG.debug("saving list view position: {}", Integer.valueOf(position))
      state.putInt(LIST_STATE_ID, position)
    }
  }

  private fun onSelectedBook(
    newUpStack: ImmutableStack<CatalogFeedArgumentsType>,
    e: FeedEntryOPDS) {
    LOG.debug("onSelectedBook: {}", this)
    CatalogBookDetailActivity.startNewActivity(this, newUpStack, e)
  }

  private fun onSelectedFeedGroup(
    newUpStack: ImmutableStack<CatalogFeedArgumentsType>,
    f: FeedGroup) {
    LOG.debug("onSelectFeed: {}", this)

    val remote = CatalogFeedArgumentsRemote(
      false, newUpStack, f.groupTitle, f.groupURI, false)
    this.catalogActivityForkNew(remote)
  }

  /**
   * Retry the current feed.
   */

  protected fun retryFeed() {
    val args = this.retrieveArguments()
    LOG.debug("retrying feed {}", args)

    val loader = Simplified.getFeedLoader()

    args.matchArguments(
      object : CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException> {
        override fun onFeedArgumentsLocalBooks(c: CatalogFeedArgumentsLocalBooks): Unit {
          this@CatalogFeedActivity.catalogActivityForkNewReplacing(args)
          if (this@CatalogFeedActivity.swipeRefreshLayout != null) {
            this@CatalogFeedActivity.swipeRefreshLayout!!.isRefreshing = false
          }
          return Unit.unit()
        }

        override fun onFeedArgumentsRemote(c: CatalogFeedArgumentsRemote): Unit {
          loader.invalidate(c.uri)
          this@CatalogFeedActivity.catalogActivityForkNewReplacing(args)
          if (this@CatalogFeedActivity.swipeRefreshLayout != null) {
            this@CatalogFeedActivity.swipeRefreshLayout!!.isRefreshing = false
          }
          return Unit.unit()
        }
      })
  }

  /**
   * @return The text to display when a feed is empty.
   */

  protected abstract fun catalogFeedGetEmptyText(): String

  /**
   * Unconditionally load a locally-generated feed.
   *
   * @param c The feed arguments
   */

  private fun doLoadLocalFeed(c: CatalogFeedArgumentsLocalBooks) {
    val resources = this.resources

    val booksUri = URI.create("Books")

    val request =
      ProfileFeedRequest.builder(
      booksUri,
      resources.getString(R.string.books),
      resources.getString(R.string.books_sort_by),
      CatalogFacetPseudoTitleProvider(resources))
      .setFeedSelection(c.selection)
      .setSearch(c.searchTerms)
      .setFacetActive(c.facetType)
      .build()

    try {
      val exec =
        Simplified.getBackgroundTaskExecutor()
      val future =
        Simplified.getProfilesController().profileFeed(request)

      future.addCallback(object: FutureCallback<FeedWithoutGroups> {
        override fun onSuccess(result: FeedWithoutGroups?) {
          onFeedResult(booksUri, FeedLoaderSuccess(result!!))
        }
        override fun onFailure(ex: Throwable) {
          LOG.error("error in feed result handler: ", ex)
          onFeedResultFailedException(booksUri, ex)
        }
      }, exec)
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    }

  }

  /**
   * Unconditionally load a remote feed.
   *
   * @param c The feed arguments
   */

  private fun doLoadRemoteFeed(c: CatalogFeedArgumentsRemote) {
    this.loadFeed(Simplified.getFeedLoader(), c.uri)
  }

  /**
   * A handler for local book searches.
   */

  private inner class BooksLocalSearchQueryHandler internal constructor(
    private val resources: Resources,
    private val args: CatalogFeedArgumentsType,
    private val facetActive: FacetType) : OnQueryTextListener {

    override fun onQueryTextChange(
      @Nullable s: String): Boolean {
      return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
      val cfa = this@CatalogFeedActivity
      val us = ImmutableStack.empty<CatalogFeedArgumentsType>()
      val title = this.resources.getString(R.string.catalog_search) + ": " + query

      val newArgs = CatalogFeedArgumentsLocalBooks(
        us,
        title,
        this.facetActive,
        Option.some(query),
        cfa.localFeedTypeSelection())
      if ("Search" == this@CatalogFeedActivity.feed!!.feedTitle) {
        cfa.catalogActivityForkNewReplacing(newArgs)
      } else {
        cfa.catalogActivityForkNew(newArgs)
      }

      return true
    }
  }

  /**
   * A handler for OpenSearch 1.1 searches.
   */

  private inner class OpenSearchQueryHandler internal constructor(
    private val resources: Resources,
    private val args: CatalogFeedArgumentsType,
    private val search: OPDSOpenSearch1_1) : OnQueryTextListener {

    override fun onQueryTextChange(text: String): Boolean {
      return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
      val target = this.search.getQueryURIForTerms(query)

      val cfa = this@CatalogFeedActivity
      val us = cfa.newUpStack(this.args)

      val title = this.resources.getString(R.string.catalog_search) + ": " + query

      val newArgs = CatalogFeedArgumentsRemote(false, us, title, target, true)

      if ("Search" == this@CatalogFeedActivity.feed!!.feedTitle) {
        cfa.catalogActivityForkNewReplacing(newArgs)
      } else {
        cfa.catalogActivityForkNew(newArgs)
      }
      return true
    }
  }

  /**
   * A launcher that can create a new catalog activity for the given facet.
   */

  private class CatalogFeedFacetLauncher internal constructor(
    private val parent: CatalogFeedActivity,
    private val feed: Feed,
    private val resources: Resources,
    private val searchTerms: OptionType<String>) : FeedFacetMatcherType<Unit, UnreachableCodeException> {

    override fun onFeedFacetOPDS(feedOpds: FeedFacetOPDS): Unit {

      val (_, uri) = feedOpds.opdsFacet
      val args = CatalogFeedArgumentsRemote(
        false,
        this.parent.upStack,
        this.feed.feedTitle,
        uri,
        false)

      this.parent.catalogActivityForkNewReplacing(args)
      return Unit.unit()
    }

    override fun onFeedFacetPseudo(fp: FeedFacetPseudo): Unit {
      val facetTitle = this.resources.getString(R.string.books_sort_by)

      val args = CatalogFeedArgumentsLocalBooks(
        this.parent.upStack,
        facetTitle,
        fp.type,
        this.searchTerms,
        this.parent.localFeedTypeSelection())

      this.parent.catalogActivityForkNewReplacing(args)
      return Unit.unit()
    }
  }

  companion object {

    private val CATALOG_ARGS: String
    private val LIST_STATE_ID: String
    private val LOG = LoggerFactory.getLogger(CatalogFeedActivity::class.java)

    init {
      this.CATALOG_ARGS = "org.nypl.simplified.app.CatalogFeedActivity.arguments"
      this.LIST_STATE_ID = "org.nypl.simplified.app.CatalogFeedActivity.list_view_state"
    }

    /**
     * Set the arguments of the activity to be created.
     * Modifies Bundle based on attributes and type (from local or remote)
     * before being given to Intent in the calling method.
     *
     * @param b    The argument bundle
     * @param args The feed arguments
     */

    fun setActivityArguments(
      b: Bundle,
      args: CatalogFeedArgumentsType) {

      b.putSerializable(this.CATALOG_ARGS, args)
      args.matchArguments(
        object : CatalogFeedArgumentsMatcherType<Unit, UnreachableCodeException> {
          override fun onFeedArgumentsLocalBooks(c: CatalogFeedArgumentsLocalBooks): Unit {
            NavigationDrawerActivity.setActivityArguments(b, false)
            CatalogActivity.setActivityArguments(b, c.upStack)
            return Unit.unit()
          }

          override fun onFeedArgumentsRemote(c: CatalogFeedArgumentsRemote): Unit {
            NavigationDrawerActivity.setActivityArguments(b, c.isDrawerOpen)
            CatalogActivity.setActivityArguments(b, c.upStack)
            return Unit.unit()
          }
        })
    }

    /**
     * On the (possible) receipt of a link to the feed's EULA, update the URI for
     * the document if one has actually been defined for the application.
     *
     * @param latest The (possible) link
     * @see EULAType
     */

    private fun onPossiblyReceivedEULALink(latest: URI?) {
      if (latest != null) {
        val docs = Simplified.getDocumentStore()
        docs.eula.map_ { eula ->
          try {
            eula.documentSetLatestURL(latest.toURL())
          } catch (e: MalformedURLException) {
            this.LOG.error("could not use latest EULA link: ", e)
          }
        }
      }
    }

    /**
     * Create a color state list that will return the correct text color for a radio button.
     */

    private fun createTextColorForRadioButton(
      resources: Resources,
      colorScheme: ApplicationColorScheme): ColorStateList {

      val mainColor = colorScheme.colorRGBA
      val states = arrayOf(intArrayOf(android.R.attr.state_checked), // Checked
        intArrayOf(-android.R.attr.state_checked))// Unchecked

      val colors = intArrayOf(resources.getColor(R.color.button_background), mainColor)

      return ColorStateList(states, colors)
    }
  }
}
