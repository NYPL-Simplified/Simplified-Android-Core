package org.nypl.simplified.tests.books.controller

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser
import org.joda.time.DateTime
import org.joda.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.librarysimplified.http.api.LSHTTPClientConfiguration
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.vanilla.LSHTTPClients
import org.mockito.Mockito
import org.mockito.internal.verification.Times
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeAdeptLoanReturnListenerType
import org.nypl.drm.core.AdobeAdeptProcedureType
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeLoanID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookEvent
import org.nypl.simplified.books.api.BookFormat.BookFormatEPUB
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.BookDRMInformationHandleACS
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistry
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import org.nypl.simplified.books.controller.BookRevokeTask
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.content.api.ContentResolverType
import org.nypl.simplified.feeds.api.FeedHTTPTransport
import org.nypl.simplified.feeds.api.FeedLoader
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSSearchParser
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.tests.mocking.MockRevokeStringResources
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Contract for the `BookRevokeTask` class that doesn't involve DRM.
 */

class BookRevokeTaskAdobeDRMTest {

  val accountID =
    AccountID(UUID.fromString("46d17029-14ba-4e34-bcaa-def02713575a"))

  val profileID =
    ProfileID(UUID.fromString("06fa7899-658a-4480-a796-ebf2ff00d5ec"))

  private val logger: Logger =
    LoggerFactory.getLogger(BookRevokeTaskAdobeDRMTest::class.java)

  private lateinit var adobeConnector: AdobeAdeptConnectorType
  private lateinit var adobeExecutor: AdobeAdeptExecutorType
  private lateinit var bookEvents: MutableList<BookEvent>
  private lateinit var bookFormatSupport: BookFormatSupportType
  private lateinit var bookRegistry: BookRegistryType
  private lateinit var bundledContent: BundledContentResolverType
  private lateinit var cacheDirectory: File
  private lateinit var clock: () -> Instant
  private lateinit var contentResolver: ContentResolverType
  private lateinit var directoryDownloads: File
  private lateinit var directoryProfiles: File
  private lateinit var executorBooks: ListeningExecutorService
  private lateinit var executorFeeds: ListeningExecutorService
  private lateinit var executorTimer: ListeningExecutorService
  private lateinit var feedLoader: FeedLoaderType
  private lateinit var http: LSHTTPClientType

  private val bookRevokeStrings = MockRevokeStringResources()

  private val basicDRMInformationReturnable =
    BookDRMInformation.ACS(
      acsmFile = null,
      rights = Pair(
        File("loan"),
        AdobeAdeptLoan(
          AdobeLoanID("a6a0f12f-cae0-46fd-afc8-e52b8b024e6c"),
          ByteArray(32),
          true
        )
      )
    )

  private val basicDRMInformationNotReturnable =
    BookDRMInformation.ACS(
      acsmFile = null,
      rights = Pair(
        File("loan"),
        AdobeAdeptLoan(
          AdobeLoanID("a6a0f12f-cae0-46fd-afc8-e52b8b024e6c"),
          ByteArray(32),
          false
        )
      )
    )

  @BeforeEach
  @Throws(Exception::class)
  fun setUp() {
    this.http =
      LSHTTPClients()
        .create(
          context = Mockito.mock(Context::class.java),
          configuration = LSHTTPClientConfiguration(
            applicationName = "simplified-tests",
            applicationVersion = "1.0.0",
            tlsOverrides = null,
            timeout = Pair(5L, TimeUnit.SECONDS)
          )
        )

    this.adobeExecutor = Mockito.mock(AdobeAdeptExecutorType::class.java)
    this.adobeConnector = Mockito.mock(AdobeAdeptConnectorType::class.java)
    this.executorBooks = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorTimer = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.executorFeeds = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    this.directoryDownloads = DirectoryUtilities.directoryCreateTemporary()
    this.directoryProfiles = DirectoryUtilities.directoryCreateTemporary()
    this.bookEvents = Collections.synchronizedList(ArrayList())
    this.bookRegistry = BookRegistry.create()
    this.bookFormatSupport = Mockito.mock(BookFormatSupportType::class.java)
    this.bundledContent =
      BundledContentResolverType { uri -> throw FileNotFoundException("missing") }
    this.contentResolver = Mockito.mock(ContentResolverType::class.java)
    this.cacheDirectory = File.createTempFile("book-borrow-tmp", "dir")
    this.cacheDirectory.delete()
    this.cacheDirectory.mkdirs()
    this.feedLoader = this.createFeedLoader(this.executorFeeds)
    this.clock = { Instant.now() }
  }

