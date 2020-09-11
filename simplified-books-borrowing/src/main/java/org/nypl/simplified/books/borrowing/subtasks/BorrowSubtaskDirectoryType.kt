package org.nypl.simplified.books.borrowing.subtasks

import org.nypl.simplified.opds.core.OPDSAcquisitionPathElement

/**
 * A directory of subtasks.
 */

interface BorrowSubtaskDirectoryType {

  /**
   * Find a suitable subtask for the given acquisition path element.
   */

  fun findSubtaskFor(
    pathElement: OPDSAcquisitionPathElement
  ): BorrowSubtaskFactoryType?
}
