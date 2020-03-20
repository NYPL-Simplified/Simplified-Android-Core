package org.nypl.simplified.documents.synced;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NonNull;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.clock.ClockType;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract implementation of the {@link SyncedDocumentType} interface.
 */

public abstract class SyncedDocumentAbstract implements SyncedDocumentType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(SyncedDocumentAbstract.class));
  }

  private final File            current;
  private final File            meta;
  private final File            current_tmp;
  private final AtomicLong      fetch_last_success;
  private final ClockType clock;
  private final HTTPType        http;
  private final ExecutorService exec;
  private final AtomicBoolean   fetch_in_progress;
  private final File            meta_tmp;
  private       URL             current_url;

  protected SyncedDocumentAbstract(
    final ClockType in_clock,
    final HTTPType in_http,
    final ExecutorService in_exec,
    final File in_current,
    final File in_current_tmp,
    final File in_meta,
    final File in_meta_tmp,
    final long in_fetch_last_success)
  {
    try {
      this.clock = NullCheck.notNull(in_clock);
      this.http = NullCheck.notNull(in_http);
      this.exec = NullCheck.notNull(in_exec);
      this.current = NullCheck.notNull(in_current);
      this.current_tmp = NullCheck.notNull(in_current_tmp);
      this.meta = NullCheck.notNull(in_meta);
      this.meta_tmp = NullCheck.notNull(in_meta_tmp);
      this.current_url = this.current.toURI().toURL();
      this.fetch_last_success = new AtomicLong(in_fetch_last_success);
      this.fetch_in_progress = new AtomicBoolean(false);
    } catch (final MalformedURLException e) {
      throw new UnreachableCodeException(e);
    }
  }

  protected static Pair<URI, Long> readMeta(final File meta)
    throws IOException
  {
    NullCheck.notNull(meta);
    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode o =
      JSONParserUtilities.checkObject(null, jom.readTree(meta));
    final URI uri = JSONParserUtilities.getURI(o, "url");
    final Long latest =
      JSONParserUtilities.getBigInteger(o, "last-fetch").longValue();
    return Pair.pair(uri, latest);
  }

  protected abstract void documentOnReceipt(
    int status,
    InputStream data,
    String type,
    File output)
    throws IOException;

  @Override public synchronized URL documentGetReadableURL()
  {
    try {
      return this.current.toURI().toURL();
    } catch (final MalformedURLException e) {
      throw new UnreachableCodeException(e);
    }
  }

  @Override public synchronized void documentSetLatestURL(final URL u)
  {
    NullCheck.notNull(u);

    try {
      final URI c_text = this.current_url.toURI();
      final URI u_text = u.toURI();

      if (c_text.equals(u_text) == false) {
        this.current_url = u;
        SyncedDocumentAbstract.LOG.debug(
          "new URL {}, fetch_in_progress {}", u, this.fetch_in_progress.get());
        this.runCheck(u);
      } else {

        /**
         * If a fetch is not currently in progress, then check to see
         * if enough time has elapsed since the last time the document
         * was successfully fetched. If so, fetch it.
         */

        if (this.fetch_in_progress.compareAndSet(false, true)) {
          final long diff =
            this.clock.clockNow().getMillis() - this.fetch_last_success.get();
          if (diff >= 86400) {
            SyncedDocumentAbstract.LOG.debug(
              "time difference {} >= 86400, fetch_in_progress {}",
              diff,
              this.fetch_in_progress.get());
            this.runCheck(u);
          } else {
            this.fetch_in_progress.set(false);
          }
        }
      }
    } catch (final URISyntaxException e) {
      SyncedDocumentAbstract.LOG.error("invalid URI {}, ignoring: ", e);
    }
  }

  private void runCheck(final URL u)
  {
    this.exec.submit(
      new Runnable()
      {
        @Override public void run()
        {
          try {
            SyncedDocumentAbstract.this.fetch(u);
          } catch (final Throwable e) {
            SyncedDocumentAbstract.LOG.error("fetch: {} failed: ", u, e);
          } finally {
            SyncedDocumentAbstract.this.fetch_in_progress.set(false);
          }
        }
      });
  }

  private void fetch(final URL u)
    throws URISyntaxException, IOException
  {
    SyncedDocumentAbstract.LOG.debug("fetch_in_progress {}", u);

    final OptionType<HTTPAuthType> no_auth = Option.none();
    this.http.get(no_auth, u.toURI(), 0L).matchResult(
      new HTTPResultMatcherType<InputStream, Unit, IOException>()
      {
        @Override public Unit onHTTPError(final HTTPResultError<InputStream> e)
          throws IOException
        {
          SyncedDocumentAbstract.this.onHTTPError(e, u);
          return Unit.unit();
        }

        @Override
        public Unit onHTTPException(final HTTPResultException<InputStream> e)
          throws IOException
        {
          SyncedDocumentAbstract.LOG.error(
            "could not connect to server: ", e.getError());
          return Unit.unit();
        }

        @Override public Unit onHTTPOK(final HTTPResultOKType<InputStream> e)
          throws IOException
        {
          SyncedDocumentAbstract.this.onHTTPOK(e, u);
          return Unit.unit();
        }
      });
  }

  private void onHTTPError(
    final HTTPResultError<InputStream> e,
    final URL u)
    throws IOException
  {
    final String type = this.getContentType(e.getResponseHeaders());
    this.documentOnReceipt(
      e.getStatus(), e.getData(), type, this.current_tmp);
    this.saveResults(u);
  }

  @NonNull
  private String getContentType(final Map<String, List<String>> headers)
  {
    final List<String> types = headers.get("Content-Type");
    final String type;
    if (types.size() > 0) {
      type = NullCheck.notNull(types.get(0));
    } else {
      type = "application/octet-stream";
    }
    return type;
  }

  private void onHTTPOK(
    final HTTPResultOKType<InputStream> e,
    final URL u)
    throws IOException
  {
    final String type = this.getContentType(e.getResponseHeaders());

    this.documentOnReceipt(
      e.getStatus(),
      e.getValue(),
      type,
      this.current_tmp);

    this.saveResults(u);
  }

  private void saveResults(final URL u)
    throws IOException
  {
    FileUtilities.fileRename(this.current_tmp, this.current);
    final long now = this.clock.clockNow().getMillis();
    this.writeMeta(u, now);
    this.fetch_last_success.set(now);
  }

  private void writeMeta(
    final URL u,
    final long now)
    throws IOException
  {
    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode o = jom.createObjectNode();
    o.put("url", u.toString());
    o.put("last-fetch", now);

    final ByteArrayOutputStream bao = new ByteArrayOutputStream();
    final ObjectWriter jw = jom.writerWithDefaultPrettyPrinter();
    jw.writeValue(bao, o);

    FileUtilities.fileWriteBytesAtomically(
      this.meta, this.meta_tmp, bao.toByteArray());
  }
}
