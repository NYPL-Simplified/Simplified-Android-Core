package org.nypl.simplified.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.app.utilities.LogUtilities;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The default implementation of the {@link EULAType} interface.
 */

public final class EULA implements EULAType
{
  private static final Logger LOG;
  private static final URL    ASSET_URL;

  static {
    LOG = LogUtilities.getLog(EULA.class);
    try {
      ASSET_URL = new URL("file:///android_asset/eula.html");
    } catch (final MalformedURLException e) {
      throw new UnreachableCodeException(e);
    }
  }

  private final SharedPreferences prefs;
  private final ExecutorService   exec;
  private final HTTPType          http;
  private final AtomicBoolean     fetching;
  private final File              stored;
  private final File              stored_tmp;
  private final URL               stored_url;

  private EULA(
    final HTTPType in_http,
    final ExecutorService in_exec,
    final SharedPreferences in_prefs,
    final File in_stored)
  {
    try {
      this.http = NullCheck.notNull(in_http);
      this.exec = NullCheck.notNull(in_exec);
      this.prefs = NullCheck.notNull(in_prefs);
      this.fetching = new AtomicBoolean(false);
      this.stored = NullCheck.notNull(in_stored);
      this.stored_tmp = new File(this.stored.toString() + ".tmp");
      this.stored_url = new URL("file://" + this.stored);
    } catch (final MalformedURLException e) {
      throw new UnreachableCodeException(e);
    }
  }

  /**
   * Retrieve the EULA, if one is defined. An EULA is assumed to be defined if
   * there is a file called {@code eula.html} in the root of the Android
   * assets.
   *
   * @param http    An HTTP interface
   * @param exec    An executor that will be used to perform asynchronous
   *                updates of the EULA text
   * @param context The application context
   *
   * @return An EULA, if any
   */

  public static OptionType<EULAType> getEULA(
    final HTTPType http,
    final ExecutorService exec,
    final Context context)
  {
    NullCheck.notNull(http);
    NullCheck.notNull(exec);
    NullCheck.notNull(context);

    final SharedPreferences prefs = context.getSharedPreferences("eula", 0);
    final File stored = new File(context.getCacheDir(), "eula.html");
    final AssetManager assets = context.getAssets();
    try {
      final InputStream is = assets.open("eula.html", 0);
      try {
        final EULAType eula = new EULA(http, exec, prefs, stored);
        return Option.some(eula);
      } finally {
        is.close();
      }
    } catch (final IOException e) {
      EULA.LOG.debug("could not open eula.html: ", e);
      return Option.none();
    }
  }

  @Override public boolean eulaHasAgreed()
  {
    return this.prefs.getBoolean("eula-agreed", false);
  }

  @Override public void eulaSetLatestURI(final URI u)
  {
    NullCheck.notNull(u);

    if (this.latestURIIsNew(u)) {
      if (this.fetching.compareAndSet(false, true)) {
        this.exec.execute(
          new Runnable()
          {
            @Override public void run()
            {
              EULA.this.fetchURI(u);
            }
          });
      }
    }
  }

  private boolean latestURIIsNew(final URI u)
  {
    final String existing = this.prefs.getString("eula-uri-latest", null);
    final String u_text = u.toString();
    final boolean is_new = u_text.equals(existing) == false;
    if (is_new) {
      EULA.LOG.debug("uri {} is new (old {})", u_text, existing);
    } else {
      EULA.LOG.debug("uri {} is not new", u_text, existing);
    }
    return is_new;
  }

  private void fetchURI(final URI u)
  {
    try {
      EULA.LOG.debug("fetching {}", u);

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
            EULA.this.onFetchedURI(u, e.getValue());
            return Unit.unit();
          }
        });
    } catch (final IOException e) {
      EULA.LOG.error("failed to fetch {}: ", u, e);
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
    edit.putString("eula-uri-latest", u.toString());
    edit.apply();

    EULA.LOG.debug("fetched {}", u);
  }

  @Override public URL eulaGetReadableURL()
  {
    final URL u = this.getCurrentURL();
    EULA.LOG.debug("current url {}", u);
    return u;
  }

  private URL getCurrentURL()
  {
    if (this.stored.isFile()) {
      return this.stored_url;
    } else {
      return EULA.ASSET_URL;
    }
  }

  @Override public void eulaSetHasAgreed(final boolean t)
  {
    final SharedPreferences.Editor edit = this.prefs.edit();
    edit.putBoolean("eula-agreed", t);
    edit.apply();
  }
}
