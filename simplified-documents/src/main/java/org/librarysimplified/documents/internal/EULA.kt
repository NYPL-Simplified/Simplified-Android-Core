package org.librarysimplified.documents.internal

import org.librarysimplified.documents.EULAType
import org.librarysimplified.http.api.LSHTTPClientType
import java.io.File
import java.io.InputStream
import java.net.URL

internal class EULA internal constructor(
  http: LSHTTPClientType,
  initialStreams: () -> InputStream,
  file: File,
  fileTmp: File,
  private val fileAgreed: File,
  remoteURL: URL
) : AbstractDocument(
  http = http,
  initialStreams = initialStreams,
  file = file,
  fileTmp = fileTmp,
  remoteURL = remoteURL
),
  EULAType {

  override var hasAgreed: Boolean
    get() = this.fileAgreed.isFile
    set(value) {
      if (value) {
        this.fileAgreed.writeText("")
      } else {
        this.fileAgreed.delete()
      }
    }
}
