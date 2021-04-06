package org.nypl.simplified.main

import androidx.fragment.app.FragmentManager
import org.nypl.simplified.navigation.api.NavigationControllerType
import org.slf4j.LoggerFactory

internal open class BaseNavigationController(
  protected val fragmentManager: FragmentManager
) : NavigationControllerType {

  private val logger =
    LoggerFactory.getLogger(BaseNavigationController::class.java)

  override fun popBackStack(): Boolean {
    this.logger.debug("popBackStack")
    this.fragmentManager.popBackStack()
    return this.backStackSize() > 0
  }

  override fun popToRoot(): Boolean {
    this.logger.debug("popToRoot")
    if (this.backStackSize() == 1) {
      return false
    }
    this.fragmentManager.popBackStack(
      null, FragmentManager.POP_BACK_STACK_INCLUSIVE
    )
    return true
  }

  override fun backStackSize(): Int {
    this.logger.debug("backStackSize")
    return this.fragmentManager.backStackEntryCount
  }
}
