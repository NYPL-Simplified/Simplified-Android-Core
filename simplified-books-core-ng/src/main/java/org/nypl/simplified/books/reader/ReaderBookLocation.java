package org.nypl.simplified.books.reader;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.OptionType;

/**
 * The current page. A specific location in an EPUB is identified by an
 * <i>idref</i> and a <i>content CFI</i>. In some cases, the <i>content CFI</i>
 * may not be present.
 */

@AutoValue
public abstract class ReaderBookLocation {

  /**
   * @return The content CFI, if any
   */

  public abstract OptionType<String> contentCFI();

  /**
   * @return The IDRef
   */

  public abstract String idRef();

  /**
   * Create a book location.
   *
   * @param cfi    The content CFI, if any
   * @param id_ref The IDRef
   * @return A book location
   */

  public static ReaderBookLocation create(
      final OptionType<String> cfi,
      final String id_ref) {
    return new AutoValue_ReaderBookLocation(cfi, id_ref);
  }
}
