package org.nypl.simplified.app.reader;

import android.support.annotation.Nullable;

import org.nypl.simplified.books.core.BookmarkAnnotation;

public interface ReaderTOCFragmentSelectionListenerType {

  /**
   * The given bookmark was selected.
   *
   * @param item Selected item in the table of contents
   */

  void onTOCItemSelected(ReaderTOC.TOCElement item);

  /**
   * The given bookmark was selected.
   *
   * @param bookmark Selected Bookmark Item in the ListView
   */

  void onBookmarkSelected(@Nullable BookmarkAnnotation bookmark);
}
