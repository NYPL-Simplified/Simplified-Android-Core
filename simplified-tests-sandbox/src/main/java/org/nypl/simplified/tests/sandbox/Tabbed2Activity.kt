package org.nypl.simplified.tests.sandbox

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.util.concurrent.MoreExecutors
import leakcanary.AppWatcher
import leakcanary.LeakCanary
import org.librarysimplified.services.api.ServiceDirectory
import org.librarysimplified.services.api.ServiceDirectoryType
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountProviderImmutable
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
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.http.core.HTTP
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.tenprint.TenPrintGenerator
import org.nypl.simplified.tests.MockBooksController
import org.nypl.simplified.tests.MockDocumentStore
import org.nypl.simplified.tests.MockProfilesController
import org.nypl.simplified.threads.NamedThreadPools
import org.nypl.simplified.ui.catalog.CatalogConfigurationServiceType
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageListenerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.images.ImageLoader
import org.nypl.simplified.ui.images.ImageLoaderType
import org.nypl.simplified.ui.screen.ScreenSizeInformation
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigationController
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

class Tabbed2Activity : AppCompatActivity(), ToolbarHostType, ErrorPageListenerType {

  private val logger = LoggerFactory.getLogger(Tabbed2Activity::class.java)

  companion object {
    val targetURI = URI.create(
      "https://circulation.librarysimplified.org/NYNYPL/")
  }

  private lateinit var toolbar: Toolbar
  private lateinit var navigationController: TabbedNavigationController
  private lateinit var navigationControllers: NavigationControllerDirectoryType

  override fun findToolbar(): Toolbar {
    this.toolbar.overflowIcon = this.getDrawable(R.drawable.toolbar_overflow)
    this.toolbar.setTitleTextAppearance(this, R.style.SimplifiedTitleTextAppearance)
    this.toolbar.setTitleTextColor(Color.WHITE)
    this.toolbar.setSubtitleTextAppearance(this, R.style.SimplifiedSubTitleTextAppearance)
    this.toolbar.setSubtitleTextColor(Color.WHITE)
    return this.toolbar
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    this.logger.debug("onCreate")
    super.onCreate(savedInstanceState)
    this.logger.debug("onCreate (super completed)")

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

    val services =
      if (Services.isInitialized()) {
        Services.serviceDirectory()
      } else {
        this.initializeServices()
      }

    this.navigationControllers =
      NavigationControllers.findDirectory(this)

    this.setContentView(R.layout.tabbed_host)

    this.toolbar = this.findViewById(R.id.toolbar)
    this.toolbar.menu.clear()
    this.toolbar.inflateMenu(R.menu.catalog)
    this.toolbar.title =
      services.requireService(ProfilesControllerType::class.java)
        .profileAccountCurrent()
        .provider
        .displayName

    this.toolbar.subtitle =
      services.requireService(ProfilesControllerType::class.java)
        .profileAccountCurrent()
        .provider
        .subtitle

    this.recreateNavigationController(this.findViewById(R.id.bottomNavigator))
    this.logger.debug("onCreate completed")
  }

  private fun initializeServices(): ServiceDirectoryType {
    val newServices = ServiceDirectory.builder()

    val executor =
      NamedThreadPools.namedThreadPool(4, "bg", 19)

    val profiles =
      MockProfilesController(
        profileCount = 2,
        accountCount = 3
      )

    profiles.profileAccountCurrent()
      .setAccountProvider(
        AccountProviderImmutable.copy(profiles.profileAccountCurrent().provider)
          .copy(
            catalogURI = targetURI,
            displayName = "The New York Public Library",
            subtitle = "Inspiring lifelong learning, advancing knowledge, and strengthening our communities.",
            logo = URI.create("http://www.librarysimplified.org/assets/logos/simplye2.png")
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
        bundledContent = BundledContentResolverType { null },
        contentResolver = this.applicationContext.contentResolver
      )

    val coverLoader =
      BookCoverProvider.newCoverProvider(
        context = this.applicationContext,
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
      object : BuildConfigurationServiceType {
        override val vcsCommit: String
          get() = "deadbeef"
        override val errorReportEmail: String
          get() = "someone@example.com"
      }

    val imageLoader =
      ImageLoader.create(this.applicationContext)

    newServices.addService(
      interfaceType = ImageLoaderType::class.java,
      service = imageLoader
    )
    newServices.addService(
      interfaceType = BuildConfigurationServiceType::class.java,
      service = buildConfig
    )
    newServices.addService(
      interfaceType = DocumentStoreType::class.java,
      service = documentStore
    )
    newServices.addService(
      interfaceType = BookCoverProviderType::class.java,
      service = coverLoader
    )
    newServices.addService(
      interfaceType = FeedLoaderType::class.java,
      service = feedLoader
    )
    newServices.addService(
      interfaceType = CatalogConfigurationServiceType::class.java,
      service = object : CatalogConfigurationServiceType {
        override val showSettingsTab: Boolean
          get() = true
        override val showHoldsTab: Boolean
          get() = true
        override val supportErrorReportEmailAddress: String
          get() = "someone@example.com"
        override val supportErrorReportSubject: String
          get() = "[printer on fire]"
        override val showAllCollectionsInLocalFeeds: Boolean
          get() = true
      }
    )
    newServices.addService(
      interfaceType = ProfilesControllerType::class.java,
      service = profiles
    )
    newServices.addService(
      interfaceType = UIThreadServiceType::class.java,
      service = object : UIThreadServiceType {}
    )
    newServices.addService(
      interfaceType = BookRegistryType::class.java,
      service = bookRegistry
    )
    newServices.addService(
      interfaceType = BookRegistryReadableType::class.java,
      service = bookRegistry
    )
    newServices.addService(
      interfaceType = ScreenSizeInformationType::class.java,
      service = ScreenSizeInformation(this.applicationContext.resources)
    )
    newServices.addService(
      interfaceType = BooksControllerType::class.java,
      service = bookController
    )

    val services = newServices.build()
    Services.initialize(services)
    return services
  }

  override fun onStart() {
    this.logger.debug("onStart (before super)")
    this.recreateNavigationController(this.findViewById(R.id.bottomNavigator))
    super.onStart()
    this.logger.debug("onStart (super completed)")
    this.logger.debug("onStart (completed)")
  }

  override fun onStop() {
    this.logger.debug("onStop (before super)")
    super.onStop()
    this.logger.debug("onStop (super completed)")
    this.navigationControllers.removeNavigationController(CatalogNavigationControllerType::class.java)
    this.navigationControllers.removeNavigationController(SettingsNavigationControllerType::class.java)
    this.logger.debug("onStop (completed)")
  }

  private fun recreateNavigationController(navigationView: BottomNavigationView) {
    val profilesController =
      Services.serviceDirectoryWaiting(30L, TimeUnit.SECONDS)
        .requireService(ProfilesControllerType::class.java)

    this.navigationController =
      TabbedNavigationController.create(
        activity = this,
        profilesController = profilesController,
        fragmentContainerId = R.id.fragmentHolder,
        navigationView = navigationView
      )
    this.navigationControllers.updateNavigationController(
      navigationInterface = CatalogNavigationControllerType::class.java,
      navigationInstance = this.navigationController
    )
    this.navigationControllers.updateNavigationController(
      navigationInterface = SettingsNavigationControllerType::class.java,
      navigationInstance = this.navigationController
    )
  }

  override fun onBackPressed() {
    if (!this.navigationController.popBackStack()) {
      super.onBackPressed()
    }
  }

  override fun onErrorPageSendReport(parameters: ErrorPageParameters<*>) {
  }
}
