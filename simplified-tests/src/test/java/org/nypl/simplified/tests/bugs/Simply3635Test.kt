package org.nypl.simplified.tests.bugs

import android.content.ContentResolver
import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import io.reactivex.subjects.PublishSubject
import okhttp3.internal.closeQuietly
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.librarysimplified.services.api.ServiceDirectory
import org.librarysimplified.services.api.ServiceDirectoryType
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountBundledCredentialsType
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore
import org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty
import org.nypl.simplified.accounts.database.AccountsDatabases
import org.nypl.simplified.accounts.registry.AccountProviderRegistry
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.audio.AudioBookManifests
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.borrowing.internal.BorrowSubtaskDirectory
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.formats.BookFormatSupport
import org.nypl.simplified.books.formats.BookFormatSupportParameters
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.books.formats.api.StandardFormatNames
import org.nypl.simplified.content.api.ContentResolverSane
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.opds.auth_document.AuthenticationDocumentParsers
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.patron.PatronUserProfileParsers
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.ProfilesDatabases
import org.nypl.simplified.profiles.api.ProfileAttributes
import org.nypl.simplified.profiles.api.ProfileCreationEvent
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimer
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.reader.api.ReaderPreferences
import org.nypl.simplified.tests.TestDirectories
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.nypl.simplified.tests.mocking.MockBundledContentResolver
import org.nypl.simplified.tests.mocking.MockStrings
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Simply3635Test {

  private val logger =
    LoggerFactory.getLogger(Simply3635Test::class.java)

  private lateinit var borrowSubtasks: BorrowSubtaskDirectory
  private lateinit var accountBundledCredentials: AccountBundledCredentialsType
  private lateinit var accountCredentialsStore: AccountAuthenticationCredentialsStoreType
  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var accountProvider: AccountProvider
  private lateinit var accountProviders: AccountProviderRegistryType
  private lateinit var accountsDatabases: AccountsDatabases
  private lateinit var analytics: AnalyticsType
  private lateinit var baseDirectory: File
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var bundledContent: MockBundledContentResolver
  private lateinit var cacheDirectory: File
  private lateinit var contentResolver: ContentResolver
  private lateinit var contentResolverSane: ContentResolverSane
  private lateinit var context: Context
  private lateinit var credentialsFile: File
  private lateinit var credentialsFileTmp: File
  private lateinit var executorService: ExecutorService
  private lateinit var feedExecutorService: ListeningExecutorService
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var http: LSHTTPClientType
  private lateinit var idleExecutorService: ExecutorService
  private lateinit var opdsParser: OPDSFeedParserType
  private lateinit var profileEvents: PublishSubject<ProfileEvent>
  private lateinit var profileIdleTimer: ProfileIdleTimerType
  private lateinit var controller: Controller
  private lateinit var profilesDatabase: ProfilesDatabaseType
  private lateinit var profilesDirectory: File
  private lateinit var server: MockWebServer
  private lateinit var services: ServiceDirectoryType

  @BeforeEach
  fun setup() {
    this.server = MockWebServer()
    this.server.start()

    this.contentResolver =
      Mockito.mock(ContentResolver::class.java)
    this.context =
      Mockito.mock(Context::class.java)

    this.accountProvider =
      MockAccountProviders.fakeProvider(
        providerId = "urn:01349b15-9821-42f5-9a34-b6c6c1daa39b",
        host = this.server.hostName,
        port = this.server.port
      )

    this.http =
      LSHTTPClients()
        .create(
          this.context,
          LSHTTPClientConfiguration(
            applicationName = "simplified-tests",
            applicationVersion = "99.99.0"
          )
        )

    this.baseDirectory =
      TestDirectories.temporaryDirectory()
    this.cacheDirectory =
      File(this.baseDirectory, "cache")
    this.credentialsFile =
      File(this.baseDirectory, "credentials.json")
    this.credentialsFileTmp =
      File(this.baseDirectory, "credentials.json.tmp")
    this.profilesDirectory =
      File(this.baseDirectory, "profiles")

    this.accountEvents =
      PublishSubject.create()
    this.profileEvents =
      PublishSubject.create()
    this.executorService =
      Executors.newFixedThreadPool(1)
    this.idleExecutorService =
      Executors.newFixedThreadPool(1)
    this.feedExecutorService =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1))
    this.bookRegistry =
      BookRegistry.create()
    this.bundledContent =
      MockBundledContentResolver()
    this.opdsParser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser())
    this.analytics =
      Mockito.mock(AnalyticsType::class.java)
    this.accountProviders =
      AccountProviderRegistry.createFrom(this.context, listOf(), this.accountProvider)
    this.accountBundledCredentials =
      AccountBundledCredentialsEmpty.getInstance()
    this.accountCredentialsStore =
      AccountAuthenticationCredentialsStore.open(this.credentialsFile, this.credentialsFileTmp)
    this.accountsDatabases =
      AccountsDatabases

    this.profilesDatabase =
      ProfilesDatabases.openWithAnonymousProfileDisabled(
        context = this.context,
        analytics = this.analytics,
        accountEvents = this.accountEvents,
        accountProviders = this.accountProviders,
        accountBundledCredentials = this.accountBundledCredentials,
        accountCredentialsStore = this.accountCredentialsStore,
        accountsDatabases = this.accountsDatabases,
        directory = this.profilesDirectory
      )

    val bookFormatSupport =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAudioBooks = null,
          supportsAxisNow = false
        )
      )

    this.contentResolverSane =
      ContentResolverSane(this.contentResolver)

    this.feedLoader =
      FeedLoader.create(
        bookFormatSupport = bookFormatSupport,
        contentResolver = contentResolverSane,
        exec = this.feedExecutorService,
        parser = this.opdsParser,
        searchParser = OPDSSearchParser.newParser(),
        transport = FeedHTTPTransport(this.http),
        bundledContent = this.bundledContent
      )

    this.profileIdleTimer =
      ProfileIdleTimer.create(this.idleExecutorService, this.profileEvents)
    this.borrowSubtasks =
      BorrowSubtaskDirectory()

    val b = ServiceDirectory.builder()
    MockStrings.populate(b)

    b.addService(AccountProviderRegistryType::class.java, this.accountProviders)
    b.addService(AnalyticsType::class.java, this.analytics)
    b.addService(AudioBookManifestStrategiesType::class.java, AudioBookManifests)
    b.addService(AuthenticationDocumentParsersType::class.java, AuthenticationDocumentParsers())
    b.addService(BookFormatSupportType::class.java, bookFormatSupport)
    b.addService(BookRegistryType::class.java, this.bookRegistry)
    b.addService(BorrowSubtaskDirectoryType::class.java, this.borrowSubtasks)
    b.addService(BundledContentResolverType::class.java, this.bundledContent)
    b.addService(ContentResolverType::class.java, this.contentResolverSane)
    b.addService(FeedLoaderType::class.java, this.feedLoader)
    b.addService(LSHTTPClientType::class.java, this.http)
    b.addService(OPDSFeedParserType::class.java, this.opdsParser)
    b.addService(PatronUserProfileParsersType::class.java, PatronUserProfileParsers())
    b.addService(ProfileIdleTimerType::class.java, this.profileIdleTimer)
    b.addService(ProfilesDatabaseType::class.java, this.profilesDatabase)

    this.services = b.build()

    this.controller =
      Controller.createFromServiceDirectory(
        services = this.services,
        executorService = this.executorService,
        accountEvents = this.accountEvents,
        profileEvents = this.profileEvents,
        cacheDirectory = this.cacheDirectory
      )
  }

  @AfterEach
  fun tearDown() {
    try {
      this.executorService.shutdown()
    } catch (e: Exception) {
      this.logger.error("", e)
    }
    try {
      this.idleExecutorService.shutdown()
    } catch (e: Exception) {
      this.logger.error("", e)
    }
    try {
      this.feedExecutorService.shutdown()
    } catch (e: Exception) {
      this.logger.error("", e)
    }
    this.server.closeQuietly()
  }

  @Test
  fun testReproduce() {
    val preferences =
      ProfilePreferences(
        dateOfBirth = null,
        showTestingLibraries = false,
        hasSeenLibrarySelectionScreen = false,
        readerPreferences = ReaderPreferences.builder().build(),
        mostRecentAccount = null
      )

    val profileDescription0 =
      ProfileDescription(
        displayName = "Kermit",
        preferences = preferences,
        attributes = ProfileAttributes(sortedMapOf())
      )

    val profileDescription1 =
      ProfileDescription(
        displayName = "Grouch",
        preferences = preferences,
        attributes = ProfileAttributes(sortedMapOf())
      )

    this.logger.debug("creating profile 0")
    val profileEvent0 =
      this.controller.profileCreate(this.accountProvider, profileDescription0)
        .get() as ProfileCreationEvent.ProfileCreationSucceeded

    this.logger.debug("creating profile 1")
    val profileEvent1 =
      this.controller.profileCreate(this.accountProvider, profileDescription1)
        .get() as ProfileCreationEvent.ProfileCreationSucceeded

    this.logger.debug("selecting profile 0")
    this.controller.profileSelect(profileEvent0.id())
      .get()

    val profile0 = this.controller.profileCurrent()
    profile0.accounts().values.forEachIndexed { index, account ->
      this.logger.debug("[{}] account {}", index, account.id)
    }

    this.logger.debug("selecting profile 1")
    this.controller.profileSelect(profileEvent1.id())
      .get()

    val profile1 = this.controller.profileCurrent()
    profile1.accounts().values.forEachIndexed { index, account ->
      this.logger.debug("[{}] account {}", index, account.id)
    }

    val bookAcquisition0 =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_OPEN_ACCESS,
        this.server.url("/book.epub").toUri(),
        StandardFormatNames.genericEPUBFiles,
        listOf()
      )

    val bookEntry0 =
      OPDSAcquisitionFeedEntry.newBuilder(
        "5b7ec7e5-b137-4a11-b2df-4378e63ffb25",
        "Example Book 0",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      ).addAcquisition(bookAcquisition0)
        .build()

    this.server.enqueue(
      MockResponse()
        .addHeader("Content-Type: application/epub+zip")
        .setBody("HELLO!")
    )

    this.logger.debug("downloading book on profile 1")
    this.controller.bookBorrow(profile1.accounts().values.first().id, bookEntry0)
      .get()

    this.logger.debug("selecting profile 0")
    this.controller.profileSelect(profile0.id)
      .get()

    this.server.enqueue(
      MockResponse()
        .addHeader("Content-Type: application/epub+zip")
        .setBody("HELLO!")
    )

    this.logger.debug("downloading book on profile 0")
    this.controller.bookBorrow(profile0.accounts().values.first().id, bookEntry0)
      .get()
  }
}
