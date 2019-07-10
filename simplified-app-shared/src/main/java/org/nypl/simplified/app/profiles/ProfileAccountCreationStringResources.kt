package org.nypl.simplified.app.profiles

import android.content.res.Resources
import org.nypl.simplified.app.R
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType

class ProfileAccountCreationStringResources(
  val resources: Resources) : ProfileAccountCreationStringResourcesType {

  override val creatingAccountFailed: String
    get() = resources.getString(R.string.profileAccountCreationCreatingAccountFailed)

  override val creatingAccount: String
    get() = resources.getString(R.string.profileAccountCreationCreatingAccount)

  override val resolvingAccountProviderFailed: String
    get() = resources.getString(R.string.profileAccountCreationResolvingAccountProviderFailed)

  override val resolvingAccountProvider: String
    get() = resources.getString(R.string.profileAccountCreationResolvingAccountProvider)

}