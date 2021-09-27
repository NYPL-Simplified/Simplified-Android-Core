package org.nypl.simplified.books.book_database

import android.content.Context
import com.io7m.jnull.Nullable
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSJSONParserType
import org.nypl.simplified.opds.core.OPDSJSONSerializerType
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.ArrayList
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.ConcurrentSkipListMap
import javax.annotation.concurrent.GuardedBy

/**
 * The default implementation of the [BookDatabaseType] interface.
 */

class BookDatabase private constructor(
  private val context: Context,
  private val owner: AccountID,
  private val directory: File,
  private val maps: BookMaps,
  private val serializer: OPDSJSONSerializerType,
  private val formats: BookFormatSupportType
) : BookDatabaseType {

  /**
   * A thread-safe map exposing read-only snapshots of database entries.
   */

  private class BookMaps internal constructor() {

    internal val mapsLock: Any = Any()

    @GuardedBy("mapsLock")
    internal val entries: ConcurrentSkipListMap<BookID, BookDatabaseEntry> =
      ConcurrentSkipListMap()

    internal fun contains(key: BookID): Boolean {
      synchronized(mapsLock) {
        LOG.debug("BookMaps.contains")
        return this.entries.containsKey(key)
      }
    }

    internal fun clear() {
      synchronized(this.mapsLock) {
        LOG.debug("BookMaps.clear")
        this.entries.clear()
      }
    }

    internal fun delete(bookID: BookID) {
      synchronized(this.mapsLock) {
        LOG.debug("BookMaps.delete: {}", bookID.value())
        this.entries.remove(bookID)
      }
    }

    internal fun addEntry(entry: BookDatabaseEntry) {
      synchronized(this.mapsLock) {
        LOG.debug("BookMaps.addEntry: {}", entry.id.value())
        this.entries.put(entry.id, entry)
      }
    }
  }

  override fun owner(): AccountID {
    return this.owner
  }

  override fun books(): SortedSet<BookID> {
    synchronized(this.maps.mapsLock) {
      return TreeSet(this.maps.entries.keys)
    }
  }

  @Throws(BookDatabaseException::class)
  override fun delete() {
    try {
      DirectoryUtilities.directoryDelete(this.directory)
    } catch (e: IOException) {
      throw BookDatabaseException("Could not delete book database", listOf<Exception>(e))
    } finally {
      this.maps.clear()
    }
  }

  @Throws(BookDatabaseException::class)
  override fun createOrUpdate(
    id: BookID,
    entry: OPDSAcquisitionFeedEntry
  ): BookDatabaseEntryType {
    synchronized(this.maps.mapsLock) {
      if (this.maps.contains(id)) {
        LOG.debug("Updating entry for {}", id)
      } else {
        LOG.debug("Adding entry for {}", id)
      }
      try {
        val bookDir = File(this.directory, id.value())
        DirectoryUtilities.directoryCreate(bookDir)

        val fileMeta = File(bookDir, "meta.json")
        val fileMetaTmp = File(bookDir, "meta.json.tmp")

        val cover = fileOrNull(directory, BookDatabaseEntry.COVER_FILENAME)
        val thumb = fileOrNull(directory, BookDatabaseEntry.THUMB_FILENAME)

        FileUtilities.fileWriteUTF8Atomically(
          fileMeta,
          fileMetaTmp,
          JSONSerializerUtilities.serializeToString(this.serializer.serializeFeedEntry(entry))
        )

        val book =
          Book(
            id = id,
            account = this.owner,
            cover = cover,
            thumbnail = thumb,
            entry = entry,
            formats = listOf()
          )

        val dbEntry =
          BookDatabaseEntry(
            context = this.context,
            bookDir = bookDir,
            serializer = this.serializer,
            formats = this.formats,
            bookRef = book,
            onDelete = Runnable { this.maps.delete(id) }
          )

        this.maps.addEntry(dbEntry)
        return dbEntry
      } catch (e: IOException) {
        throw BookDatabaseException(e.message, listOf<Exception>(e))
      }
    }
  }

  @Throws(BookDatabaseException::class)
  override fun entry(id: BookID): BookDatabaseEntryType {
    synchronized(this.maps.mapsLock) {
      return this.maps.entries[id] ?: throw BookDatabaseException(
        "Nonexistent book entry: " + id.value(), emptyList()
      )
    }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(BookDatabase::class.java)

    @Throws(BookDatabaseException::class)
    fun open(
      context: Context,
      parser: OPDSJSONParserType,
      serializer: OPDSJSONSerializerType,
      formats: BookFormatSupportType,
      owner: AccountID,
      directory: File
    ): BookDatabaseType {
      LOG.debug("opening book database: {}", directory)
      val maps = BookMaps()
      val errors = ArrayList<Exception>()

      openAllBooks(
        context = context,
        parser = parser,
        serializer = serializer,
        formats = formats,
        account = owner,
        directory = directory,
        maps = maps,
        errors = errors
      )

      if (errors.isNotEmpty()) {
        errors.forEach { exception -> LOG.error("error opening book database: ", exception) }
        throw BookDatabaseException(
          "One or more errors occurred whilst trying to open a book database.", errors
        )
      }

      return BookDatabase(
        context = context,
        owner = owner,
        directory = directory,
        maps = maps,
        serializer = serializer,
        formats = formats
      )
    }

    private fun openAllBooks(
      context: Context,
      parser: OPDSJSONParserType,
      serializer: OPDSJSONSerializerType,
      formats: BookFormatSupportType,
      account: AccountID,
      directory: File,
      maps: BookMaps,
      errors: MutableList<Exception>
    ) {
      if (!directory.exists()) {
        directory.mkdirs()
      }

      if (!directory.isDirectory) {
        errors.add(IOException("Not a directory: $directory"))
      }

      val bookDirs = directory.list()
      if (bookDirs != null) {
        for (bookID in bookDirs) {
          LOG.debug("opening book: {}/{}", directory, bookID)
          val bookDirectory = File(directory, bookID)
          val entry = openOneEntry(
            context = context,
            parser = parser,
            serializer = serializer,
            formats = formats,
            accountID = account,
            directory = bookDirectory,
            maps = maps,
            errors = errors,
            name = bookID
          ) ?: continue
          maps.addEntry(entry)
        }
      }
    }

    @Nullable
    private fun openOneEntry(
      context: Context,
      parser: OPDSJSONParserType,
      serializer: OPDSJSONSerializerType,
      formats: BookFormatSupportType,
      accountID: AccountID,
      directory: File,
      maps: BookMaps,
      errors: MutableList<Exception>,
      name: String
    ): BookDatabaseEntry? {
      try {
        LOG.debug("open: {}", directory)

        if (!directory.isDirectory) {
          return null
        }

        val bookId = BookID.create(name)
        val fileMeta = File(directory, "meta.json")
        val entry: OPDSAcquisitionFeedEntry =
          FileInputStream(fileMeta).use { stream ->
            parser.parseAcquisitionFeedEntryFromStream(stream)
          }

        val cover = fileOrNull(directory, BookDatabaseEntry.COVER_FILENAME)
        val thumb = fileOrNull(directory, BookDatabaseEntry.THUMB_FILENAME)

        val book =
          Book(
            id = bookId,
            account = accountID,
            cover = cover,
            thumbnail = thumb,
            entry = entry,
            formats = listOf()
          )

        return BookDatabaseEntry(
          context = context,
          bookDir = directory,
          serializer = serializer,
          formats = formats,
          bookRef = book,
          onDelete = Runnable { maps.delete(bookId) }
        )
      } catch (e: IOException) {
        errors.add(e)
        return null
      }
    }
  }
}

/** Return the file, or null if it does not exist or is not a file. */

private fun fileOrNull(bookDir: File, filename: String) = File(bookDir, filename)
  .run {
    if (isFile) this else null
  }
