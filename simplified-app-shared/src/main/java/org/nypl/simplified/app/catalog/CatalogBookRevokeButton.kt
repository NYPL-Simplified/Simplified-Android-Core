package org.nypl.simplified.app.catalog

import android.text.TextUtils
import android.util.TypedValue
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.app.R
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.controller.api.BooksControllerType
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
      val builder = AlertDialog.Builder(activity)

      when (revokeType) {
        CatalogBookRevokeType.REVOKE_LOAN -> {
          builder.setMessage(R.string.catalog_book_revoke_loan_confirm)
          builder.setPositiveButton(R.string.catalog_book_revoke_loan) { _, _ ->
            booksController.bookRevoke(account, bookID)
          }
        }
        CatalogBookRevokeType.REVOKE_HOLD -> {
          builder.setMessage(R.string.catalog_book_revoke_hold_confirm)
          builder.setPositiveButton(R.string.catalog_book_revoke_hold) { _, _ ->
            booksController.bookRevoke(account, bookID)
          }
        }
      }

      builder.create().show()
    }
  }
}
