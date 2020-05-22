package org.nypl.simplified.ui.catalog

import org.nypl.simplified.accounts.api.AccountID

/**
 * The ownership of a given feed.
 */

sealed class CatalogFeedOwnership {

  /**
   * The feed is owned by the given account.
   */

  data class OwnedByAccount(
    val accountId: AccountID
  ) : CatalogFeedOwnership()

  /**
   * The feed consists of entries from the given set of accounts, and has no single owner.
   */

  object CollectedFromAccounts : CatalogFeedOwnership()
}
