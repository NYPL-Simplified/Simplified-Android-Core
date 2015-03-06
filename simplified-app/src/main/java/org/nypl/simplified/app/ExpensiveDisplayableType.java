package org.nypl.simplified.app;

/**
 * The type of views that are relatively expensive to display (read: have to
 * load large images over the network) and therefore the display lifetime of
 * which should be strictly controlled and as short as possible.
 */

public interface ExpensiveDisplayableType
{
  /**
   * Start displaying the view.
   */

  void expensiveRequestDisplay();

  /**
   * Stop displaying the view. Dispose of any resources.
   */

  void expensiveRequestStopDisplaying();
}
