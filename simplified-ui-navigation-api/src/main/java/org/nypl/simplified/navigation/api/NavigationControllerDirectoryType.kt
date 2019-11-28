package org.nypl.simplified.navigation.api

/**
 * The navigation controller directory interface.
 */

interface NavigationControllerDirectoryType : NavigationControllerDirectoryReadableType {

  /**
   * Register a navigation controller, or replace an existing one, that supports the navigation
   * interface specified by [navigationInterface].
   *
   * In practice, this method is called every time the application's single activity is (re)created;
   * navigation controller implementations tend to be tied to the lifetimes of activities as they
   * tend to manipulate fragments within those activities.
   */

  fun <T : NavigationControllerType> updateNavigationController(
    navigationInterface: Class<T>,
    navigationInstance: T
  )

  /**
   * Deregister a navigation controller that was previously registered with [updateNavigationController].
   * Has no effect if no controller exists.
   *
   * In practice, this method is called every time the application's single activity is destroyed;
   * navigation controller implementations tend to be tied to the lifetimes of activities as they
   * tend to manipulate fragments within those activities.
   */

  fun <T : NavigationControllerType> removeNavigationController(
    navigationClass: Class<T>
  )
}
