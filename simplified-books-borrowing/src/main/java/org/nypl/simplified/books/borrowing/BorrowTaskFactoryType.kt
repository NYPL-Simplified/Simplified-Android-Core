package org.nypl.simplified.books.borrowing

/**
 * A factory interface for borrow tasks.
 */

interface BorrowTaskFactoryType {

  /**
   * Create a new borrow task. The borrow task may be used at most once.
   */

  fun createBorrowTask(
    requirements: BorrowRequirements,
    request: BorrowRequest
  ): BorrowTaskType
}
