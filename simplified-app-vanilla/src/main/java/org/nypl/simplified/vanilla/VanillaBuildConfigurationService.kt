package org.nypl.simplified.vanilla

import org.nypl.simplified.buildconfig.api.BuildConfigOAuthScheme
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.main.BuildConfig

class VanillaBuildConfigurationService : BuildConfigurationServiceType {
  override val vcsCommit: String
    get() = BuildConfig.GIT_COMMIT
  override val errorReportEmail: String
    get() = "simplyemigrationreports@nypl.org"
  override val oauthCallbackScheme: BuildConfigOAuthScheme
    get() = BuildConfigOAuthScheme("simplified-vanilla-oauth")
}
