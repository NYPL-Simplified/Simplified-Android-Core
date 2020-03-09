package org.nypl.simplified.ui.catalog

import org.nypl.simplified.accounts.api.AccountID
import java.io.Serializable

/**
 * Parameters for a login dialog.
 */

data class CatalogFragmentLoginDialogParameters(

  /**
   * The account to which the login dialog belongs.
   */

  val accountId: AccountID

) : Serializable
