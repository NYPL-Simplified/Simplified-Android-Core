package org.nypl.simplified.vanilla

import org.nypl.simplified.buildconfig.api.BuildConfigOAuthScheme
import org.nypl.simplified.buildconfig.api.BuildConfigurationAccountsRegistryURIs
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.main.BuildConfig
import java.net.URI

class VanillaBuildConfigurationService : BuildConfigurationServiceType {
  override val libraryRegistry: BuildConfigurationAccountsRegistryURIs
    get() = BuildConfigurationAccountsRegistryURIs(
      registry = URI("https://libraryregistry.librarysimplified.org/libraries"),
      registryQA = URI("https://libraryregistry.librarysimplified.org/libraries/qa")
    )
  override val allowAccountsAccess: Boolean
    get() = true
  override val allowAccountsRegistryAccess: Boolean
    get() = true
  override val showDebugBookDetailStatus: Boolean
    get() = false
  override val showSettingsTab: Boolean
    get() = true
  override val showHoldsTab: Boolean
    get() = true
  override val showBooksFromAllAccounts: Boolean
    get() = true
  override val vcsCommit: String
    get() = BuildConfig.GIT_COMMIT
  override val simplifiedVersion: String
    get() = BuildConfig.SIMPLIFIED_VERSION
  override val supportErrorReportEmailAddress: String
    get() = "simplyemigrationreports@nypl.org"
  override val supportErrorReportSubject: String
    get() = "[vanilla-error]"
  override val oauthCallbackScheme: BuildConfigOAuthScheme
    get() = BuildConfigOAuthScheme("simplified-vanilla-oauth")
  override val allowExternalReaderLinks: Boolean
    get() = false
  override val showChangeAccountsUi: Boolean
    get() = true
  override val showAgeGateUi: Boolean
    get() = true
}
