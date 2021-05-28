package org.nypl.simplified.tests.mocking

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

  @Volatile
  private var preferencesCurrent: AccountPreferences =
    AccountPreferences(
      bookmarkSyncingPermitted = true,
      catalogURIOverride = null,
      announcementsAcknowledged = listOf()
    )

  private val providerId = UUID.randomUUID()

  @Volatile
  var bookDatabaseProperty: BookDatabaseType =
    MockBookDatabase(owner = this.id)

  override val bookDatabase: BookDatabaseType
    get() = this.bookDatabaseProperty

  override fun setPreferences(preferences: AccountPreferences) {
    this.preferencesCurrent = preferences
  }

  @Volatile
  private var accountProviderCurrent: AccountProviderType =
    run {
      val authentication =
        AccountProviderAuthenticationDescription.Basic(
          barcodeFormat = null,
          keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          passwordMaximumLength = 4,
          passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
          description = "What?",
          labels = mapOf(),
          logoURI = null
        )

      AccountProvider(
        addAutomatically = false,
        announcements = emptyList(),
        authentication = authentication,
        authenticationAlternatives = listOf(),
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
        updated = DateTime(),
        location = null
      )
    }

  override fun setAccountProvider(accountProvider: AccountProviderType) {
    this.accountProviderCurrent = accountProvider
  }

  override fun catalogURIForAge(age: Int): URI {
    val catalogURIOverride = this.preferences.catalogURIOverride
    if (catalogURIOverride != null) {
      return catalogURIOverride
    }
    return this.accountProviderCurrent.catalogURIForAge(age)
  }

  override val directory: File
    get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

  override val provider: AccountProviderType
    get() = accountProviderCurrent

  override fun setLoginState(state: AccountLoginState) {
    this.loginStateMutable = state
  }

  private var loginStateMutable: AccountLoginState =
    AccountLoginState.AccountNotLoggedIn

  override val loginState: AccountLoginState
    get() = this.loginStateMutable

  override val preferences: AccountPreferences
    get() = this.preferencesCurrent
}
