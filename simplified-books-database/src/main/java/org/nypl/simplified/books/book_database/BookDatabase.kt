package org.nypl.simplified.books.book_database

import android.content.Context
import com.io7m.jnull.Nullable
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.books.api.BookID
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
import java.util.Collections
import java.util.SortedMap
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
  private val serializer: OPDSJSONSerializerType) : BookDatabaseType {

  /**
   * A thread-safe map exposing read-only snapshots of database entries.
   */

  private class BookMaps internal constructor() {

    internal val mapsLock: Any = Any()
    @GuardedBy("mapsLock")
    internal val entries: ConcurrentSkipListMap<BookID, BookDatabaseEntry> =
      ConcurrentSkipListMap()
    @GuardedBy("mapsLock")
    internal val entriesRead: SortedMap<BookID, org.nypl.simplified.books.book_database.api.BookDatabaseEntryType> =
      Collections.unmodifiableSortedMap(this.entries)

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

  @Throws(org.nypl.simplified.books.book_database.api.BookDatabaseException::class)
  override fun delete() {
    try {
      DirectoryUtilities.directoryDelete(this.directory)
    } catch (e: IOException) {
      throw org.nypl.simplified.books.book_database.api.BookDatabaseException("Could not delete book database", listOf<Exception>(e))
    } finally {
      this.maps.clear()
    }
  }

  @Throws(org.nypl.simplified.books.book_database.api.BookDatabaseException::class)
  override fun createOrUpdate(
    id: BookID,
    newEntry: OPDSAcquisitionFeedEntry): org.nypl.simplified.books.book_database.api.BookDatabaseEntryType {

    synchronized(this.maps.mapsLock) {
      try {
        val bookDir = File(this.directory, id.value())
        DirectoryUtilities.directoryCreate(bookDir)

        val fileMeta = File(bookDir, "meta.json")
        val fileMetaTmp = File(bookDir, "meta.json.tmp")

        FileUtilities.fileWriteUTF8Atomically(
          fileMeta,
          fileMetaTmp,
          JSONSerializerUtilities.serializeToString(this.serializer.serializeFeedEntry(newEntry)))

        val book =
          Book(
            id = id,
            account = this.owner,
            cover = null,
            thumbnail = null,
            entry = newEntry,
            formats = listOf())

        val entry =
          BookDatabaseEntry(
            context = this.context,
            bookDir = bookDir,
            serializer = this.serializer,
            bookRef = book,
            onDelete = Runnable { this.maps.delete(id) })

        this.maps.addEntry(entry)
        return entry
      } catch (e: IOException) {
        throw org.nypl.simplified.books.book_database.api.BookDatabaseException(e.message, listOf<Exception>(e))
      }
    }
  }

  @Throws(org.nypl.simplified.books.book_database.api.BookDatabaseException::class)
  override fun entry(id: BookID): org.nypl.simplified.books.book_database.api.BookDatabaseEntryType {
    synchronized(this.maps.mapsLock) {
      return this.maps.entries[id] ?: throw org.nypl.simplified.books.book_database.api.BookDatabaseException(
        "Nonexistent book entry: " + id.value(), emptyList())
    }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(BookDatabase::class.java)

    @Throws(org.nypl.simplified.books.book_database.api.BookDatabaseException::class)
    fun open(
      context: Context,
      parser: OPDSJSONParserType,
      serializer: OPDSJSONSerializerType,
      owner: AccountID,
      directory: File): BookDatabaseType {

      LOG.debug("opening book database: {}", directory)
      val maps = BookMaps()
      val errors = ArrayList<Exception>()
      openAllBooks(context, parser, serializer, owner, directory, maps, errors)

      if (!errors.isEmpty()) {
        errors.forEach { exception -> LOG.error("error opening book database: ", exception) }
        throw org.nypl.simplified.books.book_database.api.BookDatabaseException(
          "One or more errors occurred whilst trying to open a book database.", errors)
      }

      return BookDatabase(context, owner, directory, maps, serializer)
    }

    private fun openAllBooks(
      context: Context,
      parser: OPDSJSONParserType,
      serializer: OPDSJSONSerializerType,
      account: AccountID,
      directory: File,
      maps: BookMaps,
      errors: MutableList<Exception>) {

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
            accountID = account,
            directory = bookDirectory,
            maps = maps,
            errors = errors,
            name = bookID)
            ?: continue
          maps.addEntry(entry)
        }
      }
    }

    @Nullable
    private fun openOneEntry(
      context: Context,
      parser: OPDSJSONParserType,
      serializer: OPDSJSONSerializerType,
      accountID: AccountID,
      directory: File,
      maps: BookMaps,
      errors: MutableList<Exception>,
      name: String): BookDatabaseEntry? {

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

        val fileCover = File(directory, "cover.jpg")
        val cover = if (fileCover.isFile) {
          fileCover
        } else {
          null
        }
        val fileThumb = File(directory, "thumb.jpg")
        val thumb = if (fileThumb.isFile) {
          fileThumb
        } else {
          null
        }

        val book =
          Book(
            id = bookId,
            account = accountID,
            cover = cover,
            thumbnail = thumb,
            entry = entry,
            formats = listOf())

        return BookDatabaseEntry(
          context = context,
          bookDir = directory,
          serializer = serializer,
          bookRef = book,
          onDelete = Runnable { maps.delete(bookId) })
      } catch (e: IOException) {
        errors.add(e)
        return null
      }
    }
  }

}
