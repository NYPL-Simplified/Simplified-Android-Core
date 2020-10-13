package org.nypl.simplified.books.borrowing

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.profiles.api.ProfileID

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
   * The profile to which the account belongs.
   */

  abstract val profile: ProfileID

  /**
   * Start borrowing a book.
   */

  data class Start(
    override val accountId: AccountID,
    override val profile: ProfileID,
    override val opdsAcquisitionFeedEntry: OPDSAcquisitionFeedEntry
  ) : BorrowRequest()
}
