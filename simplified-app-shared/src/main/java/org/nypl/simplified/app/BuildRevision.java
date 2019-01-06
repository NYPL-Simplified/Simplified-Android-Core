package org.nypl.simplified.app;

import android.content.res.AssetManager;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A trivial class to fetch the SCM revision that was used to build this version of the application.
 * <p>
 * The build system is responsible for generating a "revision.properties" file in Java properties
 * format containing the current SCM revision.
 */

public final class BuildRevision {

  private static final Logger LOG = LoggerFactory.getLogger(BuildRevision.class);

  private BuildRevision() {

  }

  /**
   * @param assets The current asset manager
   * @return The current SCM revision, or "Unavailable" if any problems occur
   */

  public static String revision(final AssetManager assets) {
    NullCheck.notNull(assets, "assets");

    try (InputStream stream = assets.open("revision.properties")) {
      final Properties props = new Properties();
      props.load(stream);
      return props.getProperty("revision", "Unavailable");
    } catch (final IOException e) {
      LOG.error("Unable to fetch revision.properties: ", e);
      return "Unavailable";
    }
  }
}
