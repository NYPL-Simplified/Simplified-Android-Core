package org.nypl.simplified.tests.books.accounts

import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.json.AccountProvidersJSON.deserializeCollectionFromStream
import org.nypl.simplified.accounts.json.AccountProvidersJSON.deserializeFromJSON
import org.nypl.simplified.accounts.json.AccountProvidersJSON.serializeToJSON
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.URI

class AccountProvidersJSONTest {
  @Test
  @Throws(Exception::class)
  fun testEmpty() {
    Assertions.assertThrows(JSONParseException::class.java) { deserializeFromString("") }
  }

  @Throws(IOException::class)
  private fun deserializeFromString(text: String) {
    deserializeCollectionFromStream(
      ByteArrayInputStream(text.toByteArray())
    )
  }

  @Test
  @Throws(Exception::class)
  fun testEmptyArray() {
    deserializeFromString("[]")
  }

  @Test
  @Throws(Exception::class)
  fun testEmptyWrongType() {
    Assertions.assertThrows(JSONParseException::class.java) { deserializeFromString("{}") }
  }

  @Test
  @Throws(Exception::class)
  fun testDuplicateProvider() {
    val ex = Assertions.assertThrows(JSONParseException::class.java) {
      deserializeCollectionFromStream(
        readAllFromResource("providers-duplicate.json")
      )
    }
    Assertions.assertTrue(ex.message!!.contains("Duplicate provider"))
  }

  @Test
  @Throws(Exception::class)
  fun testMultipleAuthenticationTypes0() {
    val providers = deserializeCollectionFromStream(
      readAllFromResource("providers-multi-auth-0.json")
    )
    Assertions.assertEquals(1, providers.size)
    val provider = providers[URI.create("urn:uuid:c379b3b9-18e2-476f-95d1-7ba10f151d00")]
    Assertions.assertEquals(AccountProviderAuthenticationDescription.Basic::class.java, provider!!.authentication.javaClass)
    Assertions.assertEquals(0, provider.authenticationAlternatives.size)
    val providerAfter = deserializeFromJSON(serializeToJSON(provider))
    Assertions.assertEquals(providerAfter, provider)
  }

  @Test
  @Throws(Exception::class)
  fun testMultipleAuthenticationTypes1() {
    val providers = deserializeCollectionFromStream(
      readAllFromResource("providers-multi-auth-1.json")
    )
    Assertions.assertEquals(1, providers.size)
    val provider = providers[URI.create("urn:uuid:c379b3b9-18e2-476f-95d1-7ba10f151d00")]
    Assertions.assertEquals(AccountProviderAuthenticationDescription.Basic::class.java, provider!!.authentication.javaClass)
    Assertions.assertEquals(2, provider.authenticationAlternatives.size)
    val providerAfter = deserializeFromJSON(serializeToJSON(provider))
    Assertions.assertEquals(providerAfter, provider)
  }

  @Test
  @Throws(Exception::class)
  fun testSAML2() {
    val providers = deserializeCollectionFromStream(
      readAllFromResource("providers-saml.json")
    )
    Assertions.assertEquals(1, providers.size)
    val provider = providers[URI.create("urn:uuid:b67588ef-d3ce-4187-9709-04e6f4c01a13")]
    Assertions.assertEquals(AccountProviderAuthenticationDescription.SAML2_0::class.java, provider!!.authentication.javaClass)
    Assertions.assertEquals(0, provider.authenticationAlternatives.size)
    val providerAfter = deserializeFromJSON(serializeToJSON(provider))
    Assertions.assertEquals(providerAfter, provider)
  }

  @Test
  @Throws(Exception::class)
  fun testAll() {
    val c = deserializeCollectionFromStream(
      readAllFromResource("providers-all.json")
    )
    Assertions.assertEquals(172L, c.size.toLong())
  }

  @Throws(Exception::class)
  private fun readAllFromResource(
    name: String
  ): InputStream {
    val url = AccountProvidersJSONTest::class.java.getResource(
      "/org/nypl/simplified/tests/books/accounts/$name"
    )
    return url.openStream()
  }

  @Test
  @Throws(Exception::class)
  fun testOAuthClientCredentials() {
    val basicAuth =
      AccountProviderAuthenticationDescription.Basic(
        description = "First Book - JWT",
        formDescription =
          AccountProviderAuthenticationDescription.FormDescription(
            barcodeFormat = null,
            keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
            passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
            passwordMaximumLength = -1,
            labels = mapOf(),
          ),
        logoURI = URI.create("https://circulation.openebooks.us/images/FirstBookLoginButton280.png")
      )

    val oauthClientCredentialsAuth =
      AccountProviderAuthenticationDescription.OAuthClientCredentials(
        description = "First Book - JWT",
        formDescription =
          AccountProviderAuthenticationDescription.FormDescription(
            barcodeFormat = null,
            keyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
            passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.DEFAULT,
            passwordMaximumLength = -1,
            labels = mapOf(),
          ),
        logoURI = URI.create("https://circulation.openebooks.us/images/FirstBookLoginButton280.png"),
        authenticate = URI.create("https://circulation.openebooks.us/http_basic_auth_token")
      )

    val cleverAuth =
      AccountProviderAuthenticationDescription.OAuthWithIntermediary(
        description = "Clever",
        authenticate = URI.create("https://circulation.openebooks.us/USOEI/oauth_authenticate?provider=Clever"),
        logoURI = URI.create("https://circulation.openebooks.us/images/CleverLoginButton280.png")
      )

    val provider =
      AccountProvider(
        addAutomatically = true,
        authenticationDocumentURI = URI.create("https://circulation.openebooks.us/USOEI/authentication_document"),
        authentication = basicAuth,
        authenticationAlternatives = listOf(oauthClientCredentialsAuth, cleverAuth),
        cardCreatorURI = null,
        catalogURI = URI.create("https://circulation.openebooks.us/USOEI/groups"),
        displayName = "Open eBooks",
        eula = null,
        id = URI.create("urn:uuid:c379b3b9-18e2-476f-95d1-7ba10f151d00"),
        idNumeric = -1,
        isProduction = true,
        license = URI.create("http://www.librarysimplified.org/iclicenses.html"),
        loansURI = URI.create("https://circulation.openebooks.us/USOEI/loans/"),
        logo = null,
        mainColor = "teal",
        patronSettingsURI = URI.create("https://circulation.openebooks.us/USOEI/patrons/me/"),
        privacyPolicy = URI.create("https://openebooks.net/app_privacy.html"),
        subtitle = "",
        supportEmail = null,
        supportsReservations = false,
        updated = DateTime.parse("2020-05-10T00:00:00Z"),
        location = null
      )

    val providerAfter = deserializeFromJSON(serializeToJSON(provider))
    Assertions.assertEquals(providerAfter, provider)
  }
}
