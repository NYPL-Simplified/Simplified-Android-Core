package org.nypl.simplified.books.borrowing

import org.joda.time.Instant
import org.librarysimplified.http.api.LSHTTPClientType
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import org.nypl.simplified.profiles.api.ProfileReadableType
import java.io.File

/**
 * The services required by borrow tasks.
 */

data class BorrowRequirements(
  val bookFormatSupport: BookFormatSupportType,
  val bookRegistry: BookRegistryType,
  val clock: () -> Instant,
  val httpClient: LSHTTPClientType,
  val profile: ProfileReadableType,
  val subtasks: BorrowSubtaskDirectoryType,
  val temporaryDirectory: File
)
