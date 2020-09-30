package org.nypl.simplified.books.borrowing.internal

/**
 * The set of exposed borrow error codes for unit testings.
 *
 * Application code _MUST_ not depend on these values for program logic.
 */

object BorrowErrorCodes {
  const val accountCredentialsRequired = "accountCredentialsRequired"
  const val accountsDatabaseException = "accountsDatabaseException"
  const val acsNoCredentialsPost = "acsNoCredentialsPost"
  const val acsNoCredentialsPre = "acsNoCredentialsPre"
  const val acsNotSupported = "acsNotSupported"
  const val acsTimedOut = "acsTimedOut"
  const val acsUnparseableACSM = "acsUnparseableACSM"
  const val audioStrategyFailed = "audioStrategyFailed"
  const val bookDatabaseFailed = "bookDatabaseFailed"
  const val contentFileNotFound = "contentFileNotFound"
  const val httpConnectionFailed = "httpConnectionFailed"
  const val httpContentTypeIncompatible = "httpContentTypeIncompatible"
  const val httpRequestFailed = "httpRequestFailed"
  const val noFormatHandle = "noFormatHandle"
  const val noSubtaskAvailable = "noSubtaskAvailable"
  const val noSupportedAcquisitions = "noSupportedAcquisitions"
  const val opdsFeedEntryHoldable = "opdsFeedEntryHoldable"
  const val opdsFeedEntryLoanable = "opdsFeedEntryLoanable"
  const val opdsFeedEntryNoNext = "opdsFeedEntryNoNext"
  const val opdsFeedEntryParseError = "opdsFeedEntryParseError"
  const val profileNotFound = "profileNotFound"
  const val requiredURIMissing = "requiredURIMissing"
  const val subtaskFailed = "subtaskFailed"
  const val unexpectedException = "unexpectedException"
}
