package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.OnClickListener
import com.io7m.jfunctional.Some
import com.io7m.jnull.Nullable
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.app.ApplicationColorScheme
import org.nypl.simplified.app.player.AudioBookPlayerActivity
import org.nypl.simplified.app.player.AudioBookPlayerParameters
import org.nypl.simplified.app.reader.ReaderActivity
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
import org.nypl.simplified.books.feeds.FeedEntryOPDS
import org.nypl.simplified.circanalytics.CirculationAnalytics
import org.slf4j.LoggerFactory
import java.io.File

/**
 * A controller that opens a given book for reading.
 */

class CatalogBookReadController(
  val activity: AppCompatActivity,
  val account: AccountType,
  val id: BookID,
  val entry: FeedEntryOPDS,
  val colorScheme: ApplicationColorScheme) : OnClickListener {

  companion object {
    private val LOG = LoggerFactory.getLogger(CatalogBookReadController::class.java)
  }

  override fun onClick(@Nullable v: View) {
    val credentialsOpt = this.account.credentials()
    if (credentialsOpt is Some<AccountAuthenticationCredentials>) {
      val credentials = credentialsOpt.get();
      CirculationAnalytics.postEvent(credentials, this.activity, this.entry, "open_book")
    }

    val database = this.account.bookDatabase()
    val entry = database.entry(this.id)
    throw UnimplementedCodeException()
  }

  private fun launchEPUBReader(format: BookDatabaseEntryFormatSnapshotEPUB) {
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

  private fun launchAudioBookPlayer(format: BookDatabaseEntryFormatSnapshotAudioBook) {
    val manifestOpt = format.manifest
    if (manifestOpt is Some<BookDatabaseEntryFormatSnapshot.AudioBookManifestReference>) {
      AudioBookPlayerActivity.startActivity(
        from = this.activity,
        parameters = AudioBookPlayerParameters(
          manifestFile = manifestOpt.get().manifestFile,
          manifestURI = manifestOpt.get().manifestURI,
          opdsEntry = this.entry.feedEntry,
          applicationColorScheme = this.colorScheme,
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
