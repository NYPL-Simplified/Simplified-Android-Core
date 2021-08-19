package org.nypl.simplified.books.book_database

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.files.DirectoryUtilities
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSJSONSerializerType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.EnumMap
import javax.annotation.concurrent.GuardedBy

/**
 * The database entry implementation.
 */

internal class BookDatabaseEntry internal constructor(
  private val context: Context,
  private val bookDir: File,
  private val serializer: OPDSJSONSerializerType,
  private val formats: BookFormatSupportType,
  @GuardedBy("bookLock")
  private var bookRef: Book,
  private val onDelete: Runnable
) : BookDatabaseEntryType {

  private val LOG = LoggerFactory.getLogger(BookDatabaseEntry::class.java)

  private val bookLock: Any = Any()

  @GuardedBy("bookLock")
  private var formatHandlesRef: MutableMap<Class<out BookDatabaseEntryFormatHandle>, BookDatabaseEntryFormatHandle> =
    mutableMapOf()

  internal val id: BookID = this.bookRef.id

  override val book: Book
    get() = synchronized(this.bookLock) {
      Preconditions.checkArgument(!this.deleted, "Entry must not have been deleted")
      return this.bookRef
    }

  override val formatHandles: List<BookDatabaseEntryFormatHandle>
    get() = synchronized(this.bookLock) {
      Preconditions.checkArgument(!this.deleted, "Entry must not have been deleted")
      return this.formatHandlesRef.values.toList()
    }

  /**
   * The available format handle constructors.
   */

  private val formatHandleConstructors:
    EnumMap<BookFormats.BookFormatDefinition, DatabaseBookFormatHandleConstructor> =
      EnumMap(BookFormats.BookFormatDefinition::class.java)

  init {
    for (format in BookFormats.BookFormatDefinition.values()) {
      this.formatHandleConstructors[format] =
        when (format) {
          BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB -> {
            DatabaseBookFormatHandleConstructor(
              classType = DatabaseFormatHandleEPUB::class.java,
              supportedContentTypes = format.supportedContentTypes(),
              constructor = { params -> DatabaseFormatHandleEPUB(params) }
            )
          }
          BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO -> {
            DatabaseBookFormatHandleConstructor(
              classType = DatabaseFormatHandleAudioBook::class.java,
              supportedContentTypes = format.supportedContentTypes(),
              constructor = { params -> DatabaseFormatHandleAudioBook(params) }
            )
          }
          BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF -> {
            DatabaseBookFormatHandleConstructor(
              classType = DatabaseFormatHandlePDF::class.java,
              supportedContentTypes = format.supportedContentTypes(),
              constructor = { params -> DatabaseFormatHandlePDF(params) }
            )
          }
        }
    }

    val objectMapper = ObjectMapper()
    synchronized(this.bookLock) {
      this.bookRef.entry.acquisitions.forEach { acquisition ->
        createFormatHandleIfRequired(
          context = this.context,
          logger = LOG,
          owner = this,
          constructors = this.formatHandleConstructors,
          ownerDirectory = this.bookDir,
          onUpdate = { format -> this.onFormatUpdated(format) },
          existingFormats = this.formatHandlesRef,
          contentTypes = acquisition.availableFinalContentTypes(),
          objectMapper = objectMapper,
          bookFormats = this.formats
        )
      }

      this.bookRef =
        this.bookRef.copy(formats = this.formatHandlesRef.map { (_, handle) -> handle.format })
    }
  }

  private fun onFormatUpdated(format: BookFormat) {
    synchronized(this.bookLock) {
      LOG.debug("onFormatUpdated: {}", format.javaClass.canonicalName)
      this.bookRef = this.bookRef.copy(
        formats = this.formatHandles.map { handle -> handle.format }
      )
    }
  }

  @GuardedBy("bookLock")
  private val deleted: Boolean = false

  @Throws(BookDatabaseException::class)
  override fun writeOPDSEntry(opdsEntry: OPDSAcquisitionFeedEntry) {
    synchronized(this.bookLock) {
      Preconditions.checkArgument(!this.deleted, "Entry must not have been deleted")

      val fileMeta = File(this.bookDir, "meta.json")
      val fileMetaTmp = File(this.bookDir, "meta.json.tmp")

      try {
        DirectoryUtilities.directoryCreate(this.bookDir)

        FileUtilities.fileWriteUTF8Atomically(
          fileMeta,
          fileMetaTmp,
          JSONSerializerUtilities.serializeToString(
            this.serializer.serializeFeedEntry(opdsEntry)
          )
        )

        this.bookRef = this.bookRef.copy(entry = opdsEntry)
      } catch (e: IOException) {
        throw BookDatabaseException(e.message, listOf<Exception>(e))
      } finally {
        try {
          FileUtilities.fileDelete(fileMetaTmp)
        } catch (ignored: IOException) {
          LOG.error("could not delete temporary file: {}: ", fileMetaTmp, ignored)
        }
      }
    }
  }

  @Throws(BookDatabaseException::class)
  override fun delete() {
    synchronized(this.bookLock) {
      Preconditions.checkArgument(!this.deleted, "Entry must not have been deleted")

      /*
       * Delete all of the format handles individually.
       */

      val failures = mutableListOf<Exception>()
      for (handle in this.formatHandles) {
        try {
          handle.deleteBookData()
        } catch (e: Exception) {
          failures.add(e)
        }
      }

      /*
       * If any of the format handles failed, abort the deletion.
       */

      if (!failures.isEmpty()) {
        throw BookDatabaseException("Failed to delete one or more format handles", failures)
      }

      try {
        DirectoryUtilities.directoryDelete(this.bookDir)
        this.onDelete.run()
      } catch (e: IOException) {
        throw BookDatabaseException(e.message, listOf<Exception>(e))
      }
    }
  }

  @Throws(IOException::class)
  override fun setCover(file: File) {
    synchronized(this.bookLock) {
      Preconditions.checkArgument(!this.deleted, "Entry must not have been deleted")

      val fileCover = File(this.bookDir, COVER_FILENAME)
      val fileCoverTmp = File(this.bookDir, "$COVER_FILENAME.tmp")

      FileUtilities.fileCopy(file, fileCoverTmp)
      FileUtilities.fileRename(fileCoverTmp, fileCover)

      this.bookRef = this.bookRef.copy(cover = fileCover)
    }
  }

  @Throws(IOException::class)
  override fun setThumbnail(file: File) {
    synchronized(this.bookLock) {
      Preconditions.checkArgument(!this.deleted, "Entry must not have been deleted")

      val fileThumb = File(this.bookDir, THUMB_FILENAME)
      val fileThumbTmp = File(this.bookDir, "$THUMB_FILENAME.tmp")

      FileUtilities.fileCopy(file, fileThumbTmp)
      FileUtilities.fileRename(fileThumbTmp, fileThumb)

      this.bookRef = this.bookRef.copy(thumbnail = fileThumb)
    }
  }

  @Throws(IOException::class)
  override fun temporaryFile(): File {
    synchronized(this.bookLock) {
      Preconditions.checkArgument(!this.deleted, "Entry must not have been deleted")

      for (index in 0 until Integer.MAX_VALUE) {
        val file = File(this.bookDir, "temporary_$index")
        if (!file.exists()) {
          FileOutputStream(file).use { return file }
        }
      }

      throw IOException("Could not find an available temporary file")
    }
  }

  companion object {
    const val COVER_FILENAME = "cover.jpg"
    const val THUMB_FILENAME = "thumb.jpg"

    /**
     * Create a format handle if required. This checks to see if there is a content type that is
     * accepted by any of the available formats, and instantiates one if one doesn't already exist.
     */

    private fun createFormatHandleIfRequired(
      context: Context,
      logger: Logger,
      objectMapper: ObjectMapper,
      constructors: EnumMap<BookFormats.BookFormatDefinition, DatabaseBookFormatHandleConstructor>,
      ownerDirectory: File,
      owner: BookDatabaseEntryType,
      onUpdate: (BookFormat) -> Unit,
      bookFormats: BookFormatSupportType,
      existingFormats: MutableMap<Class<out BookDatabaseEntryFormatHandle>, BookDatabaseEntryFormatHandle>,
      contentTypes: Set<MIMEType>
    ) {
      for (contentType in contentTypes) {
        for ((format, constructor) in constructors) {
          if (format.supports(contentType)) {
            if (!bookFormats.isSupportedFinalContentType(contentType)) {
              logger.debug(
                "[{}]: skipping unsupported format {} for content type {}",
                owner.book.id.brief(), constructor.classType.simpleName, contentType
              )
              continue
            }

            // Skip if handler already exists for type
            if (existingFormats.containsKey(constructor.classType)) {
              logger.debug(
                "[{}]: skipping duplicate format {} for content type {}",
                owner.book.id.brief(), constructor.classType.simpleName, contentType
              )
              continue
            }

            // Add new handler for type
            logger.debug(
              "[{}]: instantiating format {} for content type {}",
              owner.book.id.brief(), constructor.classType.simpleName, contentType
            )

            val params =
              DatabaseFormatHandleParameters(
                context = context,
                bookID = owner.book.id,
                directory = ownerDirectory,
                onUpdated = onUpdate,
                entry = owner,
                contentType = contentType,
                objectMapper = objectMapper,
                bookFormatSupport = bookFormats
              )

            existingFormats[constructor.classType] = constructor.constructor.invoke(params)
            return
          }
        }
      }
    }
  }
}
