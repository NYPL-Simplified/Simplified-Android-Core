package org.nypl.simplified.content.api

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream
import java.net.URI

/**
 * A sane content resolver.
 */

class ContentResolverSane(
  private val delegate: ContentResolver
) : ContentResolverType {

  override fun openInputStream(uri: URI): InputStream? {
    return if (uri.scheme == "content") {
      this.delegate.openInputStream(Uri.parse(uri.toString()))
    } else {
      throw IllegalArgumentException("$uri is not a content URI")
    }
  }
}
