package org.nypl.simplified.books.borrowing.internal

/**
 * The set of exposed borrow error codes for unit testings.
 *
 * Application code _MUST_ not depend on these values for program logic.
 */

object BorrowErrorCodes {
  const val accountsDatabaseException = "accountsDatabaseException"
  const val bookDatabaseFailed = "bookDatabaseFailed"
  const val httpConnectionFailed = "httpConnectionFailed"
  const val httpContentTypeIncompatible = "httpContentTypeIncompatible"
  const val httpEmptyBody: String = "httpEmptyBody"
  const val httpRequestFailed: String = "httpRequestFailed"
  const val noSubtaskAvailable = "noSubtaskAvailable"
  const val noSupportedAcquisitions = "noSupportedAcquisitions"
  const val opdsFeedEntryHoldable = "opdsFeedEntryHoldable"
  const val opdsFeedEntryLoanable = "opdsFeedEntryLoanable"
  const val opdsFeedEntryNoNext = "opdsFeedEntryNoNext"
  const val opdsFeedEntryParseError = "opdsFeedEntryParseError"
  const val requiredURIMissing = "requiredURIMissing"
  const val subtaskFailed = "subtaskFailed"
  const val unexpectedException = "unexpectedException"
}
