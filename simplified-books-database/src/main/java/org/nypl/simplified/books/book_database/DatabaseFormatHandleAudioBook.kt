package org.nypl.simplified.books.book_database

import com.fasterxml.jackson.databind.ObjectMapper
import org.librarysimplified.audiobook.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.api.PlayerAudioEngines
import org.librarysimplified.audiobook.api.PlayerManifests
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerPositions
import org.librarysimplified.audiobook.api.PlayerResult
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI

/**
 * Operations on audio book formats in database entries.
 */

internal class DatabaseFormatHandleAudioBook internal constructor(
  private val parameters: DatabaseFormatHandleParameters)
  : BookDatabaseEntryFormatHandleAudioBook() {

  private val log = LoggerFactory.getLogger(DatabaseFormatHandleAudioBook::class.java)

  private val fileManifest: File =
    File(this.parameters.directory, "audiobook-manifest.json")
  private val fileManifestURI: File =
    File(this.parameters.directory, "audiobook-manifest-uri.txt")
  private val fileManifestURITmp: File =
    File(this.parameters.directory, "audiobook-manifest-uri.txt.tmp")
  private val filePosition: File =
    File(this.parameters.directory, "audiobook-position.json")
  private val filePositionTmp: File =
    File(this.parameters.directory, "audiobook-position.json.tmp")

  private val formatLock: Any = Any()
  private var formatRef: BookFormat.BookFormatAudioBook =
    synchronized(this.formatLock) {
      loadInitial(
        fileManifest = this.fileManifest,
        fileManifestURI = this.fileManifestURI,
        filePosition = this.filePosition)
    }

  override val format: BookFormat.BookFormatAudioBook
    get() = synchronized(this.formatLock) { this.formatRef }

  override fun deleteBookData() {
    val newFormat = synchronized(this.formatLock) {
      FileUtilities.fileDelete(this.filePosition)

      this.formatRef =
        this.formatRef.copy(position = null)
      this.formatRef
    }

    val briefID = this.parameters.bookID.brief()

    this.log.debug("[{}]: deleting audio book data", briefID)

    /*
     * Parse the manifest, start up an audio engine, and then tell it to delete all and any
     * downloaded parts.
     */

    if (!this.fileManifest.isFile) {
      this.log.debug("[{}]: no manifest available", briefID)
      return
    }

    try {
      FileInputStream(this.fileManifest).use { stream ->
        this.log.debug("[{}]: parsing audio book manifest", briefID)

        val manifestResult = PlayerManifests.parse(stream)
        when (manifestResult) {
          is PlayerResult.Failure -> throw manifestResult.failure
          is PlayerResult.Success -> {
            this.log.debug("[{}]: selecting audio engine", briefID)

            val engine = PlayerAudioEngines.findBestFor(
              PlayerAudioEngineRequest(
                manifest = manifestResult.result,
                filter = { true },
                downloadProvider = NullDownloadProvider()))

            if (engine == null) {
              throw UnsupportedOperationException(
                "No audio engine is available to process the given request")
            }

            this.log.debug(
              "[{}]: selected audio engine: {} {}",
              briefID,
              engine.engineProvider.name(),
              engine.engineProvider.version())

            val bookResult = engine.bookProvider.create(this.parameters.context)
            when (bookResult) {
              is PlayerResult.Success -> bookResult.result.wholeBookDownloadTask.delete()
              is PlayerResult.Failure -> throw bookResult.failure
            }

            this.log.debug("[{}]: deleted audio book data", briefID)
          }
        }
      }
    } catch (ex: Exception) {
      this.log.error("[{}]: failed to delete audio book: ", briefID, ex)
      throw ex
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun copyInManifestAndURI(file: File, manifestURI: URI) {
    val newFormat = synchronized(this.formatLock) {
      FileUtilities.fileCopy(
        file, this.fileManifest)
      FileUtilities.fileWriteUTF8Atomically(
        this.fileManifestURI, this.fileManifestURITmp, manifestURI.toString())

      this.formatRef =
        this.formatRef.copy(
          manifest = BookFormat.AudioBookManifestReference(
            manifestURI = manifestURI,
            manifestFile = this.fileManifest))
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun savePlayerPosition(position: PlayerPosition) {
    val text =
      JSONSerializerUtilities.serializeToString(PlayerPositions.serializeToObjectNode(position))

    val newFormat = synchronized(this.formatLock) {
      FileUtilities.fileWriteUTF8Atomically(this.filePosition, this.filePositionTmp, text)
      this.formatRef = this.formatRef.copy(position = position)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun clearPlayerPosition() {
    val newFormat = synchronized(this.formatLock) {
      FileUtilities.fileDelete(this.filePosition)
      this.formatRef = this.formatRef.copy(position = null)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override val formatDefinition: BookFormats.BookFormatDefinition
    get() = BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO

  companion object {

    private fun loadInitial(
      fileManifest: File,
      fileManifestURI: File,
      filePosition: File): BookFormat.BookFormatAudioBook {
      return BookFormat.BookFormatAudioBook(
        manifest = loadManifestIfNecessary(fileManifest, fileManifestURI),
        position = loadPositionIfNecessary(filePosition))
    }

    private fun loadPositionIfNecessary(
      filePosition: File): PlayerPosition? {
      return if (filePosition.isFile) {
        loadPosition(filePosition)
      } else {
        null
      }
    }

    private fun loadPosition(filePosition: File): PlayerPosition? {
      return try {
        FileInputStream(filePosition).use { stream ->
          val jom = ObjectMapper()
          val result =
            PlayerPositions.parseFromObjectNode(
              JSONParserUtilities.checkObject(null, jom.readTree(stream)))

          when (result) {
            is PlayerResult.Success -> result.result
            is PlayerResult.Failure -> throw result.failure
          }
        }
      } catch (e: FileNotFoundException) {
        null
      } catch (e: Exception) {
        throw IOException(e)
      }
    }

    private fun loadManifestIfNecessary(
      fileManifest: File,
      fileManifestURI: File): BookFormat.AudioBookManifestReference? {
      return if (fileManifest.isFile) {
        loadManifest(fileManifest, fileManifestURI)
      } else {
        null
      }
    }

    private fun loadManifest(
      fileManifest: File,
      fileManifestURI: File): BookFormat.AudioBookManifestReference {
      return BookFormat.AudioBookManifestReference(
        manifestFile = fileManifest,
        manifestURI = URI.create(FileUtilities.fileReadUTF8(fileManifestURI)))
    }
  }

}