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
import org.nypl.simplified.ui.tabs.TabbedNavigationController
import org.nypl.simplified.ui.theme.ThemeControl
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.net.URI

class Tabbed2Activity : AppCompatActivity(), ToolbarHostType, ErrorPageListenerType {

  private val logger = LoggerFactory.getLogger(Tabbed2Activity::class.java)

  private lateinit var navigationController: TabbedNavigationController

  companion object {
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
              catalogURI = org.nypl.simplified.tests.sandbox.Tabbed2Activity.Companion.targetURI,
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

    this.navigationController =
      TabbedNavigationController.create(
        activity = this,
        profilesController = services.profiles,
        fragmentContainerId = R.id.fragmentHolder,
        navigationView = navigationView
      )

    this.model.updateNavigationController(
      navigationInterface = CatalogNavigationControllerType::class.java,
      navigationInstance = this.navigationController
    )
    this.model.updateNavigationController(
      navigationInterface = SettingsNavigationControllerType::class.java,
      navigationInstance = this.navigationController
    )
  }

  override lateinit var toolbar: Toolbar

  override fun onStop() {
    super.onStop()

    this.model.removeNavigationController(CatalogNavigationControllerType::class.java)
  }

  override fun onBackPressed() {
    if (!this.navigationController.popBackStack()) {
      super.onBackPressed()
    }
  }

  override fun onErrorPageSendReport(parameters: ErrorPageParameters<*>) {

  }
}
