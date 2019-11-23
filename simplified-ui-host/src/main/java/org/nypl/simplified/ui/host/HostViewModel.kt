package org.nypl.simplified.ui.host

import androidx.lifecycle.ViewModel
import org.librarysimplified.services.api.ServiceDirectoryType
import java.util.concurrent.ConcurrentHashMap

class HostViewModel(
  override val services: ServiceDirectoryType
) : ViewModel(), HostViewModelType {

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
    this.navigationControllers.remove(navigationClass)
  }

  override fun <T : HostNavigationControllerType> updateNavigationController(
    navigationInterface: Class<T>,
    navigationInstance: T
  ) {
    this.navigationControllers[navigationInterface] = navigationInstance
  }
}