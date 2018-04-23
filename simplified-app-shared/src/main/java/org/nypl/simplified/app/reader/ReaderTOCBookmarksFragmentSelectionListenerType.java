package org.nypl.simplified.app.reader;

import org.nypl.simplified.books.core.BookmarkAnnotation;

public interface ReaderTOCBookmarksFragmentSelectionListenerType {

  /**
   * The given bookmark was selected.
   *
   * @param bookmark Selected Bookmark Item in the ListView
   */

  void onBookmarkSelected(BookmarkAnnotation bookmark);
}
