package org.nypl.simplified.books.book_database

import com.fasterxml.jackson.databind.ObjectMapper
import net.jcip.annotations.GuardedBy
import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.api.PlayerAudioEngines
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerPositions
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
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
  private val parameters: DatabaseFormatHandleParameters
) : BookDatabaseEntryFormatHandleAudioBook() {

  private val log =
    LoggerFactory.getLogger(DatabaseFormatHandleAudioBook::class.java)

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

  private val dataLock: Any = Any()

  @GuardedBy("dataLock")
  private var drmHandleRef =
    BookDRMInformationHandles.open(
      directory = this.parameters.directory,
      format = this.formatDefinition,
      onUpdate = this::onDRMUpdated
    )

  @GuardedBy("dataLock")
  private var formatRef: BookFormat.BookFormatAudioBook =
    synchronized(this.dataLock) {
      loadInitial(
        objectMapper = this.parameters.objectMapper,
        fileManifest = this.fileManifest,
        fileManifestURI = this.fileManifestURI,
        filePosition = this.filePosition,
        contentType = this.parameters.contentType,
        drmInfo = this.drmInformationHandle.info
      )
    }

  private fun onDRMUpdated() {
    this.parameters.onUpdated.invoke(this.refreshDRM())
  }

  private fun refreshDRM(): BookFormat.BookFormatAudioBook {
    return synchronized(this.dataLock) {
      this.formatRef = this.formatRef.copy(drmInformation = this.drmInformationHandle.info)
      this.formatRef
    }
  }

  override val format: BookFormat.BookFormatAudioBook
    get() = synchronized(this.dataLock) { this.formatRef }

  override val drmInformationHandle: BookDRMInformationHandle
    get() = synchronized(this.dataLock) { this.drmHandleRef }

  override fun setDRMKind(kind: BookDRMKind) {
    synchronized(this.dataLock) {
      val oldRef = (this.drmHandleRef as BookDRMInformationHandleBase)
      this.drmHandleRef = BookDRMInformationHandles.create(
        directory = this.parameters.directory,
        format = this.formatDefinition,
        drmKind = kind,
        onUpdate = this::onDRMUpdated
      )
      oldRef.close()
      this.onDRMUpdated()
    }
  }

  override fun deleteBookData() {
    val newFormat = synchronized(this.dataLock) {
      FileUtilities.fileDelete(this.filePosition)

      this.formatRef = this.formatRef.copy(position = null)
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

        val manifestResult: ParseResult<PlayerManifest> =
          ManifestParsers.parse(this.fileManifest.toURI(), stream.readBytes())

        when (manifestResult) {
          is ParseResult.Failure -> {
            for (error in manifestResult.errors) {
              this.log.debug(
                "[{}]: parse error: {}:{}: {}",
                briefID,
                error.line,
                error.column,
                error.message
              )
            }
            throw IOException("One or more manifest parse errors occurred")
          }

          is ParseResult.Success -> {
            this.log.debug("[{}]: selecting audio engine", briefID)

            val engine =
              PlayerAudioEngines.findBestFor(
                PlayerAudioEngineRequest(
                  manifest = manifestResult.result,
                  filter = { true },
                  downloadProvider = NullDownloadProvider(),
                  userAgent = PlayerUserAgent("unused")
                )
              )

            if (engine == null) {
              throw UnsupportedOperationException(
                "No audio engine is available to process the given request"
              )
            }

            this.log.debug(
              "[{}]: selected audio engine: {} {}",
              briefID,
              engine.engineProvider.name(),
              engine.engineProvider.version()
            )

            when (val bookResult = engine.bookProvider.create(this.parameters.context)) {
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

  override fun copyInManifestAndURI(
    data: ByteArray,
    manifestURI: URI
  ) {
    val newFormat = synchronized(this.dataLock) {
      FileUtilities.fileWriteBytes(
        data, this.fileManifest)
      FileUtilities.fileWriteUTF8Atomically(
        this.fileManifestURI, this.fileManifestURITmp, manifestURI.toString())

      this.formatRef =
        this.formatRef.copy(
          manifest = BookFormat.AudioBookManifestReference(
            manifestURI = manifestURI,
            manifestFile = this.fileManifest
          )
        )
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun savePlayerPosition(position: PlayerPosition) {
    val text =
      JSONSerializerUtilities.serializeToString(PlayerPositions.serializeToObjectNode(position))

    val newFormat = synchronized(this.dataLock) {
      FileUtilities.fileWriteUTF8Atomically(this.filePosition, this.filePositionTmp, text)
      this.formatRef = this.formatRef.copy(position = position)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  override fun clearPlayerPosition() {
    val newFormat = synchronized(this.dataLock) {
      FileUtilities.fileDelete(this.filePosition)
      this.formatRef = this.formatRef.copy(position = null)
      this.formatRef
    }

    this.parameters.onUpdated.invoke(newFormat)
  }

  companion object {

    private fun loadInitial(
      objectMapper: ObjectMapper,
      fileManifest: File,
      fileManifestURI: File,
      filePosition: File,
      contentType: MIMEType,
      drmInfo: BookDRMInformation
    ): BookFormat.BookFormatAudioBook {
      return BookFormat.BookFormatAudioBook(
        manifest = this.loadManifestIfNecessary(fileManifest, fileManifestURI),
        position = this.loadPositionIfNecessary(objectMapper, filePosition),
        contentType = contentType,
        drmInformation = drmInfo
      )
    }

    private fun loadPositionIfNecessary(
      objectMapper: ObjectMapper,
      filePosition: File
    ): PlayerPosition? {
      return if (filePosition.isFile) {
        this.loadPosition(objectMapper, filePosition)
      } else {
        null
      }
    }

    private fun loadPosition(
      objectMapper: ObjectMapper,
      filePosition: File
    ): PlayerPosition? {
      return try {
        FileInputStream(filePosition).use { stream ->
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
        throw IOException(e)
      }
    }

    private fun loadManifestIfNecessary(
      fileManifest: File,
      fileManifestURI: File
    ): BookFormat.AudioBookManifestReference? {
      return if (fileManifest.isFile) {
        this.loadManifest(fileManifest, fileManifestURI)
      } else {
        null
      }
    }

    private fun loadManifest(
      fileManifest: File,
      fileManifestURI: File
    ): BookFormat.AudioBookManifestReference {
      return BookFormat.AudioBookManifestReference(
        manifestFile = fileManifest,
        manifestURI = URI.create(FileUtilities.fileReadUTF8(fileManifestURI))
      )
    }
  }
}
