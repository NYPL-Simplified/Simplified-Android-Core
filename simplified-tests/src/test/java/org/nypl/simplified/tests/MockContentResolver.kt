package org.nypl.simplified.tests

import org.nypl.simplified.content.api.ContentResolverType
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

class MockContentResolver : ContentResolverType {

  var queue = mutableListOf<ByteArray>()

  override fun openInputStream(uri: URI): InputStream? {
    if (this.queue.isEmpty()) {
      return null
    }
    return ByteArrayInputStream(this.queue.removeAt(0))
  }

  fun enqueue(data: ByteArray) {
    this.queue.add(0, data)
  }
}
