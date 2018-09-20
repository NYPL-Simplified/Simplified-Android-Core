package org.nypl.simplified.books.core;

import com.io7m.junreachable.UnreachableCodeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Information about the supported book formats.
 */

public final class BookFormats {

  private static final List<String> SUPPORTED_BOOK_MIME_TYPES = makeSupportedBookMimeTypes();

  private static List<String> makeSupportedBookMimeTypes() {
    ArrayList<String> types = new ArrayList<>(4);
    types.add("application/epub+zip");
    return Collections.unmodifiableList(types);
  }

  private BookFormats() {
    throw new UnreachableCodeException();
  }

  /**
   * @return A list of the supported book format MIME types
   */

  public static List<String> supportedBookMimeTypes() {
    return SUPPORTED_BOOK_MIME_TYPES;
  }
}
