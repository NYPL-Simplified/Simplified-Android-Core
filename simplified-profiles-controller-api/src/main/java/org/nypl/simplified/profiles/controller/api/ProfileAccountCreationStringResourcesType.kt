package org.nypl.simplified.profiles.controller.api

/**
 * Profile account creation strings.
 */

interface ProfileAccountCreationStringResourcesType {

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