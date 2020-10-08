package org.nypl.simplified.tests.books

import com.io7m.jfunctional.OptionType

import org.nypl.simplified.http.core.HTTPAuthType
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultType
import org.nypl.simplified.http.core.HTTPType

import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * An HTTP interface that raises an exception on every operation.
 */

open class CrashingHTTP : HTTPType {

  override fun put(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    data: ByteArray,
    content_type: String
  ): HTTPResultType<InputStream> {
    return HTTPResultException(uri, IOException())
  }

  override fun get(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    offset: Long
  ): HTTPResultType<InputStream> {
    return HTTPResultException(uri, IOException())
  }

  override fun get(
    auth: OptionType<HTTPAuthType>?,
    uri: URI?,
    offset: Long,
    noCache: Boolean?
  ): HTTPResultType<InputStream> {
    return HTTPResultException(uri, IOException())
  }

  override fun put(
    auth: OptionType<HTTPAuthType>,
    uri: URI
  ): HTTPResultType<InputStream> {
    return HTTPResultException(uri, IOException())
  }

  override fun head(
    auth: OptionType<HTTPAuthType>,
    uri: URI
  ): HTTPResultType<InputStream> {
    return HTTPResultException(uri, IOException())
  }

  override fun post(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    data: ByteArray,
    content_type: String
  ): HTTPResultType<InputStream> {
    return HTTPResultException(uri, IOException())
  }

  override fun delete(
    auth: OptionType<HTTPAuthType>,
    uri: URI,
    content_type: String
  ): HTTPResultType<InputStream> {
    return HTTPResultException(uri, IOException())
  }
}
