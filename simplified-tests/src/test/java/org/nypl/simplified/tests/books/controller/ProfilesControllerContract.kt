package org.nypl.simplified.tests.books.controller

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.subjects.PublishSubject
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.function.Executable
import org.librarysimplified.http.api.LSHTTPClientType
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.accounts.database.AccountBundledCredentialsEmpty
import org.nypl.simplified.accounts.database.AccountsDatabases
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.audio.AudioBookManifestStrategiesType
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.borrowing.BorrowSubtasks
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.Controller
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.feeds.api.FeedFacetPseudoTitleProviderType
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.profiles.ProfilesDatabases
import org.nypl.simplified.profiles.api.ProfileAttributes
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationFailed.ErrorCode.ERROR_DISPLAY_NAME_ALREADY_USED
import org.nypl.simplified.profiles.api.ProfileCreationEvent.ProfileCreationSucceeded
import org.nypl.simplified.profiles.api.ProfileDatabaseException
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.api.idle_timer.ProfileIdleTimerType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileFeedRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.api.ReaderColorScheme
import org.nypl.simplified.reader.api.ReaderFontSelection
import org.nypl.simplified.reader.api.ReaderPreferences
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.nypl.simplified.tests.EventAssertions
import org.nypl.simplified.tests.MutableServiceDirectory
import org.nypl.simplified.tests.books.idle_timer.InoperableIdleTimer
import org.nypl.simplified.tests.mocking.FakeAccountCredentialStorage
import org.nypl.simplified.tests.mocking.MockAccountCreationStringResources
import org.nypl.simplified.tests.mocking.MockAccountDeletionStringResources
import org.nypl.simplified.tests.mocking.MockAccountLoginStringResources
import org.nypl.simplified.tests.mocking.MockAccountLogoutStringResources
import org.nypl.simplified.tests.mocking.MockAccountProviderRegistry
import org.nypl.simplified.tests.mocking.MockAccountProviderResolutionStrings
import org.nypl.simplified.tests.mocking.MockAccountProviders
import org.nypl.simplified.tests.mocking.MockAnalytics
import org.nypl.simplified.tests.mocking.MockRevokeStringResources
import org.slf4j.Logger
import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class ProfilesControllerContract {

  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var accountEventsReceived: MutableList<AccountEvent>
  private lateinit var audioBookManifestStrategies: AudioBookManifestStrategiesType
  private lateinit var authDocumentParsers: AuthenticationDocumentParsersType
  private lateinit var bookFormatSupport: BookFormatSupportType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var cacheDirectory: File
  private lateinit var contentResolver: ContentResolverType
  private lateinit var credentialsStore: FakeAccountCredentialStorage
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var executorBooks: ExecutorService
  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorTimer: ExecutorService
  private lateinit var lsHTTP: LSHTTPClientType
  private lateinit var patronUserProfileParsers: PatronUserProfileParsersType
  private lateinit var profileEvents: PublishSubject<ProfileEvent>
  private lateinit var profileEventsReceived: MutableList<ProfileEvent>
  private lateinit var readerBookmarkEvents: PublishSubject<ReaderBookmarkEvent>

  protected abstract val logger: Logger

  protected abstract fun context(): Context

  private val accountProviderResolutionStrings =
    MockAccountProviderResolutionStrings()
  private val accountLoginStringResources =
    MockAccountLoginStringResources()
  private val accountLogoutStringResources =
    MockAccountLogoutStringResources()
  private val bookRevokeStringResources =
    MockRevokeStringResources()
  private val profileAccountDeletionStringResources =
    MockAccountDeletionStringResources()
  private val profileAccountCreationStringResources =
    MockAccountCreationStringResources()
  private val analyticsLogger =
    MockAnalytics()

  private fun controller(
    profiles: ProfilesDatabaseType,
    accountProviders: AccountProviderRegistryType
  ): ProfilesControllerType {
    val parser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser())
    val transport =
      FeedHTTPTransport(this.lsHTTP)
    val bundledContent = BundledContentResolverType { uri ->
      throw FileNotFoundException(uri.toString())
    }

    val feedLoader =
      FeedLoader.create(
        bookFormatSupport = this.bookFormatSupport,
        bundledContent = bundledContent,
        contentResolver = this.contentResolver,
        exec = this.executorFeeds,
        parser = parser,
        searchParser = OPDSSearchParser.newParser(),
        transport = transport
      )

    val services = MutableServiceDirectory()
    services.putService(AccountLoginStringResourcesType::class.java, this.accountLoginStringResources)
    services.putService(AccountLogoutStringResourcesType::class.java, this.accountLogoutStringResources)
    services.putService(AccountProviderRegistryType::class.java, accountProviders)
    services.putService(AccountProviderResolutionStringsType::class.java, this.accountProviderResolutionStrings)
    services.putService(AnalyticsType::class.java, this.analyticsLogger)
    services.putService(AudioBookManifestStrategiesType::class.java, this.audioBookManifestStrategies)
    services.putService(AuthenticationDocumentParsersType::class.java, this.authDocumentParsers)
    services.putService(BookFormatSupportType::class.java, this.bookFormatSupport)
    services.putService(BookRegistryType::class.java, this.bookRegistry)
    services.putService(BookRevokeStringResourcesType::class.java, this.bookRevokeStringResources)
    services.putService(BorrowSubtaskDirectoryType::class.java, BorrowSubtasks.directory())
    services.putService(BundledContentResolverType::class.java, bundledContent)
    services.putService(ContentResolverType::class.java, this.contentResolver)
    services.putService(FeedLoaderType::class.java, feedLoader)
    services.putService(LSHTTPClientType::class.java, this.lsHTTP)
    services.putService(OPDSFeedParserType::class.java, parser)
    services.putService(PatronUserProfileParsersType::class.java, this.patronUserProfileParsers)
    services.putService(ProfileAccountCreationStringResourcesType::class.java, this.profileAccountCreationStringResources)
    services.putService(ProfileAccountDeletionStringResourcesType::class.java, this.profileAccountDeletionStringResources)
    services.putService(ProfileIdleTimerType::class.java, InoperableIdleTimer())
    services.putService(ProfilesDatabaseType::class.java, profiles)

    return Controller.createFromServiceDirectory(
      services = services,
      executorService = this.executorBooks,
      accountEvents = this.accountEvents,
      profileEvents = this.profileEvents,
      cacheDirectory = this.cacheDirectory
    )
  }

  @BeforeEach
  @Throws(Exception::class)
  fun setUp() {
    this.audioBookManifestStrategies = Mockito.mock(AudioBookManifestStrategiesType::class.java)
    this.credentialsStore = FakeAccountCredentialStorage()
    this.lsHTTP = Mockito.mock(LSHTTPClientType::class.java)
    this.authDocumentParsers = Mockito.mock(AuthenticationDocumentParsersType::class.java)
    this.executorFeeds = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorBooks = Executors.newCachedThreadPool()
    this.executorTimer = Executors.newCachedThreadPool()
    this.directoryDownloads = DirectoryUtilities.directoryCreateTemporary()
    this.directoryProfiles = DirectoryUtilities.directoryCreateTemporary()
    this.profileEvents = PublishSubject.create<ProfileEvent>()
    this.profileEventsReceived = Collections.synchronizedList(ArrayList())
    this.accountEvents = PublishSubject.create<AccountEvent>()
    this.accountEventsReceived = Collections.synchronizedList(ArrayList())
    this.cacheDirectory = File.createTempFile("book-borrow-tmp", "dir")
    this.cacheDirectory.delete()
    this.cacheDirectory.mkdirs()
    this.contentResolver = Mockito.mock(ContentResolverType::class.java)
    this.readerBookmarkEvents = PublishSubject.create()
    this.bookRegistry = BookRegistry.create()
    this.patronUserProfileParsers = Mockito.mock(PatronUserProfileParsersType::class.java)
    this.bookFormatSupport = Mockito.mock(BookFormatSupportType::class.java)
  }

  @AfterEach
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorTimer.shutdown()
  }

  private fun descriptionWith(
    description: ProfileDescription,
    gender: String,
    dateOfBirth: DateTime,
  ): ProfileDescription {
    val preferences = description.preferences.copy(
      dateOfBirth = ProfileDateOfBirth(dateOfBirth, true),
    )

    val attributeMap =
      description.attributes.attributes + Pair(ProfileAttributes.GENDER_ATTRIBUTE_KEY, gender)

    val attributes =
      ProfileAttributes(
        attributeMap.toSortedMap()
      )

    return ProfileDescription(
      description.displayName,
      preferences,
      attributes
    )
  }

  /**
   * Trying to fetch the current profile without selecting one should fail.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 3L, unit = TimeUnit.SECONDS)
  @Throws(Exception::class)
  fun testProfilesCurrentNoneCurrent() {
    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = MockAccountProviders.fakeAccountProviders()
      )

    Assertions.assertThrows(
      ProfileNoneCurrentException::class.java,
      Executable {
        controller.profileCurrent()
      }
    )
  }

  /**
   * Selecting a profile works.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 3L, unit = TimeUnit.SECONDS)
  @Throws(Exception::class)
  fun testProfilesCurrentSelectCurrent() {
    val accountProvider =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val accountProviders =
      MockAccountProviderRegistry.singleton(accountProvider)

    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = accountProviders
      )

    controller.profileCreate("Kermit", accountProvider) { desc ->
      this.descriptionWith(desc, gender = "Female", dateOfBirth = DateTime.now())
    }.get()
    controller.profileSelect(profiles.profiles().firstKey()).get()

    this.profileEventsReceived.forEach { this.logger.debug("event: {}", it) }
    this.accountEventsReceived.forEach { this.logger.debug("event: {}", it) }

    val p = controller.profileCurrent()
    Assertions.assertEquals("Kermit", p.displayName)
  }

  /**
   * Creating a profile with the same display name as an existing profile should fail.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 3L, unit = TimeUnit.SECONDS)
  @Throws(Exception::class)
  fun testProfilesCreateDuplicate() {
    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)

    val accountProvider =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val accountProviders =
      MockAccountProviderRegistry.singleton(accountProvider)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = accountProviders
      )

    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }

    val date = DateTime.now()

    controller.profileCreate("Kermit", accountProvider) { desc ->
      this.descriptionWith(desc, "Female", date)
    }.get()
    controller.profileCreate("Kermit", accountProvider) { desc ->
      this.descriptionWith(desc, "Female", date)
    }.get()

    this.profileEventsReceived.forEach { this.logger.debug("event: {}", it) }
    this.accountEventsReceived.forEach { this.logger.debug("event: {}", it) }

    EventAssertions.isType(ProfileCreationSucceeded::class.java, this.profileEventsReceived, 0)
    EventAssertions.isTypeAndMatches(
      ProfileCreationFailed::class.java,
      this.profileEventsReceived,
      1
    ) { e -> Assertions.assertEquals(ERROR_DISPLAY_NAME_ALREADY_USED, e.errorCode()) }
  }

  /**
   * Setting and getting preferences works.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 3L, unit = TimeUnit.SECONDS)
  @Throws(Exception::class)
  fun testProfilesPreferences() {
    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val accountProvider =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val accountProviders =
      MockAccountProviderRegistry.singleton(accountProvider)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = accountProviders
      )

    controller.profileCreate("Kermit", accountProvider) { desc ->
      this.descriptionWith(desc, "Female", DateTime.now())
    }.get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(accountProvider.id).get()
    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }
    controller.profileUpdate { description -> description }.get()

    this.profileEventsReceived.forEach { this.logger.debug("event: {}", it) }
    this.accountEventsReceived.forEach { this.logger.debug("event: {}", it) }

    EventAssertions.isTypeAndMatches(
      ProfileUpdated.Succeeded::class.java,
      this.profileEventsReceived,
      0
    ) { e ->
      Assertions.assertTrue(e.oldDescription == e.newDescription, "Preferences must not have changed")
    }

    this.profileEventsReceived.clear()
    controller.profileUpdate { description ->
      description.copy(
        preferences =
          description.preferences.copy(
            readerPreferences = ReaderPreferences.builder()
              .setBrightness(0.2)
              .setColorScheme(ReaderColorScheme.SCHEME_WHITE_ON_BLACK)
              .setFontFamily(ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC)
              .setFontScale(2.0)
              .build()
          )
      )
    }.get()

    EventAssertions.isTypeAndMatches(
      ProfileUpdated.Succeeded::class.java,
      this.profileEventsReceived,
      0
    ) { e ->
      Assertions.assertTrue(e.oldDescription != e.newDescription, "Preferences must have changed")
    }
  }

  /**
   * Retrieving an empty feed of books works.
   *
   * @throws Exception On errors
   */

  @Test
  @Timeout(value = 3L, unit = TimeUnit.SECONDS)
  @Throws(Exception::class)
  fun testProfilesFeed() {
    val accountProvider =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val accountProviders =
      MockAccountProviderRegistry.singleton(accountProvider)

    val profiles =
      this.profilesDatabaseWithoutAnonymous(this.directoryProfiles)
    val controller =
      this.controller(
        profiles = profiles,
        accountProviders = accountProviders
      )

    controller.profileCreate("Kermit", accountProvider) { desc ->
      this.descriptionWith(desc, "Female", DateTime.now())
    }.get()
    controller.profileSelect(profiles.profiles().firstKey()).get()
    controller.profileAccountCreate(accountProvider.id).get()
    controller.profileEvents().subscribe { this.profileEventsReceived.add(it) }

    val feed =
      controller.profileFeed(
        ProfileFeedRequest(
          uri = URI.create("Books"),
          title = "Books",
          facetTitleProvider = object : FeedFacetPseudoTitleProviderType {
            override val collection: String
              get() = "Collection"
            override val collectionAll: String
              get() = "All"
            override val sortBy: String
              get() = "Sort By"
            override val sortByAuthor: String
              get() = "Author"
            override val sortByTitle: String
              get() = "Title"
          }
        )
      ).get()

    Assertions.assertEquals(0L, feed.size.toLong())
  }

  @Throws(ProfileDatabaseException::class)
  private fun profilesDatabaseWithoutAnonymous(dir_profiles: File): ProfilesDatabaseType {
    return ProfilesDatabases.openWithAnonymousProfileDisabled(
      this.context(),
      this.analyticsLogger,
      this.accountEvents,
      MockAccountProviders.fakeAccountProviders(),
      AccountBundledCredentialsEmpty.getInstance(),
      this.credentialsStore,
      AccountsDatabases,
      dir_profiles
    )
  }

  private fun onAccountResolution(
    id: URI,
    message: String
  ) {
    this.logger.debug("resolution: {}: {}", id, message)
  }
}
