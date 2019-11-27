package org.nypl.simplified.ui.host

import androidx.lifecycle.ViewModel
import org.librarysimplified.services.api.ServiceDirectoryType
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * The default host view model, storing services and navigation controllers.
 */

class HostViewModel(
  override val services: ServiceDirectoryType
) : ViewModel(), HostViewModelType {

  private val logger =
    LoggerFactory.getLogger(HostViewModel::class.java)

  private val navigationControllers =
    ConcurrentHashMap<Class<*>, HostNavigationControllerType>()

  override fun <T : HostNavigationControllerType> navigationController(navigationClass: Class<T>): T {
    return (this.navigationControllers[navigationClass] as T?)
      ?: throw IllegalArgumentException(
        "No navigation controllers of type $navigationClass are available")
  }

  override fun <T : HostNavigationControllerType> removeNavigationController(
    navigationClass: Class<T>
  ) {
    this.logger.debug("removing navigation controller: {}", navigationClass)
    this.navigationControllers.remove(navigationClass)
  }

  override fun <T : HostNavigationControllerType> updateNavigationController(
    navigationInterface: Class<T>,
    navigationInstance: T
  ) {
    this.logger.debug("updating navigation controller: {} ({})", navigationInterface, navigationInstance)
    this.navigationControllers[navigationInterface] = navigationInstance
  }
}