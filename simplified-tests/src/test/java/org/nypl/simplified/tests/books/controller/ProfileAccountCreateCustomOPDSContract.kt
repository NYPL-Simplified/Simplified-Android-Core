package org.nypl.simplified.tests.books.controller

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.subjects.PublishSubject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.joda.time.Instant
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseIOException
import org.nypl.simplified.accounts.registry.AccountProviderRegistry
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.ProfileAccountCreateCustomOPDSTask
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSParseException
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.MockAccountCreationStringResources
import org.nypl.simplified.tests.MockAccountProviderRegistry
import org.nypl.simplified.tests.MockAccountProviderResolutionStrings
import org.nypl.simplified.tests.MockAccountProviders
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Contract for the `ProfileAccountCreateCustomOPDSTask` class.
 */

abstract class ProfileAccountCreateCustomOPDSContract {

  @JvmField
  @Rule
  val expected = ExpectedException.none()

  val accountID =
    AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  protected abstract val logger: Logger

  private lateinit var accountEvents: PublishSubject<AccountEvent>
  private lateinit var accountProviderRegistry: AccountProviderRegistryType
  private lateinit var accountProviderResolutionStrings: MockAccountProviderResolutionStrings
  private lateinit var authDocumentParsers: AuthenticationDocumentParsersType
  private lateinit var bookEvents: MutableList<BookEvent>
  private lateinit var bookFormatSupport: BookFormatSupportType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var bundledContent: BundledContentResolverType
  private lateinit var cacheDirectory: File
  private lateinit var clock: () -> Instant
  private lateinit var contentResolver: ContentResolverType
  private lateinit var context: Context
  private lateinit var defaultProvider: AccountProvider
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var executorBooks: ListeningExecutorService
  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorTimer: ListeningExecutorService
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var http: LSHTTPClientType
  private lateinit var opdsFeedParser: OPDSFeedParserType
  private lateinit var profileAccountCreationStrings: MockAccountCreationStringResources
  private lateinit var profilesDatabase: ProfilesDatabaseType
  private lateinit var server: MockWebServer

  @Before
  @Throws(Exception::class)
  fun setUp() {
    this.context = Mockito.mock(Context::class.java)
    this.http = LSHTTPClients().create(this.context, LSHTTPClientConfiguration("test", "1.0.0"))
    this.defaultProvider = MockAccountProviders.fakeProvider("urn:fake:0")
    this.accountProviderRegistry = AccountProviderRegistry.createFrom(this.context, listOf(), this.defaultProvider)
    this.accountEvents = PublishSubject.create()
    this.authDocumentParsers = Mockito.mock(AuthenticationDocumentParsersType::class.java)
    this.executorBooks = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorTimer = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorFeeds = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.directoryDownloads = DirectoryUtilities.directoryCreateTemporary()
    this.directoryProfiles = DirectoryUtilities.directoryCreateTemporary()
    this.bookEvents = Collections.synchronizedList(ArrayList())
    this.bookRegistry = BookRegistry.create()
    this.bookFormatSupport = Mockito.mock(BookFormatSupportType::class.java)
    this.bundledContent = BundledContentResolverType { uri -> throw FileNotFoundException("missing") }
    this.contentResolver = Mockito.mock(ContentResolverType::class.java)
    this.cacheDirectory = File.createTempFile("book-borrow-tmp", "dir")
    this.cacheDirectory.delete()
    this.cacheDirectory.mkdirs()
    this.opdsFeedParser = Mockito.mock(OPDSFeedParserType::class.java)
    this.profilesDatabase = Mockito.mock(ProfilesDatabaseType::class.java)
    this.accountProviderResolutionStrings = MockAccountProviderResolutionStrings()
    this.profileAccountCreationStrings = MockAccountCreationStringResources()
    this.clock = { Instant.now() }
    this.server = MockWebServer()
    this.server.start()
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorTimer.shutdown()
    this.server.close()
  }

  @Test
  fun testOPDSFeedFails() {
    val opdsURI = this.server.url("/").toUri()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(400)
        .setStatus("BAD!")
        .setBody("")
    )

