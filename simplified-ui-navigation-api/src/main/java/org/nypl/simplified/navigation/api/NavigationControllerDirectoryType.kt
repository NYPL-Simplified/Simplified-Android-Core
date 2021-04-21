package org.nypl.simplified.navigation.api

/**
 * The navigation controller directory interface.
 */

interface NavigationControllerDirectoryType {

  /**
   * Retrieve a navigation controller previously registered using [updateNavigationController], or
   * null if there isn't one.
   */

  fun <T : NavigationControllerType> navigationControllerIfAvailable(
    navigationClass: Class<T>
  ): T?
}
