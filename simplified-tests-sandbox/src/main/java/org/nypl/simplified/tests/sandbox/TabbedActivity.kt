package org.nypl.simplified.tests.sandbox

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.google.common.util.concurrent.MoreExecutors
import com.pandora.bottomnavigator.BottomNavigator
import leakcanary.AppWatcher
import leakcanary.LeakCanary
import org.librarysimplified.services.api.ServiceDirectoryProviderType
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountProviderImmutable
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverBadge
import org.nypl.simplified.books.covers.BookCoverBadgeLookupType
import org.nypl.simplified.books.covers.BookCoverGenerator
import org.nypl.simplified.books.covers.BookCoverProvider
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.documents.store.DocumentStoreType
import org.nypl.simplified.feeds.api.FeedBooksSelection
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedFacet
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.tenprint.TenPrintGenerator
import org.nypl.simplified.tests.MockBooksController
import org.nypl.simplified.tests.MockDocumentStore
import org.nypl.simplified.tests.MockProfilesController
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.nypl.simplified.ui.catalog.CatalogConfigurationServiceType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetailParameters
import org.nypl.simplified.ui.catalog.CatalogFragmentFeed
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageListenerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.host.HostViewModel
import org.nypl.simplified.ui.host.HostViewModelFactory
import org.nypl.simplified.ui.host.HostViewModelType
import org.nypl.simplified.ui.screen.ScreenSizeInformation
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.settings.SettingsFragmentAccount
import org.nypl.simplified.ui.settings.SettingsFragmentAccountParameters
import org.nypl.simplified.ui.settings.SettingsFragmentAccounts
import org.nypl.simplified.ui.settings.SettingsFragmentCustomOPDS
import org.nypl.simplified.ui.settings.SettingsFragmentMain
import org.nypl.simplified.ui.settings.SettingsFragmentVersion
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.theme.ThemeControl
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.net.URI

class TabbedActivity : AppCompatActivity(), ToolbarHostType, ErrorPageListenerType {

  private val logger = LoggerFactory.getLogger(TabbedActivity::class.java)

  private lateinit var navigator: BottomNavigator

  companion object {
//    val targetURI = URI.create(
//      "https://circulation.librarysimplified.org/NYNYPL/feed/1?entrypoint=Book")

    val targetURI = URI.create(
      "https://circulation.librarysimplified.org/NYNYPL/")
  }

  class Services(
    private val context: Context
  ) : ServiceDirectoryProviderType {

    lateinit var profiles: MockProfilesController

    @Volatile
    private var services: ServiceDirectoryType? = null

    override fun serviceDirectory(): ServiceDirectoryType {
      return this.services
        ?: this.run {
          val newServices = this.createServices()
          this.services = newServices
          newServices
        }
    }

    private fun createServices(): ServiceDirectoryType {
      val newServices = MutableServiceDirectory()

      val executor =
        NamedThreadPools.namedThreadPool(4, "bg", 19)

      this.profiles =
        MockProfilesController(
          profileCount = 2,
          accountCount = 3
        )

      this.profiles.profileAccountCurrent()
        .setAccountProvider(
          AccountProviderImmutable.copy(this.profiles.profileAccountCurrent().provider)
            .copy(
              catalogURI = org.nypl.simplified.tests.sandbox.TabbedActivity.Companion.targetURI,
              displayName = "The New York Public Library",
              subtitle = "Inspiring lifelong learning, advancing knowledge, and strengthening our communities."
            )
        )

      val bookController =
        MockBooksController()

      val bookRegistry =
        BookRegistry.create()

      val documentStore =
        MockDocumentStore()

      val feedLoader =
        FeedLoader.create(
          exec = MoreExecutors.listeningDecorator(executor),
          parser = OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes())),
          searchParser = OPDSSearchParser.newParser(),
          transport = FeedHTTPTransport.newTransport(HTTP.newHTTP()),
          bookRegistry = bookRegistry,
          bundledContent = BundledContentResolverType { null }
        )

      val coverLoader =
        BookCoverProvider.newCoverProvider(
          context = this.context,
          bookRegistry = bookRegistry,
          coverGenerator = BookCoverGenerator(TenPrintGenerator.newGenerator()),
          badgeLookup = object : BookCoverBadgeLookupType {
            override fun badgeForEntry(entry: FeedEntry.FeedEntryOPDS): BookCoverBadge? {
              return null
            }
          },
          executor = executor,
          debugCacheIndicators = true,
          debugLogging = false
        )

      val buildConfig =
        object: BuildConfigurationServiceType {
          override val vcsCommit: String
            get() = "deadbeef"
          override val errorReportEmail: String
            get() = "someone@example.com"

        }

