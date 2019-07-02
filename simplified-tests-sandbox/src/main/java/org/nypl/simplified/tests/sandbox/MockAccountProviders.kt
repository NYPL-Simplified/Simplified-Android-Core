package org.nypl.simplified.tests.sandbox

import com.google.common.base.Preconditions
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderImmutable
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.source.api.AccountProviderDescriptionRegistryEvent
import org.nypl.simplified.accounts.source.api.AccountProviderDescriptionRegistryType
import org.nypl.simplified.observable.Observable
import org.nypl.simplified.observable.ObservableReadableType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.TreeMap

object MockAccountProviders {

  private val logger = LoggerFactory.getLogger(MockAccountProviders::class.java)

  fun findAccountProviderDangerously (
    registry: AccountProviderDescriptionRegistryType,
    id: URI): AccountProviderType {
    val accountProviderDescription =
      registry.findAccountProviderDescription(id)

    Preconditions.checkState(
      accountProviderDescription != null,
      "Looking up provider $id must not fail")

    val result =
      accountProviderDescription!!
        .resolve { providerId, status -> logger.debug("status: {}: {}", providerId, status) }

    Preconditions.checkState(
      !result.failed,
      "Resolving provider $id must not fail")

    return result.result!!
  }

  fun findAccountProviderDangerously(
    registry: AccountProviderDescriptionRegistryType,
    id: String): AccountProviderType =
    this.findAccountProviderDangerously(registry, URI.create(id))

  fun fakeProvider(providerId: String): AccountProviderImmutable {
    return AccountProviderImmutable(
      id = URI.create(providerId),
      isProduction = false,
      displayName = "Fake Library",
      subtitle = "Imaginary books",
      logo = URI.create("data:text/plain;base64,U3RvcCBsb29raW5nIGF0IG1lIQo="),
      authentication = null,
      supportsSimplyESynchronization = false,
      supportsBarcodeScanner = false,
      supportsBarcodeDisplay = false,
      supportsReservations = false,
      supportsCardCreator = false,
      supportsHelpCenter = false,
      authenticationDocumentURI = null,
      catalogURI = URI.create("http://www.example.com/accounts0/feed.xml"),
      catalogURIForUnder13s = null,
      catalogURIForOver13s = null,
      supportEmail = "postmaster@example.com",
      eula = null,
      license = null,
      privacyPolicy = null,
      mainColor = "#ff0000",
      styleNameOverride = null,
      addAutomatically = false,
      patronSettingsURI = URI.create("http://example.com/accounts0/patrons/me"),
      annotationsURI = URI.create("http://example.com/accounts0/annotations"),
      updated = DateTime.parse("2000-01-01T00:00:00Z"))
  }

  fun fakeAccountProviderDefaultURI(): URI {
    return URI.create("urn:fake:2")
  }

  fun fakeAccountProviderDefaultAutoURI(): URI {
    return URI.create("urn:fake:auto-4")
  }

  fun fakeAccountProviders(): AccountProviderDescriptionRegistryType {
    val fake0 = fakeProvider("urn:fake:0")
    val fake1 = fakeProvider("urn:fake:1")
    val fake2 = fakeProvider("urn:fake:2")
    val fake3 = fakeAuthProvider("urn:fake-auth:0")

    val providers = TreeMap<URI, AccountProviderType>()
    providers[fake0.id] = fake0
    providers[fake1.id] = fake1
    providers[fake2.id] = fake2
    providers[fake3.id] = fake3

    return object: AccountProviderDescriptionRegistryType {
      override val defaultProvider: AccountProviderType =
        fake0

      override val events: ObservableReadableType<AccountProviderDescriptionRegistryEvent> =
        Observable.create()

      override fun refresh() {

      }

      override fun accountProviderDescriptions(): Map<URI, AccountProviderDescriptionType> {
        return providers.mapValues { v -> v.value.toDescription() }
      }
    }
  }

  fun fakeAccountProvidersWithAutomatic(): AccountProviderDescriptionRegistryType {
    val fake0 = fakeProvider("urn:fake:0")
    val fake1 = fakeProvider("urn:fake:1")
    val fake2 = fakeProvider("urn:fake:2")
    val fake3 = fakeAuthProvider("urn:fake-auth:0")
    val fake4 = fakeProviderAuto("urn:fake:auto-4")

    val providers = TreeMap<URI, AccountProviderType>()
    providers[fake0.id] = fake0
    providers[fake1.id] = fake1
    providers[fake2.id] = fake2
    providers[fake3.id] = fake3
    providers[fake4.id] = fake4

    return object: AccountProviderDescriptionRegistryType {
      override val defaultProvider: AccountProviderType =
        fake0

      override val events: ObservableReadableType<AccountProviderDescriptionRegistryEvent> =
        Observable.create()

      override fun refresh() {

      }

      override fun accountProviderDescriptions(): Map<URI, AccountProviderDescriptionType> {
        return providers.mapValues { v -> v.value.toDescription() }
      }
    }
  }

  fun fakeProviderAuto(id: String): AccountProviderImmutable {
    return fakeProvider(id).copy(addAutomatically = true)
  }

  fun fakeAccountProvidersMissing0(): AccountProviderDescriptionRegistryType {
    val fake1 = fakeProvider("urn:fake:1")
    val fake2 = fakeProvider("urn:fake:2")
    val fake3 = fakeAuthProvider("urn:fake-auth:0")

    val providers = TreeMap<URI, AccountProviderType>()
    providers[fake1.id] = fake1
    providers[fake2.id] = fake2
    providers[fake3.id] = fake3

    return object: AccountProviderDescriptionRegistryType {
      override val defaultProvider: AccountProviderType =
        fake1

      override val events: ObservableReadableType<AccountProviderDescriptionRegistryEvent> =
        Observable.create()

      override fun refresh() {

      }

      override fun accountProviderDescriptions(): Map<URI, AccountProviderDescriptionType> {
        return providers.mapValues { v -> v.value.toDescription() }
      }
    }
  }

  fun fakeAccountProvidersMissing1(): AccountProviderDescriptionRegistryType {
    val fake0 = fakeProvider("urn:fake:0")
    val fake2 = fakeProvider("urn:fake:2")
    val fake3 = fakeAuthProvider("urn:fake-auth:0")

    val providers = TreeMap<URI, AccountProviderType>()
    providers[fake0.id] = fake0
    providers[fake2.id] = fake2
    providers[fake3.id] = fake3

    return object: AccountProviderDescriptionRegistryType {
      override val defaultProvider: AccountProviderType =
        fake0

      override val events: ObservableReadableType<AccountProviderDescriptionRegistryEvent> =
        Observable.create()

      override fun refresh() {

      }

      override fun accountProviderDescriptions(): Map<URI, AccountProviderDescriptionType> {
        return providers.mapValues { v -> v.value.toDescription() }
      }
    }
  }

  fun fakeAuthProvider(uri: String): AccountProviderImmutable {
    return fakeProvider(uri)
      .copy(authentication = AccountProviderAuthenticationDescription.builder()
        .setLoginURI(URI.create(uri))
        .setPassCodeLength(4)
        .setPassCodeMayContainLetters(true)
        .setRequiresPin(true)
        .build())
  }
}
