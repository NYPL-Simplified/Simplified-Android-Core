package org.nypl.simplified.migration.from3master

import android.content.Context
import android.os.Environment
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerPositions
import org.librarysimplified.audiobook.api.PlayerResult
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeLoanID
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEventCreation
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkKind
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.migration.spi.MigrationEvent
import org.nypl.simplified.migration.spi.MigrationEvent.MigrationStepError
import org.nypl.simplified.migration.spi.MigrationEvent.MigrationStepInProgress
import org.nypl.simplified.migration.spi.MigrationEvent.MigrationStepSucceeded
import org.nypl.simplified.migration.spi.MigrationEvent.Subject
import org.nypl.simplified.migration.spi.MigrationEvent.Subject.ACCOUNT
import org.nypl.simplified.migration.spi.MigrationEvent.Subject.BOOK
import org.nypl.simplified.migration.spi.MigrationEvent.Subject.BOOKMARK
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.migration.spi.MigrationType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI

/**
 * A migration from version 3.0 of the app (2019 pre-LFA master branch).
 */

class MigrationFrom3Master(
  private val environment: EnvironmentQueriesType,
  private val strings: MigrationFrom3MasterStringResourcesType,
  private val services: MigrationServiceDependencies
) : MigrationType {

  private val logger =
    LoggerFactory.getLogger(MigrationFrom3Master::class.java)

  /**
   * The old master branch always stored accounts and DRM information on internal storage.
   * Actual books and bookmarks may have been stored on external storaged based on
   * [determineDiskDataDirectory].
   */

  private val oldBaseAccountsDirectory =
    this.services.context.filesDir

  private val oldBaseDataDirectory =
    this.determineDiskDataDirectory(this.services.context)

  private val objectMapper = ObjectMapper()
  private val noticesLog = mutableListOf<MigrationEvent>()
  private val noticesObservable = PublishSubject.create<MigrationEvent>()
  private val filesToDelete = mutableListOf<File>()

  /**
   * The presence of any of these files will cause the migration to run.
   */

  private val filesTriggering =
    listOf("accounts", "activation.xml", "device", "device.xml", "salt")
      .map { name -> File(this.oldBaseAccountsDirectory, name).absoluteFile }

  init {
    this.noticesObservable.subscribe { notice -> this.noticesLog.add(notice) }
  }

  override val events: Observable<MigrationEvent> =
    this.noticesObservable

  private fun publishStepSucceeded(subject: Subject, message: String) {
    this.noticesObservable.onNext(
      MigrationStepSucceeded(
        message = message,
        subject = subject
      )
    )
  }

  private fun publishStepSucceeded(message: String) {
    this.noticesObservable.onNext(MigrationStepSucceeded(message))
  }

  private fun publishStepProgress(subject: Subject, message: String) {
    this.noticesObservable.onNext(MigrationStepInProgress(message, subject))
  }

  private fun publishStepError(error: MigrationStepError) {
    this.noticesObservable.onNext(error)
  }

  override fun needsToRun(): Boolean {
    if (!this.services.applicationProfileIsAnonymous) {
      this.logger.debug("application is not in anonymous mode, cannot migrate!")
      return false
    }

    return this.filesTriggering.any { file ->
      if (file.exists()) {
        this.logger.debug("file {} exists, so migration is needed", file)
        true
      } else {
        this.logger.debug("file {} does not exist", file)
        false
      }
    }
  }

  data class EnumeratedAccount(
    val baseDirectory: File,
    val adobeDeviceXML: File?,
    val idURI: URI,
    val idNumeric: Int
  )

  data class LoadedAccount(
    val enumeratedAccount: EnumeratedAccount,
    val booksDirectory: File,
    val booksDataDirectory: File,
    val account: MigrationFrom3MasterAccount?,
    val accountSubDirectory: File,
    val accountFile: File
  )

  data class CreatedAccount(
    val loadedAccount: LoadedAccount,
    val account: AccountType
  )

  data class LoadedBook(
    val owner: CreatedAccount,
    val bookID: BookID,
    val bookDirectory: File,
    val bookEntry: OPDSAcquisitionFeedEntry,
    val epubFile: File,
    val epubAdobeLoan: AdobeAdeptLoan?,
    val epubBookmarks: List<Bookmark>?,
    val audioBookPosition: PlayerPosition?,
    val audioBookManifest: BookFormat.AudioBookManifestReference?
  )

  data class CopiedBook(
    val loadedBook: LoadedBook
  )

  override fun run(): MigrationReport {
    val subscription =
      this.services.accountEvents.subscribe { event ->
        when (event) {
          is AccountEventCreation,
          is AccountEventDeletion,
          is AccountEventLoginStateChanged ->
            this.publishStepProgress(ACCOUNT, event.message)
        }
      }

    try {
      val time = LocalDateTime.now()

      val accounts = this.enumerateAccountsToMigrate()
      this.logger.debug("{} accounts to migrate", accounts.size)

      val loadedAccounts = this.loadAccounts(accounts)
      this.logger.debug("{} accounts loaded", loadedAccounts.size)

      var createdAccounts = 0
      for (loadedAccount in loadedAccounts) {
        val createdAccount = this.createAccount(loadedAccount)
        if (createdAccount != null) {
          this.publishStepSucceeded(ACCOUNT, this.strings.successCreatedAccount(createdAccount.account.provider.displayName))
          ++createdAccounts

          this.pushLoadedAccountToDeletionQueue(createdAccount.loadedAccount)

          val books = this.loadBooksForAccount(createdAccount)
          val copiedBooks = this.copyBooks(books)
          for (copiedBook in copiedBooks) {
            this.publishStepSucceeded(BOOK, this.strings.successCopiedBook(copiedBook.loadedBook.bookEntry.title))
          }
          this.authenticateAccount(createdAccount)
        }
      }

      /*
       * If there are no accounts left that couldn't be migrated, add all of the triggering files
       * to the deletion queue so that the migration is not prompted to run again.
       */

      if (accounts.size == createdAccounts) {
        this.filesTriggering.forEach(this::pushFileToDeletionQueue)
      }

      for (file in this.filesToDelete.filter(this::isSafeToDelete)) {
        this.logger.debug("delete: {}", file)
        try {
          if (file.isFile) {
            FileUtilities.fileDelete(file)
          } else {
            DirectoryUtilities.directoryDelete(file)
          }
        } catch (e: Exception) {
          this.logger.error("could not delete file: {}: ", file, e)
        }
      }

      this.publishStepSucceeded(this.strings.successDeletedOldData)
      return MigrationReport(
        this.services.applicationVersion,
        this.javaClass.canonicalName ?: "unknown",
        time,
        this.noticesLog.toList()
      )
    } finally {
      subscription.dispose()
    }
  }

  private fun pushLoadedAccountToDeletionQueue(l: LoadedAccount) {
    this.pushFileToDeletionQueue(l.booksDataDirectory)
    this.pushFileToDeletionQueue(l.booksDirectory)
    this.pushFileToDeletionQueue(l.accountFile)
    this.pushFileToDeletionQueue(l.accountSubDirectory)
  }

  private fun isSafeToDelete(file: File): Boolean {
    val context = this.services.context
    return file != context.filesDir && file != context.getExternalFilesDir(null)
  }

  private fun authenticateAccount(createdAccount: CreatedAccount) {
    this.logger.debug("authenticating account {}", createdAccount.account.id)

    val accountTitle = createdAccount.account.provider.displayName
    return try {
      when (createdAccount.account.provider.authentication) {
        is AccountProviderAuthenticationDescription.COPPAAgeGate,
        is AccountProviderAuthenticationDescription.Anonymous -> {
          this.publishStepSucceeded(
            ACCOUNT, this.strings.successAuthenticatedAccountNotRequired(accountTitle)
          )
          return
        }
        is AccountProviderAuthenticationDescription.Basic -> {
        }
      }

      val accountData = createdAccount.loadedAccount.account
      if (accountData == null) {
        this.publishStepError(
          MigrationStepError(
            message = this.strings.errorAccountAuthenticationNoCredentials(accountTitle),
            exception = java.lang.Exception("Missing credentials"),
            attributes = mapOf(
              Pair("accountID", createdAccount.account.id.uuid.toString()),
              Pair("accountTitle", accountTitle)
            )
          )
        )
        return
      }

      val credentials =
        AccountAuthenticationCredentials.Basic(
          userName = AccountUsername(accountData.username),
          password = AccountPassword(accountData.password),
          adobeCredentials = null,
          authenticationDescription = null,
          annotationsURI = null
        )

      when (val taskResult = this.services.loginAccount(createdAccount.account, credentials)) {
        is TaskResult.Success ->
          this.publishStepSucceeded(ACCOUNT, this.strings.successAuthenticatedAccount(accountTitle))
        is TaskResult.Failure ->
          this.publishStepError(MigrationStepError(taskResult.message))
      }
    } catch (e: Exception) {
      this.logger.error("failed to authenticate account: ", e)
      this.publishStepError(
        MigrationStepError(
          message = this.strings.errorAccountAuthenticationFailure(accountTitle),
          attributes = mapOf(
            Pair("accountID", createdAccount.account.id.uuid.toString()),
            Pair("accountTitle", accountTitle)
          ),
          exception = e
        )
      )
    }
  }

  /**
   * Load Adobe loan information from the given rights and JSON metadata files.
   */

  private fun loadAdobeRights(
    fileAdobeRights: File,
    fileAdobeMeta: File
  ): AdobeAdeptLoan {
    val serialized = FileUtilities.fileReadBytes(fileAdobeRights)
    val objectMapper = ObjectMapper()
    val rootNode = objectMapper.readTree(fileAdobeMeta)
    val rootObject = JSONParserUtilities.checkObject(null, rootNode)
    val loanID = AdobeLoanID(JSONParserUtilities.getString(rootObject, "loan-id"))
    val returnable = JSONParserUtilities.getBoolean(rootObject, "returnable")
    return AdobeAdeptLoan(loanID, serialized, returnable)
  }

  /**
   * Copy all of the loaded books to their respective modern accounts.
   */

  private fun copyBooks(loadedBooks: List<LoadedBook>): List<CopiedBook> {
    this.logger.debug("copying {} books", loadedBooks.size)
    val copiedBooks = mutableListOf<CopiedBook>()
    for (book in loadedBooks) {
      this.copyBook(book)?.let { copied -> copiedBooks.add(copied) }
    }
    return copiedBooks.toList()
  }

  /**
   * Copy the loaded book to its respective modern account.
   */

  private fun copyBook(book: LoadedBook): CopiedBook? {
    this.logger.debug("copying book {}", book.bookID.value())

    val account = book.owner.account
    val bookDatabase = account.bookDatabase

    val entry = bookDatabase.createOrUpdate(book.bookID, book.bookEntry)
    entry.writeOPDSEntry(book.bookEntry)

    val formatHandle = entry.findPreferredFormatHandle()
    return when (formatHandle) {
      is BookDatabaseEntryFormatHandleEPUB ->
        this.copyBookEPUB(formatHandle, book)
      is BookDatabaseEntryFormatHandleAudioBook ->
        this.copyBookAudioBook(formatHandle, book)

      is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF,
      null -> {
        this.logger.error("strange format handle encountered")
        val formatName = this.formatHandleName(formatHandle)
        val exception = Exception("Unexpected format: $formatName")
        this.publishStepError(
          MigrationStepError(
            message = this.strings.errorBookUnexpectedFormat(book.bookEntry.title, formatName),
            attributes = mapOf(
              Pair("bookTitle", book.bookEntry.title),
              Pair("bookFormat", formatName),
              Pair("bookID", book.bookID.value())
            ),
            exception = exception
          )
        )
        null
      }
    }
  }

  private fun formatHandleName(handle: BookDatabaseEntryFormatHandle?): String =
    when (handle) {
      is BookDatabaseEntryFormatHandleEPUB -> "EPUB"
      is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF -> "PDF"
      is BookDatabaseEntryFormatHandleAudioBook -> "Audio book"
      null -> "Unknown"
    }

  private fun copyBookAudioBook(
    handle: BookDatabaseEntryFormatHandleAudioBook,
    book: LoadedBook
  ): CopiedBook {
    if (book.audioBookManifest != null) {
      handle.copyInManifestAndURI(
        data = book.audioBookManifest.manifestFile.readBytes(),
        manifestURI = book.audioBookManifest.manifestURI
      )
    }
    if (book.audioBookPosition != null) {
      handle.savePlayerPosition(book.audioBookPosition)
    }
    return CopiedBook(book)
  }

  private fun copyBookEPUB(
    handle: BookDatabaseEntryFormatHandleEPUB,
    book: LoadedBook
  ): CopiedBook? {
    var result: CopiedBook? = CopiedBook(book)

    try {
      if (book.epubFile.isFile) {
        handle.copyInBook(book.epubFile)
      }
    } catch (e: Exception) {
      this.logger.error("failed to copy epub: ", e)
      this.publishStepError(
        MigrationStepError(
          message = this.strings.errorBookCopyFailure(book.bookEntry.title),
          attributes = mapOf(
            Pair("bookTitle", book.bookEntry.title),
            Pair("bookID", book.bookID.value())
          ),
          exception = e
        )
      )
      result = null
    }

    try {
      if (book.epubBookmarks != null) {
        handle.setBookmarks(book.epubBookmarks)
        this.publishStepSucceeded(BOOKMARK, this.strings.successCopiedBookmarks(book.bookEntry.title, book.epubBookmarks.size))
        handle.setLastReadLocation(
          book.epubBookmarks.find { bookmark ->
            bookmark.kind == BookmarkKind.ReaderBookmarkLastReadLocation
          }
        )
      }
    } catch (e: Exception) {
      this.logger.error("failed to copy bookmarks: ", e)
      this.publishStepError(
        MigrationStepError(
          message = this.strings.errorBookmarksCopyFailure(book.bookEntry.title),
          attributes = mapOf(
            Pair("bookTitle", book.bookEntry.title),
            Pair("bookID", book.bookID.value())
          ),
          exception = e
        )
      )
      result = null
    }

    try {
      when (val drm = handle.drmInformationHandle) {
        is BookDRMInformationHandle.ACSHandle ->
          drm.setAdobeRightsInformation(book.epubAdobeLoan)
        is BookDRMInformationHandle.LCPHandle,
        is BookDRMInformationHandle.NoneHandle -> {
          // Nothing required
        }
      }
    } catch (e: Exception) {
      this.logger.error("failed to copy adobe DRM information: ", e)
      this.publishStepError(
        MigrationStepError(
          message = this.strings.errorBookAdobeDRMCopyFailure(book.bookEntry.title),
          attributes = mapOf(
            Pair("bookTitle", book.bookEntry.title),
            Pair("bookID", book.bookID.value())
          ),
          exception = e
        )
      )
      result = null
    }

    return result
  }

  /**
   * Load all of the books from a given account.
   */

  private fun loadBooksForAccount(createdAccount: CreatedAccount): List<LoadedBook> {
    val booksDirectory = createdAccount.loadedAccount.booksDataDirectory

    this.logger.debug("enumerating books directory {}", booksDirectory)
    val entries = booksDirectory.list()
    if (entries == null) {
      this.logger.error("could not list {}", booksDirectory)
      return listOf()
    }

    val loadedBooks = mutableListOf<LoadedBook>()
    for (entry in entries) {
      val bookDirectory = File(booksDirectory, entry)

      this.logger.debug("loading book {}", entry)

      try {
        val fileMeta =
          File(bookDirectory, "meta.json")

        val parser = OPDSJSONParser.newParser()
        val bookEntry =
          FileInputStream(fileMeta).use { stream ->
            parser.parseAcquisitionFeedEntryFromStream(stream)
          }

        try {
          val fileEPUB =
            File(bookDirectory, "book.epub")
          val fileAdobeRights =
            File(bookDirectory, "rights_adobe.xml")
          val fileAdobeMeta =
            File(bookDirectory, "meta_adobe.json")
          val fileAnnotations =
            File(bookDirectory, "annotations.json")
          val fileAudioManifest =
            File(bookDirectory, "audiobook-manifest.json")
          val fileAudioManifestURI =
            File(bookDirectory, "audiobook-manifest-uri.txt")
          val fileAudioPosition =
            File(bookDirectory, "audiobook-position.json")

          val epubAdobeLoan =
            if (fileAdobeRights.isFile && fileAdobeMeta.isFile) {
              this.loadAdobeRights(fileAdobeRights, fileAdobeMeta)
            } else {
              null
            }

          val epubBookmarks =
            if (fileAnnotations.isFile) {
              this.loadBookmarks(bookEntry.title, fileAnnotations)
            } else {
              null
            }

          val audioBookPosition =
            this.loadAudioPlayerPositionOptionally(fileAudioPosition)

          val audioBookManifest =
            if (fileAudioManifest.isFile) {
              BookFormat.AudioBookManifestReference(
                manifestURI = this.loadAudioPlayerManifestURI(fileAudioManifestURI),
                manifestFile = fileAudioManifest
              )
            } else {
              null
            }

          loadedBooks.add(
            LoadedBook(
              owner = createdAccount,
              bookID = BookIDs.newFromOPDSEntry(bookEntry),
              bookDirectory = bookDirectory,
              bookEntry = bookEntry,
              epubFile = fileEPUB,
              epubAdobeLoan = epubAdobeLoan,
              epubBookmarks = epubBookmarks,
              audioBookManifest = audioBookManifest,
              audioBookPosition = audioBookPosition
            )
          )
        } catch (e: Exception) {
          this.logger.error("could not load book: ", e)
          this.publishStepError(
            MigrationStepError(
              message = this.strings.errorBookLoadTitledFailure(bookEntry.title),
              attributes = mapOf(
                Pair("bookDirectory", bookDirectory.toString()),
                Pair("bookTitle", bookEntry.title)
              ),
              exception = e
            )
          )
        }
      } catch (e: Exception) {
        this.logger.error("could not load book: ", e)
        this.publishStepError(
          MigrationStepError(
            message = this.strings.errorBookLoadFailure(entry),
            attributes = mapOf(Pair("bookDirectory", bookDirectory.toString())),
            exception = e
          )
        )
      }
    }
    return loadedBooks.toList()
  }

  /**
   * Load bookmarks from the given file.
   */

  private fun loadBookmarks(
    title: String,
    file: File
  ): List<Bookmark>? {
    return try {
      FileInputStream(file).use { stream ->
        this.parseBookmarks(stream).mapNotNull { annotation ->
          this.parseAnnotationToBookmark(title, annotation)
        }
      }
    } catch (e: Exception) {
      this.logger.error("could not parse bookmarks: ", e)
      this.publishStepError(
        MigrationStepError(
          message = this.strings.errorBookmarksParseFailure(title),
          exception = e,
          attributes = mapOf(
            Pair("bookTitle", title)
          ),
          subject = BOOKMARK
        )
      )
      null
    }
  }

  /**
   * Parse bookmarks from the given stream.
   */

  private fun parseBookmarks(stream: InputStream): List<BookmarkAnnotation> {
    val mapper = ObjectMapper()
    val jsonObj: Map<String, List<BookmarkAnnotation>> =
      mapper.readValue(
        stream,
        object : TypeReference<Map<String, List<BookmarkAnnotation>>>() {
        }
      )
    return jsonObj["bookmarks"] ?: listOf()
  }

  /**
   * Map a bookmark from an old-style annotation to a modern bookmark.
   */

  private fun parseAnnotationToBookmark(
    title: String,
    annotation: BookmarkAnnotation
  ): Bookmark? {
    val formatter = ISODateTimeFormat.dateTimeParser()

    return try {
      val bookLocation =
        BookLocation.BookLocationR1(null, null, "x")
      val kind =
        BookmarkKind.ofMotivation(annotation.motivation)
      val time =
        formatter.parseDateTime(annotation.body.timestamp)
      val chapterTitle =
        annotation.body.chapterTitle ?: ""
      val bookProgress =
        annotation.body.bookProgress?.toDouble() ?: 0.0
      val deviceId =
        annotation.body.device
      val uri =
        annotation.id?.let(::URI)

      Bookmark.create(
        opdsId = annotation.target.source,
        location = bookLocation,
        kind = kind,
        time = time,
        chapterTitle = chapterTitle,
        bookProgress = bookProgress,
        deviceID = deviceId,
        uri = uri
      )
    } catch (e: Exception) {
      this.logger.error("could not parse bookmarks: ", e)
      this.publishStepError(
        MigrationStepError(
          message = this.strings.errorBookmarksParseFailure(title),
          exception = e,
          attributes = mapOf(
            Pair("bookTitle", title)
          ),
          subject = BOOKMARK
        )
      )
      null
    }
  }

  /**
   * Try to load an audio player manifest URI from the given file.
   */

  private fun loadAudioPlayerManifestURI(fileAudioManifestURI: File): URI {
    return URI.create(FileUtilities.fileReadUTF8(fileAudioManifestURI))
  }

  /**
   * Try to load an audio player position from the given file, failing quietly if the file
   * does not exist.
   */

  private fun loadAudioPlayerPositionOptionally(fileAudioPosition: File): PlayerPosition? {
    return try {
      FileInputStream(fileAudioPosition).use { stream ->
        val objectMapper = ObjectMapper()
        val result =
          PlayerPositions.parseFromObjectNode(
            JSONParserUtilities.checkObject(null, objectMapper.readTree(stream))
          )

        when (result) {
          is PlayerResult.Success -> result.result
          is PlayerResult.Failure -> throw result.failure
        }
      }
    } catch (e: FileNotFoundException) {
      null
    } catch (e: Exception) {
      throw e
    }
  }

  /**
   * Create a modern account based on the given loaded account.
   */

  private fun createAccount(account: LoadedAccount): CreatedAccount? {
    return when (
      val taskResult = this.services.createAccount(account.enumeratedAccount.idURI)
    ) {
      is TaskResult.Success -> {
        CreatedAccount(account, taskResult.result)
      }
      is TaskResult.Failure -> {
        this.publishStepError(
          MigrationStepError(
            message = taskResult.message,
            exception = taskResult.exception?.let { java.lang.Exception(it) },
            attributes = taskResult.attributes,
            subject = ACCOUNT
          )
        )
        null
      }
    }
  }

  /**
   * Load all of the enumerated accounts.
   */

  private fun loadAccounts(accounts: List<EnumeratedAccount>): List<LoadedAccount> {
    val loadedAccounts = mutableListOf<LoadedAccount>()
    for (account in accounts) {
      this.loadAccount(account)?.let { loadedAccounts.add(it) }
    }
    return loadedAccounts.toList()
  }

  /**
   * Try to load a single account.
   */

  private fun loadAccount(account: EnumeratedAccount): LoadedAccount? {
    this.publishStepProgress(ACCOUNT, this.strings.progressLoadingAccount(account.idNumeric))

    return try {
      val accountAccounts = File(account.baseDirectory, "accounts")
      val accountFile = File(accountAccounts, "account.json")

      val accountData =
        if (accountFile.isFile) {
          FileInputStream(accountFile).use { stream ->
            this.objectMapper.readValue(stream, MigrationFrom3MasterAccount::class.java)
          }
        } else {
          null
        }

      this.logger.debug("loading books for account {}", account.idNumeric)
      val booksDirectory =
        if (account.idNumeric == 0) {
          File(this.oldBaseDataDirectory, "books")
        } else {
          File(File(this.oldBaseDataDirectory, account.idNumeric.toString()), "books")
        }

      val booksDataDirectory =
        File(booksDirectory, "data")

      LoadedAccount(
        enumeratedAccount = account,
        booksDirectory = booksDirectory,
        booksDataDirectory = booksDataDirectory,
        accountSubDirectory = accountAccounts,
        accountFile = accountFile,
        account = accountData
      )
    } catch (e: java.lang.Exception) {
      this.logger.error("could not load account: ", e)
      this.publishStepError(
        MigrationStepError(
          message = this.strings.errorAccountLoadFailure(account.idNumeric),
          attributes = mapOf(
            Pair("idNumeric", account.idNumeric.toString())
          ),
          exception = e
        )
      )
      null
    }
  }

  private fun enumerateAccountsToMigrate(): List<EnumeratedAccount> {
    val enumeratedAccounts = mutableListOf<EnumeratedAccount>()

    /*
     * For reasons unknown to anyone, NYPL accounts (and _only_ NYPL accounts) weren't placed
     * in a numbered directory. They were simply placed directly in the base directory to ensure
     * that every bit of code in the app that dealt with directories had to special case them.
     */

    val nyplAccounts = File(this.oldBaseAccountsDirectory, "accounts")
    if (nyplAccounts.isDirectory) {
      this.enumerateAccount(
        accountDirectory = this.oldBaseAccountsDirectory,
        idNumeric = 0
      )
        ?.let { enumeratedAccounts.add(it) }
    }

    /*
     * For non NYPL accounts, each account has a numbered directory where the name of the
     * directory corresponds to the integer ID of the account provider. For each directory
     * that appears to be a number, we assume it's an account and try to look up the corresponding
     * provider.
     */

    val baseFiles = this.oldBaseAccountsDirectory.list()
    if (baseFiles != null) {
      for (file in baseFiles) {
        val id = try {
          file.toInt()
        } catch (e: NumberFormatException) {
          null
        }

        if (id != null) {
          val accountDirectory = File(this.oldBaseAccountsDirectory, id.toString())
          this.enumerateAccount(
            accountDirectory = accountDirectory,
            idNumeric = id
          )
            ?.let { enumeratedAccounts.add(it) }
        }
      }
    }

    return enumeratedAccounts.toList()
  }

  private fun enumerateAccount(
    accountDirectory: File,
    idNumeric: Int
  ): EnumeratedAccount? {
    val adobeDeviceXML = File(this.oldBaseAccountsDirectory, "device.xml")
    val idURI = MigrationFrom3MasterProviders.providers[idNumeric]

    if (idURI == null) {
      this.logger.error("no account provider for id $idNumeric")
      this.publishStepError(
        MigrationStepError(
          message = this.strings.errorUnknownAccountProvider(idNumeric),
          attributes = mapOf(Pair("idNumeric", idNumeric.toString())),
          exception = Exception()
        )
      )
      return null
    }

    this.logger.debug("will migrate account $idNumeric -> $idURI")
    return EnumeratedAccount(
      baseDirectory = accountDirectory,
      adobeDeviceXML = this.isFileOrNull(adobeDeviceXML),
      idURI = idURI,
      idNumeric = idNumeric
    )
  }

  private fun pushFileToDeletionQueue(file: File) {
    this.logger.debug("queueing file for deletion: {}", file)
    this.filesToDelete.add(file)
  }

  private fun isFileOrNull(file: File): File? {
    return if (file.isFile) {
      file
    } else {
      null
    }
  }

  /**
   * Try to determine which directory will be used to hold data.
   */

  private fun determineDiskDataDirectory(context: Context): File {
    /*
     * If external storage is mounted and is on a device that doesn't allow
     * the storage to be removed, use the external storage for data.
     */

    if (Environment.MEDIA_MOUNTED == this.environment.getExternalStorageState()) {
      this.logger.debug("trying external storage")
      if (!this.environment.isExternalStorageRemovable()) {
        val result = context.getExternalFilesDir(null)
        this.logger.debug("external storage is not removable, using it ({})", result)
        Preconditions.checkArgument(result!!.isDirectory, "Data directory {} is a directory", result)
        return result
      }
    }

    /*
     * Otherwise, use internal storage.
     */

    val result = context.filesDir
    this.logger.debug("no non-removable external storage, using internal storage ({})", result)
    Preconditions.checkArgument(result.isDirectory, "Data directory {} is a directory", result)
    return result
  }
}
