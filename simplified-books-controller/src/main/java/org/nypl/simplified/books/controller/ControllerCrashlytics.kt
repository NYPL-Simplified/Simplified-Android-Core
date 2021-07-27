package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.crashlytics.api.CrashlyticsServiceType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.slf4j.LoggerFactory
import java.security.MessageDigest

internal object ControllerCrashlytics {

  private val logger =
    LoggerFactory.getLogger(ControllerCrashlytics::class.java)

  fun configureCrashlytics(
    profile: ProfileReadableType,
    crashlytics: CrashlyticsServiceType
  ) {
    try {
      val account = profile.mostRecentAccount()
      this.configureCrashlyticsForCredentials(crashlytics, account.loginState.credentials)
    } catch (e: Exception) {
      this.logger.debug("exception raised when configuring crashlytics: ", e)
    }
  }

  private fun configureCrashlyticsForCredentials(
    crashlytics: CrashlyticsServiceType,
    credentials: AccountAuthenticationCredentials?
  ) {
    return when (credentials) {
      is AccountAuthenticationCredentials.Basic -> {
        this.setCrashlyticsUserID(crashlytics, credentials.userName.value)
      }
      is AccountAuthenticationCredentials.OAuthWithIntermediary,
      is AccountAuthenticationCredentials.SAML2_0,
      null -> {
        this.clearCrashlyticsUserID(crashlytics)
      }
    }
  }

  private fun setCrashlyticsUserID(
    crashlytics: CrashlyticsServiceType,
    value: String
  ) {
    crashlytics.setUserId(this.md5(value))
  }

  private fun clearCrashlyticsUserID(crashlytics: CrashlyticsServiceType) {
    crashlytics.setUserId("")
  }

  private fun md5(value: String): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(value.toByteArray())

    val sb = StringBuilder(64)
    md.digest().forEach { bb ->
      sb.append(String.format("%02x", bb))
    }
    return sb.toString()
  }
}
