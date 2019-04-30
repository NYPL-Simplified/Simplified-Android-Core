package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import android.text.TextUtils
import android.util.TypedValue
import org.nypl.simplified.app.R
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.controller.BooksControllerType
import java.util.Objects

/**
 * A button for revoking loans or holds.
 */

class CatalogBookRevokeButton(
  activity: AppCompatActivity,
  booksController: BooksControllerType,
  account: AccountType,
  bookID: BookID,
  revokeType: CatalogBookRevokeType) 
  : AppCompatButton(activity), CatalogBookButtonType {

  init {
    val resources =
      Objects.requireNonNull(activity.resources)

    when (revokeType) {
      CatalogBookRevokeType.REVOKE_LOAN -> {
        this.text =
          Objects.requireNonNull(resources.getString(R.string.catalog_book_revoke_loan))
        this.contentDescription =
          Objects.requireNonNull(resources.getString(R.string.catalog_accessibility_book_revoke_loan))
      }
      CatalogBookRevokeType.REVOKE_HOLD -> {
        this.text =
          Objects.requireNonNull(resources.getString(R.string.catalog_book_revoke_hold))
        this.contentDescription =
          Objects.requireNonNull(resources.getString(R.string.catalog_accessibility_book_revoke_hold))
      }
    }

    this.ellipsize = TextUtils.TruncateAt.END
    this.maxLines = 1
    this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f)

    this.setOnClickListener { view ->
      val d = CatalogBookRevokeDialog.newDialog(
        revokeType) { booksController.bookRevoke(account, bookID) }

      val fm = activity.supportFragmentManager
      d.show(fm, "revoke-confirm")
    }
  }
}
