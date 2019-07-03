package org.nypl.simplified.accounts.source.api

import java.lang.Exception

/**
 * An exception indicating a problem with the account provider registry.
 */

class AccountProviderRegistryException(
  val causes: List<Exception>)
  : Exception(causes[0])
