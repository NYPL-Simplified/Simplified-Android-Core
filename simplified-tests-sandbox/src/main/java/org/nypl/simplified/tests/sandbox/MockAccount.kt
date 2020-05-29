package org.nypl.simplified.tests.sandbox

import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountPreferences
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.book_database.api.BookDatabaseType
import java.io.File
import java.net.URI
import java.util.UUID

class MockAccount(override val id: AccountID) : AccountType {

  private val providerId = UUID.randomUUID()

  override val bookDatabase: BookDatabaseType
    get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

  override fun setPreferences(preferences: AccountPreferences) {
  }

  private fun basic(): AccountProviderAuthenticationDescription {
    return AccountProviderAuthenticationDescription.Basic(
      barcodeFormat = null,
      keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
      passwordMaximumLength = 4,
      passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
      description = "What?",
      labels = mapOf(),
      logoURI = null
    )
  }

  private fun oauth(): AccountProviderAuthenticationDescription {
    return AccountProviderAuthenticationDescription.OAuthWithIntermediary(
      authenticate = URI.create("urn:create"),
      description = "What?",
      logoURI = URI.create("https://circulation.openebooks.us/images/CleverLoginButton280.png")
    )
  }

  private var accountProviderCurrent: AccountProviderType =
    run {
      val authentication = basic()

      AccountProvider(
        addAutomatically = false,
        annotationsURI = null,
        authentication = authentication,
        authenticationAlternatives = listOf(
          this.oauth()
        ),
        authenticationDocumentURI = null,
        cardCreatorURI = null,
        catalogURI = URI.create("catalog"),
        displayName = "Library ${this.id.uuid}",
        eula = null,
        id = URI.create("urn:uuid:${this.providerId}"),
        idNumeric = -1,
        isProduction = true,
        license = null,
        logo = null,
        mainColor = "red",
        loansURI = null,
        patronSettingsURI = null,
        privacyPolicy = null,
        subtitle = "Library ${this.id.uuid} Subtitle!",
        supportEmail = null,
        supportsReservations = false,
        updated = DateTime()
      )
    }

  override fun setAccountProvider(accountProvider: AccountProviderType) {
    this.accountProviderCurrent = accountProvider
  }

  override val directory: File
    get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

  override val provider: AccountProviderType
    get() {
      return this.accountProviderCurrent
    }

  override fun setLoginState(state: AccountLoginState) {
    this.loginStateMutable = state
  }

  var loginStateMutable: AccountLoginState =
    AccountLoginState.AccountNotLoggedIn

  override val loginState: AccountLoginState
    get() = this.loginStateMutable

  override val preferences: AccountPreferences
    get() = AccountPreferences(bookmarkSyncingPermitted = true)
}
