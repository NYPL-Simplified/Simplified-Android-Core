package org.librarysimplified.documents.internal

import org.librarysimplified.documents.DocumentType
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.URL

internal abstract class AbstractDocument internal constructor(
  private val http: LSHTTPClientType,
  initialStreams: () -> InputStream,
  private val file: File,
  private val fileTmp: File,
  private val remoteURL: URL
) : DocumentType {

  private val logger =
    LoggerFactory.getLogger(AbstractDocument::class.java)

  init {
    if (!this.file.isFile) {
      this.logger.debug("creating initial file {}", this.file)
      initialStreams.invoke().use { stream ->
        this.fileTmp.outputStream().use { output ->
          stream.copyTo(output)
        }
        this.fileTmp.renameTo(this.file)
      }
    }
  }

  override fun update() {
    this.logger.debug("updating document {} from {}", this.file, this.remoteURL)

    val request =
      this.http.newRequest(this.remoteURL.toURI())
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK -> {
        val stream = status.bodyStream
        if (stream != null) {
          this.fileTmp.outputStream().use { output ->
            stream.copyTo(output)
          }
          this.fileTmp.renameTo(this.file)
          Unit
        } else {
          this.logger.debug("no body")
        }
      }
      is LSHTTPResponseStatus.Responded.Error,
      is LSHTTPResponseStatus.Failed ->
        Unit
    }
  }

  override val readableURL: URL =
    this.file.toURI().toURL()
}
