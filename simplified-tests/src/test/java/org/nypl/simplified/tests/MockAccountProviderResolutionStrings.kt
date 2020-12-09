package org.nypl.simplified.tests

import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType

class MockAccountProviderResolutionStrings : AccountProviderResolutionStringsType {

  override val resolvingAuthDocumentUnusableLink: String
    get() = "resolvingAuthDocumentUnusableLink"

  override val resolvingUnexpectedException: String
    get() = "resolvingUnexpectedException"

  override val resolvingAuthDocumentRetrievalFailed: String
    get() = "resolvingAuthDocumentRetrievalFailed"

  override val resolvingAuthDocumentCOPPAAgeGateMalformed: String
    get() = "resolvingAuthDocumentCOPPAAgeGateMalformed"

  override val resolvingAuthDocumentOAuthMalformed: String
    get() = "resolvingAuthDocumentOAuthMalformed"

  override val resolvingAuthDocumentSAML20Malformed: String
    get() = "resolvingAuthDocumentSAML20Malformed"

  override val resolvingAuthDocumentNoUsableAuthenticationTypes: String
    get() = "resolvingAuthDocumentNoUsableAuthenticationTypes"

  override val resolvingAuthDocumentNoStartURI: String
    get() = "resolvingAuthDocumentNoStartURI"

  override val resolvingAuthDocumentParseFailed: String
    get() = "resolvingAuthDocumentParseFailed"

  override val resolvingAuthDocumentMissingURI: String
    get() = "resolvingAuthDocumentMissingURI"

  override val resolving: String
    get() = "resolving"

  override val resolvingAuthDocument: String
    get() = "resolvingAuthDocument"
}
