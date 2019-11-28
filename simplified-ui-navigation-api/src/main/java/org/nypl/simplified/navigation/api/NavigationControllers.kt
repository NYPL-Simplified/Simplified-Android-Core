package org.nypl.simplified.navigation.api

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders

/**
 * Functions to obtain access to navigation controllers.
 */

object NavigationControllers {

  /**
   * Obtain access to the navigation controller directory associated with the given activity.
   */

  fun findDirectory(activity: FragmentActivity): NavigationControllerDirectoryType {
    return ViewModelProviders.of(activity)
      .get(NavigationControllerViewModel::class.java)
  }

  /**
   * Obtain access to the navigation controller associated with the given activity.
   */

  fun <T : NavigationControllerType> find(
    activity: FragmentActivity,
    interfaceType: Class<T>
  ): T {
    return this.findDirectory(activity)
      .navigationController(interfaceType)
  }

}