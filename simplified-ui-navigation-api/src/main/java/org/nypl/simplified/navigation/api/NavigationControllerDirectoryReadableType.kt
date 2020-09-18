package org.nypl.simplified.navigation.api

/**
 * The navigation controller directory interface.
 */

interface NavigationControllerDirectoryReadableType {

  /**
   * Retrieve a navigation controller previously registered using [updateNavigationController], or
   * null if there isn't one.
   */

  fun <T : NavigationControllerType> navigationControllerIfAvailable(
    navigationClass: Class<T>
  ): T?

  /**
   * Retrieve a navigation controller previously registered using [updateNavigationController].
   */

  fun <T : NavigationControllerType> navigationController(
    navigationClass: Class<T>
  ): T {
    return this.navigationControllerIfAvailable(navigationClass)
      ?: throw IllegalArgumentException(
        "No navigation controllers of type $navigationClass are available"
      )
  }
}
