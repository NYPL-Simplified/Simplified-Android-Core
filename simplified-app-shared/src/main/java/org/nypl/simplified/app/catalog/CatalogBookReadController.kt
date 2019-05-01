package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.OnClickListener
import com.io7m.jnull.Nullable
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.player.AudioBookPlayerActivity
import org.nypl.simplified.app.player.AudioBookPlayerParameters
import org.nypl.simplified.app.reader.ReaderActivity
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.Book
import org.nypl.simplified.books.book_database.BookFormat.BookFormatAudioBook
import org.nypl.simplified.books.book_database.BookFormat.BookFormatEPUB
import org.nypl.simplified.books.book_database.BookFormat.BookFormatPDF
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS
import org.slf4j.LoggerFactory

/**
 * A controller that opens a given book for reading.
 */

class CatalogBookReadController(
  val activity: AppCompatActivity,
  val account: AccountType,
  val id: BookID,
  val entry: FeedEntryOPDS) : OnClickListener {

  companion object {
    private val LOG = LoggerFactory.getLogger(CatalogBookReadController::class.java)
  }

  override fun onClick(@Nullable v: View) {
    val database = this.account.bookDatabase()
    val entry = database.entry(this.id)
    val format = entry.book.findPreferredFormat()

    if (format == null) {
      throw UnimplementedCodeException()
    }

    return when (format) {
      is BookFormatEPUB ->
        launchEPUBReader(entry.book, format)
      is BookFormatAudioBook ->
        launchAudioBookPlayer(entry.book, format)
      is BookFormatPDF ->
        launchPDFReader(entry.book, format)
    }
  }

  private fun launchPDFReader(book: Book, format: BookFormatPDF) {
    ErrorDialogUtilities.showError(
      this.activity,
      LOG,
      "PDF support is not yet implemented",
      null)
  }

  private fun launchEPUBReader(book: Book, format: BookFormatEPUB) {
    if (format.isDownloaded) {
      ReaderActivity.startActivity(this.activity, this.id, format.file, FeedEntryOPDS(book.entry))
    } else {
      ErrorDialogUtilities.showError(
        this.activity,
        LOG,
        "Bug: book claimed to be downloaded but no book file exists in storage",
        null)
    }
  }

  private fun launchAudioBookPlayer(book: Book, format: BookFormatAudioBook) {
    val manifest = format.manifest
    if (manifest != null) {
      AudioBookPlayerActivity.startActivity(
        from = this.activity,
        parameters = AudioBookPlayerParameters(
          manifestFile = manifest.manifestFile,
          manifestURI = manifest.manifestURI,
          opdsEntry = this.entry.feedEntry,
          theme = Simplified.getCurrentTheme().themeWithActionBar,
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
