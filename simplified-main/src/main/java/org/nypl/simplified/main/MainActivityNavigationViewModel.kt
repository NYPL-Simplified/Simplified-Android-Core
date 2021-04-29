package org.nypl.simplified.main

import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.disposables.Disposable
import org.nypl.simplified.navigation.api.NavigationViewModel
import org.nypl.simplified.ui.profiles.ProfilesNavigationCommand

class MainActivityNavigationViewModel : NavigationViewModel<MainActivityNavigationCommand>() {

  private val commandQueue: UnicastWorkSubject<MainActivityNavigationCommand> =
    UnicastWorkSubject.create()

  private var subscription: Disposable? = null

  override fun registerHandler(callback: (MainActivityNavigationCommand) -> Unit) {
    this.subscription =
      commandQueue.subscribe { command ->
        callback(command)
      }
  }

  override fun unregisterHandler() {
    this.subscription?.dispose()
    this.subscription = null
  }

  override val navigationControllers: Map<Class<*>, Any> =
    mapOf(ProfilesNavigationCommand::class.java to this::handleProfilesCommand)

  private fun handleProfilesCommand(command: ProfilesNavigationCommand) {
    val embeddingCommand = MainActivityNavigationCommand.ProfilesNavigationCommand(command)
    commandQueue.onNext(embeddingCommand)
  }
}
