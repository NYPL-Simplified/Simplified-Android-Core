package org.nypl.simplified.books.book_registry

import org.nypl.simplified.taskrecorder.api.TaskStep

/**
 * The result of revoking the loan of a book.
 */

data class BookStatusRevokeResult(
  val steps: List<TaskStep<BookStatusRevokeErrorDetails>>) {

  /**
   * `true` if the last step in the task failed.
   */

  val failed: Boolean
    get() = this.steps.lastOrNull()?.failed ?: false

}
