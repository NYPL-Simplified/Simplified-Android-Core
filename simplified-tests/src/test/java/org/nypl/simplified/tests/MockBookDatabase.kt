package org.nypl.simplified.tests

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.slf4j.LoggerFactory
import java.util.SortedSet

class MockBookDatabase(
  val owner: AccountID
) : BookDatabaseType {

  private val logger =
    LoggerFactory.getLogger(MockBookDatabase::class.java)

  var deleted = false
  val entries = mutableMapOf<BookID, MockBookDatabaseEntry>()

  override fun owner(): AccountID {
    return this.owner
  }

  override fun books(): SortedSet<BookID> {
    return this.entries.keys.toSortedSet()
  }

  override fun delete() {
    this.deleted = true
  }

  override fun createOrUpdate(
    id: BookID,
    entry: OPDSAcquisitionFeedEntry
  ): BookDatabaseEntryType {
    this.logger.debug("createOrUpdate: [{}]", id)

    val existing = this.entries[id]
    if (existing != null) {
      this.logger.debug("createOrUpdate: [{}]: rewriting existing", id)
      existing.writeOPDSEntry(entry)
      return existing
    }

    this.logger.debug("createOrUpdate: [{}]: creating new entry", id)

    val newEntry =
      MockBookDatabaseEntry(
        Book(
          id = id,
          account = this.owner,
          cover = null,
          thumbnail = null,
          entry = entry,
          formats = listOf()
        )
      )

    newEntry.writeOPDSEntry(entry)
    this.entries[id] = newEntry
    return newEntry
  }

  override fun entry(
    id: BookID
  ): BookDatabaseEntryType {
    return this.entries[id] ?: throw IllegalStateException("No such database entry")
  }
}
