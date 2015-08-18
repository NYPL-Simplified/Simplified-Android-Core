package org.nypl.simplified.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The abstract implementation of the {@link SyncedDocumentType}.
 */

abstract class SyncedDocumentAbstract implements SyncedDocumentType
{
  private final SharedPreferences prefs;
  private final ExecutorService   exec;
  private final HTTPType          http;
  private final AtomicBoolean     fetching;
  private final File              stored;
  private final File              stored_tmp;
  private final URL               stored_url;
  private final String            basename;
  private final URL               asset_url;
  private final Logger            log;

  protected SyncedDocumentAbstract(
    final Logger in_log,
    final HTTPType in_http,
    final ExecutorService in_exec,
    final SharedPreferences in_prefs,
    final File in_stored,
    final String in_basename)
  {
    try {
      this.log = NullCheck.notNull(in_log);
      this.http = NullCheck.notNull(in_http);
      this.exec = NullCheck.notNull(in_exec);
      this.prefs = NullCheck.notNull(in_prefs);
      this.stored = NullCheck.notNull(in_stored);
      this.basename = NullCheck.notNull(in_basename);

      this.stored_tmp = new File(this.stored.toString() + ".tmp");
      this.stored_url = new URL("file://" + this.stored);
      this.asset_url =
        new URL(String.format("file:///android_asset/%s.html", in_basename));

      this.fetching = new AtomicBoolean(false);
    } catch (final MalformedURLException e) {
      throw new UnreachableCodeException(e);
    }
  }

  protected static SharedPreferences getPreferencesForBaseName(
    final Context context,
    final String name)
  {
    NullCheck.notNull(context);
    NullCheck.notNull(name);
    return context.getSharedPreferences(name, 0);
  }

  protected static File getStoredFileForBaseName(
    final Context context,
    final String name)
  {
    NullCheck.notNull(context);
    NullCheck.notNull(name);
    return new File(context.getCacheDir(), String.format("%s.html", name));
  }

  protected static boolean baseFileExists(
    final Logger log,
    final Context context,
    final String name)
  {
    NullCheck.notNull(log);
    NullCheck.notNull(context);
    NullCheck.notNull(name);

    final String file = String.format("%s.html", name);
    final File stored = new File(context.getCacheDir(), file);

    final AssetManager assets = context.getAssets();
    try {
      final InputStream is = assets.open(file, 0);
      try {
        return true;
      } finally {
        is.close();
      }
    } catch (final IOException e) {
      log.debug("could not open {}: ", file, e);
      return false;
    }
  }

  private static long now()
  {
    return Calendar.getInstance().getTime().getTime();
  }

  @Override public URL documentGetReadableURL()
  {
    final URL u = this.getCurrentURL();
    this.log.debug("current url {}", u);
    return u;
  }

  @Override public String documentGetBaseName()
  {
    return this.basename;
  }

  @Override public void documentSetLatestURI(final URI u)
  {
    NullCheck.notNull(u);

    if (this.fetchIsRequired(u)) {
      if (this.fetching.compareAndSet(false, true)) {
        this.exec.execute(
          new Runnable()
          {
            @Override public void run()
            {
              SyncedDocumentAbstract.this.fetchURI(u);
            }
          });
      }
    }
  }

  private boolean fetchIsRequired(final URI u)
  {
    final String existing = this.prefs.getString("uri-latest", null);
    final String u_text = u.toString();
    final boolean is_new = u_text.equals(existing) == false;
    if (is_new) {
      this.log.debug(
        "uri {} is new (old {})", u_text, existing);
      return true;
    }

    final long time = this.prefs.getLong("check-time", 0);
    if (SyncedDocumentAbstract.now() - time > 86400) {
      this.log.debug(
        "check has not been attempted for {} seconds", 86400);
      return true;
    }
    return false;
  }

  private void fetchURI(final URI u)
  {
    try {
      this.log.debug("fetching {}", u);

      final OptionType<HTTPAuthType> no_auth = Option.none();
      final HTTPResultType<InputStream> r = this.http.get(no_auth, u, 0L);
      r.matchResult(
        new HTTPResultMatcherType<InputStream, Unit, IOException>()
        {
          @Override
          public Unit onHTTPError(final HTTPResultError<InputStream> e)
            throws IOException
          {
            throw new IOException(
              String.format(
                "Server error for %s: %d (%s)",
                u,
                e.getStatus(),
                e.getMessage()));
          }

          @Override
          public Unit onHTTPException(final HTTPResultException<InputStream> e)
            throws IOException
          {
            throw new IOException(e.getError());
          }

          @Override public Unit onHTTPOK(final HTTPResultOKType<InputStream> e)
            throws IOException
          {
            SyncedDocumentAbstract.this.onFetchedURI(u, e.getValue());
            return Unit.unit();
          }
        });
    } catch (final IOException e) {
      this.log.error("failed to fetch {}: ", u, e);
    } finally {
      this.fetching.set(false);
    }
  }

  private void onFetchedURI(
    final URI u,
    final InputStream stream)
    throws IOException
  {
    FileUtilities.fileWriteStream(this.stored, this.stored_tmp, stream);

    final SharedPreferences.Editor edit = this.prefs.edit();
    edit.putString("uri-latest", u.toString());
    edit.putLong("check-time", SyncedDocumentAbstract.now());
    edit.apply();

    this.log.debug("fetched {}", u);
  }

  private URL getCurrentURL()
  {
    if (this.stored.isFile()) {
      return this.stored_url;
    } else {
      return this.asset_url;
    }
  }
}
