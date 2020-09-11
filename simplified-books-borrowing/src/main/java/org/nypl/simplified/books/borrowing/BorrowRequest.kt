package org.nypl.simplified.books.borrowing

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

/**
 * The type of requests to borrow books.
 */

sealed class BorrowRequest {

  /**
   * The OPDS feed entry that will be borrowed.
   */

  abstract val opdsAcquisitionFeedEntry: OPDSAcquisitionFeedEntry

  /**
   * The ID of the account.
   */

  abstract val accountId: AccountID

  /**
   * Start borrowing a book.
   */

  data class Start(
    override val accountId: AccountID,
    override val opdsAcquisitionFeedEntry: OPDSAcquisitionFeedEntry
  ) : BorrowRequest()
}