    val task =
      ProfileAccountCreateCustomOPDSTask(
        accountEvents = this.accountEvents,
        accountProviderRegistry = this.accountProviderRegistry,
        httpClient = this.http,
        opdsURI = opdsURI,
        opdsFeedParser = this.opdsFeedParser,
        profiles = this.profilesDatabase,
        strings = this.profileAccountCreationStrings
      )

    val result = task.call()
    val failure = result as TaskResult.Failure
    Assert.assertEquals("fetchingOPDSFeedFailed", failure.steps.last().resolution.message)
  }

  @Test
  fun testOPDSFeedFailsExceptional() {
    val opdsURI = this.server.url("/").toUri()

    val task =
      ProfileAccountCreateCustomOPDSTask(
        accountEvents = this.accountEvents,
        accountProviderRegistry = this.accountProviderRegistry,
        httpClient = this.http,
        opdsURI = opdsURI,
        opdsFeedParser = this.opdsFeedParser,
        profiles = this.profilesDatabase,
        strings = this.profileAccountCreationStrings
      )

    val result = task.call()
    val failure = result as TaskResult.Failure
    Assert.assertEquals("fetchingOPDSFeedFailed", failure.steps.last().resolution.message)
  }

  @Test
  fun testOPDSFeedUnparseable() {
    val opdsURI = this.server.url("/").toUri()

    val stream =
      ByteArrayInputStream(ByteArray(0))

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
    )

    Mockito.`when`(this.opdsFeedParser.parse(opdsURI, stream))
      .thenThrow(OPDSParseException())

    val task =
      ProfileAccountCreateCustomOPDSTask(
        accountEvents = this.accountEvents,
        accountProviderRegistry = this.accountProviderRegistry,
        httpClient = this.http,
        opdsURI = opdsURI,
        opdsFeedParser = this.opdsFeedParser,
        profiles = this.profilesDatabase,
        strings = this.profileAccountCreationStrings
      )

    val result = task.call()
    val failure = result as TaskResult.Failure
    Assert.assertEquals("parsingOPDSFeedFailed", failure.lastErrorCode)
  }

  @Test
  fun testOPDSFeedNoAuthentication() {
    this.opdsFeedParser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser())

    val opdsURI = this.server.url("/").toUri()
    val accountId = AccountID.generate()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom">
  <id>http://circulation.alpha.librarysimplified.org/loans/</id>
  <title>Active loans and holds</title>
  <updated>2020-01-01T00:00:00Z</updated>
