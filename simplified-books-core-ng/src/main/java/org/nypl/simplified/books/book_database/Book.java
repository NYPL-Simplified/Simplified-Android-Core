package org.nypl.simplified.books.book_database;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.io.File;

/**
 * A known book. A book is "known" if the user has at some point tried to borrow and/or download
 * the book. A book ceases to be known when the loan for it has been revoked and the local file(s)
 * deleted.
 */

@AutoValue
public abstract class Book {

  Book() {

  }

  /**
   * @return The book ID
   */

  public abstract BookID id();

  /**
   * @return The ID of the account that owns the book
   */

  public abstract AccountID account();

  /**
   * @return The file containing the cover
   */

  public abstract OptionType<File> cover();

  /**
   * The EPUB file. Only present if the EPUB has been downloaded to the device.
   *
   * @return The EPUB file
   */

  public abstract OptionType<File> file();

  /**
   * The Adobe rights file. Only present if the EPUB has been "fulfilled" via the DRM system.
   *
   * @return The Adobe rights file
   */

  public abstract OptionType<File> adobeRightsFile();

  /**
   * The most recent OPDS entry for the book.
   */

  public abstract OPDSAcquisitionFeedEntry entry();

  /**
   * The Adobe loan information. Only present if the EPUB has been loaned via the DRM system.
   *
   * @return The Adobe loan information
   */

  public abstract OptionType<AdobeAdeptLoan> adobeLoan();

  /**
   * @return The current value as a mutable builder
   */

  public abstract Builder toBuilder();

  /**
   * @return A mutable builder for constructing book values
   */

  public static Builder builder(
      final BookID book_id,
      final AccountID account_id,
      final OPDSAcquisitionFeedEntry entry) {

    return new AutoValue_Book.Builder()
        .setAdobeLoan(Option.none())
        .setAdobeRightsFile(Option.none())
        .setCover(Option.none())
        .setFile(Option.none())
        .setEntry(entry)
        .setId(book_id)
        .setAccount(account_id);
  }

  /**
   * The type of mutable builders.
   */

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * @see #id()
     * @param id The book ID
     * @return The current builder
     */

    public abstract Builder setId(BookID id);

    /**
     * @see #account()
     * @param id The account ID
     * @return The current builder
     */

    public abstract Builder setAccount(AccountID id);

    /**
     * @see #cover()
     * @param cover_option The cover
     * @return The current builder
     */

    public abstract Builder setCover(OptionType<File> cover_option);

    /**
     * @see #cover()
     * @param cover The cover
     * @return The current builder
     */

    public final Builder setCover(final File cover) {
      return setCover(Option.some(cover));
    }

    /**
     * @see #file()
     * @param file The EPUB file
     * @return The current builder
     */

    public abstract Builder setFile(OptionType<File> file);

    /**
     * @see #file()
     * @param file The EPUB file
     * @return The current builder
     */

    public final Builder setFile(final File file) {
      return setFile(Option.some(file));
    }

    /**
     * @see #entry()
     * @param entry The OPDS entry
     * @return The current builder
     */

    public abstract Builder setEntry(OPDSAcquisitionFeedEntry entry);

    /**
     * @see #adobeLoan()
     * @param loan The Adobe loan information
     * @return The current builder
     */

    public abstract Builder setAdobeLoan(OptionType<AdobeAdeptLoan> loan);

    /**
     * @see #adobeLoan()
     * @param loan The Adobe loan information
     * @return The current builder
     */

    public final Builder setAdobeLoan(final AdobeAdeptLoan loan) {
      return setAdobeLoan(Option.some(loan));
    }

    /**
     * @see #adobeRightsFile()
     * @param file The Adobe rights file
     * @return The current builder
     */

    public abstract Builder setAdobeRightsFile(OptionType<File> file);

    /**
     * @see #adobeRightsFile()
     * @param file The Adobe rights file
     * @return The current builder
     */

    public final Builder setAdobeRightsFile(final File file) {
      return setAdobeRightsFile(Option.some(file));
    }

    /**
     * @return A book value based on all of the values given so far
     */

    public abstract Book build();
  }
}
