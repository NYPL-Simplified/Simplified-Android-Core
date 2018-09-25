package org.nypl.simplified.app.catalog

import android.app.Activity
import android.content.res.Resources
import android.support.v4.content.ContextCompat
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.R
import org.nypl.simplified.app.R.string.catalog_accessibility_book_download
import org.nypl.simplified.app.R.string.catalog_accessibility_book_read
import org.nypl.simplified.app.R.string.catalog_book_download
import org.nypl.simplified.app.R.string.catalog_book_read
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.ThemeMatcher
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.BooksType
import org.nypl.simplified.books.core.DeviceActivationListenerType
import org.nypl.simplified.books.core.FeedEntryOPDS

/**
 * A button that opens a given book for reading.
 */

class CatalogBookReadButton(
  activity: Activity,
  bookID: BookID,
  feedEntry: FeedEntryOPDS,
  books: BooksType) : CatalogLeftPaddedButton(activity), CatalogBookButtonType {

  init {
    this.textView.textSize = 12.0f
    this.setBackgroundResource(R.drawable.simplified_button)

    val resID = ThemeMatcher.color(Simplified.getCurrentAccount().mainColor)
    val mainColor = ContextCompat.getColor(this.context, resID)
    this.textView.setTextColor(mainColor)

    if (bookIsReadable(books, bookID)) {
      setButtonForRead(activity, bookID, feedEntry, activity.resources)
    } else {
      setButtonForDownload(bookID, feedEntry, books, activity.resources)
    }
  }

  private fun bookIsReadable(
    books: BooksType,
    bookID: BookID): Boolean {

    val snapshotOpt =
      books.bookGetDatabase().databaseGetEntrySnapshot(bookID)

    if (snapshotOpt is Some<BookDatabaseEntrySnapshot>) {
      val snapshot = snapshotOpt.get()
      return snapshot.isDownloaded
    }

    return false
  }

  private fun setButtonForDownload(
    bookID: BookID,
    feedEntry: FeedEntryOPDS,
    books: BooksType,
    resources: Resources) {

    this.textView.text = resources.getString(catalog_book_download)
    this.textView.contentDescription = resources.getString(catalog_accessibility_book_download)

    this.setOnClickListener { view ->
      val listener = object : DeviceActivationListenerType {
        override fun onDeviceActivationFailure(message: String) {}
        override fun onDeviceActivationSuccess() {}
      }
      books.accountActivateDeviceAndFulFillBook(bookID, feedEntry.feedEntry.licensor, listener)
    }
  }

  private fun setButtonForRead(
    activity: Activity,
    bookID: BookID,
    feedEntry: FeedEntryOPDS,
    resources: Resources) {

    this.textView.text = resources.getString(catalog_book_read)
    this.textView.contentDescription = resources.getString(catalog_accessibility_book_read)
    this.setOnClickListener(CatalogBookRead(activity, bookID, feedEntry))
  }
}
