package org.nypl.simplified.tests

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.util.SortedSet

class MockBookDatabase(
  val owner: AccountID
) : BookDatabaseType {

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
    val existing = this.entries[id]
    if (existing != null) {
      existing.writeOPDSEntry(entry)
      return existing
    }

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
    return newEntry
  }

  override fun entry(
    id: BookID
  ): BookDatabaseEntryType {
    return this.entries[id] ?: throw IllegalStateException("No such database entry")
  }
}
