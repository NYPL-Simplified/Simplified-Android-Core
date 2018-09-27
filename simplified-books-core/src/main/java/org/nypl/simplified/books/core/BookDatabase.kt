package org.nypl.simplified.books.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Pair
import com.io7m.jfunctional.ProcedureType
import com.io7m.jfunctional.Some
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeLoanID
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.values
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSJSONParserType
import org.nypl.simplified.opds.core.OPDSJSONSerializerType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.EnumMap
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A file-based book database.
 */

class BookDatabase private constructor(
  private val directory: File,
  private val jsonParser: OPDSJSONParserType,
  private val jsonSerializer: OPDSJSONSerializerType) : BookDatabaseType {

  private val log: Logger = LoggerFactory.getLogger(BookDatabase::class.java)

  private val snapshotsLock: ReentrantLock = ReentrantLock()
  private val snapshots: MutableMap<BookID, BookDatabaseEntrySnapshot> = HashMap(64)

  init {
    this.log.debug("opened database {}", this.directory)
  }

  /**
   * A format handle constructor for a particular book format.
   */

  private data class DatabaseBookFormatHandleConstructor(

    /**
     * The precise implementation class of the format. This is used as unique identifier for the
     * database entry format implementation.
     */

    val classType: Class<out BookDatabaseEntryFormatHandle>,

    /**
     * The set of content types that will trigger the creation of a format.
     */

    val supportedContentTypes: Set<String>,

    /**
     * A function to construct a format given an existing database entry.
     */

    val constructor: (BookFormatDefinition, BookDatabaseEntry) -> BookDatabaseEntryFormatHandle)

  /**
   * The available format handle constructors.
   */

  private val formatHandleConstructors:
    EnumMap<BookFormatDefinition, DatabaseBookFormatHandleConstructor> =
    EnumMap(BookFormatDefinition::class.java)

  init {
    for (format in values()) {
      this.formatHandleConstructors[format] =
        when (format) {
          BOOK_FORMAT_EPUB -> {
            DatabaseBookFormatHandleConstructor(
              classType = DBEntryFormatHandleEPUB::class.java,
              supportedContentTypes = format.supportedContentTypes(),
              constructor = { format, entry -> DBEntryFormatHandleEPUB(format, entry) })
          }
          BOOK_FORMAT_AUDIO -> {
            DatabaseBookFormatHandleConstructor(
              classType = DBEntryFormatHandleAudioBook::class.java,
              supportedContentTypes = format.supportedContentTypes(),
              constructor = { format, entry -> DBEntryFormatHandleAudioBook(format, entry) })
          }
        }
    }
  }

  /**
   * Create a format handle if required. This checks to see if there is a content type that is
   * accepted by any of the available formats, and instantiates one if one doesn't already exist.
   */

  private fun createFormatHandleIfRequired(
    owner: BookDatabaseEntry,
    existingFormats: MutableMap<Class<out BookDatabaseEntryFormatHandle>, BookDatabaseEntryFormatHandle>,
    contentTypes: Set<String>) {

    for (contentType in contentTypes) {
      for (formatDefinition in this.formatHandleConstructors.keys) {
        val formatConstructor = this.formatHandleConstructors[formatDefinition]!!
        if (formatDefinition.supportedContentTypes().contains(contentType)) {
          if (!existingFormats.containsKey(formatConstructor.classType)) {
            this.log.debug(
              "instantiating format {} for content type {}",
              formatConstructor.classType.simpleName,
              contentType)
            existingFormats[formatConstructor.classType] =
              formatConstructor.constructor.invoke(formatDefinition, owner)
            return
          }
        }
      }
    }
  }

  private fun snapshotUpdate(snapshot: BookDatabaseEntrySnapshot): BookDatabaseEntrySnapshot {
    this.snapshotsLock.withLock {
      val sid = snapshot.bookID.shortID
      this.log.debug("[{}]: updating snapshot", sid)
      this.snapshots[snapshot.bookID] = snapshot
      return snapshot
    }
  }

  private fun snapshotDelete(bookID: BookID) {
    this.snapshotsLock.withLock {
      val sid = bookID.shortID
      this.log.debug("[{}]: deleting snapshot", sid)
      this.snapshots.remove(bookID)
    }
  }

  @Throws(IOException::class)
  private fun bookDatabaseEntries(): List<BookDatabaseEntryType> {
    val xs = ArrayList<BookDatabaseEntryType>(32)

    if (this.directory.isDirectory) {
      val bookList = this.directory.listFiles { path -> path.isDirectory }

      for (file in bookList) {
        val bookID = BookID.exactString(file.name)
        xs.add(this.databaseOpenExistingEntry(bookID))
      }
    }

    return xs
  }

  @Throws(IOException::class)
  override fun databaseCreate() {
    DirectoryUtilities.directoryCreate(this.directory)
  }

  @Throws(IOException::class)
  override fun databaseDestroy() {
    if (this.directory.isDirectory) {
      val entries = this.bookDatabaseEntries()
      for (entry in entries) {
        entry.entryDestroy()
      }
      FileUtilities.fileDelete(this.directory)
    }
  }

  @Throws(IOException::class)
  override fun databaseCreateEntry(
    bookID: BookID,
    entry: OPDSAcquisitionFeedEntry): BookDatabaseEntryType {
    return BookDatabaseEntry.create(
      owner = this,
      jsonSerializer = this.jsonSerializer,
      jsonParser = this.jsonParser,
      parentDirectory = this.directory,
      bookID = bookID,
      opdsEntry = entry)
  }

  @Throws(IOException::class)
  override fun databaseOpenExistingEntry(bookID: BookID): BookDatabaseEntryType {
    return BookDatabaseEntry.open(
      owner = this,
      jsonSerializer = this.jsonSerializer,
      jsonParser = this.jsonParser,
      parentDirectory = this.directory,
      bookID = bookID)
  }

  override fun databaseNotifyAllBookStatus(
    cache: BooksStatusCacheType,
    onLoad: ProcedureType<Pair<BookID, BookDatabaseEntrySnapshot>>,
    onFailure: ProcedureType<Pair<BookID, Throwable>>) {
    if (this.directory.isDirectory) {
      val bookList = this.directory.listFiles { path -> path.isDirectory }

      for (file in bookList) {
        val bookID = BookID.exactString(file.name)
        val entry = this.databaseOpenExistingEntry(bookID)

        try {
          val snapshot = entry.entryGetSnapshot()
          val status = BookStatus.fromSnapshot(bookID, snapshot)
          cache.booksStatusUpdate(status)
          val p = Pair.pair(bookID, snapshot)
          onLoad.call(p)
        } catch (x: IOException) {
          this.log.error("[{}]: error creating snapshot: ", bookID.shortID, x)
          val p = Pair.pair(bookID, x as Throwable)
          onFailure.call(p)
        }
      }
    }
  }

  override fun databaseGetEntrySnapshot(book: BookID): OptionType<BookDatabaseEntrySnapshot> {
    return this.snapshotsLock.withLock {
      if (this.snapshots.containsKey(book)) {
        Option.some(this.snapshots[book])
      } else Option.none()
    }
  }

  override fun databaseGetBooks(): Set<BookID> {
    val bookIdSet = HashSet<BookID>(32)
    if (this.directory.isDirectory) {
      val bookList = this.directory.listFiles { path -> path.isDirectory }

      for (file in bookList) {
        bookIdSet.add(BookID.exactString(file.name))
      }
    }

    return bookIdSet
  }

  /**
   * Operations on EPUB formats in database entries.
   */

  private inner class DBEntryFormatHandleEPUB(
    override val formatDefinition: BookFormatDefinition,
    private val owner: BookDatabaseEntry) : BookDatabaseEntryFormatHandleEPUB() {

    private val fileAdobeRightsTmp: File =
      File(this.owner.directory, "rights_adobe.xml.tmp")
    private val fileAdobeRights: File =
      File(this.owner.directory, "rights_adobe.xml")
    private val fileAdobeMeta: File =
      File(this.owner.directory, "meta_adobe.json")
    private val fileAdobeMetaTmp: File =
      File(this.owner.directory, "meta_adobe.json.tmp")
    private val fileBook: File =
      File(this.owner.directory, "book.epub")

    @Throws(IOException::class)
    private fun lockedCopyInBook(file: File) {
      FileUtilities.fileCopy(file, this.fileBook)
    }

    private fun lockedBookGet(): OptionType<File> {
      return if (this.fileBook.isFile) {
        Option.some(this.fileBook)
      } else Option.none()
    }

    @Throws(IOException::class)
    private fun lockedAdobeRightsInformationGet(): OptionType<AdobeAdeptLoan> {
      if (this.fileAdobeRights.isFile) {
        val serialized = FileUtilities.fileReadBytes(this.fileAdobeRights)
        val jom = ObjectMapper()
        val jn = jom.readTree(this.fileAdobeMeta)
        val o = JSONParserUtilities.checkObject(null, jn)
        val loan_id = AdobeLoanID(JSONParserUtilities.getString(o, "loan-id"))
        val returnable = JSONParserUtilities.getBoolean(o, "returnable")
        val loan = AdobeAdeptLoan(loan_id, ByteBuffer.wrap(serialized), returnable)
        return Option.some(loan)
      }

      return Option.none()
    }

    @Throws(IOException::class)
    private fun lockedAdobeRightsInformationSet(loan: OptionType<AdobeAdeptLoan>) {
      if (loan is Some<AdobeAdeptLoan>) {
        val data = loan.get()

        FileUtilities.fileWriteBytesAtomically(
          this.fileAdobeRights,
          this.fileAdobeRightsTmp,
          data.serialized.array())

        val jom = ObjectMapper()
        val o = jom.createObjectNode()
        o.put("loan-id", data.id.value)
        o.put("returnable", data.isReturnable)

        val bao = ByteArrayOutputStream()
        JSONSerializerUtilities.serialize(o, bao)

        FileUtilities.fileWriteUTF8Atomically(
          this.fileAdobeMeta,
          this.fileAdobeMetaTmp,
          bao.toString("UTF-8"))
      } else {
        FileUtilities.fileDelete(this.fileAdobeMeta)
        FileUtilities.fileDelete(this.fileAdobeMetaTmp)
        FileUtilities.fileDelete(this.fileAdobeRights)
        FileUtilities.fileDelete(this.fileAdobeRightsTmp)
      }
    }

    @Throws(IOException::class)
    private fun lockedDestroyBookData() {
      FileUtilities.fileDelete(this.fileBook)
    }

    override fun copyInBook(file: File): BookDatabaseEntrySnapshot {
      return this.owner.entryLock.withLock {
        this.lockedCopyInBook(file)
        this.owner.lockedUpdateSnapshot()
      }
    }

    override fun deleteBookData(): BookDatabaseEntrySnapshot {
      return this.owner.entryLock.withLock {
        this.lockedDestroyBookData()
        this.owner.lockedUpdateSnapshot()
      }
    }

    override fun setAdobeRightsInformation(
      loan: OptionType<AdobeAdeptLoan>): BookDatabaseEntrySnapshot {
      return this.owner.entryLock.withLock {
        this.lockedAdobeRightsInformationSet(loan)
        this.owner.lockedUpdateSnapshot()
      }
    }

    override fun snapshot(): BookDatabaseEntryFormatSnapshotEPUB {
      return this.owner.entryLock.withLock {
        BookDatabaseEntryFormatSnapshotEPUB(
          adobeRights = this.lockedAdobeRightsInformationGet(),
          book = this.lockedBookGet())
      }
    }
  }

  /**
   * Operations on audio book formats in database entries.
   */

  private class DBEntryFormatHandleAudioBook(
    override val formatDefinition: BookFormatDefinition,
    private val owner: BookDatabaseEntry) : BookDatabaseEntryFormatHandleAudioBook() {

    override fun snapshot(): BookDatabaseEntryFormatSnapshotAudioBook {
      return BookDatabaseEntryFormatSnapshotAudioBook()
    }
  }

  /**
   * A single book directory.
   *
   * All operations on the directory are thread-safe but not necessarily
   * process-safe.
   */

  private class BookDatabaseEntry constructor(
    private val owner: BookDatabase,
    private val jsonSerializer: OPDSJSONSerializerType,
    private val jsonParser: OPDSJSONParserType,
    private val parentDirectory: File,
    private val bookID: BookID) : BookDatabaseEntryType {

    private val log: Logger =
      LoggerFactory.getLogger(BookDatabaseEntry::class.java)

    val directory: File =
      File(this.parentDirectory, this.bookID.toString())

    val entryLock: ReentrantLock = ReentrantLock()

    private val fileCover: File
    private val fileMeta: File
    private val fileMetaTmp: File
    private val fileAnnotations: File
    private val fileAnnotationsTmp: File

    private lateinit var opdsEntry: OPDSAcquisitionFeedEntry

    private val formatsHandles: MutableMap<
      Class<out BookDatabaseEntryFormatHandle>, BookDatabaseEntryFormatHandle> = mutableMapOf()

    init {
      this.fileCover = File(this.directory, "cover.jpg")
      this.fileMeta = File(this.directory, "meta.json")
      this.fileMetaTmp = File(this.directory, "meta.json.tmp")
      this.fileAnnotations = File(this.directory, "annotations.json")
      this.fileAnnotationsTmp = File(this.directory, "annotations.json.tmp")

      this.log.debug("[{}]: mkdir {}", this.bookID.shortID, this.directory)
      DirectoryUtilities.directoryCreate(this.directory)
    }

    companion object {

      internal fun create(
        owner: BookDatabase,
        jsonSerializer: OPDSJSONSerializerType,
        jsonParser: OPDSJSONParserType,
        parentDirectory: File,
        bookID: BookID,
        opdsEntry: OPDSAcquisitionFeedEntry): BookDatabaseEntry {

        val entry =
          BookDatabaseEntry(
            owner = owner,
            jsonParser = jsonParser,
            jsonSerializer = jsonSerializer,
            parentDirectory = parentDirectory,
            bookID = bookID)

        entry.entrySetFeedData(opdsEntry)
        return entry
      }

      internal fun open(
        owner: BookDatabase,
        jsonSerializer: OPDSJSONSerializerType,
        jsonParser: OPDSJSONParserType,
        parentDirectory: File,
        bookID: BookID): BookDatabaseEntry {

        val file = File(parentDirectory, bookID.toString())
        if (file.isDirectory) {
          val entry =
            BookDatabaseEntry(
              owner = owner,
              jsonParser = jsonParser,
              jsonSerializer = jsonSerializer,
              parentDirectory = parentDirectory,
              bookID = bookID)

          entry.loadMetadata()
          return entry
        }

        throw FileNotFoundException(file.absolutePath)
      }
    }

    private fun lockedConfigureForEntry(entry: OPDSAcquisitionFeedEntry) {
      entry.acquisitions.forEach { acquisition ->
        this.owner.createFormatHandleIfRequired(
          owner = this,
          existingFormats = this.formatsHandles,
          contentTypes = acquisition.availableFinalContentTypes())
      }
    }

    @Throws(IOException::class)
    private fun lockedBookmarks(): List<BookmarkAnnotation> {
      try {
        FileInputStream(this.fileAnnotations).use { stream ->
          return AnnotationsParser.parseBookmarkArray(stream)
        }
      } catch (e: FileNotFoundException) {
        this.log.debug(
          "[{}]: Bookmarks file not found. Continuing by returning an empty list.",
          this.bookID.shortID)
        return emptyList()
      }
    }

    @Throws(IOException::class)
    private fun lockedCoverGet(): OptionType<File> {
      return if (this.fileCover.isFile) {
        Option.some(this.fileCover)
      } else Option.none()
    }

    @Throws(IOException::class)
    private fun lockedCoverSet(cover: OptionType<File>) {
      if (cover is Some<File>) {
        val file = cover.get()
        FileUtilities.fileCopy(file, this.fileCover)
        file.delete()
      } else {
        this.fileCover.delete()
      }
    }

    @Throws(IOException::class)
    private fun loadMetadata(): OPDSAcquisitionFeedEntry {
      return this.entryLock.withLock {
        val loaded = FileInputStream(this.fileMeta).use { stream ->
          this.jsonParser.parseAcquisitionFeedEntryFromStream(stream)
        }
        this.opdsEntry = loaded
        loaded
      }
    }

    @Throws(IOException::class)
    private fun lockedSnapshotGet(): BookDatabaseEntrySnapshot {
      val resultCover =
        if (this.fileCover.isFile) {
          Option.some(this.fileCover)
        } else Option.none()

      val resultFormatSnapshots =
        this.formatsHandles.values.map { format -> format.snapshot() }

      return BookDatabaseEntrySnapshot(
        bookID = this.bookID,
        cover = resultCover,
        entry = this.opdsEntry,
        formats = resultFormatSnapshots)
    }

    @Throws(IOException::class)
    private fun lockedDestroy() {
      if (this.directory.isDirectory) {
        val files = this.directory.listFiles()
        for (file in files) {
          try {
            FileUtilities.fileDelete(file)
          } catch (e: Exception) {
            this.log.error("[{}]: error deleting {}: ", this.bookID.shortID, file, e)
          }
        }
      }

      FileUtilities.fileDelete(this.directory)
    }

    @Throws(IOException::class)
    private fun lockedMetadataSet(node: ObjectNode) {
      FileOutputStream(this.fileMetaTmp).use { stream ->
        this.jsonSerializer.serializeToStream(node, stream)
        stream.flush()
      }

      FileUtilities.fileRename(this.fileMetaTmp, this.fileMeta)
    }

    @Throws(IOException::class)
    private fun lockedBookmarksListSet(bookmarks: List<BookmarkAnnotation>) {
      FileOutputStream(this.fileAnnotationsTmp).use { stream ->
        try {
          val mapper = ObjectMapper()
          val bookmarksArray = mapper.valueToTree<ArrayNode>(bookmarks)
          val objNode = mapper.createObjectNode()
          objNode.putArray("bookmarks").addAll(bookmarksArray)
          this.jsonSerializer.serializeToStream(objNode, stream)
        } finally {
          stream.flush()
        }
      }

      FileUtilities.fileRename(this.fileAnnotationsTmp, this.fileAnnotations)
    }

    @Throws(IOException::class)
    internal fun lockedUpdateSnapshot(): BookDatabaseEntrySnapshot {
      val snapshot = this.lockedSnapshotGet()
      return this.owner.snapshotUpdate(snapshot)
    }

    @Throws(IOException::class)
    override fun entryDestroy() {
      this.entryLock.withLock {
        this.lockedDestroy()
        this.owner.snapshotDelete(this.bookID)
      }
    }

    override fun entryFormatHandles(): List<BookDatabaseEntryFormatHandle> {
      return this.entryLock.withLock {
        this.formatsHandles.values.toList()
      }
    }

    override fun entryExists(): Boolean {
      return this.fileMeta.isFile
    }

    @Throws(IOException::class)
    override fun entryGetCover(): OptionType<File> {
      return this.entryLock.withLock {
        this.lockedCoverGet()
      }
    }

    @Throws(IOException::class)
    override fun entrySetCover(cover: OptionType<File>): BookDatabaseEntrySnapshot {
      return this.entryLock.withLock {
        this.lockedCoverSet(cover)
        this.lockedUpdateSnapshot()
      }
    }

    @Throws(IOException::class)
    override fun entryAddBookmark(bookmark: BookmarkAnnotation): BookDatabaseEntrySnapshot {
      return this.entryLock.withLock {
        val bookmarks = this.lockedBookmarks().toMutableList()
        bookmarks.add(bookmark)

        this.lockedBookmarksListSet(bookmarks)
        this.lockedUpdateSnapshot()
      }
    }

    @Throws(IOException::class)
    override fun entryDeleteBookmark(bookmark: BookmarkAnnotation): BookDatabaseEntrySnapshot {
      return this.entryLock.withLock {
        val bookmarks = this.lockedBookmarks().toMutableList()
        bookmarks.remove(bookmark)

        this.lockedBookmarksListSet(bookmarks)
        this.lockedUpdateSnapshot()
      }
    }

    @Throws(IOException::class)
    override fun entrySetBookmarks(bookmarks: List<BookmarkAnnotation>): BookDatabaseEntrySnapshot {
      return this.entryLock.withLock {
        this.lockedBookmarksListSet(bookmarks)
        this.lockedUpdateSnapshot()
      }
    }

    @Throws(IOException::class)
    override fun entryGetBookmarks(): List<BookmarkAnnotation> {
      return this.entryLock.withLock {
        this.lockedBookmarks()
      }
    }

    @Throws(IOException::class)
    override fun entryUpdateAll(
      entry: OPDSAcquisitionFeedEntry,
      bookStatus: BooksStatusCacheType,
      http: HTTPType): BookDatabaseEntrySnapshot {
      val sid = this.bookID.shortID

      this.entrySetFeedData(entry)

      this.log.debug("[{}]: getting snapshot", sid)
      val snap = this.entryGetSnapshot()
      this.log.debug("[{}]: determining status", sid)
      val status = BookStatus.fromSnapshot(this.bookID, snap)

      this.log.debug("[{}]: updating status", sid)
      bookStatus.booksStatusUpdateIfMoreImportant(status)

      this.log.debug("[{}]: finished synchronizing book entry", sid)
      return snap
    }

    @Throws(IOException::class)
    override fun entryGetFeedData(): OPDSAcquisitionFeedEntry {
      return this.entryLock.withLock {
        this.opdsEntry
      }
    }

    @Throws(IOException::class)
    override fun entrySetFeedData(entry: OPDSAcquisitionFeedEntry): BookDatabaseEntrySnapshot {
      val node = this.jsonSerializer.serializeFeedEntry(entry)

      return this.entryLock.withLock {
        this.opdsEntry = entry
        this.lockedConfigureForEntry(entry)
        this.lockedMetadataSet(node)
        this.lockedUpdateSnapshot()
      }
    }

    override fun entryGetDirectory(): File {
      return this.directory
    }

    override fun entryGetBookID(): BookID {
      return this.bookID
    }

    @Throws(IOException::class)
    override fun entryGetSnapshot(): BookDatabaseEntrySnapshot {
      return this.entryLock.withLock {
        this.lockedUpdateSnapshot()
      }
    }

    override fun <T : BookDatabaseEntryFormatHandle> entryFindFormatHandle(clazz: Class<T>): OptionType<T> {
      this.entryLock.withLock {
        for (format in this.formatsHandles.keys) {
          if (clazz.isAssignableFrom(format)) {
            return Option.some(formatsHandles[format]!! as T)
          }
        }
        return Option.none()
      }
    }

    override fun entryFindFormatHandleForContentType(
      contentType: String): OptionType<BookDatabaseEntryFormatHandle> {

      this.entryLock.withLock {
        for (format in this.formatsHandles.values) {
          if (format.formatDefinition.supportedContentTypes().contains(contentType)) {
            return Option.some(format)
          }
        }
        return Option.none()
      }
    }
  }

  companion object {

    /**
     * Open a database at the given directory.
     *
     * @param jsonSerializer A JSON serializer
     * @param jsonParser     A JSON parser
     * @param directory      The directory
     *
     * @return A reference to the database
     */

    fun newDatabase(
      jsonSerializer: OPDSJSONSerializerType,
      jsonParser: OPDSJSONParserType,
      directory: File): BookDatabaseType {

      return BookDatabase(
        directory = directory,
        jsonParser = jsonParser,
        jsonSerializer = jsonSerializer)
    }

    /**
     * Given a path to an epub file, return the path to the associated Adobe
     * rights file, if any.
     *
     * @param file The epub file
     *
     * @return The Adobe rights file, if any
     */

    fun getAdobeRightsFileForEPUB(file: File): OptionType<File> {
      val parent = file.parentFile
      val adobe = File(parent, "rights_adobe.xml")
      return if (adobe.isFile) {
        Option.some(adobe)
      } else Option.none()
    }
  }
}
