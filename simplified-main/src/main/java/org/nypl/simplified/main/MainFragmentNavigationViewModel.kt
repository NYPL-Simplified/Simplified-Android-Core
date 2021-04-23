package org.nypl.simplified.main

import org.nypl.simplified.navigation.api.NavigationControllerViewModel
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType
import org.nypl.simplified.ui.catalog.CatalogNavigationControllerType
import org.nypl.simplified.ui.navigation.tabs.TabbedNavigationController
import org.nypl.simplified.ui.settings.SettingsNavigationControllerType

class MainFragmentNavigationViewModel : NavigationControllerViewModel() {

  val navigationController: TabbedNavigationController =
    TabbedNavigationController()

  init {
    this.updateNavigationController(
      CatalogNavigationControllerType::class.java, this.navigationController
    )
    this.updateNavigationController(
      AccountNavigationControllerType::class.java, this.navigationController
    )
    this.updateNavigationController(
      SettingsNavigationControllerType::class.java, this.navigationController
    )
  }

  override fun onCleared() {
    super.onCleared()
    navigationController.disposeInfoStream()
  }
}
