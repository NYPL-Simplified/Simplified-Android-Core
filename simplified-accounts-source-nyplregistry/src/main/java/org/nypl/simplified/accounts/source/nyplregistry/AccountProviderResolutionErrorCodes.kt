package org.nypl.simplified.accounts.source.nyplregistry

import org.nypl.simplified.accounts.api.AccountProviderDescription
import java.net.URI

/**
 * Error codes raised during account resolution. Applications MUST NOT depend on these
 * error codes to implement program logic; they are solely exposed here to facilitate
 * unit testing.
 */

object AccountProviderResolutionErrorCodes {

  fun unexpectedException(
    description: AccountProviderDescription
  ): String {
    return "unexpectedException ${description.id} ${description.title}"
  }

  fun authDocumentUnusable(
    description: AccountProviderDescription
  ): String {
    return "authDocumentUnusable ${description.id} ${description.title}"
  }

  fun authDocumentUnusableLink(
    description: AccountProviderDescription
  ): String {
    return "authDocumentUnusableLink ${description.id} ${description.title}"
  }

  fun authDocumentParseFailed(
    description: AccountProviderDescription
  ): String {
    return "authDocumentParseFailed ${description.id} ${description.title}"
  }

  fun httpRequestFailed(
    uri: URI?,
    status: Int,
    message: String
  ): String {
    return "httpRequestFailed $uri $status $message"
  }
}
