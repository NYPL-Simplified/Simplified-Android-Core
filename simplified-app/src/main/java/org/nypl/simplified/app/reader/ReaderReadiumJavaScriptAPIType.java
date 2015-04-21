package org.nypl.simplified.app.reader;

import com.io7m.jfunctional.OptionType;

/**
 * The type of the JavaScript API exposed by Readium.
 */

public interface ReaderReadiumJavaScriptAPIType
{
  /**
   * Open a book.
   *
   * @param p
   *          The package
   * @param vs
   *          The current reader viewer settings
   * @param r
   *          A request for a specific page, if any
   */

  void openBook(
    org.readium.sdk.android.Package p,
    ReaderViewerSettings vs,
    OptionType<ReaderOpenPageRequest> r);

  /**
   * Go to the previous page in the current book.
   */

  void pagePrevious();

  /**
   * Go to the next page in the current book.
   */

  void pageNext();
}
