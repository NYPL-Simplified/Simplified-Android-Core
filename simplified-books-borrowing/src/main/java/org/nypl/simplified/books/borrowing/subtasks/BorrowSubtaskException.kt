package org.nypl.simplified.books.borrowing.subtasks

/**
 * The type of exceptions that can be raised by subtasks.
 */

sealed class BorrowSubtaskException : Exception() {

  /**
   * The subtask was cancelled and therefore none of the subtasks that follow will be executed,
   * but the borrow task as a whole won't succeed or fail (it'll be cancelled!).
   */

  class BorrowSubtaskCancelled : BorrowSubtaskException()

  /**
   * The subtask was halted early and none of the subtasks that follow it should run. The borrow
   * task as a whole should be considered to have succeeded.
   */

  class BorrowSubtaskHaltedEarly : BorrowSubtaskException()

  /**
   * The subtask failed and none of the subtasks that follow it should run. The borrow
   * task as a whole should be considered to have failed.
   */

  class BorrowSubtaskFailed : BorrowSubtaskException()
}
