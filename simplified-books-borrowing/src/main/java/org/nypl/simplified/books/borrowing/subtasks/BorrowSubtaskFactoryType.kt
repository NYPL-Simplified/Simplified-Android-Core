package org.nypl.simplified.books.borrowing.subtasks

/**
 * A factory interface for subtasks.
 */

interface BorrowSubtaskFactoryType {

  /**
   * @return The name of the subtask
   */

  val name: String

  /**
   * Create a new subtask. A subtask may be used at most once.
   *
   * @return A new subtask instance
   */

  fun createSubtask(): BorrowSubtaskType
}
