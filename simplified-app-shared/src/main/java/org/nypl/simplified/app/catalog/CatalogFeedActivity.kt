package org.nypl.simplified.app.catalog

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.content.ContextCompat
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
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jnull.Nullable
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.catalog.CatalogFeedArguments.CatalogFeedArgumentsLocalBooks
import org.nypl.simplified.app.catalog.CatalogFeedArguments.CatalogFeedArgumentsRemote
import org.nypl.simplified.app.login.LoginDialogListenerType
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.feeds.api.Feed
import org.nypl.simplified.feeds.api.Feed.FeedWithGroups
import org.nypl.simplified.feeds.api.Feed.FeedWithoutGroups
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacet.FeedFacetPseudo.FacetType
import org.nypl.simplified.feeds.api.FeedFacets
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.feeds.api.FeedLoaderResult
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderSuccess
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.feeds.api.FeedSearch
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1
import org.nypl.simplified.profiles.api.ProfileAccountSelectEvent
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.stack.ImmutableStack
import org.nypl.simplified.theme.ThemeControl
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

abstract class CatalogFeedActivity : CatalogActivity(), LoginDialogListenerType {

  private val logger = LoggerFactory.getLogger(CatalogFeedActivity::class.java)

  private lateinit var feedArguments: CatalogFeedArguments
  private lateinit var profile: ProfileReadableType
  private lateinit var account: AccountType
  private var initialized = false

  private var feed: Feed? = null
  private var listView: AbsListView? = null
  private lateinit var swipeRefreshLayout: SwipeRefreshLayout
  private var loading: FluentFuture<FeedLoaderResult>? = null
  private var savedScrollPosition: Int = 0
  private var previouslyPaused: Boolean = false
  private var searchView: SearchView? = null
  private var profileEventSubscription: ObservableSubscriptionType<ProfileEvent>? = null
  private var bookEventSubscription: ObservableSubscriptionType<BookStatusEvent>? = null

  override fun onLoginDialogWantsProfilesController(): ProfilesControllerType =
    Simplified.getProfilesController()

  /**
   * @return The specific logger instance provided by subclasses
   */

  protected abstract fun log(): Logger

  override fun onBackPressed() {
    this.invalidateOptionsMenu()
    super.onBackPressed()
  }

  /**
   * Configure the "entry point" facet layout. This causes the "entry point" buttons to appear
   * (or not) at the top of the screen based on the available facets.
   *
   * @param feed        The feed
   * @param layout      The view group that will contain facet elements
   * @param resources   The app resources
   */

  private fun configureFacetEntryPointButtons(
    feed: Feed?,
    layout: ViewGroup) {

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

    val facetGroup = (facetGroupOpt as Some<List<FeedFacet>>).get()

    val size = facetGroup.size
    for (index in 0 until size) {
      val facet = facetGroup[index]
      val button = RadioButton(this)
      val buttonLayout =
        LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT, 1.0f / size.toFloat())

      button.layoutParams = buttonLayout
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

      button.text = facet.title
      button.setTextColor(this.colorStateListForFacetTabs())
      button.setOnClickListener { ignored ->
        this.logger.debug("selected entry point facet: {}", facet.title)
        this.openActivityForFeedFacet(
          facet = facet,
          feed = feed,
          feedSelection = FeedBooksSelection.BOOKS_FEED_LOANED,
          upStack = this.upStack,
          searchTerms = this.retrieveLocalSearchTerms())
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

      if (facet.isActive) {
        this.logger.debug("active entry point facet: {}", facet.title)
        facetsView.check(button.id)
      }
    }
  }

