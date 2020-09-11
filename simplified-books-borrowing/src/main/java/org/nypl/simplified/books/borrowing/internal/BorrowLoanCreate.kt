package org.nypl.simplified.books.borrowing.internal

import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType

/**
 * A task that creates an OPDS loan by hitting an acquisition URI.
 */

class BorrowLoanCreate private constructor() : BorrowSubtaskType {

  companion object : BorrowSubtaskFactoryType {
    override val name: String
      get() = "Create OPDS Loan"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowLoanCreate()
    }
  }

  override fun execute(context: BorrowContextType) {
    throw UnimplementedCodeException()
  }
}
