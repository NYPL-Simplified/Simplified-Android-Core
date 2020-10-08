package org.nypl.simplified.tests

import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType

class MockAccountCreationStringResources : ProfileAccountCreationStringResourcesType {

  override val creatingAnAccountProviderDescription: String
    get() = "creatingAnAccountProviderDescription"

  override val searchingFeedForAuthenticationDocument: String
    get() = "searchingFeedForAuthenticationDocument"

  override val fetchingOPDSFeedFailed: String
    get() = "fetchingOPDSFeedFailed"

  override val parsingOPDSFeedFailed: String
    get() = "parsingOPDSFeedFailed"

  override val parsingOPDSFeed: String
    get() = "parsingOPDSFeed"

  override val fetchingOPDSFeed: String
    get() = "fetchingOPDSFeed"

  override val findingAuthDocumentURI: String
    get() = "findingAuthDocumentURI"

  override val creatingAccountSucceeded: String
    get() = "creatingAccountSucceeded"

  override val unexpectedException: String
    get() = "unexpectedException"

  override val creatingAccountFailed: String
    get() = "creatingAccountFailed"

  override val creatingAccount: String
    get() = "creatingAccount"

  override val resolvingAccountProviderFailed: String
    get() = "resolvingAccountProviderFailed"

  override val resolvingAccountProvider: String
    get() = "resolvingAccountProvider"
}
