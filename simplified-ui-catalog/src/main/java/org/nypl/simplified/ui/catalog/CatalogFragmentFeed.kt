package org.nypl.simplified.ui.catalog

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.TEXT_ALIGNMENT_TEXT_END
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Space
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedFacets
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedAuthentication
import org.nypl.simplified.feeds.api.FeedLoaderResult.FeedLoaderFailure.FeedLoaderFailedGeneral
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.feeds.api.FeedSearch
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.ui.accounts.AccountPickerDialogFragment
import org.nypl.simplified.ui.catalog.CatalogFeedArguments.CatalogFeedArgumentsRemote
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership.CollectedFromAccounts
import org.nypl.simplified.ui.catalog.CatalogFeedOwnership.OwnedByAccount
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedAgeGate
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoadFailed
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedEmpty
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedNavigation
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoading
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.theme.ThemeControl
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * The base type of feed fragments. This class is abstract purely because the AndroidX
 * ViewModel API requires that we fetch view models by class, and we need to store separate view
 * models for each of the different app sections that want to display feeds.
 */

class CatalogFragmentFeed : Fragment(), AgeGateDialog.BirthYearSelectedListener {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.CatalogFragmentFeed.parameters"

    /**
     * Create a login fragment for the given parameters.
     */

    fun create(parameters: CatalogFeedArguments): CatalogFragmentFeed {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = CatalogFragmentFeed()
      fragment.arguments = arguments
      return fragment
    }
  }

  private lateinit var analytics: AnalyticsType
  private lateinit var bookCovers: BookCoverProviderType
  private lateinit var bookRegistry: BookRegistryReadableType
  private lateinit var borrowViewModel: CatalogBorrowViewModel
  private lateinit var buttonCreator: CatalogButtons
  private lateinit var configurationService: BuildConfigurationServiceType
  private lateinit var feedEmpty: ViewGroup
  private lateinit var feedError: ViewGroup
  private lateinit var feedErrorDetails: Button
  private lateinit var feedErrorRetry: Button
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var feedLoading: ViewGroup
  private lateinit var feedModel: CatalogFeedViewModelType
  private lateinit var feedNavigation: ViewGroup
  private lateinit var feedWithGroups: ViewGroup
  private lateinit var feedWithGroupsAdapter: CatalogFeedWithGroupsAdapter
  private lateinit var feedWithGroupsData: MutableList<FeedGroup>
  private lateinit var feedWithGroupsFacets: LinearLayout
  private lateinit var feedWithGroupsFacetsScroll: ViewGroup
  private lateinit var feedWithGroupsHeader: ViewGroup
  private lateinit var feedWithGroupsList: RecyclerView
  private lateinit var feedWithGroupsTabs: RadioGroup
  private lateinit var feedWithoutGroups: ViewGroup
  private lateinit var feedWithoutGroupsAdapter: CatalogPagedAdapter
  private lateinit var feedWithoutGroupsFacets: LinearLayout
  private lateinit var feedWithoutGroupsFacetsScroll: ViewGroup
  private lateinit var feedWithoutGroupsHeader: ViewGroup
  private lateinit var feedWithoutGroupsList: RecyclerView
  private lateinit var feedWithoutGroupsScrollListener: RecyclerView.OnScrollListener
  private lateinit var feedWithoutGroupsTabs: RadioGroup
  private lateinit var imageLoader: ImageLoaderType
  private lateinit var parameters: CatalogFeedArguments
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var screenInformation: ScreenSizeInformationType
  private lateinit var ageGateDialog: AgeGateDialog

  private val logger = LoggerFactory.getLogger(CatalogFragmentFeed::class.java)
  private val parametersId = PARAMETERS_ID
  private val listener: FragmentListenerType<CatalogFeedEvent> by fragmentListeners()

  private var accountSubscription: Disposable? = null
  private var profileSubscription: Disposable? = null
  private var feedStatusSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    ageGateDialog =
      (childFragmentManager.findFragmentByTag(AgeGateDialog::class.java.simpleName) as? AgeGateDialog)
      ?: AgeGateDialog.create()
    this.parameters = this.requireArguments()[this.parametersId] as CatalogFeedArguments
    this.feedWithGroupsData = mutableListOf()

    val services = Services.serviceDirectory()

    this.analytics =
      services.requireService(AnalyticsType::class.java)
    this.bookCovers =
      services.requireService(BookCoverProviderType::class.java)
    this.bookRegistry =
      services.requireService(BookRegistryReadableType::class.java)
    this.screenInformation =
      services.requireService(ScreenSizeInformationType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.configurationService =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.feedLoader =
      services.requireService(FeedLoaderType::class.java)
    this.imageLoader =
      services.requireService(ImageLoaderType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    this.buttonCreator =
      CatalogButtons(this.requireContext(), this.screenInformation)

    val layout =
      inflater.inflate(R.layout.feed, container, false)

    this.feedEmpty =
      layout.findViewById(R.id.feedEmpty)
    this.feedError =
      layout.findViewById(R.id.feedError)
    this.feedLoading =
      layout.findViewById(R.id.feedLoading)
    this.feedNavigation =
      layout.findViewById(R.id.feedNavigation)
    this.feedWithGroups =
      layout.findViewById(R.id.feedWithGroups)
    this.feedWithoutGroups =
      layout.findViewById(R.id.feedWithoutGroups)

    this.feedWithGroupsHeader =
      layout.findViewById(R.id.feedWithGroupsHeader)
    this.feedWithGroupsFacetsScroll =
      this.feedWithGroupsHeader.findViewById(R.id.feedHeaderFacetsScroll)
    this.feedWithGroupsFacets =
      this.feedWithGroupsHeader.findViewById(R.id.feedHeaderFacets)
    this.feedWithGroupsTabs =
      this.feedWithGroupsHeader.findViewById(R.id.feedHeaderTabs)

    this.feedWithGroupsList = this.feedWithGroups.findViewById(R.id.feedWithGroupsList)
    this.feedWithGroupsList.setHasFixedSize(true)
    this.feedWithGroupsList.setItemViewCacheSize(32)
    this.feedWithGroupsList.layoutManager = LinearLayoutManager(this.context)
    (this.feedWithGroupsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    this.feedWithGroupsList.addItemDecoration(
      CatalogFeedWithGroupsDecorator(this.screenInformation.dpToPixels(16).toInt())
    )

    this.feedWithoutGroupsHeader =
      layout.findViewById(R.id.feedWithoutGroupsHeader)
    this.feedWithoutGroupsFacetsScroll =
      this.feedWithoutGroupsHeader.findViewById(R.id.feedHeaderFacetsScroll)
    this.feedWithoutGroupsFacets =
      this.feedWithoutGroupsHeader.findViewById(R.id.feedHeaderFacets)
    this.feedWithoutGroupsTabs =
      this.feedWithoutGroupsHeader.findViewById(R.id.feedHeaderTabs)

    this.feedWithoutGroupsList = this.feedWithoutGroups.findViewById(R.id.feedWithoutGroupsList)
    this.feedWithoutGroupsList.setHasFixedSize(true)
    this.feedWithoutGroupsList.setItemViewCacheSize(32)
    this.feedWithoutGroupsList.layoutManager = LinearLayoutManager(this.context)
    (this.feedWithoutGroupsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.feedErrorRetry =
      this.feedError.findViewById(R.id.feedErrorRetry)
    this.feedErrorDetails =
      this.feedError.findViewById(R.id.feedErrorDetails)

    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    return layout
  }

  override fun onStart() {
    super.onStart()

    this.feedModel =
      this.createOrGetFeedModel()
    this.borrowViewModel =
      CatalogBorrowViewModelFactory.get(this)

    /*
     * Configure the lanes based on the viewmodel.
     */

    this.feedWithGroupsAdapter =
      CatalogFeedWithGroupsAdapter(
        groups = this.feedWithGroupsData,
        coverLoader = this.bookCovers,
        onFeedSelected = this::onFeedSelected,
        onBookSelected = this::onBookSelected
      )
    this.feedWithGroupsList.adapter = this.feedWithGroupsAdapter

    this.feedStatusSubscription = this.feedModel.feedStatus
      .map { this.feedModel.feedState() }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::reconfigureUI)

    this.feedWithGroupsList.layoutManager!!.onRestoreInstanceState(
      this.feedModel.restoreFeedWithGroupsViewState()
    )
    this.feedWithoutGroupsList.layoutManager!!.onRestoreInstanceState(
      this.feedModel.restoreFeedWithoutGroupsViewState()
    )

    this.feedWithoutGroupsScrollListener = CatalogScrollListener(this.bookCovers)
    this.feedWithoutGroupsList.addOnScrollListener(this.feedWithoutGroupsScrollListener)
    this.reconfigureUI(this.feedModel.feedState())

    this.accountSubscription =
      this.profilesController.accountEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onAccountEvent)
    this.profileSubscription =
      this.profilesController.profileEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onProfileEvent)

    /*
     * Refresh the feed if it is locally generated.
     */

    if (this.parameters.isLocallyGenerated) {
      this.feedModel.reloadFeed(this.parameters)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.catalog, menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    // Necessary to reconfigure the Toolbar here due to the "Switch Account" action.
    this.configureToolbar()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.catalogMenuActionSearch -> {
        this.feedModel.feedState().search?.let { search ->
          this.openSearchDialog(requireContext(), search)
        }
        true
      }
      R.id.catalogMenuActionReload -> {
        this.feedModel.reloadFeed(this.feedModel.feedState().arguments)
        true
      }
      android.R.id.home -> {
        if (isAccountCatalogRoot()) {
          this.openAccountPickerDialog()
          true
        } else {
          super.onOptionsItemSelected(item)
        }
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun onAccountEvent(event: AccountEvent) {
    return when (event) {
      is AccountEventCreation.AccountEventCreationSucceeded,
      is AccountEventDeletion.AccountEventDeletionSucceeded -> {
        if (this.parameters.isLocallyGenerated) {
          this.feedModel.reloadFeed(this.parameters)
        } else {
          // No reload necessary
        }
      }
      else -> {
      }
    }
  }

  private fun onProfileEvent(event: ProfileEvent) {
    when (event) {
      is ProfileUpdated.Succeeded -> {
        val feedState = this.feedModel.feedState()
        when (val ownership = feedState.arguments.ownership) {
          is OwnedByAccount -> {
            val ageChanged =
              event.newDescription.preferences.dateOfBirth != event.oldDescription.preferences.dateOfBirth
            if (ageChanged) {
              val account = this.profilesController.profileCurrent().account(ownership.accountId)
              onAgeUpdateSuccess(account, ownership, event)
            }
          }
        }
      }
    }
  }

  private fun createOrGetFeedModel(): CatalogFeedViewModel {
    return ViewModelProvider(
      this,
      CatalogFeedViewModelFactory(
        context = this.requireContext(),
        services = Services.serviceDirectory(),
        feedArguments = this.parameters
      )
    ).get(CatalogFeedViewModel::class.java)
  }

  private fun onBookSelected(opdsEntry: FeedEntry.FeedEntryOPDS) {
    this.listener.post(
      CatalogFeedEvent.OpenBookDetail(this.parameters, opdsEntry)
    )
  }

  private fun onFeedSelected(title: String, uri: URI) {
    this.listener.post(
      CatalogFeedEvent.OpenFeed(this.feedModel.resolveFeed(title, uri, false))
    )
  }

  @UiThread
  private fun reconfigureUI(feedState: CatalogFeedState) {
    val activity = this.activity
    if (activity == null) {
      this.logger.warn("fragment is not attached")
      return
    }

    return when (feedState) {
      is CatalogFeedAgeGate ->
        this.onCatalogFeedAgeGateUI(feedState)
      is CatalogFeedLoading ->
        this.onCatalogFeedLoadingUI(feedState)
      is CatalogFeedWithGroups ->
        this.onCatalogFeedWithGroupsUI(feedState)
      is CatalogFeedWithoutGroups ->
        this.onCatalogFeedWithoutGroupsUI(feedState)
      is CatalogFeedNavigation ->
        this.onCatalogFeedNavigationUI(feedState)
      is CatalogFeedLoadFailed ->
        this.onCatalogFeedLoadFailed(feedState)
      is CatalogFeedEmpty ->
        this.onCatalogFeedEmpty(feedState)
    }
  }

  override fun onStop() {
    super.onStop()

    this.feedModel.saveFeedWithGroupsViewState(
      this.feedWithGroupsList.layoutManager!!.onSaveInstanceState()
    )
    this.feedModel.saveFeedWithoutGroupsViewState(
      this.feedWithoutGroupsList.layoutManager!!.onSaveInstanceState()
    )

    /*
     * We aggressively unset adapters here in order to try to encourage prompt unsubscription
     * of views from the book registry.
     */

    this.feedWithoutGroupsList.removeOnScrollListener(this.feedWithoutGroupsScrollListener)
    this.feedWithoutGroupsList.adapter = null
    this.feedWithGroupsList.adapter = null
    this.feedStatusSubscription?.dispose()
    this.accountSubscription?.dispose()
    this.profileSubscription?.dispose()
  }

  @UiThread
  private fun onCatalogFeedAgeGateUI(
    @Suppress("UNUSED_PARAMETER") feedState: CatalogFeedAgeGate
  ) {
    if (!ageGateDialog.isAdded) {
      ageGateDialog.show(childFragmentManager, AgeGateDialog::class.simpleName)
    }
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()
  }

  private fun onAgeUpdateSuccess(
    account: AccountReadableType,
    ownership: OwnedByAccount,
    result: ProfileUpdated.Succeeded
  ) {
    val now = DateTime.now()
    val oldAge = result.oldDescription.preferences.dateOfBirth?.yearsOld(now)
    val newAge = result.newDescription.preferences.dateOfBirth?.yearsOld(now)
    this.logger.debug("age updated from {} to {}", oldAge, newAge)

    newAge?.let { age ->
      val newParameters = CatalogFeedArgumentsRemote(
        title = this.parameters.title,
        ownership = ownership,
        feedURI = account.catalogURIForAge(age),
        isSearchResults = false
      )
      this.feedModel.reloadFeed(newParameters)
      if (ageGateDialog.isVisible) {
        ageGateDialog.dismissAllowingStateLoss()
      }
    }
  }

  @UiThread
  private fun onCatalogFeedEmpty(
    @Suppress("UNUSED_PARAMETER") feedState: CatalogFeedEmpty
  ) {
    this.feedEmpty.visibility = View.VISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()
  }

  @UiThread
  private fun onCatalogFeedLoadingUI(
    @Suppress("UNUSED_PARAMETER") feedState: CatalogFeedLoading
  ) {
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.VISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()
  }

  @UiThread
  private fun onCatalogFeedNavigationUI(
    @Suppress("UNUSED_PARAMETER") feedState: CatalogFeedNavigation
  ) {
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.VISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()
  }

  @UiThread
  private fun onCatalogFeedWithoutGroupsUI(
    feedState: CatalogFeedWithoutGroups
  ) {
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.VISIBLE

    this.configureToolbar()

    this.configureFacets(
      facetHeader = this.feedWithoutGroupsHeader,
      facetTabs = this.feedWithoutGroupsTabs,
      facetLayoutScroller = this.feedWithoutGroupsFacetsScroll,
      facetLayout = this.feedWithoutGroupsFacets,
      facetsByGroup = feedState.facetsByGroup
    )

    this.feedWithoutGroupsAdapter =
      CatalogPagedAdapter(
        borrowViewModel = this.borrowViewModel,
        buttonCreator = this.buttonCreator,
        context = requireActivity(),
        listener = this.listener,
        onBookSelected = this::onBookSelected,
        services = Services.serviceDirectory(),
        ownership = feedState.arguments.ownership
      )

    this.feedWithoutGroupsList.adapter = this.feedWithoutGroupsAdapter
    feedState.entries.observe(
      this,
      Observer { newPagedList ->
        this.logger.debug("received paged list ({} elements)", newPagedList.size)
        this.feedWithoutGroupsAdapter.submitList(newPagedList)
      }
    )
  }

  @UiThread
  private fun onCatalogFeedWithGroupsUI(
    feedState: CatalogFeedWithGroups
  ) {
    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.VISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()

    this.configureFacets(
      facetHeader = this.feedWithGroupsHeader,
      facetTabs = this.feedWithGroupsTabs,
      facetLayoutScroller = this.feedWithGroupsFacetsScroll,
      facetLayout = this.feedWithGroupsFacets,
      facetsByGroup = feedState.feed.facetsByGroup
    )

    this.feedWithGroupsData.clear()
    this.feedWithGroupsData.addAll(feedState.feed.feedGroupsInOrder)
    this.feedWithGroupsAdapter.notifyDataSetChanged()
  }

  @UiThread
  private fun onCatalogFeedLoadFailed(
    feedState: CatalogFeedLoadFailed
  ) {
    /*
     * If the feed can't be loaded due to an authentication failure, then open
     * the account screen (if possible).
     */

    when (feedState.failure) {
      is FeedLoaderFailedGeneral -> {
        // Display the error.
      }
      is FeedLoaderFailedAuthentication -> {
        when (val ownership = this.parameters.ownership) {
          is OwnedByAccount -> {
            val shouldAuthenticate =
              this.profilesController.profileCurrent()
                .account(ownership.accountId)
                .requiresCredentials

            if (shouldAuthenticate) {
              /*
               * Explicitly deferring the opening of the fragment is required due to the
               * tabbed navigation controller eagerly instantiating fragments and causing
               * fragment transaction exceptions. This will go away when we have a replacement
               * for the navigator library.
               */

              this.listener.post(CatalogFeedEvent.LoginRequired(ownership.accountId))
            }
          }
          CollectedFromAccounts -> {
            // Nothing we can do here! We don't know which account owns the feed.
          }
        }
      }
    }

    this.feedEmpty.visibility = View.INVISIBLE
    this.feedError.visibility = View.VISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.configureToolbar()

    this.feedErrorRetry.isEnabled = true
    this.feedErrorRetry.setOnClickListener { button ->
      button.isEnabled = false
      this.feedModel.reloadFeed(this.feedModel.feedState().arguments)
    }

    this.feedErrorDetails.isEnabled = true
    this.feedErrorDetails.setOnClickListener {
      this.listener.post(
        CatalogFeedEvent.OpenErrorPage(
          this.errorPageParameters(feedState.failure)
        )
      )
    }
  }

  @UiThread
  private fun configureToolbar() {
    this.configureToolbarNavigation()
    this.configureToolbarTitles()
  }

  @UiThread
  private fun configureToolbarNavigation() {
    fun showAccountPickerAction() {
      // Configure the 'Home Action' in the Toolbar to show the account picker when tapped.
      this.supportActionBar?.apply {
        // Configure whether or not the user should be able to change accounts
        if (configurationService.showChangeAccountsUi) {
          setHomeAsUpIndicator(R.drawable.accounts)
          setHomeActionContentDescription(R.string.catalogAccounts)
          setDisplayHomeAsUpEnabled(true)
        } else {
          setDisplayHomeAsUpEnabled(false)
        }
      }
    }

    try {
      if (isAccountCatalogRoot()) {
        showAccountPickerAction()
      }
    } catch (e: Exception) {
      // Nothing to do
    }
  }

  @UiThread
  private fun configureToolbarTitles() {
    try {
      when (val ownership = this.parameters.ownership) {
        is OwnedByAccount -> {
          val accountProvider =
            this.profilesController.profileCurrent()
              .account(ownership.accountId)
              .provider

          this.supportActionBar?.apply {
            title = this@CatalogFragmentFeed.parameters.title
            subtitle = accountProvider.displayName
          }
        }

        is CollectedFromAccounts -> {
          this.supportActionBar?.apply {
            title = this@CatalogFragmentFeed.parameters.title
            subtitle = null
          }
        }
      }
    } catch (e: Exception) {
      this.supportActionBar?.apply {
        title = this@CatalogFragmentFeed.parameters.title
        subtitle = null
      }
    }
  }

  @UiThread
  private fun openAccountPickerDialog() {
    return when (val ownership = this.parameters.ownership) {
      is OwnedByAccount -> {
        val dialog =
          AccountPickerDialogFragment.create(
            currentId = ownership.accountId,
            showAddAccount = this.configurationService.allowAccountsAccess
          )
        dialog.show(parentFragmentManager, dialog.tag)
      }
      CollectedFromAccounts -> {
        throw IllegalStateException("Can't switch account from collected feed!")
      }
    }
  }

  @SuppressLint("InflateParams")
  @UiThread
  private fun openSearchDialog(
    context: Context,
    search: FeedSearch
  ) {
    val view = LayoutInflater.from(context).inflate(R.layout.search_dialog, null)
    val searchView = view.findViewById<TextView>(R.id.searchDialogText)!!

    fun performSearch(searchView: TextView) {
      val query = searchView.text.toString().trim()
      this@CatalogFragmentFeed.logSearchToAnalytics(query)
      this@CatalogFragmentFeed.listener.post(
        CatalogFeedEvent.OpenFeed(
          this@CatalogFragmentFeed.feedModel.resolveSearch(search, query)
        )
      )
    }

    val builder = AlertDialog.Builder(context).apply {
      setPositiveButton(R.string.catalogSearch) { dialog, _ ->
        performSearch(searchView)
        dialog.dismiss()
      }
      setNegativeButton(R.string.cancel) { dialog, _ ->
        dialog.dismiss()
      }
      setView(view)
    }

    val dialog = builder.create()
    searchView.setOnEditorActionListener { _, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_SEARCH -> {
          performSearch(searchView)
          dialog.dismiss()
          true
        }
        else -> false
      }
    }
    dialog.show()
  }

  private fun logSearchToAnalytics(query: String) {
    try {
      val profile = this.profilesController.profileCurrent()
      val accountId =
        when (val ownership = this.parameters.ownership) {
          is OwnedByAccount -> ownership.accountId
          is CollectedFromAccounts -> null
        }

      if (accountId != null) {
        val account = profile.account(accountId)
        this.analytics.publishEvent(
          AnalyticsEvent.CatalogSearched(
            timestamp = LocalDateTime.now(),
            credentials = account.loginState.credentials,
            profileUUID = profile.id.uuid,
            accountProvider = account.provider.id,
            accountUUID = account.id.uuid,
            searchQuery = query
          )
        )
      }
    } catch (e: Exception) {
      this.logger.error("could not log to analytics: ", e)
    }
  }

  @UiThread
  private fun configureFacets(
    facetHeader: ViewGroup,
    facetTabs: RadioGroup,
    facetLayoutScroller: ViewGroup,
    facetLayout: LinearLayout,
    facetsByGroup: Map<String, List<FeedFacet>>
  ) {
    /*
     * If the facet groups are empty, hide the header entirely.
     */

    if (facetsByGroup.isEmpty()) {
      facetHeader.visibility = View.GONE
      return
    }

    /*
     * If one of the groups is an entry point, display it as a set of tabs. Otherwise, hide
     * the tab layout entirely.
     */

    this.configureFacetTabs(FeedFacets.findEntryPointFacetGroup(facetsByGroup), facetTabs)

    /*
     * Otherwise, for each remaining non-entrypoint facet group, show a drop-down menu allowing
     * the selection of individual facets. If there are no remaining groups, hide the button
     * bar.
     */

    val remainingGroups = facetsByGroup
      .filter { entry ->
        /*
         * SIMPLY-2923: Hide the 'Collection' Facet until approved by UX.
         */
        entry.key != "Collection"
      }
      .filter { entry ->
        !FeedFacets.facetGroupIsEntryPointTyped(entry.value)
      }

    if (remainingGroups.isEmpty()) {
      facetLayoutScroller.visibility = View.GONE
      return
    }

    val buttonLayoutParams =
      LinearLayout.LayoutParams(
        this.screenInformation.dpToPixels(96).toInt(),
        LinearLayout.LayoutParams.WRAP_CONTENT
      )

    val textLayoutParams =
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      )

    textLayoutParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL

    val spacerLayoutParams =
      LinearLayout.LayoutParams(
        this.screenInformation.dpToPixels(8).toInt(),
        LinearLayout.LayoutParams.MATCH_PARENT
      )

    val sortedNames = remainingGroups.keys.sorted()
    val context = this.requireContext()

    facetLayout.removeAllViews()
    sortedNames.forEach { groupName ->
      val group = remainingGroups.getValue(groupName)
      if (FeedFacets.facetGroupIsEntryPointTyped(group)) {
        return@forEach
      }

      val button = AppCompatButton(context)
      val buttonLabel = AppCompatTextView(context)
      val spaceStart = Space(context)
      val spaceMiddle = Space(context)
      val spaceEnd = Space(context)

      val active =
        group.find { facet -> facet.isActive }
          ?: group.firstOrNull()

      buttonLayoutParams.weight = 1.0f
      button.id = View.generateViewId()
      button.layoutParams = buttonLayoutParams
      button.text = active?.title
      button.ellipsize = TextUtils.TruncateAt.END
      button.setOnClickListener {
        this.showFacetSelectDialog(groupName, group)
      }

      spaceStart.layoutParams = spacerLayoutParams
      spaceMiddle.layoutParams = spacerLayoutParams
      spaceEnd.layoutParams = spacerLayoutParams

      buttonLabel.layoutParams = textLayoutParams
      buttonLabel.text = "$groupName: "
      buttonLabel.labelFor = button.id
      buttonLabel.maxLines = 1
      buttonLabel.ellipsize = TextUtils.TruncateAt.END
      buttonLabel.textAlignment = TEXT_ALIGNMENT_TEXT_END
      buttonLabel.gravity = Gravity.END or Gravity.CENTER_VERTICAL

      facetLayout.addView(spaceStart)
      facetLayout.addView(buttonLabel)
      facetLayout.addView(spaceMiddle)
      facetLayout.addView(button)
      facetLayout.addView(spaceEnd)
    }

    facetLayoutScroller.scrollTo(0, 0)
  }

  private fun configureFacetTabs(
    facetGroup: List<FeedFacet>?,
    facetTabs: RadioGroup
  ) {
    if (facetGroup == null) {
      facetTabs.visibility = View.GONE
      return
    }

    /*
     * Add a set of radio buttons to the view.
     */

    facetTabs.removeAllViews()
    val size = facetGroup.size
    for (index in 0 until size) {
      val facet = facetGroup[index]
      val button = RadioButton(this.requireContext())
      val buttonLayout =
        LinearLayout.LayoutParams(
          this.screenInformation.dpToPixels(160).toInt(),
          ViewGroup.LayoutParams.MATCH_PARENT,
          1.0f / size.toFloat()
        )

      button.layoutParams = buttonLayout
      button.gravity = Gravity.CENTER
      button.maxLines = 1
      button.ellipsize = TextUtils.TruncateAt.END

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
      button.setOnClickListener {
        this.logger.debug("selected entry point facet: {}", facet.title)
        this.listener.post(
          CatalogFeedEvent.OpenFeed(this.feedModel.resolveFacet(facet))
        )
      }
      facetTabs.addView(button)
    }

    /*
     * Uncheck all of the buttons, and then check the one that corresponds to the current
     * active facet.
     */

    facetTabs.clearCheck()

    for (index in 0 until size) {
      val facet = facetGroup[index]
      val button = facetTabs.getChildAt(index) as RadioButton

      if (facet.isActive) {
        this.logger.debug("active entry point facet: {}", facet.title)
        facetTabs.check(button.id)
      }
    }
  }

  private fun colorStateListForFacetTabs(): ColorStateList {
    val activity = this.requireActivity()

    val states =
      arrayOf(
        intArrayOf(android.R.attr.state_checked),
        intArrayOf(-android.R.attr.state_checked)
      )

    val colors =
      intArrayOf(
        ContextCompat.getColor(activity, R.color.simplifiedColorBackground),
        ThemeControl.resolveColorAttribute(activity.theme, R.attr.colorPrimary)
      )

    return ColorStateList(states, colors)
  }

  @UiThread
  private fun showFacetSelectDialog(
    groupName: String,
    group: List<FeedFacet>
  ) {
    val choices = group.sortedBy { it.title }
    val names = choices.map { it.title }.toTypedArray()
    val checkedItem = choices.indexOfFirst { it.isActive }

    // Build the dialog
    val alertBuilder = AlertDialog.Builder(this.requireContext())
    alertBuilder.setTitle(groupName)
    alertBuilder.setSingleChoiceItems(names, checkedItem) { dialog, checked ->
      val selected = choices[checked]
      this.logger.debug("selected facet: {}", selected)
      this.listener.post(
        CatalogFeedEvent.OpenFeed(this.feedModel.resolveFacet(selected))
      )
      dialog.dismiss()
    }
    alertBuilder.create().show()
  }

  private fun errorPageParameters(
    failure: FeedLoaderFailure
  ): ErrorPageParameters {
    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep(this.resources.getString(R.string.catalogFeedLoading))
    taskRecorder.addAttributes(failure.attributes)
    taskRecorder.currentStepFailed(failure.message, "feedLoadingFailed", failure.exception)
    val taskFailure = taskRecorder.finishFailure<Unit>()

    return ErrorPageParameters(
      emailAddress = this.configurationService.supportErrorReportEmailAddress,
      body = "",
      subject = this.configurationService.supportErrorReportSubject,
      attributes = taskFailure.attributes.toSortedMap(),
      taskSteps = taskFailure.steps
    )
  }

  private fun isAccountCatalogRoot(): Boolean {
    val parameters = this.parameters
    if (parameters !is CatalogFeedArgumentsRemote) {
      return false
    }

    val ownership = this.parameters.ownership
    if (ownership !is OwnedByAccount) {
      return false
    }

    val accountProvider =
      this.profilesController.profileCurrent()
        .account(ownership.accountId)
        .provider

    return accountProvider.feedIsRoot(parameters.feedURI)
  }

  override fun onBirthYearSelected(isOver13: Boolean) {
    feedModel.updateBirthYear(isOver13)
  }
}
