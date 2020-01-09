package org.nypl.simplified.profiles.controller.api

/**
 * Profile account creation strings.
 */

interface ProfileAccountCreationStringResourcesType {

  val creatingAnAccountProviderDescription: String

  val searchingFeedForAuthenticationDocument: String

  val fetchingOPDSFeedFailed: String

  val parsingOPDSFeedFailed: String

  val parsingOPDSFeed: String

  val fetchingOPDSFeed: String

  /**
   * Locating the authentication document URI...
   */

  val findingAuthDocumentURI: String

  /**
   * Creating an account succeeded.
   */

  val creatingAccountSucceeded: String

  /**
   * Creating an account failed due to an unexpected exception.
   */

  val unexpectedException: String

  /**
   * Creating an account failed.
   */

  val creatingAccountFailed: String

  /**
   * Creating an account...
   */

  val creatingAccount: String

  /**
   * Resolving an account provider failed.
   */

  val resolvingAccountProviderFailed: String

  /**
   * Resolving an account provider...
   */

  val resolvingAccountProvider: String
}
