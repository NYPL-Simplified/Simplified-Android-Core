package org.nypl.simplified.accounts.api

import java.net.URI

/**
 * The type of receivers of account resolution status messages.
 */

typealias AccountProviderResolutionListenerType =
  (accountProvider: URI, message: String) -> Unit
