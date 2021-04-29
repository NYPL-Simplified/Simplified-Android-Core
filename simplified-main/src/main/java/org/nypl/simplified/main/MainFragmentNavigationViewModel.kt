package org.nypl.simplified.main

import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.disposables.Disposable
import org.nypl.simplified.navigation.api.NavigationViewModel
import org.nypl.simplified.ui.accounts.AccountsNavigationCommand
import org.nypl.simplified.ui.catalog.CatalogNavigationCommand
import org.nypl.simplified.ui.settings.SettingsNavigationCommand

class MainFragmentNavigationViewModel : NavigationViewModel<MainFragmentNavigationCommand>() {

  private val commandQueue: UnicastWorkSubject<MainFragmentNavigationCommand> =
    UnicastWorkSubject.create()

  private var subscription: Disposable? = null

  override fun registerHandler(callback: (MainFragmentNavigationCommand) -> Unit) {
    this.subscription =
      commandQueue.subscribe { command ->
        callback(command)
      }
  }

  override fun unregisterHandler() {
    this.subscription?.dispose()
    this.subscription = null
  }

  override val navigationControllers: Map<Class<*>, Any> = mapOf(
    CatalogNavigationCommand::class.java to this::handleCatalogCommand,
    AccountsNavigationCommand::class.java to this::handleAccountCommand,
    SettingsNavigationCommand::class.java to this::handleSettingsCommand
  )

  private fun handleCatalogCommand(command: CatalogNavigationCommand) {
    val embeddingCommand = MainFragmentNavigationCommand.CatalogNavigationCommand(command)
    commandQueue.onNext(embeddingCommand)
  }

  private fun handleAccountCommand(command: AccountsNavigationCommand) {
    val embeddingCommand = MainFragmentNavigationCommand.AccountsNavigationCommand(command)
    commandQueue.onNext(embeddingCommand)
  }

  private fun handleSettingsCommand(command: SettingsNavigationCommand) {
    val embeddingCommand = MainFragmentNavigationCommand.SettingsNavigationCommand(command)
    commandQueue.onNext(embeddingCommand)
  }
}
