package org.nypl.simplified.ui.catalog.saml20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.controller.api.BooksControllerType
import java.io.File

/**
 * A factory for SAML 2.0 view state.
 */

class CatalogSAML20ViewModelFactory(
  private val booksController: BooksControllerType,
  private val bookRegistry: BookRegistryType,
  private val account: AccountType,
  private val bookID: BookID,
  private val webViewDataDir: File
) : ViewModelProvider.Factory {

  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass == CatalogSAML20ViewModel::class.java) {
      return CatalogSAML20ViewModel(
        booksController = this.booksController,
        bookRegistry = this.bookRegistry,
        account = this.account,
        bookID = this.bookID,
        webViewDataDir = this.webViewDataDir
      ) as T
    }
    throw IllegalStateException("Can't create values of $modelClass")
  }
}
