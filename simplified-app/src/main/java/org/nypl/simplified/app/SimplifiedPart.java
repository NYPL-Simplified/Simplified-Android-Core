package org.nypl.simplified.app;

import android.content.res.Resources;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * The separate parts of the application, excluding the reader.
 */

public enum SimplifiedPart
{
  PART_BOOKS,
  PART_CATALOG,
  PART_HOLDS,
  PART_SETTINGS;

  public String getPartName(
    final Resources r)
  {
    NullCheck.notNull(r);
    switch (this) {
      case PART_BOOKS:
      {
        return NullCheck.notNull(r.getString(R.string.books));
      }
      case PART_CATALOG:
      {
        return NullCheck.notNull(r.getString(R.string.catalog));
      }
      case PART_HOLDS:
      {
        return NullCheck.notNull(r.getString(R.string.holds));
      }
      case PART_SETTINGS:
      {
        return NullCheck.notNull(r.getString(R.string.settings));
      }
    }

    throw new UnreachableCodeException();
  }
}
