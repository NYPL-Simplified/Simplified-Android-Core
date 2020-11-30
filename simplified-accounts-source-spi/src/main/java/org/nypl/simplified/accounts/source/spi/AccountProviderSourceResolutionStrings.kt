package org.nypl.simplified.accounts.source.spi

import android.content.res.Resources
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType

/**
 * The resolution strings.
 */

class AccountProviderSourceResolutionStrings(
  private val resources: Resources
) : AccountProviderResolutionStringsType {

  override val resolvingAuthDocumentUnusableLink: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentUnusableLink)

  override val resolvingUnexpectedException: String
    get() = this.resources.getString(R.string.resolvingUnexpectedException)

  override val resolvingAuthDocumentRetrievalFailed: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentRetrievalFailed)

  override val resolvingAuthDocumentCOPPAAgeGateMalformed: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentCOPPAAgeGateMalformed)

  override val resolvingAuthDocumentOAuthMalformed: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentOAuthMalformed)

  override val resolvingAuthDocumentSAML20Malformed: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentSAML20Malformed)

  override val resolvingAuthDocumentNoUsableAuthenticationTypes: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentNoUsableAuthenticationTypes)

  override val resolvingAuthDocumentNoStartURI: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentNoStartURI)

  override val resolvingAuthDocumentParseFailed: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentParseFailed)

  override val resolvingAuthDocumentMissingURI: String
    get() = this.resources.getString(R.string.resolvingAuthDocumentMissingURI)

  override val resolving: String
    get() = this.resources.getString(R.string.resolving)

  override val resolvingAuthDocument: String
    get() = this.resources.getString(R.string.resolvingAuthDocument)
}
