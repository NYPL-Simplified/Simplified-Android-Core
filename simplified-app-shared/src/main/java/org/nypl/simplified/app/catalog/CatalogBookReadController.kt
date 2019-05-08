package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.OnClickListener
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jnull.Nullable
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.player.AudioBookPlayerActivity
import org.nypl.simplified.app.player.AudioBookPlayerParameters
import org.nypl.simplified.app.reader.ReaderActivity
import org.nypl.simplified.app.utilities.ErrorDialogUtilities
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat.BookFormatAudioBook
import org.nypl.simplified.books.api.BookFormat.BookFormatEPUB
import org.nypl.simplified.books.api.BookFormat.BookFormatPDF
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.slf4j.LoggerFactory

/**
 * A controller that opens a given book for reading.
 */

class CatalogBookReadController(
  val activity: AppCompatActivity,
  val analytics: AnalyticsType,
  val profile: ProfileReadableType,
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
        this.launchEPUBReader(entry.book, format)
      is BookFormatAudioBook ->
        this.launchAudioBookPlayer(entry.book, format)
      is BookFormatPDF ->
        this.launchPDFReader(entry.book, format)
    }
  }

  private fun launchPDFReader(book: Book, format: BookFormatPDF) {
    ErrorDialogUtilities.showError(
      this.activity,
      LOG,
      "PDF support is not yet implemented",
      null)
  }

  private fun <T> orElseNull(x: OptionType<T>): T? {
    return if (x is Some<T>) {
      x.get()
    } else {
      null
    }
  }

  private fun launchEPUBReader(book: Book, format: BookFormatEPUB) {
    if (format.isDownloaded) {
      this.sendAnalytics(book)
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
      this.sendAnalytics(book)
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

  private fun sendAnalytics(book: Book) {
    this.analytics.publishEvent(
      AnalyticsEvent.BookOpened(
        credentials = this.account.loginState().credentials,
        profileUUID = this.profile.id().uuid,
        profileDisplayName = this.profile.displayName(),
        accountProvider = this.account.provider().id(),
        accountUUID = this.account.id().uuid,
        bookOPDSId = book.entry.id,
        bookTitle = book.entry.title,
        targetURI = this.orElseNull(book.entry.analytics)))
  }
}
