package org.nypl.simplified.ui.navigation.tabs

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pandora.bottomnavigator.BottomNavigator
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory

/**
 * A tabbed navigation system based on Pandora's BottomNavigator.
 *
 * @see BottomNavigator
 * @see BottomNavigationView
 */

class TabbedNavigator private constructor (private val navigator: BottomNavigator) {

  companion object {

    fun create(
      fragment: Fragment,
      @IdRes fragmentContainerId: Int,
      navigationView: BottomNavigationView,
      accountProviders: AccountProviderRegistryType,
      profilesController: ProfilesControllerType,
      settingsConfiguration: BuildConfigurationServiceType,
    ): TabbedNavigator {
      val bottomNavigator =
        BottomNavigators.create(
          fragment = fragment,
          fragmentContainerId = fragmentContainerId,
          navigationView = navigationView,
          accountProviders = accountProviders,
          profilesController = profilesController,
          settingsConfiguration = settingsConfiguration,
        )

      return TabbedNavigator(bottomNavigator)
    }
  }

  private val logger =
    LoggerFactory.getLogger(TabbedNavigator::class.java)

  val infoStream
    get() = navigator.infoStream()

  fun currentTab(): Int {
    return this.navigator.currentTab()
  }

  fun addFragment(fragment: Fragment, @IdRes tab: Int) {
    this.navigator.addFragment(fragment, tab)
  }

  fun reset(@IdRes tab: Int, resetRootFragment: Boolean) {
    this.navigator.reset(tab, resetRootFragment)
  }

  fun clearHistory() {
    this.logger.debug("clearing bottom navigator history")
    this.navigator.clearAll()
  }

  fun popBackStack(): Boolean {
    return this.navigator.pop()
  }

  fun popToRoot(): Boolean {
    val isAtRootOfStack = (1 == this.navigator.currentStackSize())
    if (isAtRootOfStack) {
      return false // Nothing to do
    }
    val currentTab = this.navigator.currentTab()
    this.navigator.reset(currentTab, false)
    return true
  }

  fun backStackSize(): Int {
    // Note: currentStackSize() is not safe to call here as it may throw an NPE.
    return this.navigator.stackSize(this.navigator.currentTab()) - 1
  }
}
