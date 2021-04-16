package org.nypl.simplified.tests.mocking

import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskDirectoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType

class MockBorrowSubtaskDirectory : BorrowSubtaskDirectoryType {
  override var subtasks: List<BorrowSubtaskFactoryType> =
    listOf()
}
