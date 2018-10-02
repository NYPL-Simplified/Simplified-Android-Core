package org.nypl.simplified.app.catalog

import android.app.Activity
import android.view.View
import android.view.View.OnClickListener
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jnull.Nullable
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.player.AudioBookPlayerActivity
import org.nypl.simplified.app.player.AudioBookPlayerParameters
import org.nypl.simplified.app.reader.ReaderActivity
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.books.core.LogUtilities
import org.nypl.simplified.circanalytics.CirculationAnalytics
import java.io.File

/**
 * A controller that opens a given book for reading.
 */

class CatalogBookRead(
  val activity: Activity,
  val id: BookID,
  val entry: FeedEntryOPDS) : OnClickListener {

  override fun onClick(@Nullable v: View) {

    val prefs = Simplified.getSharedPrefs()
    prefs.putBoolean("post_last_read", false)
    LOG.debug("CurrentPage prefs {}", prefs.getBoolean("post_last_read"))

    val app = Simplified.getCatalogAppServices()
    val books = app.books

    books.accountGetCachedLoginDetails(
      object : AccountGetCachedCredentialsListenerType {
        override fun onAccountIsNotLoggedIn() {
          throw UnreachableCodeException()
        }

        override fun onAccountIsLoggedIn(creds: AccountCredentials) {
          CirculationAnalytics.postEvent(creds,
            this@CatalogBookRead.activity,
            this@CatalogBookRead.entry,
            "open_book")
        }
      })

    val database = books.bookGetDatabase()
    val snapshotOpt = database.databaseGetEntrySnapshot(this.id)

    if (snapshotOpt is Some<BookDatabaseEntrySnapshot>) {
      val snap = snapshotOpt.get()
      val formatOpt: OptionType<BookDatabaseEntryFormatSnapshot> = snap.findPreferredFormat()
      if (formatOpt is Some<BookDatabaseEntryFormatSnapshot>) {
        val format = formatOpt.get()
        return when (format) {
          is BookDatabaseEntryFormatSnapshotEPUB -> {
            val bookOpt = format.book
            if (bookOpt is Some<File>) {
              ReaderActivity.startActivity(this.activity, this.id, bookOpt.get(), this.entry)
            } else {
              ErrorDialogUtilities.showError(
                this.activity,
                LOG,
                "Bug: book claimed to be downloaded but no book file exists in storage",
                null)
            }
          }

          is BookDatabaseEntryFormatSnapshotAudioBook -> {
            val manifestOpt = format.manifest
            if (manifestOpt is Some<BookDatabaseEntryFormatSnapshot.AudioBookManifestReference>) {
              AudioBookPlayerActivity.startActivity(
                from = this.activity,
                parameters = AudioBookPlayerParameters(
                  manifestFile = manifestOpt.get().manifestFile,
                  manifestURI = manifestOpt.get().manifestURI,
                  opdsEntry = this.entry.feedEntry,
                  bookID = this.id))
            } else {
              ErrorDialogUtilities.showError(
                this.activity,
                LOG,
                "Bug: book claimed to be downloaded but no book file exists in storage",
                null)
            }
          }
        }
      }
    } else {
      ErrorDialogUtilities.showError(this.activity, LOG, "Book no longer exists!", null)
    }
  }

  companion object {
    private val LOG = LogUtilities.getLog(CatalogBookRead::class.java)
  }
}
