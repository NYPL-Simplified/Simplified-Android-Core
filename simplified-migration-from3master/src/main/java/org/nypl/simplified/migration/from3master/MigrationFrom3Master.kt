package org.nypl.simplified.migration.from3master

import android.content.Context
import android.os.Environment
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerPositions
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeLoanID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.migration.spi.MigrationNotice
import org.nypl.simplified.migration.spi.MigrationNotice.MigrationError
import org.nypl.simplified.migration.spi.MigrationNotice.MigrationInfo
import org.nypl.simplified.migration.spi.MigrationNotice.Subject
import org.nypl.simplified.migration.spi.MigrationNotice.Subject.ACCOUNT
import org.nypl.simplified.migration.spi.MigrationNotice.Subject.BOOK
import org.nypl.simplified.migration.spi.MigrationReport
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.migration.spi.MigrationType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.net.URI
import java.nio.ByteBuffer

/**
 * A migration from version 3.0 of the app (2019 pre-LFA master branch).
 */

class MigrationFrom3Master(
  private val environment: EnvironmentQueriesType,
  private val strings: MigrationFrom3MasterStringResourcesType,
  private val services: MigrationServiceDependencies) : MigrationType {

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
  private val notices = mutableListOf<MigrationNotice>()

  private fun publishNotice(message: String) {
    this.notices.add(MigrationInfo(message))
  }

  private fun publishNotice(subject: Subject, message: String) {
    this.notices.add(MigrationInfo(message, subject))
  }

  override fun needsToRun(): Boolean {
    if (!this.services.applicationProfileIsAnonymous) {
      this.logger.debug("application is not in anonymous mode, cannot migrate!")
      return false
    }

    val candidates =
      listOf("accounts", "activation.xml", "device", "device.xml", "salt")
        .map { name -> File(this.oldBaseAccountsDirectory, name) }

    return candidates.any { file ->
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
    val idNumeric: Int)

  data class LoadedAccount(
    val enumeratedAccount: EnumeratedAccount,
    val account: MigrationFrom3MasterAccount)

  data class CreatedAccount(
    val loadedAccount: LoadedAccount,
    val account: AccountType)

  data class LoadedBook(
    val owner: CreatedAccount,
    val bookID: BookID,
    val bookDirectory: File,
    val bookEntry: OPDSAcquisitionFeedEntry,
    val epubFile: File,
    val epubAdobeLoan: AdobeAdeptLoan?,
    val audioBookPosition: PlayerPosition?,
    val audioBookManifest: BookFormat.AudioBookManifestReference?)

  data class CopiedBook(
    val loadedBook: LoadedBook)

  override fun run(): MigrationReport {
    val accounts = this.enumerateAccountsToMigrate()
    this.logger.debug("{} accounts to migrate", accounts.size)
    val loadedAccounts = this.loadAccounts(accounts)
    this.logger.debug("{} accounts loaded", loadedAccounts.size)

    for (loadedAccount in loadedAccounts) {
      val createdAccount = this.createAccount(loadedAccount)
      if (createdAccount != null) {
        this.publishNotice(ACCOUNT, this.strings.reportCreatedAccount(createdAccount.account.provider.displayName))
        val books = this.loadBooksForAccount(createdAccount)
        val copiedBooks = this.copyBooks(books)
        for (copiedBook in copiedBooks) {
          this.publishNotice(BOOK, this.strings.reportCopiedBook(copiedBook.loadedBook.bookEntry.title))
        }
      }
    }

    return MigrationReport(this.notices.toList())
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
    return AdobeAdeptLoan(loanID, ByteBuffer.wrap(serialized), returnable)
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
        val formatName = formatHandleName(formatHandle)
        val exception = Exception("Unexpected format: $formatName")
        this.notices.add(MigrationError(
          message = this.strings.errorBookUnexpectedFormat(book.bookEntry.title, formatName),
          attributes = mapOf(
            Pair("bookTitle", book.bookEntry.title),
            Pair("bookFormat", formatName),
            Pair("bookID", book.bookID.value())
          ),
          exception = exception))
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
        file = book.audioBookManifest.manifestFile,
        manifestURI = book.audioBookManifest.manifestURI)
    }
    if (book.audioBookPosition != null) {
      handle.savePlayerPosition(book.audioBookPosition)
    }
    return CopiedBook(book)
  }

  private fun copyBookEPUB(
    handle: BookDatabaseEntryFormatHandleEPUB,
    book: LoadedBook
  ): CopiedBook {
    if (book.epubFile.isFile) {
      handle.copyInBook(book.epubFile)
    }
    handle.setAdobeRightsInformation(book.epubAdobeLoan)
    return CopiedBook(book)
  }

  /**
   * Load all of the books from the given list of accounts.
   */

  private fun loadBooks(createdAccounts: List<CreatedAccount>): List<LoadedBook> {
    val enumeratedBooks = mutableListOf<LoadedBook>()
    for (createdAccount in createdAccounts) {
      enumeratedBooks.addAll(this.loadBooksForAccount(createdAccount))
    }
    return enumeratedBooks.toList()
  }

  /**
   * Load all of the books from a given account.
   */

  private fun loadBooksForAccount(createdAccount: CreatedAccount): List<LoadedBook> {
    val idNumeric = createdAccount.loadedAccount.enumeratedAccount.idNumeric
    this.logger.debug("loading books for account {}", idNumeric)
    val booksDirectory =
      if (idNumeric == 0) {
        File(File(this.oldBaseDataDirectory, "books"), "data")
      } else {
        File(File(File(this.oldBaseDataDirectory, idNumeric.toString()), "books"), "data")
      }

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

          val audioBookPosition =
            this.loadAudioPlayerPositionOptionally(fileAudioPosition)

          val audioBookManifest =
            if (fileAudioManifest.isFile) {
              BookFormat.AudioBookManifestReference(
                manifestURI = this.loadAudioPlayerManifestURI(fileAudioManifestURI),
                manifestFile = fileAudioManifest)
            } else {
              null
            }

          loadedBooks.add(LoadedBook(
            owner = createdAccount,
            bookID = BookIDs.newFromOPDSEntry(bookEntry),
            bookDirectory = bookDirectory,
            bookEntry = bookEntry,
            epubFile = fileEPUB,
            epubAdobeLoan = epubAdobeLoan,
            audioBookManifest = audioBookManifest,
            audioBookPosition = audioBookPosition
          ))
        } catch (e: Exception) {
          this.logger.error("could not load book: ", e)
          this.notices.add(MigrationError(
            message = this.strings.errorBookLoadTitledFailure(bookEntry.title),
            attributes = mapOf(
              Pair("bookDirectory", bookDirectory.toString()),
              Pair("bookTitle", bookEntry.title)
            ),
            exception = e))
        }
      } catch (e: Exception) {
        this.logger.error("could not load book: ", e)
        this.notices.add(MigrationError(
          message = this.strings.errorBookLoadFailure(entry),
          attributes = mapOf(Pair("bookDirectory", bookDirectory.toString())),
          exception = e))
      }
    }
    return loadedBooks.toList()
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
            JSONParserUtilities.checkObject(null, objectMapper.readTree(stream)))

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
   * Create modern accounts based on the given loaded accounts.
   */

  private fun createAccounts(accounts: List<LoadedAccount>): List<CreatedAccount> {
    val createdAccounts = mutableListOf<CreatedAccount>()
    for (account in accounts) {
      this.createAccount(account)?.let { createdAccounts.add(it) }
    }
    return createdAccounts.toList()
  }

  /**
   * Create a modern account based on the given loaded account.
   */

  private fun createAccount(account: LoadedAccount): CreatedAccount? {
    return when (val taskResult =
      this.services.createAccount(account.enumeratedAccount.idURI)) {
      is TaskResult.Success -> {
        CreatedAccount(account, taskResult.result)
      }
      is TaskResult.Failure -> {
        val message = taskResult.steps.last().resolution.message
        val causes =
          taskResult.steps.mapNotNull { step ->
            when (val resolution = step.resolution) {
              is TaskStepResolution.TaskStepSucceeded -> null
              is TaskStepResolution.TaskStepFailed -> resolution.errorValue
            }
          }

        this.notices.add(MigrationError(message = message, causes = causes))
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
    return try {
      val accountFile = File(account.baseDirectory, "account.json")
      val accountData = FileInputStream(accountFile).use { stream ->
        this.objectMapper.readValue(stream, MigrationFrom3MasterAccount::class.java)
      }
      LoadedAccount(account, accountData)
    } catch (e: java.lang.Exception) {
      this.logger.error("could not load account: ", e)
      this.notices.add(MigrationError(
        message = this.strings.errorAccountLoadFailure(account.idNumeric),
        attributes = mapOf(Pair("idNumeric", account.idNumeric.toString())),
        exception = e))
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
        idNumeric = 0)
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
          this.enumerateAccount(
            accountDirectory = File(this.oldBaseAccountsDirectory, id.toString()),
            idNumeric = id)
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
      this.notices.add(MigrationError(
        message = this.strings.errorUnknownAccountProvider(idNumeric),
        attributes = mapOf(Pair("idNumeric", idNumeric.toString())),
        exception = Exception()))
      return null
    }

    this.logger.debug("will migrate account $idNumeric -> $idURI")
    return EnumeratedAccount(
      baseDirectory = accountDirectory,
      adobeDeviceXML = this.isFileOrNull(adobeDeviceXML),
      idURI = idURI,
      idNumeric = idNumeric)
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
