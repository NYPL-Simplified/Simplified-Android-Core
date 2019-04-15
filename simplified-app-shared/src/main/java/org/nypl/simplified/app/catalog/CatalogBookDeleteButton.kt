package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
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

    this.setOnClickListener { view ->
      val d = CatalogBookDeleteDialog.newDialog()
      d.setOnConfirmListener { booksController.bookDelete(account, bookID) }
      val fm = activity.supportFragmentManager
      d.show(fm, "delete-confirm")
    }
  }
}
