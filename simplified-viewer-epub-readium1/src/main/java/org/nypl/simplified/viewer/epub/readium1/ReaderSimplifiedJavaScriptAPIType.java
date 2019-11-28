package org.nypl.simplified.viewer.epub.readium1;

/**
 * The type of the JavaScript API exposed by Simplified.
 */

public interface ReaderSimplifiedJavaScriptAPIType
{
  /**
   * Get the CFI from the JS Readium SDK.
   */
  void getReadiumCFI();

  /**
   * Set the CFI in the JS Readium SDK.
   */
  void setReadiumCFI();

  /**
   * Notify the Javascript code that the page has changed in some way and
   * therefore new event listeners should be registered.
   */

  void pageHasChanged();
}
