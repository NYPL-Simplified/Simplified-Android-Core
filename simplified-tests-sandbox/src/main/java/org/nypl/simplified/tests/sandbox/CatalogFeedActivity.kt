package org.nypl.simplified.tests.sandbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.MoreExecutors
import org.librarysimplified.services.api.ServiceDirectoryProviderType
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
import org.nypl.simplified.feeds.api.FeedEntry
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
import org.nypl.simplified.tests.MockProfilesController
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.ui.catalog.CatalogConfigurationServiceType
import org.nypl.simplified.ui.catalog.CatalogFeedArguments
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail
import org.nypl.simplified.ui.catalog.CatalogFragmentBookDetailParameters
import org.nypl.simplified.ui.catalog.CatalogFragmentFeed
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.screen.ScreenSizeInformation
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import java.net.URI
import java.util.concurrent.Executors

class CatalogFeedActivity : AppCompatActivity(), ServiceDirectoryProviderType {

  private lateinit var fragment: Fragment

  override val serviceDirectory = MutableServiceDirectory()

  private val executor = Executors.newSingleThreadExecutor()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.fragment_host)

    MockProfilesController.profileAccountCurrent()
      .setAccountProvider(
        AccountProviderImmutable.copy(MockProfilesController.profileAccountCurrent().provider)
          .copy(catalogURI = URI.create("https://circulation.librarysimplified.org/"))
      )

    val bookController = MockBooksController()

    val bookRegistry = BookRegistry.create()

    val feedLoader =
      FeedLoader.create(
        exec = MoreExecutors.listeningDecorator(this.executor),
        parser = OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes())),
        searchParser = OPDSSearchParser.newParser(),
        transport = FeedHTTPTransport.newTransport(HTTP.newHTTP()),
        bookRegistry = bookRegistry,
        bundledContent = BundledContentResolverType { null }
      )

    val coverLoader =
      BookCoverProvider.newCoverProvider(
        context = this,
        bookRegistry = bookRegistry,
        coverGenerator = BookCoverGenerator(TenPrintGenerator.newGenerator()),
        badgeLookup = object: BookCoverBadgeLookupType {
          override fun badgeForEntry(entry: FeedEntry.FeedEntryOPDS): BookCoverBadge? {
            return null
          }
        },
        executor = this.executor,
        debugCacheIndicators = true,
        debugLogging = false
      )

    this.serviceDirectory.putService(
      interfaceType = BookCoverProviderType::class.java,
      service = coverLoader
    )
    this.serviceDirectory.putService(
      interfaceType = FeedLoaderType::class.java,
      service = feedLoader
    )
    this.serviceDirectory.putService(
      interfaceType = CatalogConfigurationServiceType::class.java,
      service = object: CatalogConfigurationServiceType {
        override val showAllCollectionsInLocalFeeds: Boolean
          get() = true
      }
    )
    this.serviceDirectory.putService(
      interfaceType = CatalogNavigationControllerType::class.java,
      service = object: CatalogNavigationControllerType {
        override fun openFeed(feedArguments: CatalogFeedArguments) {
          this@CatalogFeedActivity.supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
              R.anim.slide_in_right,
              R.anim.slide_out_left,
              android.R.anim.slide_in_left,
              android.R.anim.slide_out_right
            )
            .addToBackStack(null)
            .replace(R.id.fragmentHolder, CatalogFragmentFeed.create(feedArguments))
            .commit()
        }

        override fun popBackStack() {
          this@CatalogFeedActivity.supportFragmentManager.popBackStack()
        }

        override fun openBookDetail(entry: FeedEntry.FeedEntryOPDS) {
          val parameters =
            CatalogFragmentBookDetailParameters(
              accountId = MockProfilesController.profileAccountCurrent().id,
              feedEntry = entry
            )

          this@CatalogFeedActivity.supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
              R.anim.slide_in_right,
              R.anim.slide_out_left,
              android.R.anim.slide_in_left,
              android.R.anim.slide_out_right
            )
            .addToBackStack(null)
            .replace(R.id.fragmentHolder, CatalogFragmentBookDetail.create(parameters))
            .commit()
        }

        override fun <E : PresentableErrorType> openErrorPage(parameters: ErrorPageParameters<E>) {

        }
      }
    )
    this.serviceDirectory.putService(
      interfaceType = ProfilesControllerType::class.java,
      service = MockProfilesController
    )
    this.serviceDirectory.putService(
      interfaceType = UIThreadServiceType::class.java,
      service = object : UIThreadServiceType {}
    )
    this.serviceDirectory.putService(
      interfaceType = BookRegistryType::class.java,
      service = bookRegistry
    )
    this.serviceDirectory.putService(
      interfaceType = BookRegistryReadableType::class.java,
      service = bookRegistry
    )
    this.serviceDirectory.putService(
      interfaceType = ScreenSizeInformationType::class.java,
      service = ScreenSizeInformation(this.resources)
    )
    this.serviceDirectory.putService(
      interfaceType = BooksControllerType::class.java,
      service = bookController
    )

    this.fragment =
      CatalogFragmentFeed.create(
        CatalogFeedArguments.CatalogFeedArgumentsRemote(
          title = "Catalog",
          feedURI = URI.create("https://circulation.librarysimplified.org/"),
          isSearchResults = false
        )
      )

    this.supportFragmentManager.beginTransaction()
      .setCustomAnimations(
        R.anim.slide_in_right,
        R.anim.slide_out_left,
        android.R.anim.slide_in_left,
        android.R.anim.slide_out_right
      )
      .replace(R.id.fragmentHolder, this.fragment, "MAIN")
      .commit()
  }
}
