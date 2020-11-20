package org.nypl.simplified.ui.accounts

import java.io.Serializable

/**
 * Parameters for the accounts screen.
 */

data class AccountListFragmentParameters(

  /**
   * If set to `true`, then show the library registry menu in the toolbar.
   */

  val shouldShowLibraryRegistryMenu: Boolean

) : Serializable
