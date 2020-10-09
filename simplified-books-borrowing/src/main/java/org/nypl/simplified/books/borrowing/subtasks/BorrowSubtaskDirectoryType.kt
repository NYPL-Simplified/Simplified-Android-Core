package org.nypl.simplified.books.borrowing.subtasks

import one.irradia.mime.api.MIMEType
import java.net.URI

/**
 * A directory of subtasks.
 */

interface BorrowSubtaskDirectoryType {

  /**
   * @return All of the available subtasks in the directory
   */

  val subtasks: List<BorrowSubtaskFactoryType>

  /**
   * Find a suitable subtask for the given URI and MIME type.
   */

  fun findSubtaskFor(
    mimeType: MIMEType,
    target: URI?
  ): BorrowSubtaskFactoryType? {
    return this.subtasks.firstOrNull { factory -> factory.isApplicableFor(mimeType, target) }
  }
}
