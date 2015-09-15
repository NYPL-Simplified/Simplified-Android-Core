package org.nypl.simplified.app.catalog;

import android.content.res.Resources;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.app.R;
import org.nypl.simplified.books.core.BookStatusDownloadFailed;
import org.nypl.simplified.books.core.BookUnsupportedTypeException;

final class CatalogBookErrorStrings
{
  private CatalogBookErrorStrings()
  {
    throw new UnreachableCodeException();
  }

  /**
   * @param r Application resources
   * @param s Download status
   *
   * @return An appropriate failure string for the given status value.
   */

  public static String getFailureString(
    final Resources r,
    final BookStatusDownloadFailed s)
  {
    NullCheck.notNull(r);
    NullCheck.notNull(s);

    final OptionType<Throwable> error_opt = s.getError();
    if (error_opt.isSome()) {
      final Some<Throwable> error_some = (Some<Throwable>) error_opt;
      final Throwable error = error_some.get();

      if (error instanceof BookUnsupportedTypeException) {
        final BookUnsupportedTypeException unsupported =
          (BookUnsupportedTypeException) error;
        if ("application/pdf".equals(unsupported.getType())) {
          return r.getString(R.string.catalog_download_failed_unsupported_pdf);
        }

        return r.getString(
          R.string.catalog_download_failed_unsupported_unknown);
      }
    }

    return r.getString(R.string.catalog_download_failed);
  }
}
