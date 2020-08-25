package org.nypl.simplified.books.book_database

import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle

class BookDRMInformationHandleNone : BookDRMInformationHandle.NoneHandle() {
  override val info: BookDRMInformation.None =
    BookDRMInformation.None
}
