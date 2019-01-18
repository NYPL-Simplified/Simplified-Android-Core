package org.nypl.simplified.app.reader

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.app.AnnotationsManager
import org.nypl.simplified.app.ApplicationColorScheme
import org.nypl.simplified.app.R
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountsControllerType
import org.nypl.simplified.books.core.BookmarkAnnotation
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.books.reader.ReaderBookLocationJSON
import org.nypl.simplified.multilibrary.Account
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.readium.sdk.android.Package
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Timer
import kotlin.concurrent.schedule

enum class ReadingLocationSyncStatus {
  IDLE, BUSY
}

/**
 * Provide support for sync-related activities and logic.
 * Utilize ReaderSyncManagerDelegate Interface to
 * communicate commands to a book renderer.
 */
class ReaderSyncManager(private val feedEntry: OPDSAcquisitionFeedEntry,
                        credentials: AccountCredentials,
                        private val libraryAccount: Account,
                        context: Context,
                        private val pageNavigationListener: (ReaderBookLocation) -> Unit,
                        private val applicationColorScheme: ApplicationColorScheme) {

  private companion object {
    val LOG = LoggerFactory.getLogger(ReaderSyncManager::class.java)!!
  }

  var bookPackage: Package? = null

  private val delayTimeInterval = 120 * 1000L
  private val jsonMapper = ObjectMapper()
  
  private var delayReadingPositionSync = true
  private val annotationsManager = AnnotationsManager(this.libraryAccount, credentials, context)
  private var status = ReadingLocationSyncStatus.IDLE
  private var queuedReadingPosition: ReaderBookLocation? = null


  /**
   * See if sync is enabled on the server before attempting to synchronize
   * bookmarks or reading position. Set sharedPrefs accordingly.
   * Execute the closure if it's already on or successfully enabled on the server.
   */
  fun serverSyncPermission(account: AccountsControllerType,
                           completion: () -> Unit) {
    this.annotationsManager.requestServerSyncPermissionStatus(account) { shouldEnable ->
      if (shouldEnable) {
        this.setPermissionSharedPref(true)
        completion()
      }
    }
  }

  fun syncReadingLocation(device: String,
                          currentLocation: ReaderBookLocation,
                          context: Context) {
    if (!this.annotationsManager.syncIsPossibleAndPermitted()) {
      this.delayReadingPositionSync = false
      LOG.debug("Account does not support sync or sync is disabled.")
      return
    }
    if (!this.feedEntry.annotations.isSome) {
      this.delayReadingPositionSync = false
      LOG.error("No annotations uri or feed entry exists for this book. Abandoning sync attempt.")
      return
    }

    val uriString = (this.feedEntry.annotations as Some<URI>).get().toString()

    this.annotationsManager.requestReadingPositionOnServer(this.feedEntry.id, uriString) { serverLocation ->
      this.interpretUXForSync(device, serverLocation, currentLocation, context)
    }
  }

  private fun interpretUXForSync(device: String,
                                 serverLocation: BookmarkAnnotation?,
                                 currentLocation: ReaderBookLocation,
                                 context: Context) {
    this.delayReadingPositionSync = false

    if (serverLocation == null) {
      LOG.debug("No server location object returned.")
      return
    }

    val serverBookLocation = ReaderBookLocationJSON.deserializeFromString(
      this.jsonMapper, serverLocation.target.selector.value)

    val alert = this.createAlertForSyncLocation(serverBookLocation, context) { shouldMove ->
      if (shouldMove) {
        pageNavigationListener(serverBookLocation)
      }
    }

    // Pass through without presenting the Alert Dialog for any of the following:
    // 1 - The server and the client have the same page marked
    // 2 - There is no recent page saved on the server
    // 3 - The server mark came from the same device
    if (currentLocation.toString() != serverBookLocation.toString() &&
        device != serverLocation.body.device) {
      if ((context as? Activity)?.isFinishing == false) {
        UIThread.runOnUIThread {
          alert.show()
          val mainTextColor = this.applicationColorScheme.colorRGBA
          alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(mainTextColor)
          alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mainTextColor)
        }
      }
    }
  }

  private fun createAlertForSyncLocation(location: ReaderBookLocation, context: Context,
                                         completion: (shouldMove: Boolean) -> Unit): AlertDialog {
    val builder = AlertDialog.Builder(context)

    with(builder) {

      this.setTitle(context.getString(R.string.syncManagerSyncLocationAlertTitle))

      val p = this@ReaderSyncManager.bookPackage
      if (p != null) {
        val chapterTitle = p.getSpineItem(location.idRef()).title
        val formatString = String.format(context.getString(R.string.syncManagerSyncLocationAlertMessage), chapterTitle)
        this.setMessage(formatString)
      } else {
        this.setMessage(context.getString(R.string.syncManagerSyncLocationAlertMessageGeneral))
      }

      this.setPositiveButton(context.getString(R.string.syncManagerSyncLocationAlertMove)) { _, _ ->
        completion(true)
        LOG.debug("User chose to jump to synced page: ${location.contentCFI()}")
      }

      this.setNegativeButton(context.getString(R.string.syncManagerSyncLocationAlertStay)) { _, _ ->
        completion(false)
        LOG.debug("User declined jump to synced page.")
      }

      return this.create()
    }
  }

  /**
   * Attempt to update current page to the server.
   * Prevent high-frequency requests to the server by queueing and shooting
   * off tasks at a set time interval.
   * @param location The page to be sent as the current "left off" page
   */
  fun updateServerReadingLocation(location: ReaderBookLocation) {

    if (this.delayReadingPositionSync) {
      LOG.debug("Post of last read position delayed. Initial sync still in progress.")
      return
    }

    if (this.annotationsManager.syncIsPossibleAndPermitted()) {
      synchronized(this) {

        val loc = ReaderBookLocationJSON.serializeToString(this.jsonMapper, location)
        if (loc == null) {
          LOG.error("Skipped upload of location due to unexpected null json representation")
          return
        }

        when (this.status) {

          ReadingLocationSyncStatus.IDLE -> {
            this.status = ReadingLocationSyncStatus.BUSY
            this.annotationsManager.updateReadingPosition(this.feedEntry.id, loc)

            Timer("ReadingPositionTimer", false).schedule(this.delayTimeInterval) {
              synchronized(this@ReaderSyncManager) {
                this@ReaderSyncManager.status = ReadingLocationSyncStatus.IDLE
                if (this@ReaderSyncManager.queuedReadingPosition != null) {
                  this@ReaderSyncManager.annotationsManager.updateReadingPosition(this@ReaderSyncManager.feedEntry.id, loc)
                  this@ReaderSyncManager.queuedReadingPosition = null
                }
              }
            }
          }

          ReadingLocationSyncStatus.BUSY -> {
            this.queuedReadingPosition = location
          }
        }
      }
    }
  }

  fun sendOffAnyQueuedRequest() {
    synchronized(this) {
      val positionJson = this.queuedReadingPosition
      if (positionJson != null) {
        val pos = ReaderBookLocationJSON.serializeToString(this.jsonMapper, positionJson)
        if (pos != null) {
          this.annotationsManager.updateReadingPosition(this.feedEntry.id, pos)
        }
      }
    }
  }

  /**
   * Try to re-upload any bookmarks that may not have been previously saved on the server.
   * This is NOT a serial action on a single thread.
   */
  fun retryOnDiskBookmarksUpload(marks: List<BookmarkAnnotation>) {
    marks.filter { it.id == null }
         .map { this.postBookmarkToServer(it, null) }
  }

  /**
   * Download list of bookmark annotations from the server for the particular book,
   * and synchronize that list as best as possible with the current list
   * of bookmarks saved in the local database.
   * @param completion returns a List of bookmarks, empty if none exist
   */
  fun syncBookmarks(onDiskBookmarks: List<BookmarkAnnotation>,
                    completion: ((syncedBookmarks: List<BookmarkAnnotation>?) -> Unit)?) {

    val uri = if (this.feedEntry.annotations.isSome) {
      (this.feedEntry.annotations as Some<URI>).get().toString()
    } else {
      LOG.error("No annotations URI present in feed entry.")
      return
    }

    this.annotationsManager.requestBookmarksFromServer(uri) { serverBookmarks ->

      /*
      Synchronize:
      1. Do not omit any bookmark that came from the server
      2. Only delete an on-disk bookmark if it's missing from the server, and it has an ID.
       */

      val localBookmarksToKeep = onDiskBookmarks.toMutableList()

      for (onDiskMark in onDiskBookmarks) {
        val match = serverBookmarks.filter { it.id == onDiskMark.id }
        if (match.count() < 1 && onDiskMark.id != null) {
          localBookmarksToKeep.remove(onDiskMark)
        }
      }

      localBookmarksToKeep.addAll(0, serverBookmarks)

      //Squash duplicates and prioritize ones with an ID
      val syncedMarks = localBookmarksToKeep.sortedWith(nullsLast(compareBy({ it.id })))
                                            .distinctBy { it.target.selector.value }

      completion?.let { it(syncedMarks) }
    }
  }

  fun postBookmarkToServer(bookAnnotation: BookmarkAnnotation,
                           completion: ((serverID: String?) -> Unit)?) {
    this.annotationsManager.postBookmarkToServer(bookAnnotation, completion)
  }

  fun deleteBookmarkOnServer(annotationID: String,
                             completion: (success: Boolean) -> Unit) {
    this.annotationsManager.deleteBookmarkOnServer(annotationID, completion)
  }

  private fun setPermissionSharedPref(status: Boolean) {
    throw UnimplementedCodeException()
  }
}
