package org.nypl.simplified.app.reader;

/**
 * The type of the JavaScript API exposed by Simplified.
 */

public interface ReaderSimplifiedJavaScriptAPIType
{
  /**
   * Notify the Javascript code that the page has changed in some way and
   * therefore new event listeners should be registered.
   */

  void pageHasChanged();
}
