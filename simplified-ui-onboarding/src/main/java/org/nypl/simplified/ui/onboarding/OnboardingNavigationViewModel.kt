package org.nypl.simplified.ui.onboarding

import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.disposables.Disposable
import org.nypl.simplified.navigation.api.NavigationViewModel
import org.nypl.simplified.ui.accounts.AccountsNavigationCommand

class OnboardingNavigationViewModel : NavigationViewModel<OnboardingNavigationCommand>() {

  private val commandQueue: UnicastWorkSubject<OnboardingNavigationCommand> =
    UnicastWorkSubject.create()

  private var subscription: Disposable? =
    null

  override fun registerHandler(callback: (OnboardingNavigationCommand) -> Unit) {
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
    mapOf(AccountsNavigationCommand::class.java to this::handleAccountCommand)

  private fun handleAccountCommand(command: AccountsNavigationCommand) {
    val embeddingCommand = OnboardingNavigationCommand.AccountsNavigationCommand(command)
    commandQueue.onNext(embeddingCommand)
  }
}
