package org.nypl.simplified.main

import android.content.Context
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.migration.api.Migrations
import org.nypl.simplified.migration.api.MigrationsType
import org.nypl.simplified.migration.spi.MigrationServiceDependencies
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfileAccountLoginRequest
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

internal object MainMigrations {

  private val logger = LoggerFactory.getLogger(MainMigrations::class.java)

  fun create(
    context: Context,
    profilesController: ProfilesControllerType
  ): MigrationsType {
    val isAnonymous =
      profilesController.profileAnonymousEnabled() == ProfilesDatabaseType.AnonymousProfileEnabled.ANONYMOUS_PROFILE_ENABLED

    val dependencies = MigrationServiceDependencies(
      createAccount = { uri ->
        this.doCreateAccount(profilesController, uri)
      },
      loginAccount = { account, credentials ->
        this.doLoginAccount(profilesController, account, credentials)
      },
      accountEvents = profilesController.accountEvents(),
      applicationProfileIsAnonymous = isAnonymous,
      applicationVersion = this.applicationVersion(context),
      context = context
    )

    return Migrations.create(dependencies)
  }

  private fun applicationVersion(context: Context): String {
    return try {
      val packageInfo =
        context
          .packageManager
          .getPackageInfo(context.packageName, 0)

      "${packageInfo.packageName} ${packageInfo.versionName} (${packageInfo.versionCode})"
    } catch (e: Exception) {
      this.logger.error("could not get package info: ", e)
      "unknown"
    }
  }

  private fun doLoginAccount(
    profilesController: ProfilesControllerType,
    account: AccountType,
    credentials: AccountAuthenticationCredentials
  ): TaskResult<Unit> {
    this.logger.debug("doLoginAccount")

    val taskRecorder = TaskRecorder.create()
    taskRecorder.beginNewStep("Logging in...")

    if (account.provider.authenticationAlternatives.isEmpty()) {
      when (val description = account.provider.authentication) {
        is AccountProviderAuthenticationDescription.COPPAAgeGate,
        AccountProviderAuthenticationDescription.Anonymous -> {
          return taskRecorder.finishSuccess(Unit)
        }
        is AccountProviderAuthenticationDescription.Basic -> {
          when (credentials) {
            is AccountAuthenticationCredentials.Basic -> {
              return profilesController.profileAccountLogin(
                ProfileAccountLoginRequest.Basic(
                  account.id,
                  description,
                  credentials.userName,
                  credentials.password
                )
              ).get(3L, TimeUnit.MINUTES)
            }
            is AccountAuthenticationCredentials.OAuthWithIntermediary -> {
              val message = "Can't use OAuth authentication during migrations."
              taskRecorder.currentStepFailed(message, "missingInformation")
              return taskRecorder.finishFailure()
            }
            is AccountAuthenticationCredentials.SAML2_0 -> {
              val message = "Can't use SAML 2.0 authentication during migrations."
              taskRecorder.currentStepFailed(message, "missingInformation")
              return taskRecorder.finishFailure()
            }
          }
        }
        is AccountProviderAuthenticationDescription.OAuthWithIntermediary -> {
          val message = "Can't use OAuth authentication during migrations."
          taskRecorder.currentStepFailed(message, "missingInformation")
          return taskRecorder.finishFailure()
        }
        is AccountProviderAuthenticationDescription.SAML2_0 -> {
          val message = "Can't use SAML 2.0 authentication during migrations."
          taskRecorder.currentStepFailed(message, "missingInformation")
          return taskRecorder.finishFailure()
        }
      }
    } else {
      val message = "Can't determine which authentication method is required."
      taskRecorder.currentStepFailed(message, "missingInformation")
      return taskRecorder.finishFailure()
    }
  }

  private fun doCreateAccount(
    profilesController: ProfilesControllerType,
    provider: URI
  ): TaskResult<AccountType> {
    this.logger.debug("doCreateAccount")
    return profilesController.profileAccountCreateOrReturnExisting(provider)
      .get(3L, TimeUnit.MINUTES)
  }
}
