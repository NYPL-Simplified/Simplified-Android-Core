package org.nypl.simplified.main

import androidx.fragment.app.FragmentManager
import org.librarysimplified.services.api.Services
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.ui.profiles.ProfileModificationDefaultFragment
import org.nypl.simplified.ui.profiles.ProfileModificationFragmentParameters
import org.nypl.simplified.ui.profiles.ProfileModificationFragmentServiceType
import org.nypl.simplified.ui.profiles.ProfileSelectionFragment
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType
import org.slf4j.LoggerFactory

internal class ProfilesNavigationController(
  private val supportFragmentManager: FragmentManager,
  private val mainViewModel: MainFragmentViewModel
) : ProfilesNavigationControllerType {

  private val logger =
    LoggerFactory.getLogger(ProfilesNavigationController::class.java)

  private fun openModificationFragment(
    parameters: ProfileModificationFragmentParameters
  ) {
    val fragmentService =
      Services.serviceDirectory()
        .optionalService(ProfileModificationFragmentServiceType::class.java)

    val fragment =
      if (fragmentService != null) {
        this.logger.debug("found a profile modification fragment service: {}", fragmentService)
        fragmentService.createModificationFragment(parameters)
      } else {
        ProfileModificationDefaultFragment.create(parameters)
      }

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, fragment, "MAIN")
      .addToBackStack(null)
      .commit()
  }

  override fun openMain() {
    this.logger.debug("openMain")
    this.mainViewModel.clearHistory = true

    val mainFragment = MainFragment()
    this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, mainFragment, "MAIN")
      .addToBackStack(null)
      .commit()
  }

  override fun openProfileSelect() {
    this.logger.debug("openProfileSelect")
    this.mainViewModel.clearHistory = true

    val newFragment = ProfileSelectionFragment()
    this.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, newFragment, "MAIN")
      .commit()
  }

  override fun openProfileModify(id: ProfileID) {
    this.logger.debug("openProfileModify: ${id.uuid}")
    this.openModificationFragment(ProfileModificationFragmentParameters(id))
  }

  override fun openProfileCreate() {
    this.logger.debug("openProfileCreate")
    this.openModificationFragment(ProfileModificationFragmentParameters(null))
  }

  override fun popBackStack(): Boolean {
    this.logger.debug("popBackStack")
    this.supportFragmentManager.popBackStack()
    return this.backStackSize() > 0
  }

  override fun popToRoot(): Boolean {
    this.logger.debug("popToRoot")
    if (this.backStackSize() == 1) {
      return false
    }
    this.supportFragmentManager.popBackStack(
      null, FragmentManager.POP_BACK_STACK_INCLUSIVE
    )
    return true
  }

  override fun backStackSize(): Int {
    this.logger.debug("backStackSize")
    return this.supportFragmentManager.backStackEntryCount
  }
}
