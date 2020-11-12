package org.librarysimplified.documents.internal

import org.librarysimplified.http.api.LSHTTPClientType
import java.io.File
import java.io.InputStream
import java.net.URL

internal class PlainDocument internal constructor(
  http: LSHTTPClientType,
  initialStreams: () -> InputStream,
  file: File,
  fileTmp: File,
  remoteURL: URL
) : AbstractDocument(
  http = http,
  initialStreams = initialStreams,
  file = file,
  fileTmp = fileTmp,
  remoteURL = remoteURL
)
