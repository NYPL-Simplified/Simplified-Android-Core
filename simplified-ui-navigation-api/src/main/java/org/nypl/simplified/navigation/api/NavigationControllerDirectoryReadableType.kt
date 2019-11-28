package org.nypl.simplified.navigation.api

/**
 * The navigation controller directory interface.
 */

interface NavigationControllerDirectoryReadableType {

  /**
   * Retrieve a navigation controller previously registered using [updateNavigationController].
   */

  fun <T : NavigationControllerType> navigationController(
    navigationClass: Class<T>
  ): T
}
