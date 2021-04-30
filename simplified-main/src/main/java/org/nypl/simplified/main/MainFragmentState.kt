package org.nypl.simplified.main

sealed class MainFragmentState {

  /**
   * Nothing to remember.
   */

  object EmptyState : MainFragmentState()

  /**
   * Catalog required the patron to be logged in.
   */

  object CatalogWaitingForLogin : MainFragmentState()
}
