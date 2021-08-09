package org.nypl.simplified.tests.mocking

import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.formats.api.BookFormatSupportType

class MockBookFormatSupport : BookFormatSupportType {

  var onIsSupportedFinalContentType: (MIMEType) -> Boolean = { mime ->
    true
  }

  var onIsSupportedPath: (List<MIMEType>) -> Boolean = { mimes ->
    true
  }

  override fun isSupportedFinalContentType(mime: MIMEType): Boolean =
    this.onIsSupportedFinalContentType(mime)

  override fun isSupportedPath(typePath: List<MIMEType>): Boolean =
    this.onIsSupportedPath(typePath)

  override fun isDRMSupported(drmKind: BookDRMKind): Boolean {
    return true
  }
}
