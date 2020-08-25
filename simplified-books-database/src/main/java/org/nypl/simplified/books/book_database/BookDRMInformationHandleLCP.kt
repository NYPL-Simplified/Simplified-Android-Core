package org.nypl.simplified.books.book_database

import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle

class BookDRMInformationHandleLCP : BookDRMInformationHandle.LCPHandle() {
  override val info: BookDRMInformation.LCP =
    BookDRMInformation.LCP()
}
