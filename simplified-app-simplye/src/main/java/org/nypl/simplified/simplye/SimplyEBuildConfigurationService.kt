package org.nypl.simplified.simplye

import org.nypl.simplified.buildconfig.api.BuildConfigOAuthScheme
import org.nypl.simplified.buildconfig.api.BuildConfigurationAccountsRegistryURIs
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.main.BuildConfig
import java.net.URI

class SimplyEBuildConfigurationService : BuildConfigurationServiceType {
  override val libraryRegistry: BuildConfigurationAccountsRegistryURIs
    get() = BuildConfigurationAccountsRegistryURIs(
      registry = URI("https://libraryregistry.librarysimplified.org/libraries"),
      registryQA = URI("https://libraryregistry.librarysimplified.org/libraries/qa")
    )
  override val allowAccountsAccess: Boolean
    get() = true
  override val allowAccountsRegistryAccess: Boolean
    get() = true
  override val oauthCallbackScheme: BuildConfigOAuthScheme
    get() = BuildConfigOAuthScheme("simplye-oauth")
  override val allowExternalReaderLinks: Boolean
    get() = true
  override val showBooksFromAllAccounts: Boolean
    get() = false
  override val showChangeAccountsUi: Boolean
    get() = true
  override val showDebugBookDetailStatus: Boolean
    get() = false
  override val showHoldsTab: Boolean
    get() = true
  override val showSettingsTab: Boolean
    get() = true
  override val supportErrorReportEmailAddress: String
    get() = "simplyemigrationreports@nypl.org"
  override val supportErrorReportSubject: String
    get() = "[SimplyE error report]"
  override val vcsCommit: String
    get() = BuildConfig.GIT_COMMIT
  override val simplifiedVersion: String
    get() = BuildConfig.SIMPLIFIED_VERSION
  override val showAgeGateUi: Boolean
    get() = true
}
