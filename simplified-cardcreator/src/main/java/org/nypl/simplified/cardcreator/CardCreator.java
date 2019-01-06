package org.nypl.simplified.cardcreator;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by aferditamuriqi on 9/29/16.
 *
 */

public final class CardCreator {

  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CardCreator.class);
  }

  private String url;
  private String version;
  private String username;
  private String password;
  private Resources resources;

  /**
   * @param in_manager asset manager
   * @param in_environment development environment
   * @param in_resources resources
   */
  public CardCreator(final AssetManager in_manager,
                     final String in_environment,
                     final Resources in_resources) {

    NullCheck.notNull(in_manager);

    this.resources = in_resources;

    InputStream s = null;
    try {
      s = in_manager.open("cardcreator.conf");
      final Properties p = new Properties();
      p.load(s);

      this.url = p.getProperty("cardcreator." + in_environment + ".url");
      this.version = p.getProperty("cardcreator." + in_environment + ".version");
      this.username = p.getProperty("cardcreator." + in_environment + ".username");
      this.password = p.getProperty("cardcreator." + in_environment + ".password");

      if (this.url == null) {
        throw new CardCreatorConfigurationMissingParameter("cardcreator." + in_environment + ".url");
      }
      if (this.username == null) {
        throw new CardCreatorConfigurationMissingParameter("cardcreator." + in_environment + ".username");
      }
      if (this.password == null) {
        throw new CardCreatorConfigurationMissingParameter("cardcreator." + in_environment + ".password");
      }
      if (this.version == null) {
        throw new CardCreatorConfigurationMissingParameter("cardcreator." + in_environment + ".version");
      }
    } catch (final IOException e) {
      CardCreator.LOG.debug(
        "i/o error on attempting to open cardcreator.conf: ", e);

    } catch (final CardCreatorConfigurationMissingParameter e) {
      CardCreator.LOG.debug(
        "missing parameter in cardcreator.conf: {}: ", e.getMessage(), e);
    } finally {
      try {
        if (s != null) {
          s.close();
        }
      } catch (final IOException e) {
        CardCreator.LOG.debug("ignoring exception raised on close: ", e);
      }
    }
  }


  /**
   * @return card creator url
   */
  public String getUrl() {
    return this.url;
  }

  /**
   * @return card creator version
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * @return basic auth password
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * @return basic auth username
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * @return context resources
   */
  public Resources getResources() {
    return this.resources;
  }


  private static abstract class CardCreatorConfigurationError extends Exception
  {
    CardCreatorConfigurationError(final String in_message)
    {
      super(in_message);
    }
  }

  private static final class CardCreatorConfigurationMissingParameter
    extends CardCreatorConfigurationError
  {
    CardCreatorConfigurationMissingParameter(final String in_message)
    {
      super(in_message);
    }
  }

}
