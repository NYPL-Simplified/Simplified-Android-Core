package org.nypl.simplified.books.borrowing.subtasks

import org.nypl.simplified.books.borrowing.BorrowContextType

/**
 * A created subtask. A subtask may be used at most once.
 */

interface BorrowSubtaskType {

  /**
   * Execute the subtask.
   */

  @Throws(BorrowSubtaskException::class)
  fun execute(context: BorrowContextType)
}
