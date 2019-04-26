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
 * A button for deleting books.
 */

class CatalogBookDeleteButton(
  activity: AppCompatActivity,
  booksController: BooksControllerType,
  account: AccountType,
  bookID: BookID)
  : AppCompatButton(activity), CatalogBookButtonType {

  init {
    val resources =
      Objects.requireNonNull(activity.resources)
    this.text =
      Objects.requireNonNull(resources.getString(R.string.catalog_book_delete))
    this.contentDescription =
      Objects.requireNonNull(resources.getString(R.string.catalog_accessibility_book_delete))

    this.ellipsize = TextUtils.TruncateAt.END
    this.maxLines = 1
    this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f)

    this.setOnClickListener { view ->
      val d = CatalogBookDeleteDialog.newDialog()
      d.setOnConfirmListener { booksController.bookDelete(account, bookID) }
      val fm = activity.supportFragmentManager
      d.show(fm, "delete-confirm")
    }
  }
}
