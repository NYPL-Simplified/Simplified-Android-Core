package org.nypl.simplified.ui.settings

import org.nypl.simplified.accounts.api.AccountID
import java.io.Serializable

/**
 * Parameters for the account screen.
 */

data class SettingsFragmentAccountParameters(

  /**
   * The account that will be displayed.
   */

  val accountId: AccountID
) : Serializable