      newServices.putService(
        interfaceType = BuildConfigurationServiceType::class.java,
        service = buildConfig
      )
      newServices.putService(
        interfaceType = DocumentStoreType::class.java,
        service = documentStore
      )
      newServices.putService(
        interfaceType = BookCoverProviderType::class.java,
        service = coverLoader
      )
      newServices.putService(
        interfaceType = FeedLoaderType::class.java,
        service = feedLoader
      )
      newServices.putService(
        interfaceType = CatalogConfigurationServiceType::class.java,
        service = object : CatalogConfigurationServiceType {
          override val supportErrorReportEmailAddress: String
            get() = "someone@example.com"
          override val supportErrorReportSubject: String
            get() = "[printer on fire]"
          override val showAllCollectionsInLocalFeeds: Boolean
            get() = true
        }
      )
      newServices.putService(
        interfaceType = ProfilesControllerType::class.java,
        service = this.profiles
      )
      newServices.putService(
        interfaceType = UIThreadServiceType::class.java,
        service = object : UIThreadServiceType {}
      )
      newServices.putService(
        interfaceType = BookRegistryType::class.java,
        service = bookRegistry
      )
      newServices.putService(
        interfaceType = BookRegistryReadableType::class.java,
        service = bookRegistry
      )
      newServices.putService(
        interfaceType = ScreenSizeInformationType::class.java,
        service = ScreenSizeInformation(this.context.resources)
      )
      newServices.putService(
        interfaceType = BooksControllerType::class.java,
        service = bookController
      )
      return newServices
    }
  }

  class SettingsNavigationController(
    private val navigator: BottomNavigator
  ) : SettingsNavigationControllerType {

    override fun backStackSize(): Int {
      return this.navigator.stackSize(this.navigator.currentTab())
    }

    override fun openSettingsAbout() {

    }

    override fun openSettingsAccount(id: AccountID) {
      this.navigator.addFragment(
        fragment = SettingsFragmentAccount.create(SettingsFragmentAccountParameters(id)),
        tab = this.navigator.currentTab()
      )
    }

    override fun openSettingsAccounts() {
      this.navigator.addFragment(
        fragment = SettingsFragmentAccounts(),
        tab = this.navigator.currentTab()
      )
    }

    override fun openSettingsAcknowledgements() {

    }

    override fun openSettingsEULA() {

    }

    override fun openSettingsFaq() {

    }

    override fun openSettingsLicense() {

    }

    override fun openSettingsVersion() {
      this.navigator.addFragment(
        fragment = SettingsFragmentVersion(),
        tab = this.navigator.currentTab()
      )
    }

    override fun popBackStack(): Boolean {
      return this.navigator.pop()
    }

    override fun openSettingsCustomOPDS() {
      this.navigator.addFragment(
        fragment = SettingsFragmentCustomOPDS(),
        tab = this.navigator.currentTab()
      )
    }

    override fun <E : PresentableErrorType> openErrorPage(parameters: ErrorPageParameters<E>) {
      this.navigator.addFragment(
        fragment = ErrorPageFragment.create(parameters),
        tab = this.navigator.currentTab()
      )
    }
  }

  class CatalogNavigationController(
    private val services: Services,
    private val navigator: BottomNavigator
  ) : CatalogNavigationControllerType {

    override fun backStackSize(): Int {
      return this.navigator.stackSize(this.navigator.currentTab())
    }

    override fun openEPUBReader(
      book: Book,
      format: BookFormat.BookFormatEPUB
    ) {

    }

    override fun openAudioBookListener(
      book: Book,
      format: BookFormat.BookFormatAudioBook
    ) {

    }

    override fun openPDFReader(
      book: Book,
      format: BookFormat.BookFormatPDF
    ) {

    }

    override fun openFeed(feedArguments: CatalogFeedArguments) {
      this.navigator.addFragment(
        fragment = CatalogFragmentFeed.create(feedArguments),
        tab = this.navigator.currentTab()
      )
    }

    override fun popBackStack(): Boolean {
      return this.navigator.pop()
    }

    override fun openBookDetail(entry: FeedEntry.FeedEntryOPDS) {
      val parameters =
        CatalogFragmentBookDetailParameters(
          accountId = this.services.profiles.profileAccountCurrent().id,
          feedEntry = entry
        )

      this.navigator.addFragment(
        fragment = CatalogFragmentBookDetail.create(parameters),
        tab = this.navigator.currentTab()
      )
    }

    override fun <E : PresentableErrorType> openErrorPage(parameters: ErrorPageParameters<E>) {
      this.navigator.addFragment(
        fragment = ErrorPageFragment.create(parameters),
        tab = this.navigator.currentTab()
      )
    }
  }

  private lateinit var model: HostViewModelType

  override fun onCreate(savedInstanceState: Bundle?) {

    LeakCanary.config =
      LeakCanary.config.copy(
        dumpHeap = true
      )

    AppWatcher.config =
      AppWatcher.config.copy(
        enabled = true,
        watchActivities = true,
        watchFragments = true,
        watchDurationMillis = 1_000L,
        watchFragmentViews = true
      )

    val services = Services(this.applicationContext)
    services.serviceDirectory()

    this.model =
      ViewModelProviders.of(this, HostViewModelFactory(services))
        .get(HostViewModel::class.java)

    super.onCreate(savedInstanceState)
    this.setContentView(R.layout.tabbed_host)

    this.toolbar = this.findViewById(R.id.toolbar)
    this.toolbar.overflowIcon = this.getDrawable(R.drawable.toolbar_overflow)

    this.toolbar.menu.clear()
    this.toolbar.inflateMenu(R.menu.catalog)

    this.toolbar.title =
      services.profiles.profileAccountCurrent().provider.displayName
    this.toolbar.subtitle =
      services.profiles.profileAccountCurrent().provider.subtitle

    this.toolbar.setTitleTextAppearance(
      this, R.style.SimplifiedTitleTextAppearance)
    this.toolbar.setSubtitleTextAppearance(
      this, R.style.SimplifiedSubTitleTextAppearance)

    val navigationView =
      this.findViewById<BottomNavigationView>(R.id.bottomNavigator)

    this.navigator = BottomNavigator.onCreate(
      fragmentContainer = R.id.fragmentHolder,
      bottomNavigationView = navigationView,
      rootFragmentsFactory = mapOf(
        R.id.tabCatalog to {
          this.createCatalogFragment(services.profiles)
        },
        R.id.tabBooks to {
          this.createBooksFragment(services.profiles)
        },
        R.id.tabHolds to {
          this.createHoldsFragment(services.profiles)
        },
        R.id.tabSettings to {
          this.createSettingsFragment()
        }
      ),
      defaultTab = R.id.tabCatalog,
      activity = this
    )

    navigationView.itemIconTintList = this.colorStateListForTabs()
    navigationView.itemTextColor = this.colorStateListForTabs()
    navigationView.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED

    this.model.updateNavigationController(
      navigationInterface = CatalogNavigationControllerType::class.java,
      navigationInstance = CatalogNavigationController(services, this.navigator)
    )
    this.model.updateNavigationController(
      navigationInterface = SettingsNavigationControllerType::class.java,
      navigationInstance = SettingsNavigationController(this.navigator)
    )
  }

  private fun colorStateListForTabs(): ColorStateList {
    val states =
      arrayOf(
        intArrayOf(android.R.attr.state_checked),
        intArrayOf(-android.R.attr.state_checked))

    val colors =
      intArrayOf(
        ThemeControl.resolveColorAttribute(this.theme, R.attr.colorPrimary),
        Color.DKGRAY)

    return ColorStateList(states, colors)
  }

  private fun createSettingsFragment(): Fragment {
    return SettingsFragmentMain()
  }

  private fun createHoldsFragment(profiles: MockProfilesController): Fragment {
    val account = profiles.profileAccountCurrent()
    return CatalogFragmentFeed.create(CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
      title = "Holds",
      facetType = FeedFacet.FeedFacetPseudo.FacetType.SORT_BY_TITLE,
      searchTerms = null,
      selection = FeedBooksSelection.BOOKS_FEED_HOLDS,
      accountId = account.id
    ))
  }

  private fun createBooksFragment(profiles: MockProfilesController): Fragment {
    val account = profiles.profileAccountCurrent()
    return CatalogFragmentFeed.create(CatalogFeedArguments.CatalogFeedArgumentsLocalBooks(
      title = "Books",
      facetType = FeedFacet.FeedFacetPseudo.FacetType.SORT_BY_TITLE,
      searchTerms = null,
      selection = FeedBooksSelection.BOOKS_FEED_LOANED,
      accountId = account.id
    ))
  }

  private fun createCatalogFragment(profiles: MockProfilesController): Fragment {
    val account = profiles.profileAccountCurrent()
    return CatalogFragmentFeed.create(CatalogFeedArguments.CatalogFeedArgumentsRemote(
      title = account.provider.displayName,
      feedURI = account.provider.catalogURI,
      isSearchResults = false,
      accountId = account.id
    ))
  }

  override lateinit var toolbar: Toolbar

  override fun onStop() {
    super.onStop()

    this.model.removeNavigationController(CatalogNavigationControllerType::class.java)
  }

  override fun onBackPressed() {
    if (!this.navigator.pop()) {
      super.onBackPressed()
    }
  }

  override fun onErrorPageSendReport(parameters: ErrorPageParameters<*>) {

  }
}
