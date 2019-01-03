package org.nypl.simplified.books.synced_document;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.clock.ClockType;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * The default implementation of the {@link SyncedDocumentType} interface.
 */

public final class SyncedDocument extends SyncedDocumentAbstract
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(SyncedDocument.class));
  }

  private SyncedDocument(
    final ClockType in_clock,
    final HTTPType in_http,
    final ExecutorService in_exec,
    final File in_current,
    final File in_current_tmp,
    final File in_meta,
    final File in_meta_tmp,
    final long in_fetch_last_success)
  {
    super(
      in_clock,
      in_http,
      in_exec,
      in_current,
      in_current_tmp,
      in_meta,
      in_meta_tmp,
      in_fetch_last_success);
  }

  /**
   * Construct a new synced document. The document will be placed in {@code
   * directory} with base name {@code name}. The initial content of the file if
   * it does not already exist will be {@code initial}.
   *
   * @param in_clock  The clock
   * @param in_http   An HTTP interface
   * @param in_exec   An executor
   * @param directory The directory
   * @param name      The base name
   * @param initial   A function that, when evaluated, yields a stream of data
   *
   * @return A synced document
   *
   * @throws IOException If the document does not exist and cannot be created
   */

  public static SyncedDocumentType newDocument(
    final ClockType in_clock,
    final HTTPType in_http,
    final ExecutorService in_exec,
    final File directory,
    final String name,
    final FunctionType<Unit, InputStream> initial)
    throws IOException
  {
    NullCheck.notNull(in_clock);
    NullCheck.notNull(in_http);
    NullCheck.notNull(in_exec);
    NullCheck.notNull(directory);
    NullCheck.notNull(name);
    NullCheck.notNull(initial);

    final File current = new File(directory, name);
    final File current_tmp = new File(directory, name + ".tmp");
    final File meta = new File(directory, name + ".meta");
    final File meta_tmp = new File(directory, name + ".meta.tmp");

    if (current.isFile() == false) {
      FileUtilities.fileWriteStreamAtomically(
        current, current_tmp, initial.call(Unit.unit()));
    }

    long fetched_last = 0;
    if (meta.isFile()) {
      try {
        final Pair<URI, Long> meta_data = SyncedDocumentAbstract.readMeta(meta);
        fetched_last = meta_data.getRight().longValue();
      } catch (final IOException e) {
        SyncedDocument.LOG.error("could not read metadata: ", e);
      }
    }

    return new SyncedDocument(
      in_clock,
      in_http,
      in_exec,
      current,
      current_tmp,
      meta,
      meta_tmp,
      fetched_last);
  }

  @Override protected void documentOnReceipt(
    final int status,
    final InputStream data,
    final long length,
    final String type,
    final File output)
    throws IOException
  {
    if (status >= 200 && status < 300) {
      FileUtilities.fileWriteStream(output, data);
    } else {
      throw new IOException(
        String.format("Server returned status %d", status));
    }
  }
}
