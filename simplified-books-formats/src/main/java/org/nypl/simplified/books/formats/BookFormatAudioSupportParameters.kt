package org.nypl.simplified.books.formats

/**
 * Information about which audio book formats are supported by the current application configuration.
 */

data class BookFormatAudioSupportParameters(

  /**
   * The application is configured to support Findaway audio books.
   */

  val supportsFindawayAudioBooks: Boolean,

  /**
   * The application is configured to support Overdrive audio books.
   */

  val supportsOverdriveAudioBooks: Boolean,

  /**
   * The application is configured to support DPLA audio books.
   */

  val supportsDPLAAudioBooks: Boolean
)
