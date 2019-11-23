package org.nypl.simplified.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.Disposable
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoadFailed
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedNavigation
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoading
import org.nypl.simplified.ui.host.HostViewModel
import org.nypl.simplified.ui.host.HostViewModelReadableType
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * The base type of feed fragments. This class is abstract purely because the AndroidX
 * ViewModel API requires that we fetch view models by class, and we need to store separate view
 * models for each of the different app sections that want to display feeds.
 */

class CatalogFragmentFeed : Fragment() {

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

  private lateinit var bookCovers: BookCoverProviderType
  private lateinit var bookRegistry: BookRegistryReadableType
  private lateinit var buttonCreator: CatalogButtons
  private lateinit var catalogNavigation: CatalogNavigationControllerType
  private lateinit var configurationService: CatalogConfigurationServiceType
  private lateinit var feedError: ViewGroup
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var feedLoading: ViewGroup
  private lateinit var feedModel: CatalogFeedViewModelType
  private lateinit var feedNavigation: ViewGroup
  private lateinit var feedWithGroups: ViewGroup
  private lateinit var feedWithGroupsAdapter: CatalogFeedWithGroupsAdapter
  private lateinit var feedWithGroupsData: MutableList<FeedGroup>
  private lateinit var feedWithGroupsList: RecyclerView
  private lateinit var feedWithoutGroups: ViewGroup
  private lateinit var feedWithoutGroupsAdapter: CatalogPagedAdapter
  private lateinit var feedWithoutGroupsList: RecyclerView
  private lateinit var hostModel: HostViewModelReadableType
  private lateinit var loginDialogModel: CatalogLoginViewModel
  private lateinit var parameters: CatalogFeedArguments
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var screenInformation: ScreenSizeInformationType
  private lateinit var uiThread: UIThreadServiceType
  private val logger = LoggerFactory.getLogger(CatalogFragmentFeed::class.java)
  private val parametersId = PARAMETERS_ID
  private var feedStatusSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.parameters = this.arguments!![this.parametersId] as CatalogFeedArguments
    this.feedWithGroupsData = mutableListOf()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    this.hostModel =
      ViewModelProviders.of(this.requireActivity())
        .get(HostViewModel::class.java)

    this.bookCovers =
      this.hostModel.services.requireService(BookCoverProviderType::class.java)
    this.bookRegistry =
      this.hostModel.services.requireService(BookRegistryReadableType::class.java)
    this.screenInformation =
      this.hostModel.services.requireService(ScreenSizeInformationType::class.java)
    this.profilesController =
      this.hostModel.services.requireService(ProfilesControllerType::class.java)
    this.configurationService =
      this.hostModel.services.requireService(CatalogConfigurationServiceType::class.java)
    this.feedLoader =
      this.hostModel.services.requireService(FeedLoaderType::class.java)
    this.uiThread =
      this.hostModel.services.requireService(UIThreadServiceType::class.java)

    this.catalogNavigation =
      this.hostModel.navigationController(CatalogNavigationControllerType::class.java)

    this.buttonCreator =
      CatalogButtons(this.requireContext(), this.screenInformation)

    val layout =
      inflater.inflate(R.layout.feed, container, false)

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

