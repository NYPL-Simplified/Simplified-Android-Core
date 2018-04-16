package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;

/**
 * A listener that receives the results of TOC item selection.
 */

public interface ReaderTOCContentsFragmentSelectionListenerType
{
  /**
   * No TOC item was selected.
   */

  //TODO I don't think this method is needed anymore with how a viewpager handles pressing the back button
  void onTOCBackSelected();

  /**
   * The given TOC item was selected.
   *
   * @param e The selected item
   */

  void onTOCItemSelected(
    TOCElement e);
}
