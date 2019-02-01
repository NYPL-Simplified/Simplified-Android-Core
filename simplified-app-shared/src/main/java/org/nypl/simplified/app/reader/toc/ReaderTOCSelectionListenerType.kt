package org.nypl.simplified.app.reader.toc

interface ReaderTOCSelectionListenerType {

  /**
   * Something was selected from the TOC.
   *
   * @param selection The selected item
   */

  fun onTOCItemSelected(selection: ReaderTOCSelection)

}
