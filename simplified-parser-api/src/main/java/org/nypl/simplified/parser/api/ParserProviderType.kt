package org.nypl.simplified.parser.api

import java.io.InputStream
import java.net.URI

/**
 * A generic provider of parsers.
 */

interface ParserProviderType<T> {

  fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean = false
  ): ParserType<T>
}
