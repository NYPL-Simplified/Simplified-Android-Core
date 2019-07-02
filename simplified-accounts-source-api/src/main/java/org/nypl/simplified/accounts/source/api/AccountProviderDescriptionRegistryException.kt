package org.nypl.simplified.accounts.source.api

import java.lang.Exception

/**
 * An exception indicating a problem with the account registry.
 */

class AccountProviderDescriptionRegistryException(
  val causes: List<Exception>)
  : Exception(causes[0])
