package org.nypl.simplified.tests.books

import org.nypl.simplified.books.formats.BookFormatAudioSupportParameters
import org.nypl.simplified.books.formats.BookFormatSupport
import org.nypl.simplified.books.formats.BookFormatSupportParameters

object BookFormatsTesting {

  val supportsEverything =
    BookFormatSupport.create(
      BookFormatSupportParameters(
        supportsLCP = true,
        supportsAudioBooks = BookFormatAudioSupportParameters(
          supportsDPLAAudioBooks = true,
          supportsFindawayAudioBooks = true,
          supportsOverdriveAudioBooks = true
        ),
        supportsAxisNow = true,
        supportsPDF = true,
        supportsAdobeDRM = true
      )
    )

  val supportsNothing =
    BookFormatSupport.create(
      BookFormatSupportParameters(
        supportsLCP = false,
        supportsAudioBooks = null,
        supportsAxisNow = false,
        supportsPDF = false,
        supportsAdobeDRM = false
      )
    )
}
