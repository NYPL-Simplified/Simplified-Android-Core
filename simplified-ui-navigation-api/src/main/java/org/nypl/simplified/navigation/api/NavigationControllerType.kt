package org.nypl.simplified.navigation.api

/**
 * The base type of navigation controllers. Different parts of the application
 * are expected to extend this interface with methods to navigate through the
 * various application screens.
 */

interface NavigationControllerType {

  /**
   * A screen wants to pop the current screen from the stack.
   */

  fun popBackStack(): Boolean

  /**
   * A screen wants to pop to the root of the current navigation stack.
   */

  fun popToRoot(): Boolean

  /**
   * @return The current size of the backstack
   */

  fun backStackSize(): Int
}
