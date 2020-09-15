package org.nypl.simplified.viewer.epub.readium1;

import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.reader.api.ReaderPreferences;

/**
 * The type of the JavaScript API exposed by Readium.
 */

public interface ReaderReadiumJavaScriptAPIType
{
  /**
   * Retrieve the current page.
   *
   * @param l The current page listener
   */

  void getCurrentPage(
    ReaderCurrentPageListenerType l);

  /**
   * Determine whether or not a media overlay is available.
   *
   * @param l The media overlay listener
   */

  void mediaOverlayIsAvailable(
    ReaderMediaOverlayAvailabilityListenerType l);

  /**
   * Move to the next media overlay.
   */

  void mediaOverlayNext();

  /**
   * Move to the previous media overlay.
   */

  void mediaOverlayPrevious();

  /**
   * Toggle the media overlay.
   */

  void mediaOverlayToggle();

  /**
   * Open a book.
   *
   * @param p  The package
   * @param vs The current reader viewer settings
   * @param r  A request for a specific page, if any
   */

  void openBook(
    org.readium.sdk.android.Package p,
    ReaderReadiumViewerSettings vs,
    OptionType<ReaderOpenPageRequestType> r);

  /**
   * Go to the specific location in the book.
   *
   * @param content_ref The content ref
   * @param source_href The source href
   */

  void openContentURL(
    String content_ref,
    String source_href);

  /**
   * Go to the next page in the current book.
   */

  void pageNext();

  /**
   * Go to the previous page in the current book.
   */

  void pagePrevious();

  /**
   * Configure book styles based on the given preferences.
   *
   * @param r The preferences
   */

  void setBookStyles(
    ReaderPreferences r);

  /**
   * Configure Readium settings based on the given preferences.
   *
   * @param r The preferences
   */

  void updateSettings(
    ReaderPreferences r);
}
