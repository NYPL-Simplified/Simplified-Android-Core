package org.nypl.simplified.books.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Pair
import com.io7m.jfunctional.PartialFunctionType
import com.io7m.jfunctional.ProcedureType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeLoanID
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileLocking
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
import java.util.HashMap
import java.util.HashSet

/**
 * A file-based book database.
 */

class BookDatabase private constructor(
  private val directory: File,
  private val jsonParser: OPDSJSONParserType,
  private val jsonSerializer: OPDSJSONSerializerType) : BookDatabaseType {

  private val log: Logger = LoggerFactory.getLogger(BookDatabase::class.java)
  private val snapshots: MutableMap<BookID, BookDatabaseEntrySnapshot> = HashMap(64)

  @Throws(IOException::class)
  private fun bookDatabaseEntries(): List<BookDatabaseEntryType> {
    val xs = ArrayList<BookDatabaseEntryType>(32)

    if (this.directory.isDirectory) {
      val bookList = this.directory.listFiles { path -> path.isDirectory }

      for (file in bookList) {
        val bookID = BookID.exactString(file.name)
        xs.add(BookDatabaseEntry(
          jsonSerializer = this.jsonSerializer,
          jsonParser = this.jsonParser,
          parentDirectory = this.directory,
          bookID = bookID))
      }
    }

    return xs
  }

  init {
    this.log.debug("opened database {}", this.directory)
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

  override fun databaseOpenEntryForWriting(bookID: BookID): BookDatabaseEntryType {
    return BookDatabaseEntry(
      jsonSerializer = this.jsonSerializer,
      jsonParser = this.jsonParser,
      parentDirectory = this.directory,
      bookID = bookID)
  }

  @Throws(IOException::class)
  override fun databaseOpenEntryForReading(bookID: BookID): BookDatabaseEntryReadableType {
    val file = File(this.directory, bookID.toString())
    if (file.isDirectory) {
      return BookDatabaseEntry(
        jsonSerializer = this.jsonSerializer,
        jsonParser = this.jsonParser,
        parentDirectory = this.directory,
        bookID = bookID)
    }

    throw FileNotFoundException(file.toString())
  }

  override fun databaseNotifyAllBookStatus(
    cache: BooksStatusCacheType,
    onLoad: ProcedureType<Pair<BookID, BookDatabaseEntrySnapshot>>,
    onFailure: ProcedureType<Pair<BookID, Throwable>>) {
    if (this.directory.isDirectory) {
      val bookList = this.directory.listFiles { path -> path.isDirectory }

      for (file in bookList) {
        val bookID = BookID.exactString(file.name)
        val entry =
          BookDatabaseEntry(
            jsonSerializer = this.jsonSerializer,
            jsonParser = this.jsonParser,
            parentDirectory = this.directory,
            bookID = bookID)

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
    synchronized(this.snapshots) {
      return if (this.snapshots.containsKey(book)) {
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
   * A single book directory.
   *
   * All operations on the directory are thread-safe but not necessarily
   * process-safe.
   */

  private inner class BookDatabaseEntry constructor(
    private val jsonSerializer: OPDSJSONSerializerType,
    private val jsonParser: OPDSJSONParserType,
    private val parentDirectory: File,
    private val bookID: BookID) : BookDatabaseEntryType {

    private val log: Logger = LoggerFactory.getLogger(BookDatabaseEntry::class.java)

    private val directory: File =
      File(this.parentDirectory, this.bookID.toString())
    private val fileLock: File =
      File(this.parentDirectory, this.bookID.toString() + ".lock")

    private val fileAdobeRightsTmp: File
    private val fileAdobeRights: File
    private val fileAdobeMeta: File
    private val fileAdobeMetaTmp: File
    private val fileBook: File
    private val fileCover: File
    private val fileMeta: File
    private val fileMetaTmp: File
    private val fileAnnotations: File
    private val fileAnnotationsTmp: File

    private fun bookLocked(): OptionType<File> {
      return if (this.fileBook.isFile) {
        Option.some(this.fileBook)
      } else Option.none()
    }

    @Throws(IOException::class)
    private fun bookmarksLocked(): List<BookmarkAnnotation> {
      try {
        FileInputStream(this.fileAnnotations).use { stream ->
          return AnnotationsParser.parseBookmarkArray(stream)
        }
      } catch (e: FileNotFoundException) {
        this.log.debug("Bookmarks file not found. Continuing by returning an empty list.")
        return ArrayList(0)
      }
    }

    @Throws(IOException::class)
    private fun adobeAdobeRightsInformationLocked(): OptionType<AdobeAdeptLoan> {
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
    private fun coverLocked(): OptionType<File> {
      return if (this.fileCover.isFile) {
        Option.some(this.fileCover)
      } else Option.none()
    }

    @Throws(IOException::class)
    private fun coverSetLocked(cover: OptionType<File>) {
      if (cover is Some<File>) {
        val file = cover.get()
        FileUtilities.fileCopy(file, this.fileCover)
        file.delete()
      } else {
        this.fileCover.delete()
      }
    }

    @Throws(IOException::class)
    private fun dataLocked(): OPDSAcquisitionFeedEntry {
      FileInputStream(this.fileMeta).use { stream ->
        return this.jsonParser.parseAcquisitionFeedEntryFromStream(stream)
      }
    }

    @Throws(IOException::class)
    private fun snapshotLocked(): BookDatabaseEntrySnapshot {
      val entry = this.dataLocked()
      val cover = this.coverLocked()
      val book = this.bookLocked()
      val rights = this.adobeAdobeRightsInformationLocked()
      return BookDatabaseEntrySnapshot(
        bookID = this.bookID,
        book = book,
        entry = entry,
        adobeRights = rights,
        cover = cover)
    }

    init {
      this.fileCover = File(this.directory, "cover.jpg")
      this.fileMeta = File(this.directory, "meta.json")
      this.fileMetaTmp = File(this.directory, "meta.json.tmp")
      this.fileBook = File(this.directory, "book.epub")
      this.fileAdobeRights = File(this.directory, "rights_adobe.xml")
      this.fileAdobeRightsTmp = File(this.directory, "rights_adobe.xml.tmp")
      this.fileAdobeMeta = File(this.directory, "meta_adobe.json")
      this.fileAdobeMetaTmp = File(this.directory, "meta_adobe.json.tmp")
      this.fileAnnotations = File(this.directory, "annotations.json")
      this.fileAnnotationsTmp = File(this.directory, "annotations.json.tmp")
    }

    @Throws(IOException::class)
    override fun entryCopyInBookFromSameFilesystem(
      file: File): BookDatabaseEntrySnapshot {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException> {
          this@BookDatabaseEntry.copyInBookFromSameFilesystemLocked(file)
          this@BookDatabaseEntry.updateSnapshotLocked()
        })
    }

    @Throws(IOException::class)
    override fun entryCopyInBook(file: File): BookDatabaseEntrySnapshot {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException> {
          this@BookDatabaseEntry.copyInBookLocked(file)
          this@BookDatabaseEntry.updateSnapshotLocked()
        })
    }

    @Throws(IOException::class)
    private fun copyInBookLocked(file: File) {
      FileUtilities.fileCopy(file, this.fileBook)
    }

    @Throws(IOException::class)
    private fun copyInBookFromSameFilesystemLocked(file: File) {
      FileUtilities.fileRename(file, this.fileBook)
    }

    @Throws(IOException::class)
    override fun entryCreate(entry: OPDSAcquisitionFeedEntry): BookDatabaseEntrySnapshot {
      DirectoryUtilities.directoryCreate(this.directory)
      return this.entrySetFeedData(entry)
    }

    @Throws(IOException::class)
    override fun entryDestroy() {
      FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, Unit, IOException> {
          this@BookDatabaseEntry.destroyLocked()
          this@BookDatabaseEntry.deleteSnapshot()
          Unit.unit()
        })
    }

    @Throws(IOException::class)
    override fun entryDeleteBookData(): BookDatabaseEntrySnapshot {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException> {
          this@BookDatabaseEntry.destroyBookDataLocked()
          this@BookDatabaseEntry.updateSnapshotLocked()
        })
    }

    @Throws(IOException::class)
    private fun destroyBookDataLocked() {
      FileUtilities.fileDelete(this.fileBook)
    }

    @Throws(IOException::class)
    private fun destroyLocked() {
      if (this.directory.isDirectory) {
        val files = this.directory.listFiles()
        for (file in files) {
          FileUtilities.fileDelete(file)
        }
      }

      FileUtilities.fileDelete(this.directory)
      FileUtilities.fileDelete(this.fileLock)
    }

    override fun entryExists(): Boolean {
      return this.fileMeta.isFile
    }

    @Throws(IOException::class)
    override fun entryGetCover(): OptionType<File> {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, OptionType<File>, IOException> {
          this@BookDatabaseEntry.coverLocked()
        })
    }

    @Throws(IOException::class)
    override fun entrySetCover(cover: OptionType<File>): BookDatabaseEntrySnapshot {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException> {
          this@BookDatabaseEntry.coverSetLocked(cover)
          this@BookDatabaseEntry.updateSnapshotLocked()
        })
    }

    @Throws(IOException::class)
    override fun entrySetAdobeRightsInformation(
      loan: OptionType<AdobeAdeptLoan>): BookDatabaseEntrySnapshot {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException> {
          this@BookDatabaseEntry.setAdobeRightsInformationLocked(loan)
          this@BookDatabaseEntry.updateSnapshotLocked()
        })
    }

    /*
    Bookmarks - Public Methods
     */

    @Throws(IOException::class)
    override fun entryAddBookmark(bookmark: BookmarkAnnotation): BookDatabaseEntrySnapshot {
      val bookmarks = this.entryGetBookmarks().toMutableList()
      bookmarks.add(bookmark)
      return this.entrySetBookmarksList(bookmarks)
    }

    @Throws(IOException::class)
    override fun entryDeleteBookmark(bookmark: BookmarkAnnotation): BookDatabaseEntrySnapshot {
      val bookmarks = this.entryGetBookmarks().toMutableList()
      bookmarks.remove(bookmark)
      return this.entrySetBookmarksList(bookmarks)
    }

    @Throws(IOException::class)
    override fun entrySetBookmarks(bookmarks: List<BookmarkAnnotation>): BookDatabaseEntrySnapshot {
      return this.entrySetBookmarksList(bookmarks)
    }

    @Throws(IOException::class)
    override fun entryGetBookmarks(): List<BookmarkAnnotation> {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, List<BookmarkAnnotation>, IOException> {
          this@BookDatabaseEntry.bookmarksLocked()
        })
    }

    /*
    Bookmarks - Private Overrides
     */

    @Throws(IOException::class)
    internal fun entrySetBookmarksList(
      bookmarks: List<BookmarkAnnotation>): BookDatabaseEntrySnapshot {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException> {
          this@BookDatabaseEntry.setBookmarksListLocked(bookmarks)
          this@BookDatabaseEntry.updateSnapshotLocked()
        })
    }

    @Throws(IOException::class)
    private fun setBookmarksListLocked(bookmarks: List<BookmarkAnnotation>) {
      val stream = FileOutputStream(this.fileAnnotationsTmp)

      try {
        val mapper = ObjectMapper()
        val bookmarksArray = mapper.valueToTree<ArrayNode>(bookmarks)
        val objNode = mapper.createObjectNode()
        objNode.putArray("bookmarks").addAll(bookmarksArray)
        this.jsonSerializer.serializeToStream(objNode, stream)
      } finally {
        stream.flush()
        stream.close()
      }
      FileUtilities.fileRename(this.fileAnnotationsTmp, this.fileAnnotations)
    }

    @Throws(IOException::class)
    override fun entryUpdateAll(
      entry: OPDSAcquisitionFeedEntry,
      bookStatus: BooksStatusCacheType,
      http: HTTPType): BookDatabaseEntrySnapshot {
      val sid = this.bookID.shortID

      this.entryCreate(entry)

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
    private fun setAdobeRightsInformationLocked(
      loan: OptionType<AdobeAdeptLoan>) {
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
    override fun entryGetFeedData(): OPDSAcquisitionFeedEntry {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, OPDSAcquisitionFeedEntry, IOException> {
          this@BookDatabaseEntry.dataLocked()
        })
    }

    @Throws(IOException::class)
    override fun entrySetFeedData(entry: OPDSAcquisitionFeedEntry): BookDatabaseEntrySnapshot {
      val node = this.jsonSerializer.serializeFeedEntry(entry)

      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException> {
          this@BookDatabaseEntry.setDataLocked(node)
          this@BookDatabaseEntry.updateSnapshotLocked()
        })
    }

    private fun deleteSnapshot() {
      synchronized(this@BookDatabase.snapshots) {
        val sid = this.bookID.shortID
        this.log.debug("[{}]: deleting snapshot", sid)
        this@BookDatabase.snapshots.remove(this.bookID)
      }
    }

    @Throws(IOException::class)
    private fun updateSnapshotLocked(): BookDatabaseEntrySnapshot {
      val snapshot = this.snapshotLocked()
      synchronized(this@BookDatabase.snapshots) {
        val sid = this.bookID.shortID
        this.log.debug("[{}]: updating snapshot {}", sid, snapshot)
        this@BookDatabase.snapshots[this.bookID] = snapshot
        return snapshot
      }
    }

    @Throws(IOException::class)
    private fun setDataLocked(node: ObjectNode) {
      this.log.debug("updating data {}", this.fileMeta)

      FileOutputStream(this.fileMetaTmp).use { stream ->
        this.jsonSerializer.serializeToStream(node, stream)
        stream.flush()
      }

      FileUtilities.fileRename(this.fileMetaTmp, this.fileMeta)
    }

    override fun entryGetDirectory(): File {
      return this.directory
    }

    override fun entryGetBookID(): BookID {
      return this.bookID
    }

    @Throws(IOException::class)
    override fun entryGetSnapshot(): BookDatabaseEntrySnapshot {
      return FileLocking.withFileThreadLocked(
        this.fileLock,
        BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS.toLong(),
        PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException> {
          this@BookDatabaseEntry.updateSnapshotLocked()
        })
    }
  }

  companion object {
    private const val LOCK_WAIT_MAXIMUM_MILLISECONDS: Int = 1000

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
