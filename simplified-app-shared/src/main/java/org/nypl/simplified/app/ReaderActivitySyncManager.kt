package org.nypl.simplified.app

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.reader.ReaderBookLocation
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.multilibrary.Account
import org.readium.sdk.android.Package
import org.slf4j.LoggerFactory
import java.util.Timer
import kotlin.concurrent.schedule


interface ReaderSyncManagerDelegate {
  fun navigateToLocation(location: ReaderBookLocation)
  fun bookmarkUploadDidFinish(bookmark: NYPLBookmark, bookID: String)
  fun bookmarkSyncDidFinish(success: Boolean, bookmarks: List<NYPLBookmark>)
}

class ReaderSyncManager(private val bookID: String,
                        private val bookPackage: Package,
                        private val credentials: AccountCredentials,
                        private val libraryAccount: Account,
                        private val delegate: ReaderSyncManagerDelegate) : ReaderSyncManagerDelegate by delegate {

  //TODO reader will allocate sync manager, and sync manager will allocate annotations manager
  private val annotationsManager = AnnotationsManager(libraryAccount, credentials, delegate as Context)

  // The purpose of this property is to delay any new read-position postings
  // until the initial synchronize has completed and user input has been received.
  private var shouldPostLastRead: Boolean = false

  private companion object {
    val LOG = LoggerFactory.getLogger(ReaderSyncManager::class.java)!!
  }

  private var queueTimer = Timer()

  fun synchronizeLastReadPosition(bookID: String,
                                  currentLocation: ReaderBookLocation,
                                  uri: String,
                                  context: Context) {

    if (!annotationsManager.syncIsPossibleAndPermitted()) {
      LOG.debug("Account does not support sync or sync is disabled.")
      return
    }

    annotationsManager.requestReadingPositionOnServer(bookID, uri, { serverLocation ->

      //TODO will this ever be null? Also, what would a null contentCFI look like within ReaderBookLocation?
      //TODO update saving of last read position to match what is on iOS (check reader activity).
      if (serverLocation == null) {
        //TODO Do I want code execution here, if there is no saved location on the server? What is the effect of shouldPostLastRead not being set?
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
      if (currentLocation.toString() == serverLocation.toString()) {
        shouldPostLastRead = true
      } else {
        alert.show()
      }
    })
  }

  private fun createAlertForSyncLocation(location: ReaderBookLocation, context: Context,
                                       completion: (shouldMove: Boolean) -> Unit): AlertDialog {

    val builder= AlertDialog.Builder(context)
    builder.setTitle("Sync Reading Position")

    val chapterTitle = bookPackage.getSpineItem(location.idRef).title
    builder.setMessage("Would you like to go to the latest page read? \n\nChapter:\n\"${chapterTitle}\"")

    builder.setPositiveButton("YES", DialogInterface.OnClickListener() { dialog, which ->
      completion(true)
      shouldPostLastRead = true
      LOG.debug("User chose to jump to synced page: ${location.contentCFI}")
    })

    builder.setNegativeButton("NO", DialogInterface.OnClickListener() { dialog, which ->
      completion(false)
      shouldPostLastRead = true
      LOG.debug("User declined jump to synced page.")
    })

    return builder.create()
  }


  fun postLastReadPosition(location: ReaderBookLocation, uri: String) {

    if (!shouldPostLastRead) {
      return
    }

    queueTimer.cancel()
    queueTimer = Timer()
    queueTimer.schedule(3000) {

      if (location.contentCFI.isSome) {
        val someLocation = location.contentCFI as Some<String>
        annotationsManager.updateReadingPosition(bookID, uri, someLocation.get())
      }

    }

    //TODO 2 - update THIS method to have what is currently in ReaderActivity
  }
}


// TODO TEMP UNTIL I CAN FIGURE THIS OUT
// Would represent a bookmark element to be saved to the server and also de/serialized for TOC
class NYPLBookmark(val whatever: Int)
