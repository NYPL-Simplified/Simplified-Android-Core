package org.nypl.simplified.presentableerror.api

/**
 * An abstract exception class that is also a presentable error.
 */

abstract class PresentableException(
  override val message: String,
  override val attributes: Map<String, String> = mapOf(),
  override val cause: Exception? = null,
  override val causes: List<PresentableErrorType> = listOf())
  : Exception(message, cause), PresentableErrorType
