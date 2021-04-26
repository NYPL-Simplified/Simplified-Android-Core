package org.nypl.simplified.accounts.api

import one.irradia.mime.api.MIMEType
import org.joda.time.DateTime
import org.nypl.simplified.links.Link
import org.nypl.simplified.opds.core.OPDSFeedConstants.AUTHENTICATION_DOCUMENT_RELATION_URI_TEXT
import java.net.URI

/**
 * The metadata associated with the description of an account provider.
 */

data class AccountProviderDescription(

  /**
   * The unique identifier for the account provider.
   *
   * @see [AccountProviderType.id]
   */

  val id: URI,

  /**
   * The account title.
   *
   * @see [AccountProviderType.displayName]
   */

  val title: String,

  /**
   * The time that the description was last updated.
   */

  val updated: DateTime,

  /**
   * The links associated with the provider description.
   */

  val links: List<Link>,

  /**
   * The images associated with the provider description.
   */

  val images: List<Link>,

  /**
   * `true` if the account should be automatically added.
   *
   * @see [AccountProviderType.addAutomatically]
   */

  val isAutomatic: Boolean,

  /**
   * `true` if the library is a production library.
   */

  val isProduction: Boolean,

  /**
   * The location of the library, if any
   */

  val location: AccountLibraryLocation?

) : Comparable<AccountProviderDescription> {

  private val authenticationDocumentType =
    MIMEType("application", "vnd.opds.authentication.v1.0+json", mapOf())

  override fun compareTo(other: AccountProviderDescription): Int =
    this.title.compareTo(other.title)

  /**
   * The logo URI, if one is available
   */

  val logoURI: Link?
    get() = this.images.find { link -> link.relation == "http://opds-spec.org/image/thumbnail" }

  /**
   * The authentication document URI, if one is available
   */

  val authenticationDocumentURI: Link?
    get() = this.links.find { link ->
      when (link) {
        is Link.LinkBasic ->
          link.type == this.authenticationDocumentType ||
            link.relation == AUTHENTICATION_DOCUMENT_RELATION_URI_TEXT
        is Link.LinkTemplated -> false
      }
    }

  /**
   * The catalog URI, if one is available.
   */

  val catalogURI: Link?
    get() = this.links.find { link -> link.relation == "http://opds-spec.org/catalog" }
}
