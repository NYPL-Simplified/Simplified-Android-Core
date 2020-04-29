package org.nypl.simplified.accounts.api

import org.joda.time.DateTime
import org.nypl.simplified.links.Link
import java.net.URI
import javax.annotation.concurrent.ThreadSafe

/**
 * A provider of accounts.
 *
 * Implementations are required to be safe to manipulate from multiple threads.
 */

@ThreadSafe
interface AccountProviderType : Comparable<AccountProviderType> {

  /**
   * @return The account provider URI
   */

  val id: URI

  /**
   * @return The old-style numeric ID of the account
   */

  @Deprecated("Use URI-based IDs")
  val idNumeric: Int

  /**
   * @return `true` if this account is in production
   */

  val isProduction: Boolean

  /**
   * @return The display name
   */

  val displayName: String

  /**
   * @return The subtitle
   */

  val subtitle: String?

  /**
   * @return The logo image
   */

  val logo: URI?

  /**
   * @return An authentication description if authentication is required, or nothing if it isn't
   */

  val authentication: AccountProviderAuthenticationDescription?

  /**
   * @return `true` iff the SimplyE synchronization is supported
   * @see .annotationsURI
   * @see .patronSettingsURI
   */

  val supportsSimplyESynchronization: Boolean

  /**
   * @return `true` iff reservations are supported
   */

  val supportsReservations: Boolean

  /**
   * @return The URI of the user loans feed, if supported
   */

  val loansURI: URI?

  /**
   * @return The URI of the card creator iff card creation is supported
   */

  val cardCreatorURI: URI?

  /**
   * @return The address of the authentication document for the account provider
   */

  val authenticationDocumentURI: URI?

  /**
   * @return The base URI of the catalog
   */

  val catalogURI: URI

  /**
   * @return The support email address
   */

  val supportEmail: String?

  /**
   * @return The URI of the EULA if one is required
   */

  val eula: URI?

  /**
   * @return The URI of the EULA if one is required
   */

  val license: URI?

  /**
   * @return The URI of the privacy policy if one is required
   */

  val privacyPolicy: URI?

  /**
   * @return The main color used to decorate the application when using this provider
   */

  val mainColor: String

  /**
   * @return `true` iff the account should be added by default
   */

  val addAutomatically: Boolean

  /**
   * The patron settings URI. This is the URI used to get and set patron settings.
   *
   * @return The patron settings URI
   */

  val patronSettingsURI: URI?

  /**
   * The annotations URI. This is the URI used to get and set annotations for bookmark
   * syncing.
   *
   * @return The annotations URI
   * @see .supportsSimplyESynchronization
   */

  val annotationsURI: URI?

  /**
   * Determine the correct catalog URI to use for readers of a given age.
   *
   * @param age The age of the reader
   * @return The correct catalog URI for the given age
   */

  fun catalogURIForAge(age: Int): URI {
    return when (val auth = this.authentication) {
      null -> this.catalogURI

      is AccountProviderAuthenticationDescription.COPPAAgeGate ->
        if (age >= 13) {
          auth.greaterEqual13
        } else {
          auth.under13
        }

      is AccountProviderAuthenticationDescription.Basic ->
        this.catalogURI
    }
  }

  /**
   * @return The time that this account provider was most recently updated
   */

  val updated: DateTime

  /**
   * @return `true` if the authentication settings imply that barcode scanning and display is supported
   */

  val supportsBarcodeDisplay: Boolean
    get() = when (val auth = this.authentication) {
      is AccountProviderAuthenticationDescription.COPPAAgeGate -> false
      is AccountProviderAuthenticationDescription.Basic -> {
        when (auth.barcodeFormat) {
          "Codabar" -> true
          else -> false
        }
      }
      null -> false
    }

  fun toDescription(): AccountProviderDescription {
    val imageLinks = mutableListOf<Link>()
    this.logo?.let { uri ->
      imageLinks.add(
        Link.LinkBasic(
          href = uri,
          type = null,
          relation = "http://opds-spec.org/image/thumbnail"
        )
      )
    }

    val links =
      mutableListOf<Link>()

    this.annotationsURI?.let { uri ->
      addLink(links, uri, "http://www.w3.org/ns/oa#annotationService")
    }
    this.cardCreatorURI?.let { uri ->
      addLink(links, uri, "register")
    }
    this.eula?.let { uri ->
      addLink(links, uri, "terms-of-service")
    }
    this.license?.let { uri ->
      addLink(links, uri, "license")
    }
    this.loansURI?.let { uri ->
      addLink(links, uri, "http://opds-spec.org/shelf")
    }
    this.patronSettingsURI?.let { uri ->
      addLink(links, uri, "http://librarysimplified.org/terms/rel/user-profile")
    }
    this.privacyPolicy?.let { uri ->
      addLink(links, uri, "privacy-policy")
    }

    return AccountProviderDescription(
      id = this.id,
      title = this.displayName,
      updated = this.updated,
      links = links.toList(),
      images = imageLinks.toList(),
      isAutomatic = this.addAutomatically,
      isProduction = this.isProduction
    )
  }

  private fun addLink(links: MutableList<Link>, uri: URI, relation: String): Boolean {
    return links.add(
      Link.LinkBasic(
        href = uri,
        type = null,
        relation = relation
      )
    )
  }
}
