package org.nypl.simplified.main;

import android.content.res.AssetManager;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Bugsnag config loader
 */
public final class MainBugsnag {
  private static final Logger LOG = LoggerFactory.getLogger(MainBugsnag.class);

  /**
   * Construct bugsnag
   */
  private MainBugsnag() {
  }

  /**
   * Get BugSnag API token if it has been provided as part of the build.
   *
   * @param in_mgr The current asset manager
   * @return The BugSnag API token.
   */
  public static OptionType<String> getApiToken(final AssetManager in_mgr) {
    NullCheck.notNull(in_mgr);

    InputStream s = null;
    try {
      s = in_mgr.open("bugsnag.conf");
      final Properties p = new Properties();
      p.load(s);
      final String api_token = p.getProperty("bugsnag.api_token");
      if (api_token == null) {
        throw new BugsnagConfigurationMissingParameter("bugsnag.api_token");
      }
      return Option.some(api_token);
    } catch (final IOException e) {
      MainBugsnag.LOG.debug(
        "i/o error on attempting to open bugsnag.conf: ", e);
      return Option.none();
    } catch (final BugsnagConfigurationMissingParameter e) {
      MainBugsnag.LOG.debug(
        "missing parameter in bugsnag.conf: {}: ", e.getMessage(), e);
      return Option.none();
    } finally {
      try {
        if (s != null) {
          s.close();
        }
      } catch (final IOException e) {
        MainBugsnag.LOG.debug("ignoring exception raised on close: ", e);
      }
    }
  }

  private static abstract class BugsnagConfigurationError extends Exception {
    BugsnagConfigurationError(final String in_message) {
      super(in_message);
    }
  }

  private static final class BugsnagConfigurationMissingParameter
    extends BugsnagConfigurationError {
    BugsnagConfigurationMissingParameter(final String in_message) {
      super(in_message);
    }
  }
}
