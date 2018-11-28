package org.nypl.simplified.app.catalog

import android.app.Activity
import android.support.v4.content.ContextCompat
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.ThemeMatcher
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.BooksType

/**
 * A button for revoking loans or holds.
 */

class CatalogBookRevokeButton(
  private val activity: Activity,
  private val bookID: BookID,
  private val revokeType: CatalogBookRevokeType,
  private val books: BooksType) : CatalogLeftPaddedButton(activity), CatalogBookButtonType {

  init {
    if (this.isRevocable(
        this.books, this.books.bookGetDatabase().databaseGetEntrySnapshot(this.bookID))) {

      this.textView.textSize = 12.0f
      this.setBackgroundResource(R.drawable.simplified_button)
      val resID = ThemeMatcher.color(Simplified.getCurrentAccount().mainColor)
      val mainColor = ContextCompat.getColor(this.context, resID)
      this.textView.setTextColor(mainColor)

      when (this.revokeType) {
        CatalogBookRevokeType.REVOKE_LOAN -> {
          this.textView.text =
            this.activity.resources.getString(R.string.catalog_book_revoke_loan)
          this.textView.contentDescription =
            this.activity.resources.getString(R.string.catalog_accessibility_book_revoke_loan)
        }
        CatalogBookRevokeType.REVOKE_HOLD -> {
          this.textView.text =
            this.activity.resources.getString(R.string.catalog_book_revoke_hold)
          this.textView.contentDescription =
            this.activity.resources.getString(R.string.catalog_accessibility_book_revoke_hold)
        }
      }

      this.setOnClickListener { view ->
        val dialog = CatalogBookRevokeDialog.newDialog(this.revokeType)
        dialog.setOnConfirmListener {
          val app = Simplified.getCatalogAppServices()
          val books = app.books
          books.bookRevoke(this.bookID, Simplified.getCurrentAccount().needsAuth())
        }
        val fm = this.activity.fragmentManager
        dialog.show(fm, "revoke-confirm")
      }
    } else {
      super.removeView(this)
    }
  }

  /**
   * XXX: The book controller should be making this determination. It's not the job of the
   * views to do this kind of work.
   */

  private fun isRevocable(
    books: BooksType,
    snapshotOption: OptionType<BookDatabaseEntrySnapshot>): Boolean {

    if (books.accountIsDeviceActive()) {
      return true
    }

    if (snapshotOption is Some<BookDatabaseEntrySnapshot>) {
      val snapshot = snapshotOption.get()

      for (format in snapshot.formats) {
        return when (format) {
          is BookDatabaseEntryFormatSnapshotEPUB -> format.adobeRights.isNone
          is BookDatabaseEntryFormatSnapshotAudioBook -> true
        }
      }
    }

    return false
  }
}
