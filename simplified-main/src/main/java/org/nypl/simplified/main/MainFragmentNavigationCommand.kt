package org.nypl.simplified.main

sealed class MainFragmentNavigationCommand {

  data class CatalogNavigationCommand(
    val command: org.nypl.simplified.ui.catalog.CatalogNavigationCommand
  ) : MainFragmentNavigationCommand()

  data class AccountsNavigationCommand(
    val command: org.nypl.simplified.ui.accounts.AccountsNavigationCommand
  ) : MainFragmentNavigationCommand()

  data class SettingsNavigationCommand(
    val command: org.nypl.simplified.ui.settings.SettingsNavigationCommand
  ) : MainFragmentNavigationCommand()
}
