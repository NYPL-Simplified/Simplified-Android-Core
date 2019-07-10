package org.nypl.simplified.books.book_registry

import org.nypl.simplified.taskrecorder.api.TaskStep

/**
 * The result of borrowing a book.
 */

data class BookStatusDownloadResult(
  val steps: List<TaskStep<BookStatusDownloadErrorDetails>>) {

  /**
   * `true` if the last step in the task failed.
   */

  val failed: Boolean
    get() = this.steps.lastOrNull()?.failed ?: false

}
