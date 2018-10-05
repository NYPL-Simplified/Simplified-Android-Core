package org.nypl.simplified.tests.books;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthMatcherType;
import org.nypl.simplified.http.core.HTTPAuthOAuth;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPProblemReport;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultOK;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An HTTP interface that serves a list of loans when presented with the correct credentials.
 */

public class AuthenticatedHTTP implements HTTPType {

  private final Map<String, List<String>> empty_headers;
  private final AccountBarcode barcode;
  private final AccountPIN pin;
  private final Logger logger;
  private final URI loans_uri;

  public AuthenticatedHTTP(
    final Logger logger,
    final URI loans_uri,
    final AccountBarcode barcode,
    final AccountPIN pin) {
    this.logger = logger;
    this.loans_uri = loans_uri;
    this.empty_headers = new HashMap<>();
    this.barcode = barcode;
    this.pin = pin;
  }

  @Override
  public HTTPResultType<InputStream> get(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri,
    final long offset) {
    this.logger.debug("get: {} {} {}", auth_opt, uri, offset);

    if (uri.equals(this.loans_uri)) {
      this.logger.debug("serving loans");
      return this.getLoans(auth_opt);
    }

    this.logger.debug("serving garbage bytes");
    return new HTTPResultOK<InputStream>(
      "OK",
      200,
      new ByteArrayInputStream("DATA".getBytes()),
      4L,
      empty_headers,
      0L);
  }

  @Override
  public HTTPResultType<InputStream> put(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri) {
    this.logger.debug("put: {} {}", auth_opt, uri);
    return this.get(auth_opt, uri, 0);
  }

  @Override
  public HTTPResultType<InputStream> post(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final byte[] data,
    final String content_type) {
    this.logger.debug("post: {} {} {} {}", auth, uri, data, content_type);
    this.logger.debug("serving garbage bytes");
    return new HTTPResultOK<InputStream>(
      "OK",
      200,
      new ByteArrayInputStream("DATA".getBytes()),
      4L,
      empty_headers,
      0L);
  }

  @Override
  public HTTPResultType<InputStream> delete(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final String content_type) {
    this.logger.debug("post: {} {} {}", auth, uri, content_type);
    this.logger.debug("serving garbage bytes");
    return new HTTPResultOK<InputStream>(
      "OK",
      200,
      new ByteArrayInputStream("DATA".getBytes()),
      4L,
      empty_headers,
      0L);
  }

  private HTTPResultType<InputStream> getLoans(
    final OptionType<HTTPAuthType> auth_opt) {
    if (auth_opt.isNone()) {
      return this.unauthorized();
    }

    final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
    final HTTPAuthType auth = some.get();
    try {
      return auth.matchAuthType(
        new HTTPAuthMatcherType<HTTPResultType<InputStream>, IOException>() {
          private boolean isAuthorized(final HTTPAuthBasic b) {
            boolean ok = b.getUser().equals(barcode.toString());
            ok = ok && b.getPassword().equals(pin.toString());
            logger.debug("isAuthorized: {}", ok);
            return ok;
          }

          @Override
          public HTTPResultType<InputStream> onAuthBasic(final HTTPAuthBasic b)
            throws IOException {
            logger.debug("onAuthBasic: {}", b);

            final boolean ok = this.isAuthorized(b);
            if (!ok) {
              return unauthorized();
            }

            final URL resource_url =
              BooksContract.class.getResource(
                "/org/nypl/simplified/tests/opds/loans.xml");

            logger.debug("onAuthBasic: serving {}", resource_url);

            final InputStream stream = resource_url.openStream();
            return new HTTPResultOK<>(
              "OK",
              200,
              stream,
              1L,
              empty_headers,
              0L);
          }

          @Override
          public HTTPResultType<InputStream> onAuthOAuth(HTTPAuthOAuth b) {
            logger.debug("onAuthOAuth: {}", b);
            return null;
          }
        });
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  @Override
  public HTTPResultType<InputStream> head(
    final OptionType<HTTPAuthType> auth_opt,
    final URI uri) {

    if (uri.equals(this.loans_uri)) {
      return this.headLoans(auth_opt);
    }

    return new HTTPResultOK<InputStream>(
      "OK", 200, new ByteArrayInputStream("DATA".getBytes()), 1L, empty_headers, 0L);
  }

  private HTTPResultType<InputStream> headLoans(
    final OptionType<HTTPAuthType> auth_opt) {
    if (auth_opt.isNone()) {
      return this.unauthorized();
    }

    final Some<HTTPAuthType> some = (Some<HTTPAuthType>) auth_opt;
    final HTTPAuthType auth = some.get();
    try {
      return auth.matchAuthType(
        new HTTPAuthMatcherType<HTTPResultType<InputStream>, IOException>() {
          private boolean isAuthorized(final HTTPAuthBasic b) {
            boolean ok = b.getUser().equals(barcode.toString());
            ok = ok && b.getPassword().equals(pin.toString());
            return ok;
          }

          @Override
          public HTTPResultType<InputStream> onAuthBasic(final HTTPAuthBasic b) {
            final boolean ok = this.isAuthorized(b);
            if (ok == false) {
              return unauthorized();
            }

            return new HTTPResultOK<InputStream>(
              "OK", 200, new ByteArrayInputStream("DATA".getBytes()), 1L, empty_headers, 0L);
          }

          @Override
          public HTTPResultType<InputStream> onAuthOAuth(HTTPAuthOAuth b) {
            return null;
          }
        });
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  private <T> HTTPResultType<T> unauthorized() {
    final OptionType<HTTPProblemReport> report = Option.none();
    return new HTTPResultError<>(
      401,
      "Unauthorized",
      0L,
      empty_headers,
      0L,
      new ByteArrayInputStream(new byte[0]),
      report);
  }
}
