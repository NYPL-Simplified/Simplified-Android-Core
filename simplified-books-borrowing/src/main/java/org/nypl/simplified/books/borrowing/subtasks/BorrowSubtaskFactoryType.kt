package org.nypl.simplified.books.borrowing.subtasks

import one.irradia.mime.api.MIMEType
import org.nypl.simplified.accounts.api.AccountReadableType
import java.net.URI

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

  /**
   * @return `true` if the factory produces subtasks applicable for the given MIME type and URI
   */

  fun isApplicableFor(
    type: MIMEType,
    target: URI?,
    account: AccountReadableType?
  ): Boolean
}
