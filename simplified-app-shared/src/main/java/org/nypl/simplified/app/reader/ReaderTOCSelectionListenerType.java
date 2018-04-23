package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.books.core.BookmarkAnnotation;

/**
 * A listener that receives the results of the TOC selection:
 * A TOC Item or a Bookmark Annotation.
 */

public interface ReaderTOCSelectionListenerType
{
  /**
   * A TOC item was selected.
   *
   * @param e The selected item
   */

  void onTOCSelectionReceived(TOCElement e);

  /**
   * A Bookmark annotation was selected.
   *
   * @param bm The selected bookmark
   */

  void onBookmarkSelectionReceived(BookmarkAnnotation bm);
}
