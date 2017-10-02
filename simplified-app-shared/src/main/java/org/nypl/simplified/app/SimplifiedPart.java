package org.nypl.simplified.app;

import android.content.res.Resources;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * The separate parts of the application, excluding the reader.
 *
 * When an activity is asked, it returns a {@code SimplifiedPart} value, and
 * this value is used to determine which item in the application's navigation
 * drawer will be highlighted when that activity is in the foreground.
 */

public enum SimplifiedPart
{


  /**
   *
   */
  PART_SWITCHER,

  /**
   * The "My Books" section.
   */

  PART_BOOKS,

  /**
   * The catalog (feed viewer) section.
   */

  PART_CATALOG,

  /**
   * The Holds section.
   */

  PART_HOLDS,

  /**
   * The settings section.
   */

  PART_SETTINGS,

  /**
   * The settings section.
   */

  PART_MANAGE_ACCOUNTS,

  /**
   * The account section.
   */

  PART_ACCOUNT,

  /**
   * The help section.
   */

  PART_HELP;

  /**
   * @param r The application resources
   *
   * @return The title of the given part
   */

  public String getPartName(
    final Resources r)
  {
    NullCheck.notNull(r);
    switch (this) {
      case PART_BOOKS: {
        return NullCheck.notNull(r.getString(R.string.books));
      }
      case PART_CATALOG: {
        return NullCheck.notNull(r.getString(R.string.catalog));
      }
      case PART_HOLDS: {
        return NullCheck.notNull(r.getString(R.string.holds));
      }
      case PART_SETTINGS: {
        return NullCheck.notNull(r.getString(R.string.settings));
      }
      case PART_HELP: {
        return NullCheck.notNull(r.getString(R.string.help));
      }
      case PART_ACCOUNT: {
        return NullCheck.notNull(r.getString(R.string.settings_account));
      }
      case PART_SWITCHER: {
        return "current library";
      }

    }

    throw new UnreachableCodeException();
  }
}
