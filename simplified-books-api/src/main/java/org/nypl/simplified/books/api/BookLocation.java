package org.nypl.simplified.books.api;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.OptionType;

import java.io.Serializable;

/**
 * The current page. A specific location in an EPUB is identified by an
 * <i>idref</i> and a <i>content CFI</i>. In some cases, the <i>content CFI</i>
 * may not be present.
 *
 * <p>Note: The type is {@link Serializable} purely because the Android API requires this
 * in order pass values of this type between activities. We make absolutely no guarantees
 * that serialized values of this class will be compatible with future releases.</p>
 */

@AutoValue
public abstract class BookLocation implements Serializable {

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

  public static BookLocation create(
      final OptionType<String> cfi,
      final String id_ref) {
    return new AutoValue_BookLocation(cfi, id_ref);
  }
}
