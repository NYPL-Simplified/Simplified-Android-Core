package org.nypl.simplified.app.reader;

import org.nypl.simplified.opds.core.annotation.BookAnnotation;

public interface ReaderTOCBookmarksFragmentSelectionListenerType {

  /**
   * The given bookmark was selected.
   *
   * @param bookmark Selected Bookmark Item in the ListView
   */

  void onBookmarkSelected(BookAnnotation bookmark);
}
