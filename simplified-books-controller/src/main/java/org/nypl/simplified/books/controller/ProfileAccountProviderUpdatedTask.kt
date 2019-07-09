package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.profiles.api.ProfileType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable

/**
 * A trivial task that checks to see if the given profile has an account from the given
 * account provider and, if it does, updates the account with the latest provider definition
 * from the registry.
 */

class ProfileAccountProviderUpdatedTask(
  private val profile: ProfileType,
  private val accountProviderID: URI,
  private val accountProviders: AccountProviderRegistryType) : Callable<Unit> {

  private val logger = LoggerFactory.getLogger(ProfileAccountProviderUpdatedTask::class.java)

  override fun call() {
    try {
      val accounts = profile.accountsByProvider()
      val account = accounts[this.accountProviderID]
      if (account != null) {
        val updatedProvider = this.accountProviders.resolvedProviders[accountProviderID]
        if (updatedProvider != null) {
          account.setAccountProvider(updatedProvider)
        }
      }
    } catch (e: Exception) {
      this.logger.error("could not update account provider: ", e)
    }
  }
}