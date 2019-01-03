package org.nypl.simplified.tests.http;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A trivial implementation of the {@link HTTPType} that simply returns preconfigured responses
 * when requests are made of given URIs.
 */

public final class MockingHTTP implements HTTPType {

  private static final Logger LOG = LoggerFactory.getLogger(MockingHTTP.class);
  private final HashMap<URI, List<HTTPResultType<InputStream>>> responses;

  public MockingHTTP() {
    this.responses = new HashMap<>();
  }

  /**
   * Set that the next request made for {@code uri} will receive {@code result}.
   *
   * @param uri    The request
   * @param result The result
   */

  public void addResponse(
      final URI uri,
      final HTTPResultType<InputStream> result) {
    NullCheck.notNull(uri, "uri");
    NullCheck.notNull(result, "result");

    synchronized (this.responses) {
      final List<HTTPResultType<InputStream>> xs;
      if (this.responses.containsKey(uri)) {
        xs = this.responses.get(uri);
      } else {
        xs = new ArrayList<>();
      }
      xs.add(result);
      this.responses.put(uri, xs);
    }
  }

  /**
   * Set that the next request made for {@code uri} will receive {@code result}.
   *
   * @param uri    The request
   * @param result The result
   */

  public void addResponse(
      final String uri,
      final HTTPResultType<InputStream> result) {
    NullCheck.notNull(uri, "uri");
    NullCheck.notNull(result, "result");
    addResponse(URI.create(uri), result);
  }

  @Override
  public HTTPResultType<InputStream> get(
      final OptionType<HTTPAuthType> auth,
      final URI uri,
      final long offset) {

    LOG.debug("get: {} {} {}", auth, uri, offset);
    return response(uri);
  }

  private HTTPResultType<InputStream> response(final URI uri) {
    synchronized (this.responses) {
      final List<HTTPResultType<InputStream>> xs = this.responses.get(uri);
      if (xs != null && !xs.isEmpty()) {
        return xs.remove(0);
      }
      throw new IllegalStateException("No responses available for " + uri);
    }
  }

  @Override
  public HTTPResultType<InputStream> put(
      final OptionType<HTTPAuthType> auth,
      final URI uri) {

    LOG.debug("put: {} {}", auth, uri);
    return response(uri);
  }

  @Override
  public HTTPResultType<InputStream> post(
      final OptionType<HTTPAuthType> auth,
      final URI uri,
      final byte[] data,
      final String content_type) {

    LOG.debug("post: {} {} {} {}", auth, uri, data, content_type);
    return response(uri);
  }

  @Override
  public HTTPResultType<InputStream> delete(
      final OptionType<HTTPAuthType> auth,
      final URI uri,
      final String content_type) {

    LOG.debug("delete: {} {} {}", auth, uri, content_type);
    return response(uri);
  }

  @Override
  public HTTPResultType<InputStream> head(
      final OptionType<HTTPAuthType> auth,
      final URI uri) {

    LOG.debug("head: {} {}", auth, uri);
    return response(uri);
  }
}