    this.feedWithGroupsList = this.feedWithGroups.findViewById(R.id.feedWithGroupsList)
    this.feedWithGroupsList.setHasFixedSize(true)
    this.feedWithGroupsList.setItemViewCacheSize(32)
    this.feedWithGroupsList.layoutManager = LinearLayoutManager(this.context)
    (this.feedWithGroupsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    this.feedWithGroupsList.addItemDecoration(
      CatalogFeedWithGroupsDecorator(this.screenInformation.dpToPixels(16).toInt())
    )

    this.feedWithoutGroupsList = this.feedWithoutGroups.findViewById(R.id.feedWithoutGroupsList)
    this.feedWithoutGroupsList.setHasFixedSize(true)
    this.feedWithoutGroupsList.setItemViewCacheSize(32)
    this.feedWithoutGroupsList.layoutManager = LinearLayoutManager(this.context)
    (this.feedWithoutGroupsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

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
      ViewModelProviders.of(
        this,
        CatalogFeedViewModelFactory(
          context = this.requireContext(),
          services = this.hostModel.services,
          feedArguments = this.parameters
        ))
        .get(CatalogFeedViewModel::class.java)

    this.loginDialogModel =
      ViewModelProviders.of(this.requireActivity())
        .get(CatalogLoginViewModel::class.java)

    /*
     * Configure the lanes based on the viewmodel.
     */

    this.feedWithGroupsAdapter =
      CatalogFeedWithGroupsAdapter(
        groups = this.feedWithGroupsData,
        uiThread = this.uiThread,
        coverLoader = this.bookCovers,
        onFeedSelected = this::onFeedSelected,
        onBookSelected = this::onBookSelected
      )
    this.feedWithGroupsList.adapter = this.feedWithGroupsAdapter

    this.feedStatusSubscription =
      this.feedModel.feedStatus.subscribe {
        this.uiThread.runOnUIThread {
          this.reconfigureUI(this.feedModel.feedState())
        }
      }

    this.feedWithGroupsList.layoutManager!!.onRestoreInstanceState(
      this.feedModel.restoreFeedWithGroupsViewState())
    this.feedWithoutGroupsList.layoutManager!!.onRestoreInstanceState(
      this.feedModel.restoreFeedWithoutGroupsViewState())

    this.reconfigureUI(this.feedModel.feedState())
  }

  private fun onBookSelected(opdsEntry: FeedEntry.FeedEntryOPDS) {
    this.catalogNavigation.openBookDetail(opdsEntry)
  }

  private fun onFeedSelected(title: String, uri: URI) {
    this.catalogNavigation.openFeed(this.feedModel.resolveFeed(title, uri, false))
  }

  @UiThread
  private fun reconfigureUI(feedState: CatalogFeedState) {
    this.uiThread.checkIsUIThread()

    return when (feedState) {
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
    }
  }

  override fun onStop() {
    super.onStop()

    this.feedModel.saveFeedWithGroupsViewState(
      this.feedWithGroupsList.layoutManager!!.onSaveInstanceState())
    this.feedModel.saveFeedWithoutGroupsViewState(
      this.feedWithoutGroupsList.layoutManager!!.onSaveInstanceState())

    /*
     * We aggressively unset adapters here in order to try to encourage prompt unsubscription
     * of views from the book registry.
     */

    this.feedWithoutGroupsList.adapter = null
    this.feedWithGroupsList.adapter = null
    this.feedStatusSubscription?.dispose()
  }

  @UiThread
  private fun onCatalogFeedLoadingUI(feedState: CatalogFeedLoading) {
    this.uiThread.checkIsUIThread()

    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.VISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE
  }

  @UiThread
  private fun onCatalogFeedNavigationUI(feedState: CatalogFeedNavigation) {
    this.uiThread.checkIsUIThread()

    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.VISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE
  }

  @UiThread
  private fun onCatalogFeedWithoutGroupsUI(feedState: CatalogFeedWithoutGroups) {
    this.uiThread.checkIsUIThread()

    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.VISIBLE

    this.feedWithoutGroupsAdapter =
      CatalogPagedAdapter(
        bookCovers = this.bookCovers,
        bookRegistry = this.bookRegistry,
        buttonCreator = this.buttonCreator,
        context = this.requireContext(),
        fragmentManager = this.requireFragmentManager(),
        loginViewModel = this.loginDialogModel,
        onBookSelected = this::onBookSelected,
        profilesController = this.profilesController,
        uiThread = this.uiThread
      )

    this.feedWithoutGroupsList.adapter = this.feedWithoutGroupsAdapter
    feedState.pagedList.observe(this, Observer { newPagedList ->
      this.logger.debug("received paged list ({} elements)", newPagedList.size)
      this.feedWithoutGroupsAdapter.submitList(newPagedList)
    })
  }

  @UiThread
  private fun onCatalogFeedWithGroupsUI(feedState: CatalogFeedWithGroups) {
    this.uiThread.checkIsUIThread()

    this.feedError.visibility = View.INVISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.VISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE

    this.feedWithGroupsData.clear()
    this.feedWithGroupsData.addAll(feedState.feed.feedGroupsInOrder)
    this.feedWithGroupsAdapter.notifyDataSetChanged()
  }

  @UiThread
  private fun onCatalogFeedLoadFailed(feedState: CatalogFeedLoadFailed) {
    this.uiThread.checkIsUIThread()

    this.feedError.visibility = View.VISIBLE
    this.feedLoading.visibility = View.INVISIBLE
    this.feedNavigation.visibility = View.INVISIBLE
    this.feedWithGroups.visibility = View.INVISIBLE
    this.feedWithoutGroups.visibility = View.INVISIBLE
  }
}
