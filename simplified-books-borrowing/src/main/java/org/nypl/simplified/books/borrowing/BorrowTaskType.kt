package org.nypl.simplified.books.borrowing

import org.nypl.simplified.taskrecorder.api.TaskResult

/**
 * A borrow task.
 */

interface BorrowTaskType {

  /**
   * Execute the borrow task.
   */

  fun execute(): TaskResult<*>

  /**
   * Mark the borrow task as being cancelled.
   */

  fun cancel()
}
