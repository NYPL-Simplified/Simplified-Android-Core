package org.nypl.simplified.tests.http

import com.io7m.jfunctional.OptionType
import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPResultType
import org.nypl.simplified.http.core.HTTPType
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.HashMap

/**
 * A trivial implementation of the [HTTPType] that simply returns preconfigured responses
 * when requests are made of given URIs.
 */

class MockingHTTP : HTTPType {

  private val responses: HashMap<URI, MutableList<HTTPResultType<InputStream>>>

  init {
    this.responses = HashMap()
  }

  fun responsesNow(): Map<URI, List<HTTPResultType<InputStream>>> =
    synchronized(this.responses) {
      this.responses.toMap()
    }

  /**
   * Set that the next request made for `uri` will receive `result`.
   *
   * @param uri The request
   * @param result The result
   */

  fun addResponse(
    uri: URI,
    result: HTTPResultType<InputStream>
  ) {
    synchronized(this.responses) {
      val xs: MutableList<HTTPResultType<InputStream>>
      if (this.responses.containsKey(uri)) {
        xs = this.responses[uri]!!
      } else {
        xs = ArrayList()
      }
      xs.add(result)
      this.responses.put(uri, xs)
    }
  }

  /**
   * Set that the next request made for `uri` will receive `result`.
   *
   * @param uri The request
   * @param result The result
   */

  fun addResponse(
    uri: String,
    result: HTTPResultType<InputStream>
  ) {
    addResponse(URI.create(uri), result)
  }

  override fun get(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    offset: Long
  ): HTTPResultType<InputStream> {
    LOG.debug("get: {} {} {}", auth, uri, offset)
    return response(uri)
  }

  override fun get(
    auth: OptionType<HTTPAuthType>?,
    uri: URI,
    offset: Long,
    noCache: Boolean?
  ): HTTPResultType<InputStream> {
    LOG.debug("get: {} {} {}", auth, uri, offset)
    return response(uri)
  }

  private fun response(uri: URI): HTTPResultType<InputStream> {
    synchronized(this.responses) {
      val xs = this.responses[uri]
      if (xs != null && !xs.isEmpty()) {
        return xs.removeAt(0)
      }
      throw IllegalStateException("No responses available for $uri")
    }
  }

  override fun put(
    auth: OptionType<HTTPAuthType>,
    uri: URI
  ): HTTPResultType<InputStream> {
    LOG.debug("put: {} {}", auth, uri)
    return response(uri)
  }

  override fun put(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    data: ByteArray,
    content_type: String
  ): HTTPResultType<InputStream> {
    LOG.debug("put: {} {} {} {}", auth, uri, data, content_type)
    return response(uri)
  }

  override fun post(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    data: ByteArray,
    content_type: String
  ): HTTPResultType<InputStream> {
    LOG.debug("post: {} {} {} {}", auth, uri, data, content_type)
    return response(uri)
  }

  override fun delete(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    content_type: String
  ): HTTPResultType<InputStream> {
    LOG.debug("delete: {} {} {}", auth, uri, content_type)
    return response(uri)
  }

  override fun head(
    auth: OptionType<HTTPAuthType>,
    uri: URI
  ): HTTPResultType<InputStream> {
    LOG.debug("head: {} {}", auth, uri)
    return response(uri)
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(MockingHTTP::class.java)
  }
}
