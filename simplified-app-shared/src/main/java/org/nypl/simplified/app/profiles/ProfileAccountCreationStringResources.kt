package org.nypl.simplified.app.profiles

import android.content.res.Resources
import org.nypl.simplified.app.R
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType

class ProfileAccountCreationStringResources(
  val resources: Resources) : ProfileAccountCreationStringResourcesType {

  override val creatingAnAccountProviderDescription: String
    get() = this.resources.getString(R.string.profileAccountCreatingAnAccountProviderDescription)

  override val searchingFeedForAuthenticationDocument: String
    get() = this.resources.getString(R.string.profileAccountSearchingFeedForAuthenticationDocument)

  override val fetchingOPDSFeedFailed: String
    get() = this.resources.getString(R.string.profileAccountFetchingOPDSFeedFailed)

  override val parsingOPDSFeedFailed: String
    get() = this.resources.getString(R.string.profileAccountParsingOPDSFeedFailed)

  override val parsingOPDSFeed: String
    get() = this.resources.getString(R.string.profileAccountParsingOPDSFeed)

  override val fetchingOPDSFeed: String
    get() = this.resources.getString(R.string.profileAccountFetchingOPDSFeed)

  override val findingAuthDocumentURI: String
    get() = this.resources.getString(R.string.profileAccountFindingAuthDocumentURI)

  override val creatingAccountSucceeded: String
    get() = this.resources.getString(R.string.profileAccountCreationSucceeded)

  override val unexpectedException: String
    get() = this.resources.getString(R.string.unexpectedException)

  override val creatingAccountFailed: String
    get() = resources.getString(R.string.profileAccountCreationCreatingAccountFailed)

  override val creatingAccount: String
    get() = resources.getString(R.string.profileAccountCreationCreatingAccount)

  override val resolvingAccountProviderFailed: String
    get() = resources.getString(R.string.profileAccountCreationResolvingAccountProviderFailed)

  override val resolvingAccountProvider: String
    get() = resources.getString(R.string.profileAccountCreationResolvingAccountProvider)

}