package org.nypl.simplified.books.formats

/**
 * Information about which formats are supported by the current application configuration.
 */

data class BookFormatSupportParameters(

  /**
   * The application is configured to support PDF files. That is, there is a [org.nypl.simplified.viewer.spi.ViewerProviderType]
   * registered that supports PDFs.
   */

  val supportsPDF: Boolean,

  /**
   * The application has a working Adobe DRM service.
   */

  val supportsAdobeDRM: Boolean,

  /**
   * The application has a working AxisNow service.
   */

  val supportsAxisNow: Boolean,

  /**
   * The application is configured to support audio books. That is, there is a [org.nypl.simplified.viewer.spi.ViewerProviderType]
   * registered that supports audio books.
   */

  val supportsAudioBooks: BookFormatAudioSupportParameters?,

  /**
   * The application has a working LCP service.
   */

  val supportsLCP: Boolean
)
