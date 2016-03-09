package org.nypl.simplified.app;

import android.app.Activity;
import android.app.Application;
import android.content.res.AssetManager;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.tenmiles.helpstack.HSHelpStack;
import com.tenmiles.helpstack.gears.HSDeskGear;
import com.tenmiles.helpstack.gears.HSZendeskGear;
import com.tenmiles.helpstack.logic.HSGear;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The main {@link HelpstackType} implementation.
 */

public final class Helpstack implements HelpstackType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(Helpstack.class);
  }

  private final HSHelpStack helpstack;

  private Helpstack(final HSHelpStack h)
  {
    this.helpstack = NullCheck.notNull(h);
  }

  /**
   * Initialize HelpStack and return a reference to the interface iff
   * credentials have been provided as part of the build.
   *
   * @param in_app The current application
   * @param in_mgr The current asset manager
   *
   * @return A reference to the HelpStack interface
   */

  public static OptionType<HelpstackType> get(
    final Application in_app,
    final AssetManager in_mgr)
  {
    NullCheck.notNull(in_app);
    NullCheck.notNull(in_mgr);

    InputStream s = null;
    try {
      s = in_mgr.open("helpstack.conf");
      final Properties p = new Properties();
      p.load(s);
      return Helpstack.getFromProperties(in_app, p);
    } catch (final IOException e) {
      Helpstack.LOG.debug(
        "i/o error on attempting to open helpstack.conf: ", e);
      return Option.none();
    } catch (final HelpstackConfigurationMissingParameter e) {
      Helpstack.LOG.debug(
        "missing parameter in helpstack.conf: {}: ", e.getMessage(), e);
      return Option.none();
    } catch (final HelpstackConfigurationUnknownGear e) {
      Helpstack.LOG.debug(
        "unknown gear specified in helpstack.conf: {}: ", e.getMessage(), e);
      return Option.none();
    } finally {
      try {
        if (s != null) {
          s.close();
        }
      } catch (final IOException e) {
        Helpstack.LOG.debug("ignoring exception raised on close: ", e);
      }
    }
  }

  private static OptionType<HelpstackType> getFromProperties(
    final Application in_app,
    final Properties p)
    throws
    HelpstackConfigurationMissingParameter,
    HelpstackConfigurationUnknownGear
  {
    final String gear_name = p.getProperty("helpstack.gear");
    if (gear_name == null) {
      throw new HelpstackConfigurationMissingParameter("helpstack.gear");
    }

    final HSGear g = Helpstack.getGear(p, gear_name);
    final HSHelpStack hs = HSHelpStack.getInstance(in_app);
    hs.setGear(g);
    final Helpstack h = new Helpstack(hs);
    return Option.some((HelpstackType) h);
  }

  private static HSGear getGear(
    final Properties p,
    final String gear_name)
    throws
    HelpstackConfigurationMissingParameter,
    HelpstackConfigurationUnknownGear
  {
    final HSGear g;
    if ("zendesk".equals(gear_name)) {
      return Helpstack.getZendeskGear(p);
    }
    else  if ("desk".equals(gear_name)) {
      return Helpstack.getDeskGear(p);
    }

    throw new HelpstackConfigurationUnknownGear(gear_name);
  }

  private static HSZendeskGear getZendeskGear(final Properties p)
    throws HelpstackConfigurationMissingParameter
  {
    final HSGear zg;
    final String url = p.getProperty("helpstack.zendesk.instance_url");
    if (url == null) {
      throw new HelpstackConfigurationMissingParameter(
        "helpstack.zendesk.instance_url");
    }

    final String email = p.getProperty("helpstack.zendesk.staff_email");
    if (email == null) {
      throw new HelpstackConfigurationMissingParameter(
        "helpstack.zendesk.staff_email");
    }

    final String api_token = p.getProperty("helpstack.zendesk.api_token");
    if (api_token == null) {
      throw new HelpstackConfigurationMissingParameter(
        "helpstack.zendesk.api_token");
    }

    return new HSZendeskGear(url, email, api_token);
  }

  private static HSDeskGear getDeskGear(final Properties p)
          throws HelpstackConfigurationMissingParameter
  {
    final HSGear zg;
    final String instance_url = p.getProperty("helpstack.desk.instance_url");
    if (instance_url == null) {
      throw new HelpstackConfigurationMissingParameter(
              "helpstack.desk.instance_url");
    }

    final String to_help_email = p.getProperty("helpstack.desk.to_help_email");
    if (to_help_email == null) {
      throw new HelpstackConfigurationMissingParameter(
              "helpstack.desk.to_help_email");
    }

    final String staff_login_email = p.getProperty("helpstack.desk.staff_login_email");
    if (staff_login_email == null) {
      throw new HelpstackConfigurationMissingParameter(
              "helpstack.desk.staff_login_email");
    }

    final String staff_login_password = p.getProperty("helpstack.desk.staff_login_password");
    if (staff_login_password == null) {
      throw new HelpstackConfigurationMissingParameter(
              "helpstack.desk.staff_login_password");
    }

    return new HSDeskGear(instance_url, to_help_email, staff_login_email, staff_login_password);
  }

  @Override public void show(final Activity a)
  {
    NullCheck.notNull(a);
    this.helpstack.showHelp(a);
  }

  private static abstract class HelpstackConfigurationError extends Exception
  {
    HelpstackConfigurationError(final String in_message)
    {
      super(in_message);
    }
  }

  private static final class HelpstackConfigurationMissingParameter
    extends HelpstackConfigurationError
  {
    HelpstackConfigurationMissingParameter(final String in_message)
    {
      super(in_message);
    }
  }

  private static final class HelpstackConfigurationUnknownGear
    extends HelpstackConfigurationError
  {
    HelpstackConfigurationUnknownGear(final String in_message)
    {
      super(in_message);
    }
  }

}
