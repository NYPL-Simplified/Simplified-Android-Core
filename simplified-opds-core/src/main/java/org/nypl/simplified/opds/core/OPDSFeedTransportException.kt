package org.nypl.simplified.opds.core

/**
 * The type of exceptions raised by feed transports.
 */

abstract class OPDSFeedTransportException : Exception {

  /**
   * Construct an exception.
   *
   * @param message The message
   * @param cause   The cause
   */

  constructor(
    message: String,
    cause: Throwable
  ) : super(message, cause)

  /**
   * Construct an exception.
   *
   * @param message The message
   */

  constructor(message: String) : super(message)
}
