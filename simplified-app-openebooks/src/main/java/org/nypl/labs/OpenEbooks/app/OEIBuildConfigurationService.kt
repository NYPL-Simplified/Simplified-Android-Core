package org.nypl.labs.OpenEbooks.app

import org.nypl.simplified.buildconfig.api.BuildConfigOAuthScheme
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.main.BuildConfig

class OEIBuildConfigurationService : BuildConfigurationServiceType {
  override val vcsCommit: String
    get() = BuildConfig.GIT_COMMIT
  override val showSettingsTab: Boolean
    get() = true
  override val showHoldsTab: Boolean
    get() = false
  override val supportErrorReportEmailAddress: String
    get() = "simplyemigrationreports@nypl.org"
  override val supportErrorReportSubject: String
    get() = "[Open eBooks error report]"
  override val oauthCallbackScheme: BuildConfigOAuthScheme
    get() = BuildConfigOAuthScheme("simplified-openebooks-oauth")
  override val showDebugBookDetailStatus: Boolean
    get() = false
  override val simplifiedVersion: String
    get() = BuildConfig.SIMPLIFIED_VERSION
  override val allowAccountsAccess: Boolean =
    true
  override val allowAccountsRegistryAccess: Boolean =
    false
  override val showChangeAccountsUi: Boolean =
    false
}