</feed>
          """.trimIndent()
        )
    )

    val profile =
      Mockito.mock(ProfileType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    val preferences =
      AccountPreferences(
        bookmarkSyncingPermitted = true,
        catalogURIOverride = null
      )
    val preferencesWithURI =
      AccountPreferences(
        bookmarkSyncingPermitted = true,
        catalogURIOverride = opdsURI
      )

    Mockito.`when`(this.profilesDatabase.currentProfileUnsafe())
      .thenReturn(profile)
    Mockito.`when`(profile.createAccount(anyNonNull()))
      .thenReturn(account)
    Mockito.`when`(account.id)
      .thenReturn(accountId)
    Mockito.`when`(account.preferences)
      .thenReturn(preferences)

    val accountProvider =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val accountProviders =
      MockAccountProviderRegistry.singleton(accountProvider)

    accountProviders.returnForNextResolution(accountProvider)

    val task =
      ProfileAccountCreateCustomOPDSTask(
        accountEvents = this.accountEvents,
        accountProviderRegistry = accountProviders,
        httpClient = this.http,
        opdsURI = opdsURI,
        opdsFeedParser = this.opdsFeedParser,
        profiles = this.profilesDatabase,
        strings = this.profileAccountCreationStrings
      )

    val result = task.call()
    val success = result as TaskResult.Success

    Assert.assertSame(account, success.result)

    Mockito.verify(profile, Mockito.times(1))
      .createAccount(anyNonNull())
    Mockito.verify(account, Mockito.times(1))
      .setPreferences(preferencesWithURI)
  }

  @Test
  fun testProviderResolutionFails() {
    this.opdsFeedParser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser())

    val authURI = this.server.url("/auth").toUri()
    val opdsURI = this.server.url("/").toUri()
    val accountId = AccountID.generate()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom">
  <id>http://circulation.alpha.librarysimplified.org/loans/</id>
  <title>Active loans and holds</title>
  <updated>2020-01-01T00:00:00Z</updated>
  <link href="https://circulation.librarysimplified.org/NYNYPL/authentication_document" rel="http://opds-spec.org/auth/document"/>
</feed>
          """.trimIndent()
        )
    )

    this.server.enqueue(MockResponse().setResponseCode(400))

    val profile =
      Mockito.mock(ProfileType::class.java)
    val account =
      Mockito.mock(AccountType::class.java)

    Mockito.`when`(this.profilesDatabase.currentProfileUnsafe())
      .thenReturn(profile)
    Mockito.`when`(profile.createAccount(anyNonNull()))
      .thenReturn(account)
    Mockito.`when`(account.id)
      .thenReturn(accountId)

    val task =
      ProfileAccountCreateCustomOPDSTask(
        accountEvents = this.accountEvents,
        accountProviderRegistry = this.accountProviderRegistry,
        httpClient = this.http,
        opdsURI = opdsURI,
        opdsFeedParser = this.opdsFeedParser,
        profiles = this.profilesDatabase,
        strings = this.profileAccountCreationStrings
      )

    val result = task.call()
    val failure = result as TaskResult.Failure
    Assert.assertEquals("resolvingAccountProviderFailed", failure.steps.last().resolution.message)
  }

  @Test
  fun testAccountCreationFails() {
    this.opdsFeedParser =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser())

    val opdsURI = this.server.url("/").toUri()
    val accountId = AccountID.generate()

    this.server.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(
          """
<feed xmlns="http://www.w3.org/2005/Atom">
  <id>http://circulation.alpha.librarysimplified.org/loans/</id>
  <title>Active loans and holds</title>
  <updated>2020-01-01T00:00:00Z</updated>
</feed>
          """.trimIndent()
        )
    )

    val profile =
      Mockito.mock(ProfileType::class.java)

    Mockito.`when`(this.profilesDatabase.currentProfileUnsafe())
      .thenReturn(profile)
    Mockito.`when`(profile.createAccount(anyNonNull()))
      .thenThrow(AccountsDatabaseIOException("FAILED!", IOException()))

    val accountProvider =
      MockAccountProviders.fakeProvider("urn:fake:0")
    val accountProviders =
      MockAccountProviderRegistry.singleton(accountProvider)

    accountProviders.returnForNextResolution(accountProvider)

    val task =
      ProfileAccountCreateCustomOPDSTask(
        accountEvents = this.accountEvents,
        accountProviderRegistry = accountProviders,
        httpClient = this.http,
        opdsURI = opdsURI,
        opdsFeedParser = this.opdsFeedParser,
        profiles = this.profilesDatabase,
        strings = this.profileAccountCreationStrings
      )

    val result = task.call()
    val failure = result as TaskResult.Failure
    Assert.assertEquals("creatingAccountFailed", failure.lastErrorCode)
  }

  private fun <T> anyNonNull(): T =
    Mockito.argThat { x -> x != null }

  private fun logBookEventsFor(bookId: BookID) {
    this.bookRegistry.bookEvents().subscribe {
      this.bookRegistry.bookStatus(bookId).map_ { status ->
        this.logger.debug("status: {}", status)
      }
    }
  }

  private fun resource(file: String): InputStream {
    return ProfileAccountCreateCustomOPDSContract::class.java.getResourceAsStream(file)!!
  }

  @Throws(IOException::class)
  private fun resourceSize(file: String): Long {
    var total = 0L
    val buffer = ByteArray(8192)
    this.resource(file).use { stream ->
      while (true) {
        val r = stream.read(buffer)
        if (r <= 0) {
          break
        }
        total += r.toLong()
      }
    }
    return total
  }
}
