package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.NYPLBookmark;

public interface ReaderTOCBookmarksFragmentSelectionListenerType {

  /**
   * The given bookmark was selected.
   *
   * @param bookmark Selected Bookmark Item in the ListView
   */

  void onBookmarkSelected(NYPLBookmark bookmark);
}
