package org.nypl.simplified.profiles.controller.api

/**
 * Profile account deletion strings.
 */

interface ProfileAccountDeletionStringResourcesType {

  /**
   * Deleting an account succeeded.
   */

  val deletionSucceeded: String

  /**
   * Deleting an account failed due to an unexpected exception.
   */

  val unexpectedException: String

  /**
   * Deleting an account failed.
   */

  val deletionFailed: String

  /**
   * Deleting an account failed because there's only one account.
   */

  val onlyOneAccountRemaining: String

  /**
   * Deleting account...
   */

  val deletingAccount: String
}