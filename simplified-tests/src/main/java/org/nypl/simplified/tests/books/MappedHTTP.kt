package org.nypl.simplified.tests.books

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPResultType
import org.nypl.simplified.http.core.HTTPType
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

class MappedHTTP(
  val logger: Logger,
  val content: MutableMap<String, HTTPResultType<InputStream>> = mutableMapOf()
) : HTTPType {

  data class MappedResource(
    val stream: InputStream,
    val size: Long
  )

  fun addResource(uri: String, resource: MappedResource, contentType: String = "application/octet-stream") {
    this.content[uri] =
      HTTPResultOK(
        "OK",
        200,
        resource.stream,
        resource.size,
        mutableMapOf(Pair("Content-Type", listOf(contentType))),
        0L
      )
  }

  override fun get(auth: OptionType<HTTPAuthType>, uri: URI, offset: Long): HTTPResultType<InputStream> {
    this.logger.debug("get: {} {} {}", auth, uri, offset)
    return this.content[uri.toASCIIString()] ?: this.notFound(uri)
  }

  override fun get(
    auth: OptionType<HTTPAuthType>?,
    uri: URI,
    offset: Long,
    noCache: Boolean?
  ): HTTPResultType<InputStream> {
    this.logger.debug("get: {} {} {}", auth, uri, offset)
    return this.content[uri.toASCIIString()] ?: this.notFound(uri)
  }

  private fun notFound(uri: URI): HTTPResultError<InputStream> {
    this.logger.debug("NOT FOUND: {}", uri)
    return HTTPResultError(
      404,
      "NOT FOUND",
      0L, mapOf(),
      0L,
      ByteArrayInputStream(ByteArray(1)),
      Option.none()
    )
  }

  override fun put(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    data: ByteArray,
    content_type: String
  ): HTTPResultType<InputStream> {
    this.logger.debug("put: {} {} {} {}", auth, uri, data, content_type)
    return this.content[uri.toASCIIString()] ?: this.notFound(uri)
  }

  override fun put(auth: OptionType<HTTPAuthType>, uri: URI): HTTPResultType<InputStream> {
    this.logger.debug("put: {} {}", auth, uri)
    return this.content[uri.toASCIIString()] ?: this.notFound(uri)
  }

  override fun post(auth: OptionType<HTTPAuthType>, uri: URI, data: ByteArray, content_type: String): HTTPResultType<InputStream> {
    this.logger.debug("post: {} {} {} {}", auth, uri, data, content_type)
    return this.content[uri.toASCIIString()] ?: this.notFound(uri)
  }

  override fun delete(auth: OptionType<HTTPAuthType>, uri: URI, content_type: String): HTTPResultType<InputStream> {
    this.logger.debug("delete: {} {} {}", auth, uri, content_type)
    return this.content[uri.toASCIIString()] ?: this.notFound(uri)
  }

  override fun head(auth: OptionType<HTTPAuthType>, uri: URI): HTTPResultType<InputStream> {
    this.logger.debug("head: {} {} {}", auth, uri)
    return this.content[uri.toASCIIString()] ?: this.notFound(uri)
  }
}
