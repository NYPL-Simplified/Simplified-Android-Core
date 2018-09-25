package org.nypl.simplified.books.core;

import com.io7m.junreachable.UnreachableCodeException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Information about the supported book formats.
 */

public final class BookFormats {

  private static final Set<String> AUDIO_BOOK_MIME_TYPES = makeAudioBookMimeTypes();
  private static final Set<String> EPUB_MIME_TYPES = makeEPUBMimeTypes();
  private static final Set<String> SUPPORTED_BOOK_MIME_TYPES =
    makeSupportedBookMimeTypes(EPUB_MIME_TYPES, AUDIO_BOOK_MIME_TYPES);

  private BookFormats() {
    throw new UnreachableCodeException();
  }

  private static Set<String> makeEPUBMimeTypes() {
    final HashSet<String> types = new HashSet<>(1);
    types.add("application/epub+zip");
    return Collections.unmodifiableSet(types);
  }

  private static Set<String> makeAudioBookMimeTypes() {
    final HashSet<String> types = new HashSet<>(2);
    types.add("application/vnd.librarysimplified.findaway.license+json");
    types.add("application/audiobook+json");
    return Collections.unmodifiableSet(types);
  }

  private static Set<String> makeSupportedBookMimeTypes(
    final Set<String> epub,
    final Set<String> audio_book) {
    final HashSet<String> types = new HashSet<>(epub.size());
    types.addAll(epub);
    return Collections.unmodifiableSet(types);
  }

  /**
   * @return A set of the supported book format MIME types
   */

  public static Set<String> supportedBookMimeTypes() {
    return SUPPORTED_BOOK_MIME_TYPES;
  }

  /**
   * @return The set of MIME types for EPUBs
   */

  public static Set<String> epubMimeTypes() {
    return EPUB_MIME_TYPES;
  }

  /**
   * @return The set of MIME types for EPUBs
   */

  public static Set<String> audioBookMimeTypes() {
    return AUDIO_BOOK_MIME_TYPES;
  }
}
