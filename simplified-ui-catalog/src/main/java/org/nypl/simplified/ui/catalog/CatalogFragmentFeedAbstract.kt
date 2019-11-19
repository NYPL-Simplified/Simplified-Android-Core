package org.nypl.simplified.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.librarysimplified.services.api.ServiceDirectoryProviderType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoadFailed
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedNavigation
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoaded.CatalogFeedWithoutGroups
import org.nypl.simplified.ui.catalog.CatalogFeedState.CatalogFeedLoading
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

/**
 * The base type of feed fragments. This class is abstract purely because the AndroidX
 * ViewModel API requires that we fetch view models by class, and we need to store separate view
 * models for each of the different app sections that want to display feeds.
 */

abstract class CatalogFragmentFeedAbstract<T : CatalogFeedViewModelAbstract> : Fragment() {

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
  private lateinit var host: ServiceDirectoryProviderType
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType
  private var feedStatusSubscription: ObservableSubscriptionType<Unit>? = null

  /**
   * The precise type of view model used for implementations of this class.
   */

  abstract val viewModelClass: Class<T>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val context = this.requireContext()
    if (context is ServiceDirectoryProviderType) {
      this.host = context
    } else {
      throw IllegalStateException(
        "The context hosting this fragment must implement ${ServiceDirectoryProviderType::class.java}")
    }

    this.profilesController =
      this.host.serviceDirectory.requireService(ProfilesControllerType::class.java)
    this.configurationService =
      this.host.serviceDirectory.requireService(CatalogConfigurationServiceType::class.java)
    this.catalogNavigation =
      this.host.serviceDirectory.requireService(CatalogNavigationControllerType::class.java)
    this.feedLoader =
      this.host.serviceDirectory.requireService(FeedLoaderType::class.java)
    this.uiThread =
      this.host.serviceDirectory.requireService(UIThreadServiceType::class.java)

    this.feedWithGroupsData = mutableListOf()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
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

    this.feedWithGroupsList =
      this.feedWithGroups.findViewById(R.id.feedWithGroupsList)

    this.feedWithGroupsList.setHasFixedSize(true)
    this.feedWithGroupsList.layoutManager = LinearLayoutManager(this.context)
    (this.feedWithGroupsList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

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
        this.requireActivity(),
        CatalogFeedViewModelFactory(this.requireContext(), this.host.serviceDirectory))
        .get(this.viewModelClass)
        as CatalogFeedViewModelType

    /*
     * Configure the lanes based on the viewmodel.
     */

    this.feedWithGroupsAdapter =
      CatalogFeedWithGroupsAdapter(
        groups = this.feedWithGroupsData,
        uiThread = this.uiThread,
        context = this.requireContext(),
        coverLoader = this.host.serviceDirectory.requireService(BookCoverProviderType::class.java),
        onBookSelected = this.catalogNavigation::openBookDetail,
        onFeedSelected = { title, uri ->
          this.feedModel.resolveAndLoadFeed(
            title = title,
            uri = uri,
            isSearchResults = false
          )
        }
      )

    this.feedWithGroupsList.adapter =
      this.feedWithGroupsAdapter

    this.feedStatusSubscription =
      this.feedModel.feedStatus.subscribe {
        this.uiThread.runOnUIThread {
          this.reconfigureUI(this.feedModel.feedState())
        }
      }

    this.reconfigureUI(this.feedModel.feedState())
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

    this.feedStatusSubscription?.unsubscribe()
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
