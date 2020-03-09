package org.nypl.simplified.viewer.epub.readium1.toc

interface ReaderTOCSelectionListenerType {

  /**
   * Something was selected from the TOC.
   *
   * @param selection The selected item
   */

  fun onTOCItemSelected(selection: ReaderTOCSelection)
}
