package org.nypl.simplified.tests

import org.nypl.simplified.books.bundled.api.BundledContentResolverType
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI

class MockBundledContentResolver : BundledContentResolverType {

  var queue = mutableListOf<ByteArray>()

  fun enqueue(data: ByteArray) {
    this.queue.add(0, data)
  }

  override fun resolve(uri: URI): InputStream {
    if (this.queue.isEmpty()) {
      throw FileNotFoundException(uri.toString())
    }
    return ByteArrayInputStream(this.queue.removeAt(0))
  }
}
