package org.nypl.simplified.books.core

import android.content.Context
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.io7m.jfunctional.Some
import org.nypl.audiobook.android.api.PlayerAudioEngineRequest
import org.nypl.audiobook.android.api.PlayerAudioEngines
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerDownloadRequest
import org.nypl.audiobook.android.api.PlayerManifests
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.util.concurrent.Callable

internal class BooksControllerDeleteBookDataTask(
  private val context: Context,
  private val bookStatus: BooksStatusCacheType,
  private val bookDatabase: BookDatabaseType,
  private val bookID: BookID,
  private val needsAuthentication: Boolean) : Callable<Unit> {

  private val log = LoggerFactory.getLogger(BooksControllerDeleteBookDataTask::class.java)

  override fun call() {
    try {
      LOG.debug("[{}]: deleting book data", this.bookID.shortID)

      val databaseEntry =
        this.bookDatabase.databaseOpenExistingEntry(this.bookID)

      for (format in databaseEntry.entryFormatHandles()) {
        when (format) {
          is BookDatabaseEntryFormatHandleEPUB ->
            deleteEPUBData(format)
          is BookDatabaseEntryFormatHandleAudioBook ->
            deleteAudioBook(format)
        }
      }

      val snap = databaseEntry.entryGetSnapshot()
      val status = BookStatus.fromSnapshot(this.bookID, snap)
      this.bookStatus.booksStatusUpdate(status)

      // destroy entry, this is needed after deletion has been broadcasted, so the book doesn't stay in loans,
      // especially needed for collection without auth requirement where no syn is happening
      if (!this.needsAuthentication) {
        databaseEntry.entryDestroy()
      }
    } catch (e: Throwable) {
      LOG.error("[{}]: could not destroy book data: ", this.bookID.shortID, e)
      throw e
    } finally {
      LOG.debug("[{}]: deletion completed", this.bookID.shortID)
    }
  }

  private class NullDownloadProvider : PlayerDownloadProviderType {
    override fun download(request: PlayerDownloadRequest): ListenableFuture<Unit> {
      return Futures.immediateFailedFuture(UnsupportedOperationException())
    }
  }

  private fun deleteAudioBook(format: BookDatabaseEntryFormatHandleAudioBook) {

    val snapshot = format.snapshot()

    /*
     * We can currently only delete an audio book if the manifest is present and is parseable.
     */

    if (snapshot.manifest is Some<BookDatabaseEntryFormatSnapshot.AudioBookManifestReference>) {
      val manifest = snapshot.manifest.get()

      /*
       * Parse the manifest, start up an audio engine, and then tell it to delete all and any
       * downloaded parts.
       */

      FileInputStream(manifest.manifestFile).use { stream ->
        val manifestResult = PlayerManifests.parse(stream)
        when (manifestResult) {
          is PlayerResult.Failure -> throw manifestResult.failure
          is PlayerResult.Success -> {
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
              "selected audio engine: {} {}",
              engine.engineProvider.name(),
              engine.engineProvider.version())

            val bookResult = engine.bookProvider.create(this.context)
            when (bookResult) {
              is PlayerResult.Success -> bookResult.result.deleteLocalChapterData()
              is PlayerResult.Failure -> throw bookResult.failure
            }
          }
        }
      }
    }
  }

  private fun deleteEPUBData(format: BookDatabaseEntryFormatHandleEPUB) {
    format.deleteBookData()
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(BooksControllerDeleteBookDataTask::class.java)
  }
}
