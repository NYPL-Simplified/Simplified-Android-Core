package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import org.nypl.simplified.app.ApplicationColorScheme
import org.nypl.simplified.app.R
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
  val colorScheme: ApplicationColorScheme) : CatalogLeftPaddedButton(activity), CatalogBookButtonType {

  init {
    this.textView.textSize = 12.0f
    this.setBackgroundResource(R.drawable.simplified_button)
    this.setOnClickListener(CatalogBookReadController(
      this.activity,
      this.account,
      this.bookID,
      this.entry,
      this.colorScheme))
  }
}
