package org.nypl.simplified.ui.accounts

import java.io.Serializable

/**
 * Parameters for the accounts screen.
 */

data class AccountListFragmentParameters(

  /**
   * If set to `true`, then fragment won't navigate after a new account is added
   */

  val addMultipleAccounts: Boolean

) : Serializable