  @AfterEach
  @Throws(Exception::class)
  fun tearDown() {
    this.executorBooks.shutdown()
    this.executorFeeds.shutdown()
    this.executorTimer.shutdown()
  }

  private fun createFeedLoader(executorFeeds: ListeningExecutorService): FeedLoaderType {
    val entryParser =
      OPDSAcquisitionFeedEntryParser.newParser()
    val parser =
      OPDSFeedParser.newParser(entryParser)
    val searchParser =
      OPDSSearchParser.newParser()
    val transport =
      FeedHTTPTransport(this.http)

    return FeedLoader.create(
      bookFormatSupport = this.bookFormatSupport,
      bookRegistry = this.bookRegistry,
      bundledContent = this.bundledContent,
      contentResolver = this.contentResolver,
      exec = executorFeeds,
      parser = parser,
      searchParser = searchParser,
      transport = transport
    )
  }

  /**
   * Attempting to revoke a loan that requires DRM, but is not returnable, succeeds trivially.
   */

  @Test
  fun testRevokeDRMNonReturnable() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val drmHandle =
      Mockito.mock(BookDRMInformationHandleACS::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val adobeExecutor =
      Mockito.mock(AdobeAdeptExecutorType::class.java)

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)
    Mockito.`when`(bookDatabaseFormatHandle.drmInformationHandle)
      .thenReturn(drmHandle)
    Mockito.`when`(drmHandle.info)
      .thenReturn(this.basicDRMInformationNotReturnable)

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = adobeExecutor,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)

    result as TaskResult.Success
    Assertions.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * Attempting to revoke a loan that requires DRM, without DRM support, succeeds trivially.
   */

  @Test
  fun testRevokeDRMUnsupported() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val drmHandle =
      Mockito.mock(BookDRMInformationHandleACS::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)
    Mockito.`when`(bookDatabaseFormatHandle.drmInformationHandle)
      .thenReturn(drmHandle)
    Mockito.`when`(drmHandle.info)
      .thenReturn(this.basicDRMInformationReturnable)

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = null,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)

    result as TaskResult.Success
    Assertions.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
  }

  /**
   * If the DRM connector says everything succeeded, and there's no revocation URI, then everything
   * succeeded.
   */

  @Test
  fun testRevokeDRMOK() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val drmHandle =
      Mockito.mock(BookDRMInformationHandleACS::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("user"),
            password = AccountPassword("password"),
            adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
              vendorID = AdobeVendorID("NYPL"),
              clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
              deviceManagerURI = null,
              postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
                AdobeDeviceID("4361a1f6-aea8-4681-ad8b-df7e6923049f"),
                AdobeUserID("2bb42d71-42aa-4eb7-b8af-366171adcae8")
              )
            ),
            authenticationDescription = null
          )
        )
      )
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)
    Mockito.`when`(bookDatabaseFormatHandle.drmInformationHandle)
      .thenReturn(drmHandle)
    Mockito.`when`(drmHandle.info)
      .thenReturn(this.basicDRMInformationReturnable)

    /*
     * When the code tells the connector to return the loan, it succeeds if the connector reports
     * success.
     */

    Mockito.`when`(
      this.adobeConnector.loanReturn(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptLoanReturnListenerType
      receiver.onLoanReturnSuccess()
      Unit
    }

    Mockito.`when`(this.adobeExecutor.execute(anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adobeConnector)
      }

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = adobeExecutor,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)

    result as TaskResult.Success
    Assertions.assertEquals(Option.none<BookStatus>(), this.bookRegistry.book(bookId))

    Mockito.verify(bookDatabaseEntry, Times(1)).delete()
    Mockito.verify(drmHandle, Times(1)).setAdobeRightsInformation(null)
  }

  /**
   * If the DRM connector doesn't respond, then the revocation fails.
   */

  @Test
  fun testRevokeDRMDidNothing() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("user"),
            password = AccountPassword("password"),
            adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
              vendorID = AdobeVendorID("NYPL"),
              clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
              deviceManagerURI = null,
              postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
                AdobeDeviceID("4361a1f6-aea8-4681-ad8b-df7e6923049f"),
                AdobeUserID("2bb42d71-42aa-4eb7-b8af-366171adcae8")
              )
            ),
            authenticationDescription = null
          )
        )
      )
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)

    /*
     * When the code tells the connector to return the loan, it fails if the connector does nothing.
     */

    Mockito.`when`(
      this.adobeConnector.loanReturn(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptLoanReturnListenerType
      Unit
    }

    Mockito.`when`(this.adobeExecutor.execute(anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adobeConnector)
      }

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = adobeExecutor,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)
    result as TaskResult.Failure

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * If the DRM connector crashes, then the revocation fails.
   */

  @Test
  fun testRevokeDRMRaisedException() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("user"),
            password = AccountPassword("password"),
            adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
              vendorID = AdobeVendorID("NYPL"),
              clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
              deviceManagerURI = null,
              postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
                AdobeDeviceID("4361a1f6-aea8-4681-ad8b-df7e6923049f"),
                AdobeUserID("2bb42d71-42aa-4eb7-b8af-366171adcae8")
              )
            ),
            authenticationDescription = null
          )
        )
      )
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)

    /*
     * When the code tells the connector to return the loan, it fails if the connector crashes.
     */

    Mockito.`when`(
      this.adobeConnector.loanReturn(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      throw IOException("I/O error")
    }

    Mockito.`when`(this.adobeExecutor.execute(anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adobeConnector)
      }

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = adobeExecutor,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)
    result as TaskResult.Failure

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * If the DRM connector raises an error code, then the revocation fails.
   */

  @Test
  fun testRevokeDRMRaisedErrorCode() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val drmHandle =
      Mockito.mock(BookDRMInformationHandleACS::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("user"),
            password = AccountPassword("password"),
            adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
              vendorID = AdobeVendorID("NYPL"),
              clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
              deviceManagerURI = null,
              postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
                AdobeDeviceID("4361a1f6-aea8-4681-ad8b-df7e6923049f"),
                AdobeUserID("2bb42d71-42aa-4eb7-b8af-366171adcae8")
              )
            ),
            authenticationDescription = null
          )
        )
      )
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.drmInformationHandle)
      .thenReturn(drmHandle)
    Mockito.`when`(drmHandle.info)
      .thenReturn(this.basicDRMInformationReturnable)
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)

    /*
     * When the code tells the connector to return the loan, it fails if the connector fails.
     */

    Mockito.`when`(
      this.adobeConnector.loanReturn(
        this.anyNonNull(),
        this.anyNonNull(),
        this.anyNonNull()
      )
    ).then { invocation ->
      val receiver = invocation.arguments[0] as AdobeAdeptLoanReturnListenerType
      receiver.onLoanReturnFailure("E_DEFECTIVE")
      Unit
    }

    Mockito.`when`(this.adobeExecutor.execute(anyNonNull()))
      .then { invocation ->
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adobeConnector)
      }

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = this.adobeExecutor,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)
    result as TaskResult.Failure

    Assertions.assertEquals("Adobe ACS: E_DEFECTIVE", result.lastErrorCode)
    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * If the device is not activated, then the revocation fails.
   */

  @Test
  fun testRevokeDRMNotActivated() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val drmHandle =
      Mockito.mock(BookDRMInformationHandleACS::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("user"),
            password = AccountPassword("password"),
            adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
              vendorID = AdobeVendorID("NYPL"),
              clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
              deviceManagerURI = null,
              postActivationCredentials = null
            ),
            authenticationDescription = null
          )
        )
      )
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.drmInformationHandle)
      .thenReturn(drmHandle)
    Mockito.`when`(drmHandle.info)
      .thenReturn(this.basicDRMInformationReturnable)
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = this.adobeExecutor,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)
    result as TaskResult.Failure

    Assertions.assertEquals("Adobe ACS: drmDeviceNotActive", result.lastErrorCode)
    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * If the user is not authenticated, then the revocation fails.
   */

  @Test
  fun testRevokeDRMNotAuthenticated() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val drmHandle =
      Mockito.mock(BookDRMInformationHandleACS::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.loginState)
      .thenReturn(AccountLoginState.AccountNotLoggedIn)
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.drmInformationHandle)
      .thenReturn(drmHandle)
    Mockito.`when`(drmHandle.info)
      .thenReturn(this.basicDRMInformationReturnable)
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = this.adobeExecutor,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)
    result as TaskResult.Failure

    Assertions.assertEquals("revokeCredentialsRequired", result.lastErrorCode)
    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  /**
   * If the DRM connector fails to delete credentials, revocation fails.
   */

  @Test
  fun testRevokeDRMDeleteCredentials() {
    val account =
      Mockito.mock(AccountType::class.java)
    val accountProvider =
      Mockito.mock(AccountProviderType::class.java)
    val profile =
      Mockito.mock(ProfileType::class.java)
    val profilesDatabase =
      Mockito.mock(ProfilesDatabaseType::class.java)
    val bookDatabase =
      Mockito.mock(BookDatabaseType::class.java)
    val bookDatabaseEntry =
      Mockito.mock(BookDatabaseEntryType::class.java)
    val bookDatabaseFormatHandle =
      Mockito.mock(BookDatabaseEntryFormatHandleEPUB::class.java)
    val drmHandle =
      Mockito.mock(BookDRMInformationHandleACS::class.java)

    val bookFormat =
      BookFormatEPUB(
        drmInformation = this.basicDRMInformationReturnable,
        file = null,
        lastReadLocation = null,
        bookmarks = listOf(),
        contentType = BookFormats.epubMimeTypes().first()
      )

    val bookId =
      BookID.create("a")

    val acquisition =
      OPDSAcquisition(
        OPDSAcquisition.Relation.ACQUISITION_BORROW,
        URI.create("http://www.example.com/0.feed"),
        mimeOf("application/epub+zip"),
        listOf()
      )

    val opdsEntryBuilder =
      OPDSAcquisitionFeedEntry.newBuilder(
        "a",
        "Title",
        DateTime.now(),
        OPDSAvailabilityOpenAccess.get(Option.none())
      )
    opdsEntryBuilder.addAcquisition(acquisition)

    val opdsEntry =
      opdsEntryBuilder.build()

    this.logBookEventsFor(bookId)

    val book =
      Book(
        id = bookId,
        account = this.accountID,
        cover = null,
        thumbnail = null,
        entry = opdsEntry,
        formats = listOf()
      )

    Mockito.`when`(account.id)
      .thenReturn(this.accountID)
    Mockito.`when`(account.loginState)
      .thenReturn(
        AccountLoginState.AccountLoggedIn(
          AccountAuthenticationCredentials.Basic(
            userName = AccountUsername("user"),
            password = AccountPassword("password"),
            adobeCredentials = AccountAuthenticationAdobePreActivationCredentials(
              vendorID = AdobeVendorID("NYPL"),
              clientToken = AccountAuthenticationAdobeClientToken.parse("NYNYPL|536818535|b54be3a5-385b-42eb-9496-3879cb3ac3cc|TWFuIHN1ZmZlcnMgb25seSBiZWNhdXNlIGhlIHRha2VzIHNlcmlvdXNseSB3aGF0IHRoZSBnb2RzIG1hZGUgZm9yIGZ1bi4K"),
              deviceManagerURI = null,
              postActivationCredentials = AccountAuthenticationAdobePostActivationCredentials(
                AdobeDeviceID("4361a1f6-aea8-4681-ad8b-df7e6923049f"),
                AdobeUserID("2bb42d71-42aa-4eb7-b8af-366171adcae8")
              )
            ),
            authenticationDescription = null
          )
        )
      )
    Mockito.`when`(profile.id)
      .thenReturn(this.profileID)
    Mockito.`when`(profilesDatabase.profiles())
      .thenReturn(ConcurrentSkipListMap(mapOf(this.profileID to profile)))
    Mockito.`when`(profile.account(this.accountID))
      .thenReturn(account)
    Mockito.`when`(account.provider)
      .thenReturn(accountProvider)
    Mockito.`when`(accountProvider.displayName)
      .thenReturn("Display name")
    Mockito.`when`(account.bookDatabase)
      .thenReturn(bookDatabase)
    Mockito.`when`(bookDatabase.entry(bookId))
      .thenReturn(bookDatabaseEntry)
    Mockito.`when`(bookDatabaseEntry.book)
      .thenReturn(book)
    Mockito.`when`(bookDatabaseEntry.findPreferredFormatHandle())
      .thenReturn(bookDatabaseFormatHandle)
    Mockito.`when`(bookDatabaseFormatHandle.drmInformationHandle)
      .thenReturn(drmHandle)
    Mockito.`when`(drmHandle.info)
      .thenReturn(this.basicDRMInformationReturnable)
    Mockito.`when`(drmHandle.setAdobeRightsInformation(Mockito.any()))
      .thenThrow(IOException("Ouch!"))
    Mockito.`when`(bookDatabaseFormatHandle.format)
      .thenReturn(bookFormat)

    /*
     * When the code tells the connector to return the loan, it succeeds if the connector reports
     * success.
     */

    Mockito.`when`(
      this.adobeConnector.loanReturn(
        Mockito.any(),
        Mockito.any(),
        Mockito.any()
      )
    ).then { invocation ->
      this.logger.debug("executing loanReturn: {}", invocation)
      val receiver = invocation.arguments[0] as AdobeAdeptLoanReturnListenerType
      receiver.onLoanReturnSuccess()
      Unit
    }

    Mockito.`when`(this.adobeExecutor.execute(anyNonNull()))
      .then { invocation ->
        this.logger.debug("executing procedure: {}", invocation)
        val procedure = invocation.arguments[0] as AdobeAdeptProcedureType
        procedure.executeWith(this.adobeConnector)
      }

    val task =
      BookRevokeTask(
        accountID = account.id,
        profileID = profile.id,
        profiles = profilesDatabase,
        adobeDRM = adobeExecutor,
        bookID = bookId,
        bookRegistry = this.bookRegistry,
        feedLoader = this.feedLoader,
        revokeStrings = this.bookRevokeStrings
      )

    val result = task.call()
    TaskDumps.dump(logger, result)
    result as TaskResult.Failure

    Assertions.assertEquals(IOException::class.java, result.steps.last().resolution.exception!!::class.java)

    Mockito.verify(bookDatabaseEntry, Times(0)).delete()
  }

  private fun <T> optionUnsafe(opt: OptionType<T>): T {
    return if (opt is Some<T>) {
      opt.get()
    } else {
      throw IllegalStateException("Expected something, got nothing!")
    }
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

  private fun resource(file: String): InputStream? {
    return BookRevokeTaskAdobeDRMTest::class.java.getResourceAsStream(file)
  }

  @Throws(IOException::class)
  private fun resourceSize(file: String): Long {
    var total = 0L
    val buffer = ByteArray(8192)
    this.resource(file)?.use { stream ->
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

  private fun mimeOf(name: String): MIMEType {
    return MIMEParser.parseRaisingException(name)
  }
}
