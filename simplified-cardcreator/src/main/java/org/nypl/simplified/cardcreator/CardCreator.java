package org.nypl.simplified.cardcreator;

import android.content.res.AssetManager;

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
    LOG = LogUtilities.getLog(CardCreator.class);
  }

  /**
   *
   */
  public CardCreator(final AssetManager in_mgr, final String env) {
    NullCheck.notNull(in_mgr);

    InputStream s = null;
    try {
      s = in_mgr.open("cardcreator.conf");
      final Properties p = new Properties();
      p.load(s);

      this.url = p.getProperty("cardcreator." + env + ".url");
      this.username = p.getProperty("cardcreator." + env + ".username");
      this.password = p.getProperty("cardcreator." + env + ".password");

      if (this.url == null) {
        throw new CardCreatorConfigurationMissingParameter("cardcreator." + env + ".url");
      }
      if (this.username == null) {
        throw new CardCreatorConfigurationMissingParameter("cardcreator." + env + ".username");
      }
      if (this.password == null) {
        throw new CardCreatorConfigurationMissingParameter("cardcreator." + env + ".password");
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

  private String url;

  private String username;

  private String password;

  /**
   * @return card creator url
   */
  public String getUrl() {
    return this.url;
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
