package org.nypl.simplified.main

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import org.librarysimplified.services.api.Services
import org.nypl.simplified.navigation.api.NavigationViewModel
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.ui.profiles.ProfileModificationDefaultFragment
import org.nypl.simplified.ui.profiles.ProfileModificationFragmentParameters
import org.nypl.simplified.ui.profiles.ProfileModificationFragmentServiceType
import org.nypl.simplified.ui.profiles.ProfileSelectionFragment
import org.nypl.simplified.ui.profiles.ProfilesNavigationCommand
import org.slf4j.LoggerFactory

internal class MainActivityNavigationDelegate(
  private val navigationViewModel: NavigationViewModel<MainActivityNavigationCommand>,
  private val fragmentManager: FragmentManager,
  private val mainActivityViewModel: MainActivityViewModel
) : LifecycleObserver {

  private val logger =
    LoggerFactory.getLogger(MainActivityNavigationDelegate::class.java)

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onStart() {
    this.navigationViewModel.registerHandler(this::handleCommand)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onStop() {
    this.navigationViewModel.unregisterHandler()
  }

  private fun handleCommand(command: MainActivityNavigationCommand) {
    return when (command) {
      is MainActivityNavigationCommand.ProfilesNavigationCommand ->
        this.handleProfilesCommand(command.command)
    }
  }

  private fun handleProfilesCommand(command: ProfilesNavigationCommand) {
    return when (command) {
      ProfilesNavigationCommand.OnProfileModificationCancelled ->
        this.onProfileModificationCancelled()
      ProfilesNavigationCommand.OnProfileModificationSucceeded ->
        this.onProfileModificationSucceeded()
      ProfilesNavigationCommand.OpenMain ->
        this.openMain()
      ProfilesNavigationCommand.OpenProfileCreate ->
        this.openProfileCreate()
      is ProfilesNavigationCommand.OpenProfileModify ->
        this.openProfileModify(command.id)
      ProfilesNavigationCommand.OpenProfileSelect ->
        this.openProfileSelect()
    }
  }

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

    this.fragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, fragment, "MAIN")
      .addToBackStack(null)
      .commit()
  }

  private fun openMain() {
    this.logger.debug("openMain")
    this.mainActivityViewModel.clearHistory = true

    val mainFragment = MainFragment()
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, mainFragment, "MAIN")
      .addToBackStack(null)
      .commit()
  }

  private fun openProfileSelect() {
    this.logger.debug("openProfileSelect")
    this.mainActivityViewModel.clearHistory = true

    val newFragment = ProfileSelectionFragment()
    this.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    this.fragmentManager.beginTransaction()
      .replace(R.id.mainFragmentHolder, newFragment, "MAIN")
      .commit()
  }

  private fun openProfileModify(id: ProfileID) {
    this.logger.debug("openProfileModify: ${id.uuid}")
    this.openModificationFragment(ProfileModificationFragmentParameters(id))
  }

  private fun openProfileCreate() {
    this.logger.debug("openProfileCreate")
    this.openModificationFragment(ProfileModificationFragmentParameters(null))
  }

  private fun onProfileModificationSucceeded() {
    this.fragmentManager.popBackStack()
  }

  private fun onProfileModificationCancelled() {
    this.fragmentManager.popBackStack()
  }
}
