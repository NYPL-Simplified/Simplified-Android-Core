package org.nypl.simplified.books.controller

import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.profiles.api.ProfileType
import org.slf4j.LoggerFactory

class ProfileDataLoadTask(
  private val profile: ProfileType,
  private val bookRegistry: BookRegistryType
) : Runnable {

  private val logger = LoggerFactory.getLogger(ProfileDataLoadTask::class.java)

  override fun run() {
    try {
      this.logger.debug("load: profile {}", this.profile.displayName)
      this.logger.debug("clearing the book registry")
      this.bookRegistry.clear()

      val accounts = this.profile.accounts()
      for (account in accounts.values) {
        this.logger.debug("load: profile {} / account {}", this.profile.displayName, account.id)
        val books = account.bookDatabase
        val bookIDs = books.books()
        this.logger.debug("load: updating {} books", bookIDs.size)
        for (bookId in bookIDs) {
          try {
            val entry = books.entry(bookId)
            val book = entry.book
            val status = BookStatus.fromBook(book)
            this.bookRegistry.update(BookWithStatus(book, status))
          } catch (e: BookDatabaseException) {
            this.logger.error("load: could not load book {}: ", bookId, e)
          }
        }
      }
    } finally {
      this.logger.debug("load: profile {} loaded", this.profile.displayName)
    }
  }
}
