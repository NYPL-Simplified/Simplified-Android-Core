package org.nypl.simplified.documents.eula;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.documents.clock.ClockType;
import org.nypl.simplified.documents.synced.SyncedDocument;
import org.nypl.simplified.documents.synced.SyncedDocumentType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The default implementation of the {@link EULAType} interface.
 */

public final class EULA implements EULAType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(EULA.class));
  }

  private final SyncedDocumentType actual;
  private final ExecutorService    exec;
  private final File               file_agreed;
  private final AtomicBoolean      agreed;

  private EULA(
    final ExecutorService in_exec,
    final SyncedDocumentType in_actual,
    final File in_agreed)
  {
    this.actual = NullCheck.notNull(in_actual);
    this.exec = in_exec;
    this.file_agreed = in_agreed;
    this.agreed = new AtomicBoolean(in_agreed.exists());
  }

  /**
   * Construct a new EULA document.
   *
   * @param clock   The clock
   * @param http    An HTTP interface
   * @param exec    An executor
   * @param base    The base directory
   * @param initial A function yielding an initial EULA text
   *
   * @return A document
   *
   * @throws IOException On I/O errors
   */

  public static EULAType newEULA(
    final ClockType clock,
    final HTTPType http,
    final ExecutorService exec,
    final File base,
    final FunctionType<Unit, InputStream> initial)
    throws IOException
  {
    final SyncedDocumentType sd =
      SyncedDocument.newDocument(clock, http, exec, base, "eula.html", initial);
    final File agree = new File(base, "eula_agreed.dat");
    return new EULA(exec, sd, agree);
  }

  @Override public URL documentGetReadableURL()
  {
    return this.actual.documentGetReadableURL();
  }

  @Override public void documentSetLatestURL(final URL u)
  {
    this.actual.documentSetLatestURL(u);
  }

  @Override public boolean eulaHasAgreed()
  {
    return this.agreed.get();
  }

  @Override public void eulaSetHasAgreed(final boolean t)
  {
    this.agreed.set(t);
    this.exec.submit(
      new Runnable()
      {
        @Override public void run()
        {
          try {
            EULA.this.setFile(t);
          } catch (final IOException e) {
            EULA.LOG.error("could not save agreement flag file: ", e);
          }
        }
      });

  }

  /**
   * The presence of the empty agreement file indicates that the user agreed.
   */

  private void setFile(final boolean t)
    throws IOException
  {
    if (t) {
      this.file_agreed.createNewFile();
    } else {
      this.file_agreed.delete();
    }
  }
}