  private fun colorStateListForFacetTabs(): ColorStateList {
    val states =
      arrayOf(
        intArrayOf(android.R.attr.state_checked),
        intArrayOf(-android.R.attr.state_checked))

    val colors =
      intArrayOf(
        ContextCompat.getColor(this, R.color.simplifiedColorBackground),
        ThemeControl.resolveColorAttribute(this.theme, R.attr.colorPrimary))

    return ColorStateList(states, colors)
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
    layout: ViewGroup) {

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
      tv.textSize = 12.0f
      tv.setText(groupName + ":")
      tv.layoutParams = tvp
      facetsView.addView(tv)

      val searchTerms =
        this.retrieveLocalSearchTerms()

      val facetListener =
        CatalogFacetSelectionListenerType { selected ->
          this.openActivityForFeedFacet(
            facet = selected,
            feed = feed,
            feedSelection = FeedBooksSelection.BOOKS_FEED_LOANED,
            upStack = this.upStack,
            searchTerms = searchTerms)
        }

      val facetButton =
        CatalogFacetButton(this, groupName, groupCopy, facetListener)

      facetButton.layoutParams = tvp
      facetsView.addView(facetButton)
    }
  }

  private fun retrieveLocalSearchTerms(): OptionType<String> {
    val searchTerms: OptionType<String>
    val args = this.feedArguments
    if (args is CatalogFeedArgumentsLocalBooks) {
      searchTerms = args.searchTerms
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
    upStack: ImmutableStack<CatalogFeedArguments>,
    title: String) {
    val bar = this.supportActionBar
    if (!upStack.isEmpty) {
      bar!!.title = title
    }
  }

  private fun loadFeed(
    feedLoader: FeedLoaderType,
    feedURI: URI) {
    this.logger.debug("loading feed: {}", feedURI)

    val loginState = this.account.loginState()
    val authentication =
      if (loginState.credentials != null) {
        Option.some(AccountAuthenticatedHTTP.createAuthenticatedHTTP(loginState.credentials))
      } else {
        Option.none()
      }

    val future =
      feedLoader.fetchURIWithBookRegistryEntries(feedURI, authentication)

    future.addCallback(object : FutureCallback<FeedLoaderResult> {
      override fun onSuccess(result: FeedLoaderResult?) {
        this@CatalogFeedActivity.onFeedResult(feedURI, result!!)
      }

      override fun onFailure(ex: Throwable) {
        this@CatalogFeedActivity.logger.error("error in feed result handler: ", ex)
        this@CatalogFeedActivity.onFeedResultFailedException(feedURI, ex)
      }
    }, MoreExecutors.directExecutor())

    this.loading = future
  }

  private fun onFeedResultFailedException(feedURI: URI, ex: Throwable) {
    UIThread.runOnUIThread { this.onFeedResultFailedExceptionUI(feedURI, ex) }
  }

  private fun onFeedResultFailedExceptionUI(feedURI: URI, e: Throwable) {
    UIThread.checkIsUIThread()

    this.logger.info("Failed to get feed: ", e)
    this.invalidateOptionsMenu()

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
      is FeedLoaderSuccess ->
        this.onFeedResultSuccess(feedURI, result.feed)
      is FeedLoaderFailedGeneral ->
        this.onFeedResultFailedException(feedURI, result.exception)
      is FeedLoaderFailedAuthentication ->
        this.onFeedResultFailedAuthentication(feedURI, result.exception)
    }
  }

  private fun onFeedResultFailedAuthentication(
    feedURI: URI,
    exception: java.lang.Exception) {

    // XXX: ?
  }

  private fun onFeedResultSuccess(uri: URI, feed: Feed) {
    this.logger.debug("received feed for {}", uri)
    this.feed = feed

    UIThread.runOnUIThread { this.configureUpButton(this.upStack, feed.feedTitle) }
    return when (feed) {
      is FeedWithoutGroups ->
        this.onFeedResultSuccessWithoutGroups(feed)
      is FeedWithGroups ->
        this.onFeedResultSuccessWithGroups(feed)
    }
  }

  private fun onFeedResultSuccessWithGroups(feed: FeedWithGroups) {
    this.logger.debug("received feed with groups: {}", feed.feedURI)

    UIThread.runOnUIThread { this.onFeedResultSuccessWithGroupsUI(feed) }
    onPossiblyReceivedEULALink(feed.feedTermsOfService)
  }

  private fun onFeedResultSuccessWithGroupsUI(feed: FeedWithGroups) {
    this.logger.debug("received feed with groups: {}", feed.feedURI)

    UIThread.checkIsUIThread()

    this.invalidateOptionsMenu()

    this.contentFrame.removeAllViews()

    val layout =
      this.layoutInflater.inflate(
        R.layout.catalog_feed_groups_list,
        this.contentFrame,
        false) as ViewGroup

    this.contentFrame.addView(layout)
    this.contentFrame.requestLayout()

    this.logger.debug("restoring scroll position: {}", this.savedScrollPosition)

    val list =
      layout.findViewById<ListView>(R.id.catalog_feed_groups_list)

    this.swipeRefreshLayout = layout.findViewById(R.id.swipe_refresh_layout)
    this.swipeRefreshLayout.setOnRefreshListener { this.retryFeed() }

    list.post { list.setSelection(this.savedScrollPosition) }
    list.dividerHeight = 0
    this.listView = list

    this.configureFacetEntryPointButtons(
      this.feed,
      layout)

    val newUpStack =
      this.newUpStack(this.feedArguments)

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
        feed
      )
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    }

    list.adapter = cfl
    list.setOnScrollListener(cfl)
  }

  private fun onFeedResultSuccessWithoutGroups(feed: FeedWithoutGroups) {
    this.logger.debug("received feed without blocks: {}", feed.feedURI)

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
    this.logger.debug("received feed without blocks (empty): {}", feed.feedURI)

    UIThread.checkIsUIThread()
    Preconditions.checkArgument(feed.entriesInOrder.isEmpty(), "Feed is empty")

    this.invalidateOptionsMenu()

    this.contentFrame.removeAllViews()

    val layout =
      this.layoutInflater.inflate(
        R.layout.catalog_feed_nogroups_empty,
        this.contentFrame,
        false) as ViewGroup
    val emptyText =
      layout.findViewById<TextView>(R.id.catalog_feed_nogroups_empty_text)

    if (this.feedArguments.isSearchResults) {
      emptyText.text = this.resources.getText(R.string.catalog_empty_feed)
    } else {
      emptyText.text = this.catalogFeedGetEmptyText()
    }

    this.contentFrame.addView(layout)
    this.contentFrame.requestLayout()
  }

  private fun onFeedResultSuccessWithoutGroupsNonEmptyUI(feedWithoutGroups: FeedWithoutGroups) {
    this.logger.debug("received feed without blocks (non-empty): {}", feedWithoutGroups.feedURI)

    UIThread.checkIsUIThread()
    Preconditions.checkArgument(
      !feedWithoutGroups.entriesInOrder.isEmpty(), "Feed is non-empty")

    this.invalidateOptionsMenu()
    this.contentFrame.removeAllViews()

    val layout =
      this.layoutInflater.inflate(
        R.layout.catalog_feed_nogroups,
        this.contentFrame,
        false) as ViewGroup

    this.contentFrame.addView(layout)
    this.contentFrame.requestLayout()

    this.logger.debug("restoring scroll position: {}", this.savedScrollPosition)

    val gridView = layout.findViewById<GridView>(R.id.catalog_feed_nogroups_grid)
    this.swipeRefreshLayout = layout.findViewById(R.id.swipe_refresh_layout)
    this.swipeRefreshLayout.setOnRefreshListener({ this.retryFeed() })

    this.configureFacetEntryPointButtons(
      feedWithoutGroups, layout)
    this.configureFacets(
      Simplified.getScreenSizeInformation(), feedWithoutGroups, layout)

    gridView.post { gridView.setSelection(this.savedScrollPosition) }
    this.listView = gridView

    val newUpStack =
      this.newUpStack(this.feedArguments)

    val bookSelectListener =
      CatalogBookSelectionListenerType { v, e -> this.onSelectedBook(newUpStack, e) }

    val without: CatalogFeedWithoutGroups
    try {
      without = CatalogFeedWithoutGroups(
        activity = this,
        analytics = Simplified.getAnalytics(),
        account = Simplified.getProfilesController().profileAccountCurrent(),
        bookCoverProvider = Simplified.getCoverProvider(),
        bookSelectionListener = bookSelectListener,
        bookRegistry = Simplified.getBooksRegistry(),
        bookController = Simplified.getBooksController(),
        profilesController = Simplified.getProfilesController(),
        feedLoader = Simplified.getFeedLoader(),
        feed = feedWithoutGroups,
        networkConnectivity = Simplified.getNetworkConnectivity(),
        executor = Simplified.getBackgroundTaskExecutor(),
        screenSizeInformation = Simplified.getScreenSizeInformation())
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
      .subscribe { event -> without.onBookEvent(event) }
  }

  override fun navigationDrawerShouldShowIndicator(): Boolean = this.upStack.isEmpty

  private fun newUpStack(args: CatalogFeedArguments): ImmutableStack<CatalogFeedArguments> =
    this.upStack.push(args)

  private fun onProfileEvent(event: ProfileEvent) {

    /*
     * If the current profile changed accounts, start a new catalog feed activity. The
     * new activity will automatically navigate to the root of the new account's catalog.
     */

    if (event is ProfileAccountSelectEvent.ProfileAccountSelectSucceeded) {
      UIThread.runOnUIThread {
        val i = Intent(this@CatalogFeedActivity, MainCatalogActivity::class.java)
        val b = Bundle()
        setActivityArguments(b, false)
        i.putExtras(b)
        this.startActivity(i)
        this.finish()
      }
    }
  }

  /*
   * Attempt to fetch arguments explicitly passed to the activity.
   */

  private fun getExplicitActivityArguments(): CatalogFeedArguments? =
    this.intent.extras?.getSerializable(CATALOG_ARGS) as CatalogFeedArguments?

  /*
   * Attempt to fetch arguments explicitly passed to the activity.
   */

  private fun getOrSynthesizeActivityArguments(): CatalogFeedArguments {
    return this.getExplicitActivityArguments() ?: this.run {
      this.logger.debug("synthesizing remote feed arguments")

      /*
       * If there were no arguments (because, for example, this activity is the
       * initial one started for the app), synthesize some.
       */

      CatalogFeedArgumentsRemote(
        title = this.resources.getString(R.string.catalog),
        upStack = ImmutableStack.empty(),
        drawerShouldOpen = false,
        feedURI = this.account.provider().catalogURI(),
        isSearchResults = false)
    }
  }

  override fun onCreate(@Nullable state: Bundle?) {
    super.onCreate(state)

    /*
     * Attempt to restore the saved scroll position, if there is one.
     */

    if (state != null) {
      this.logger.debug("received state")
      this.savedScrollPosition = state.getInt(LIST_STATE_ID)
    } else {
      this.savedScrollPosition = 0
    }

    try {
      this.profile = Simplified.getProfilesController().profileCurrent()
      this.account = this.profile.accountCurrent()
      this.initialized = true
    } catch (e: ProfileNoneCurrentException) {
      this.logger.error("no profile is current: ", e)
      // We expect a superclass to handle this problem
      return
    }

    this.feedArguments = this.getOrSynthesizeActivityArguments()
  }

  private fun isRootOfCollection(): Boolean =
    when (val arguments = this.feedArguments) {
      is CatalogFeedArgumentsRemote ->
        arguments.feedURI == account.provider().catalogURI()
      is CatalogFeedArgumentsLocalBooks ->
        false
    }

  private fun ageGateIsPresent(): Boolean =
    this.account.provider().hasAgeGate()

  private fun ageGateIsSatisfied(): Boolean {
    val dateOfBirthOpt = this.profile.preferences().dateOfBirth()
    return if (dateOfBirthOpt is Some<ProfileDateOfBirth>) {
      dateOfBirthOpt.get().yearsOld(LocalDate.now()) >= 13
    } else {
      false
    }
  }

  /**
   * Remove any current views and show the age gate.
   */

  private fun showAgeGate() {
    val newLayout =
      this.layoutInflater.inflate(
        R.layout.catalog_feed_age_gate,
        this.contentFrame,
        false) as ViewGroup

    val buttonUnder13 =
      newLayout.findViewById<Button>(R.id.catalog_age_gate_younger)
    val buttonOver13 =
      newLayout.findViewById<Button>(R.id.catalog_age_gate_older)

    /*
     * A button that synthesizes a fake age that happens to be under 13 and then loads
     * the correct feed.
     */

    buttonUnder13.setOnClickListener {
      Simplified.getProfilesController()
        .profilePreferencesUpdate(
          this.profile.preferences()
            .toBuilder()
            .setDateOfBirth(this.synthesizeDateOfBirth(0))
            .build())
        .map {
          UIThread.runOnUIThread {
            this.startDisplayingFeed()
          }
        }
    }

    /*
     * A button that synthesizes a fake age that happens to be over 13 and then loads
     * the correct feed.
     */

    buttonOver13.setOnClickListener {
      Simplified.getProfilesController()
        .profilePreferencesUpdate(
          this.profile.preferences()
            .toBuilder()
            .setDateOfBirth(this.synthesizeDateOfBirth(14))
            .build())
        .map {
          UIThread.runOnUIThread {
            this.startDisplayingFeed()
          }
        }
    }

    this.contentFrame.removeAllViews()
    this.contentFrame.addView(newLayout)
    this.contentFrame.requestLayout()
  }

  /**
   * Synthesize a fake date of birth based on the current date and given age in years.
   */

  private fun synthesizeDateOfBirth(years: Int): ProfileDateOfBirth =
    ProfileDateOfBirth(
      date = LocalDate.now().minusYears(years),
      isSynthesized = true)

  override fun onStart() {
    super.onStart()

    /*
     * If the activity was not correctly initialized, it almost certainly means that no
     * profile was active and this activity is scheduled for destruction at the earliest
     * opportunity. There's no point trying to do any useful work.
     */

    if (!this.initialized) {
      return
    }

    /*
     * Subscribe to profile change events.
     */

    this.profileEventSubscription =
      Simplified.getProfilesController()
        .profileEvents()
        .subscribe { event -> this.onProfileEvent(event) }

    this.startForCurrentFeedArguments()
  }

  private fun startForCurrentFeedArguments() {

    /*
     * Decide whether or not it's necessary to show an age gate.
     */

    if (this.ageGateIsPresent()) {
      if (this.ageGateIsSatisfied()) {
        this.startDisplayingFeed()
        return
      }

      if (this.isRootOfCollection()) {
        this.showAgeGate()
        return
      }
    }

    /*
     * Otherwise, we're going to be displaying a feed.
     */

    this.startDisplayingFeed()
  }

  /**
   * Select the correct feed based on the profile age gate.
   */

  private fun ageGateSelectCorrectFeed() {
    val ageOpt = this.profile.preferences().dateOfBirth()
    if (!(ageOpt is Some<ProfileDateOfBirth>)) {
      throw UnreachableCodeException()
    }

    val age = ageOpt.get()
    val yearsOld = age.yearsOld(LocalDate.now())
    this.logger.debug("updating feed arguments for age gate ({} years)", yearsOld)
    this.feedArguments =
      CatalogFeedArgumentsRemote(
        title = this.resources.getString(R.string.catalog),
        upStack = ImmutableStack.empty(),
        drawerShouldOpen = false,
        feedURI = this.account.provider().catalogURIForAge(yearsOld),
        isSearchResults = false)
  }

  /*
   * The main function that starts the actual loading and displaying of a feed.
   */

  private fun startDisplayingFeed() {

    if (this.ageGateIsPresent()) {
      Preconditions.checkArgument(
        this.ageGateIsSatisfied(),
        "Age gate must have been satisfied")
    }

    if (this.isRootOfCollection() && this.ageGateIsPresent()) {
      this.ageGateSelectCorrectFeed()
    }

    /*
     * If the feed requires network connectivity, and the network is not
     * available, then fail fast and display an error message.
     */

    val net = Simplified.getNetworkConnectivity()
    if (this.feedArguments.requiresNetworkConnectivity) {
      if (!net.isNetworkAvailable) {
        this.onNetworkUnavailable()
        return
      }
    }

    /*
     * Display a progress bar until the feed is either loaded or fails.
     */

    this.showProgressView()

    /*
     * Load a feed based on the given arguments.
     */

    return when (val arguments = this.feedArguments) {
      is CatalogFeedArgumentsRemote ->
        this.doLoadRemoteFeed(arguments)
      is CatalogFeedArgumentsLocalBooks ->
        this.doLoadLocalFeed(arguments)
    }
  }

  /**
   * Remove any current views and show a progress indicator.
   */

  private fun showProgressView() {
    val newProgressLayout =
      this.layoutInflater.inflate(R.layout.catalog_loading, this.contentFrame, false)
        as ViewGroup

    this.contentFrame.removeAllViews()
    this.contentFrame.addView(newProgressLayout)
    this.contentFrame.requestLayout()
  }

  override fun onStop() {
    super.onStop()

    this.profileEventSubscription?.unsubscribe()
    this.bookEventSubscription?.unsubscribe()
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
        this.retryFeed()
        extras.putBoolean("reload", false)
      }
    }

    if (this.previouslyPaused && !didRetry) {
      if (!this.feedArguments.requiresNetworkConnectivity) {
        this.retryFeed()
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    this.logger.debug("inflating menu")
    this.menuInflater.inflate(R.menu.catalog, menu)

    if (this.feed == null) {
      this.logger.debug("menu creation requested but feed is not yet present")
      return true
    }

    this.logger.debug("menu creation requested and feed is present")
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
    var searchOk = false

    // XXX: Update to support library search item
    if (search != null && false) {
      this.searchView = searchItem.actionView as SearchView
      searchOk = when (search) {
        FeedSearch.FeedSearchLocal -> {
          this.searchView!!.setOnQueryTextListener(
            this.BooksLocalSearchQueryHandler(this.resources, this.feedArguments, FacetType.SORT_BY_TITLE))
          true
        }
        is FeedSearch.FeedSearchOpen1_1 -> {
          this.searchView!!.setOnQueryTextListener(
            this.OpenSearchQueryHandler(this.resources, this.feedArguments, search.search))
          true
        }
      }
    } else {
      this.logger.debug("Feed has no search opts.")
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
      if (this.feedArguments.title.startsWith(this.getString(R.string.search_hint_prefix))) {
        this.searchView!!.queryHint = this.feedArguments.title
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
    this.logger.debug("onDestroy")

    val future = this.loading
    future?.cancel(true)

    val profileSub = this.profileEventSubscription
    profileSub?.unsubscribe()

    val bookSub = this.bookEventSubscription
    bookSub?.unsubscribe()
  }

  /**
   * The network is unavailable. Simply display a message and a button to allow
   * the user to retry loading when they have fixed their connection.
   */

  private fun onNetworkUnavailable() {
    UIThread.checkIsUIThread()

    this.logger.debug("network is unavailable")

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

    this.logger.debug("saving state")

    /*
     * Save the scroll position in the hope that it can be restored later.
     */

    val lv = this.listView
    if (lv != null) {
      val position = lv.firstVisiblePosition
      this.logger.debug("saving list view position: {}", Integer.valueOf(position))
      state.putInt(LIST_STATE_ID, position)
    }
  }

  private fun onSelectedBook(
    newUpStack: ImmutableStack<CatalogFeedArguments>,
    e: FeedEntryOPDS) {
    this.logger.debug("onSelectedBook: {}", this)
    CatalogBookDetailActivity.startNewActivity(this, newUpStack, e)
  }

  private fun onSelectedFeedGroup(
    newUpStack: ImmutableStack<CatalogFeedArguments>,
    f: FeedGroup) {
    this.logger.debug("onSelectFeed: {}", this)

    this.catalogActivityForkNew(CatalogFeedArgumentsRemote(
      title = f.groupTitle,
      upStack = newUpStack,
      drawerShouldOpen = false,
      feedURI = f.groupURI,
      isSearchResults = false))
  }

  /**
   * Retry the current feed.
   */

  private fun retryFeed() {
    this@CatalogFeedActivity.swipeRefreshLayout.isRefreshing = false
    this.loading?.cancel(true)
    this.loading = null

    return when (val arguments = this.feedArguments) {
      is CatalogFeedArgumentsRemote -> {
        this.logger.debug("invalidating {} in feed cache", arguments.feedURI)
        Simplified.getFeedLoader().invalidate(arguments.feedURI)
        this.startDisplayingFeed()
      }
      is CatalogFeedArgumentsLocalBooks -> {
        this.startDisplayingFeed()
      }
    }
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
    val booksUri = URI.create("Books")

    val request =
      ProfileFeedRequest.builder(
        booksUri,
        this.resources.getString(R.string.books),
        this.resources.getString(R.string.books_sort_by),
        CatalogFacetPseudoTitleProvider(this.resources))
        .setFeedSelection(c.selection)
        .setSearch(c.searchTerms)
        .setFacetActive(c.facetType)
        .build()

    try {
      val future =
        Simplified.getProfilesController().profileFeed(request)

      future.addCallback(object : FutureCallback<FeedWithoutGroups> {
        override fun onSuccess(result: FeedWithoutGroups?) {
          this@CatalogFeedActivity.onFeedResult(booksUri, FeedLoaderSuccess(result!!))
        }

        override fun onFailure(ex: Throwable) {
          this@CatalogFeedActivity.logger.error("error in feed result handler: ", ex)
          this@CatalogFeedActivity.onFeedResultFailedException(booksUri, ex)
        }
      }, MoreExecutors.directExecutor())
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    }

  }

  /**
   * Unconditionally load a remote feed.
   *
   * @param c The feed arguments
   */

  private fun doLoadRemoteFeed(c: CatalogFeedArgumentsRemote) =
    this.loadFeed(Simplified.getFeedLoader(), c.feedURI)

  /**
   * A handler for local book searches.
   */

  private inner class BooksLocalSearchQueryHandler internal constructor(
    private val resources: Resources,
    private val feedArguments: CatalogFeedArguments,
    private val facetActive: FacetType) : OnQueryTextListener {

    override fun onQueryTextChange(@Nullable s: String): Boolean = true

    override fun onQueryTextSubmit(query: String): Boolean {
      val newArgs =
        CatalogFeedArgumentsLocalBooks(
          title = this.resources.getString(R.string.catalog_search) + ": " + query,
          upStack = ImmutableStack.empty(),
          facetType = this.facetActive,
          searchTerms = Option.some(query),
          selection = this@CatalogFeedActivity.localFeedTypeSelection())

      if (this.feedArguments.isSearchResults) {
        this@CatalogFeedActivity.catalogActivityForkNewReplacing(newArgs)
      } else {
        this@CatalogFeedActivity.catalogActivityForkNew(newArgs)
      }

      return true
    }
  }

  /**
   * A handler for OpenSearch 1.1 searches.
   */

  private inner class OpenSearchQueryHandler internal constructor(
    private val resources: Resources,
    private val feedArguments: CatalogFeedArguments,
    private val search: OPDSOpenSearch1_1) : OnQueryTextListener {

    override fun onQueryTextChange(text: String): Boolean = true

    override fun onQueryTextSubmit(query: String): Boolean {
      val profile =
        Simplified.getProfilesController().profileCurrent()
      val account =
        profile.accountCurrent()

      Simplified.getAnalytics()
        .publishEvent(AnalyticsEvent.CatalogSearched(
          profileUUID = profile.id().uuid,
          timestamp = LocalDateTime.now(),
          credentials = account.loginState().credentials,
          accountProvider = account.provider().id(),
          accountUUID = account.id().uuid,
          searchQuery = query))

      val target = this.search.getQueryURIForTerms(query)
      val us =
        this@CatalogFeedActivity.newUpStack(this.feedArguments)
      val title = this.resources.getString(R.string.catalog_search) + ": " + query
      val newArgs =
        CatalogFeedArgumentsRemote(
          title = title,
          upStack = us,
          drawerShouldOpen = false,
          feedURI = target,
          isSearchResults = true)

      if (this@CatalogFeedActivity.feedArguments.isSearchResults) {
        this@CatalogFeedActivity.catalogActivityForkNewReplacing(newArgs)
      } else {
        this@CatalogFeedActivity.catalogActivityForkNew(newArgs)
      }
      return true
    }
  }

  private fun openActivityForFeedFacet(
    facet: FeedFacet,
    feed: Feed,
    feedSelection: FeedBooksSelection,
    upStack: ImmutableStack<CatalogFeedArguments>,
    searchTerms: OptionType<String>) =
    when (facet) {
      is FeedFacet.FeedFacetOPDS ->
        this.openActivityForFeedFacetOPDS(feed, upStack, facet)
      is FeedFacet.FeedFacetPseudo ->
        this.openActivityForFeedFacetPseudo(feed, upStack, facet, searchTerms, feedSelection)
    }

  private fun openActivityForFeedFacetPseudo(
    feed: Feed,
    upStack: ImmutableStack<CatalogFeedArguments>,
    facet: FeedFacet.FeedFacetPseudo,
    searchTerms: OptionType<String>,
    feedSelection: FeedBooksSelection) =
    this.catalogActivityForkNewReplacing(
      CatalogFeedArgumentsLocalBooks(
        title = feed.feedTitle,
        upStack = upStack,
        facetType = facet.type,
        searchTerms = searchTerms,
        selection = feedSelection))

  private fun openActivityForFeedFacetOPDS(
    feed: Feed,
    upStack: ImmutableStack<CatalogFeedArguments>,
    facet: FeedFacet.FeedFacetOPDS) =
    this.catalogActivityForkNewReplacing(
      CatalogFeedArgumentsRemote(
        title = feed.feedTitle,
        drawerShouldOpen = false,
        upStack = upStack,
        feedURI = facet.opdsFacet.uri,
        isSearchResults = false))

  companion object {

    private const val CATALOG_ARGS: String =
      "org.nypl.simplified.app.CatalogFeedActivity.arguments"
    private const val LIST_STATE_ID: String =
      "org.nypl.simplified.app.CatalogFeedActivity.list_view_state"

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
      args: CatalogFeedArguments) {

      b.putSerializable(this.CATALOG_ARGS, args)

      when (args) {
        is CatalogFeedArgumentsRemote -> {
          setActivityArguments(b, args.drawerShouldOpen)
          setActivityArguments(b, args.upStack)
        }
        is CatalogFeedArgumentsLocalBooks -> {
          setActivityArguments(b, false)
          setActivityArguments(b, args.upStack)
        }
      }
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
            // We don't care about this. Eventually the right URL will be given to us.
          }
        }
      }
    }
  }
}
