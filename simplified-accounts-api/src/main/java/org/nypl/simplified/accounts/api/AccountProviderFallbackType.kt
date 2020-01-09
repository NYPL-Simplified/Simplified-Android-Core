package org.nypl.simplified.accounts.api

/**
 * A fallback interface that provides an account provider in the case that no other providers
 * are available.
 *
 * Implementations should be registered as services using [java.util.ServiceLoader].
 */

interface AccountProviderFallbackType {

  /**
   * Retrieve the fallback provider.
   */

  fun get(): AccountProviderType
}
