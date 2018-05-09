package org.nypl.simplified.app

import android.app.AlertDialog
import android.content.Context
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.reader.ReaderBookLocation
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountsControllerType
import org.nypl.simplified.books.core.BookmarkAnnotation
import org.nypl.simplified.multilibrary.Account
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.readium.sdk.android.Package
import org.slf4j.LoggerFactory
import java.io.Reader
import java.net.URI
import java.util.*
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
                        private val pageNavigationListener: (ReaderBookLocation) -> Unit) {

  private companion object {
    val LOG = LoggerFactory.getLogger(ReaderSyncManager::class.java)!!
  }

  //TODO not being set right now.. is this not available during init?
  val bookPackage: Package? = null

  private val delayTimeInterval = 120L

  private var delayReadingPositionSync = true
  private val annotationsManager = AnnotationsManager(libraryAccount, credentials, context)
  private var status = ReadingLocationSyncStatus.IDLE
  private var queuedReadingPosition: ReaderBookLocation? = null


  /**
   * See if sync is enabled on the server before attempting to synchronize
   * bookmarks or reading position. Set sharedPrefs accordingly.
   * Execute the closure if it's already on or successfully enabled on the server.
   */
  fun serverSyncPermission(account: AccountsControllerType,
                           completion: () -> Unit) {
    annotationsManager.requestServerSyncPermissionStatus(account) { shouldEnable ->
      if (shouldEnable) {
        setPermissionSharedPref(true)
        completion()
      }
    }
  }

  fun syncReadingLocation(currentLocation: ReaderBookLocation,
                          context: Context) {
    if (!annotationsManager.syncIsPossibleAndPermitted()) {
      delayReadingPositionSync = false
      LOG.debug("Account does not support sync or sync is disabled.")
      return
    }
    if (!feedEntry.annotations.isSome) {
      delayReadingPositionSync = false
      LOG.error("No annotations uri or feed entry exists for this book. Abandoning sync attempt.")
      return
    }

    val uriString = (feedEntry.annotations as Some<URI>).get().toString()

    annotationsManager.requestReadingPositionOnServer(feedEntry.id, uriString, { serverLocation ->
      interpretUXForSync(serverLocation, context, currentLocation)
    })
  }

  private fun interpretUXForSync(serverLocation: ReaderBookLocation?,
                                 context: Context,
                                 currentLocation: ReaderBookLocation) {
    delayReadingPositionSync = false

    if (serverLocation == null) {
      LOG.debug("No server location object returned.")
      return
    }

    val alert = createAlertForSyncLocation(serverLocation, context) { shouldMove ->
      if (shouldMove) {
        pageNavigationListener(serverLocation)
      }
    }

    // Pass through without presenting the Alert Dialog if:
    // 1 - The server and the client have the same page marked
    // 2 - There is no recent page saved on the server
    if (currentLocation.toString() != serverLocation.toString()) {
      UIThread.runOnUIThread {
        alert.show()
      }
    }
  }

  private fun createAlertForSyncLocation(location: ReaderBookLocation, context: Context,
                                         completion: (shouldMove: Boolean) -> Unit): AlertDialog {
    val builder = AlertDialog.Builder(context)

    with(builder) {

      setTitle(context.getString(R.string.syncManagerSyncLocationAlertTitle))

      if (bookPackage != null) {
        val chapterTitle = bookPackage.getSpineItem(location.idRef).title
        val formatString = String.format(context.getString(R.string.syncManagerSyncLocationAlertMessage), chapterTitle)
        setMessage(formatString)
      } else {
        setMessage(context.getString(R.string.syncManagerSyncLocationAlertMessageGeneral))
      }

      setPositiveButton("YES") { _, _ ->
        completion(true)
        LOG.debug("User chose to jump to synced page: ${location.contentCFI}")
      }

      setNegativeButton("NO") { _, _ ->
        completion(false)
        LOG.debug("User declined jump to synced page.")
      }

      return create()
    }
  }

  /**
   * Attempt to update current page to the server.
   * Prevent high-frequency requests to the server by queueing and shooting
   * off tasks at a set time interval.
   * @param location The page to be sent as the current "left off" page
   */
  fun updateServerReadingLocation(location: ReaderBookLocation) {

    if (delayReadingPositionSync) {
      LOG.debug("Post of last read position delayed. Initial sync still in progress.")
      return
    }

    if (annotationsManager.syncIsPossibleAndPermitted()) {

      synchronized(this) {

        val loc = location.toJsonString()
        if (loc == null) {
          LOG.error("Skipped upload of location due to unexpected null json representation")
          return
        }

        when (this.status) {

          ReadingLocationSyncStatus.IDLE -> {
            this.status = ReadingLocationSyncStatus.BUSY
            annotationsManager.updateReadingPosition(feedEntry.id, loc)

            Timer("ReadingPositionTimer", false).schedule(delayTimeInterval) {
              synchronized(this) {
                status = ReadingLocationSyncStatus.IDLE
                if (queuedReadingPosition != null) {
                  annotationsManager.updateReadingPosition(feedEntry.id, loc)
                  queuedReadingPosition = null
                }
              }
            }
          }

          ReadingLocationSyncStatus.BUSY -> {
            queuedReadingPosition = location
          }
        }
      }
    }
  }

  fun sendOffAnyQueuedRequest() {
    synchronized(this) {
      val pos = queuedReadingPosition?.toJsonString()
      if (pos != null) {
        annotationsManager.updateReadingPosition(feedEntry.id, pos)
      }
    }
  }

  /**
   * Download list of bookmark annotations from the server for the particular book,
   * and synchronize that list as best as possible with the current list
   * of bookmarks saved in the local database.
   * @param completion returns a List of bookmarks, empty if none exist
   */
  fun syncBookmarks(completion: ((bookmarks: List<BookmarkAnnotation>?) -> Unit)?) {

    val uri = if (feedEntry.annotations.isSome) {
      (feedEntry.annotations as Some<URI>).get().toString()
    } else {
      LOG.error("No annotations URI present in feed entry.")
      return
    }

    annotationsManager.requestBookmarksFromServer(uri) { bookmarks ->

      //TODO WIP

      LOG.debug("Bookmarks: $bookmarks")
      completion?.invoke(bookmarks)

    }

  }

  fun postBookmarkToServer(bookAnnotation: BookmarkAnnotation,
                           completion: (serverID: String?) -> Unit) {
    annotationsManager.postBookmarkToServer(bookAnnotation, completion)
  }

  fun deleteBookmarkOnServer(annotationID: String,
                             completion: (success: Boolean) -> Unit) {
    annotationsManager.deleteBookmarkOnServer(annotationID, completion)
  }

  private fun setPermissionSharedPref(status: Boolean) {
    Simplified.getSharedPrefs().putBoolean("syncPermissionGranted", libraryAccount.id, status)
  }
}
