package org.nypl.simplified.app

import android.app.AlertDialog
import android.content.Context
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.reader.ReaderBookLocation
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountsControllerType
import org.nypl.simplified.multilibrary.Account
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.readium.sdk.android.Package
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Timer
import kotlin.concurrent.schedule


interface ReaderSyncManagerDelegate {
  fun navigateToLocation(location: ReaderBookLocation)
  fun bookmarkUploadDidFinish(bookmark: NYPLBookmark, bookID: String)
  fun bookmarkSyncDidFinish(success: Boolean, bookmarks: List<NYPLBookmark>)
}

/**
 * Provide support for sync-related activities and logic.
 * Utilize ReaderSyncManagerDelegate Interface to
 * communicate commands to a book renderer.
 */
class ReaderSyncManager(private val entryID: String,
                        credentials: AccountCredentials,
                        private val libraryAccount: Account,
                        private val delegate: ReaderSyncManagerDelegate) : ReaderSyncManagerDelegate by delegate
{
  private companion object {
    val LOG = LoggerFactory.getLogger(ReaderSyncManager::class.java)!!
  }

  //TODO Work In Progress -
  //TODO observe when set, then show jump to location dialog with appropriate messaging
  val bookPackage: Package? = null

  // Delay server posts until first page sync is complete
  private var delayPageSync: Boolean = true
  private val annotationsManager = AnnotationsManager(libraryAccount, credentials, delegate as Context)
  private var queueTimer = Timer()

  /**
   * See if sync is enabled on the server before attempting to synchronize
   * bookmarks or reading position. Set sharedPrefs accordingly.
   * Execute the closure if it's already on or successfully enabled on the server.
   */
  fun serverSyncPermission(account: AccountsControllerType,
                           completion: () -> Unit)
  {
    annotationsManager.requestServerSyncPermissionStatus(account) { shouldEnable->
      if (shouldEnable) {
        setPermissionSharedPref(true)
        completion()
      }
    }
  }

  fun synchronizeReadingLocation(currentLocation: ReaderBookLocation,
                                 feedEntry: OPDSAcquisitionFeedEntry?,
                                 context: Context)
  {
    if (!annotationsManager.syncIsPossibleAndPermitted()) {
      delayPageSync = false
      LOG.debug("Account does not support sync or sync is disabled.")
      return
    }
    if (feedEntry == null || !feedEntry.annotations.isSome) {
      delayPageSync = false
      LOG.error("No annotations uri or feed entry exists for this book. Abandoning sync attempt.")
      return
    }

    val uriString = (feedEntry.annotations as Some<URI>).get().toString()

    annotationsManager.requestReadingPositionOnServer(entryID, uriString, { serverLocation ->

      delayPageSync = false

      if (serverLocation == null) {
        LOG.debug("No server location object returned.")
        return@requestReadingPositionOnServer
      }

      val alert = createAlertForSyncLocation(serverLocation, context) { shouldMove ->
        if (shouldMove) {
          navigateToLocation(serverLocation)
        }
      }

      // Pass through without presenting the Alert Dialog if:
      // 1 - The server and the client have the same page marked
      // 2 - There is no recent page saved on the server
      //TODO make sure this conditional is working...
      if (currentLocation.toString() != serverLocation.toString()) {
        UIThread.runOnUIThread {
          alert.show()
        }
      }
    })
  }

  private fun createAlertForSyncLocation(location: ReaderBookLocation, context: Context,
                                       completion: (shouldMove: Boolean) -> Unit): AlertDialog
  {
    val builder= AlertDialog.Builder(context)
    builder.setTitle("Sync Reading Position")

    //TODO add cases once i figure out null content cfi equivalent from
    if (bookPackage != null) {
      val chapterTitle = bookPackage.getSpineItem(location.idRef).title
      builder.setMessage("Would you like to go to the latest page read? \n\nChapter:\n\"${chapterTitle}\"")
    } else {
      builder.setMessage("Would you like to go to the latest page read?")
    }

    builder.setPositiveButton("YES") { dialog, which ->
      completion(true)
      LOG.debug("User chose to jump to synced page: ${location.contentCFI}")
    }

    builder.setNegativeButton("NO") { dialog, which ->
      completion(false)
      LOG.debug("User declined jump to synced page.")
    }

    return builder.create()
  }


  /**
   * Attempt to update current page to the server.
   * @param location The page to be sent as the current "left off" page
   * @param uri Annotation URI for the specific entry
   */
  fun updateServerReadingLocation(location: ReaderBookLocation)
  {
    //TODO note to self: delayPageSync does not seem to work on second attempt.

    if (delayPageSync) {
      LOG.debug("Post of last read position delayed. Initial sync still in progress.")
      return
    }

    if (annotationsManager.syncIsPossibleAndPermitted()) {
      queueTimer.cancel()
      queueTimer = Timer()
      queueTimer.schedule(3000) {
        val locString = location.toJsonString()
        if (locString != null) {
          annotationsManager.updateReadingPosition(entryID, locString)
        } else {
          LOG.error("Skipped upload of location due to unexpected null json representation")
        }
      }
    }
  }

  private fun setPermissionSharedPref(status: Boolean)
  {
    Simplified.getSharedPrefs().putBoolean("syncPermissionGranted", libraryAccount.id, status)
  }
}


// TODO STUB WIP
// Would represent a bookmark element to be saved to the server and also de/serialized for TOC
class NYPLBookmark(val whatever: Int)
