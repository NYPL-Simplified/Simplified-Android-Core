package org.nypl.simplified.ui.host

/**
 * The base type of navigation controllers. Different parts of the application
 * are expected to extend this interface with methods to navigate through the
 * various application screens.
 */

interface HostNavigationControllerType {

  /**
   * A screen wants to pop the current screen from the stack.
   */

  fun popBackStack(): Boolean

  /**
   * @return The current size of the backstack
   */

  fun backStackSize(): Int
}
