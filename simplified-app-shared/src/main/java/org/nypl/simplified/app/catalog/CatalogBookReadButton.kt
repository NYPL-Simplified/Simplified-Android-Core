package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import org.nypl.simplified.app.ApplicationColorScheme
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS

/**
 * A button that opens a given book for reading.
 */

class CatalogBookReadButton(
  val activity: AppCompatActivity,
  val account: AccountType,
  val bookID: BookID,
  val entry: FeedEntryOPDS,
  val colorScheme: ApplicationColorScheme)
  : AppCompatButton(activity), CatalogBookButtonType {

  init {
    this.setOnClickListener(CatalogBookReadController(
      this.activity,
      this.account,
      this.bookID,
      this.entry,
      this.colorScheme))
  }
}
