package org.nypl.simplified.books.controller

import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.reader.ReaderBookmark

/**
 * Controller functions related to bookmarks.
 */

interface BookmarksControllerType {

  /**
   * A set of bookmarks for a book.
   */

  data class Bookmarks(
    val lastRead: ReaderBookmark?,
    val bookmarks: List<ReaderBookmark>)

  /**
   * Save a set of bookmarks for the given book.
   *
   * @param account   The account to which the book belongs
   * @param id        The ID of the book
   * @param lastRead  The last read location
   * @param bookmarks The list of bookmarks
   * @return A future representing the operation in progress
   */

  fun bookmarksSave(
    account: AccountType,
    id: BookID,
    bookmarks: Bookmarks): FluentFuture<Bookmarks> {
    return this.bookmarksUpdate(account, id) { bookmarks }
  }

  /**
   * Update the set of bookmarks for the given book.
   *
   * @param account   The account to which the book belongs
   * @param id        The ID of the book
   * @param lastRead  The last read location
   * @param bookmarks A function that will be applied to the current bookmarks
   * @return A future representing the operation in progress
   */

  fun bookmarksUpdate(
    account: AccountType,
    id: BookID,
    bookmarks: (Bookmarks) -> Bookmarks): FluentFuture<Bookmarks>

  /**
   * Load bookmarks for the given book.
   *
   * @param account The account to which the book belongs
   * @param id      The ID of the book
   * @return A future representing the operation in progress
   */

  fun bookmarksLoad(
    account: AccountType,
    id: BookID): FluentFuture<Bookmarks>

}
