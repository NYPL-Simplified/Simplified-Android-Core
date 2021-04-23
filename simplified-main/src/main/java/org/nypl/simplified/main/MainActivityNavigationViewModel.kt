package org.nypl.simplified.main

import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import org.nypl.simplified.navigation.api.NavigationControllerViewModel
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.ui.profiles.ProfilesNavigationControllerType

class MainActivityNavigationViewModel :
  NavigationControllerViewModel(),
  ProfilesNavigationControllerType {

  val commandQueue: UnicastWorkSubject<MainActivityNavigationCommand> =
    UnicastWorkSubject.create()

  init {
    this.updateNavigationController(
      ProfilesNavigationControllerType::class.java,
      this
    )
  }

  override fun openMain() {
    val command = MainActivityNavigationCommand.ProfileCommand.OpenMain
    commandQueue.onNext(command)
  }

  override fun openProfileModify(id: ProfileID) {
    val command = MainActivityNavigationCommand.ProfileCommand.OpenProfileModify(id)
    commandQueue.onNext(command)
  }

  override fun openProfileCreate() {
    val command = MainActivityNavigationCommand.ProfileCommand.OpenProfileCreate
    commandQueue.onNext(command)
  }

  override fun openProfileSelect() {
    val command = MainActivityNavigationCommand.ProfileCommand.OpenProfileSelect
    commandQueue.onNext(command)
  }

  override fun onProfileModificationCancelled() {
    val command = MainActivityNavigationCommand.ProfileCommand.OnProfileModificationCancelled
    commandQueue.onNext(command)
  }

  override fun onProfileModificationSucceeded() {
    val command = MainActivityNavigationCommand.ProfileCommand.OnProfileModificationSucceeded
    commandQueue.onNext(command)
  }
}
