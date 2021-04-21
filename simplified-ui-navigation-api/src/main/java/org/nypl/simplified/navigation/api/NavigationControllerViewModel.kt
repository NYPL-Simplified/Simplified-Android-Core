package org.nypl.simplified.navigation.api

import androidx.lifecycle.ViewModel
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

abstract class NavigationControllerViewModel : ViewModel(), NavigationControllerDirectoryType {

  private val logger =
    LoggerFactory.getLogger(NavigationControllerViewModel::class.java)

  private val navigationControllers =
    ConcurrentHashMap<Class<*>, NavigationControllerType>()

  override fun <T : NavigationControllerType> navigationControllerIfAvailable(
    navigationClass: Class<T>
  ): T? {
    return (this.navigationControllers[navigationClass] as T?)
  }

  protected fun <T : NavigationControllerType> updateNavigationController(
    navigationInterface: Class<T>,
    navigationInstance: T
  ) {
    this.logger.debug("updating navigation controller: {} ({})", navigationInterface, navigationInstance)
    this.navigationControllers[navigationInterface] = navigationInstance
  }
}
