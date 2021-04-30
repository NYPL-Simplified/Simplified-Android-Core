package org.nypl.simplified.main

sealed class MainFragmentListenedEvent {

  data class CatalogSAML20Event(
    val event: org.nypl.simplified.ui.catalog.saml20.CatalogSAML20Event
  ) : MainFragmentListenedEvent()

  data class CatalogFeedEvent(
    val event: org.nypl.simplified.ui.catalog.CatalogFeedEvent
  ) : MainFragmentListenedEvent()

  data class CatalogBookDetailEvent(
    val event: org.nypl.simplified.ui.catalog.CatalogBookDetailEvent
  ) : MainFragmentListenedEvent()

  data class AccountSAML20Event(
    val event: org.nypl.simplified.ui.accounts.saml20.AccountSAML20Event
  ) : MainFragmentListenedEvent()

  data class AccountDetailEvent(
    val event: org.nypl.simplified.ui.accounts.AccountDetailEvent
  ) : MainFragmentListenedEvent()

  data class AccountListEvent(
    val event: org.nypl.simplified.ui.accounts.AccountListEvent
  ) : MainFragmentListenedEvent()

  data class AccountListRegistryEvent(
    val event: org.nypl.simplified.ui.accounts.AccountListRegistryEvent
  ) : MainFragmentListenedEvent()

  data class AccountPickerEvent(
    val event: org.nypl.simplified.ui.accounts.AccountPickerEvent
  ) : MainFragmentListenedEvent()

  data class SettingsMainEvent(
    val event: org.nypl.simplified.ui.settings.SettingsMainEvent
  ) : MainFragmentListenedEvent()

  data class SettingsDebugEvent(
    val event: org.nypl.simplified.ui.settings.SettingsDebugEvent
  ) : MainFragmentListenedEvent()

  data class ProfileTabEvent(
    val event: org.nypl.simplified.ui.profiles.ProfileTabEvent
  ) : MainFragmentListenedEvent()
}
