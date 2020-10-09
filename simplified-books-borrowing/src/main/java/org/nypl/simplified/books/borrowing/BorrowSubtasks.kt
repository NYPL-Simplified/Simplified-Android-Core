package org.nypl.simplified.books.borrowing

import org.nypl.simplified.books.borrowing.internal.BorrowSubtaskDirectory
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType

/**
 * Access to the available borrowing subtasks.
 */

object BorrowSubtasks {

  private val directory =
    BorrowSubtaskDirectory()

  /**
   * @return The default subtask directory
   */

  fun directory(): BorrowSubtaskDirectoryType =
    this.directory
}
