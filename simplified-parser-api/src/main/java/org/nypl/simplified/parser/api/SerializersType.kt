package org.nypl.simplified.parser.api

import java.io.OutputStream
import java.net.URI

/**
 * A provider of serializers.
 */

interface SerializersType<T> {

  fun createSerializer(
    uri: URI,
    stream: OutputStream,
    document: T
  ): SerializerType
}
