package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.io.File;

/**
 * A snapshot of the state of a book.
 */

public final class BookSnapshot
{
  private final OptionType<AdobeAdeptLoan> adobe_rights;
  private final OptionType<File>           book;
  private final OptionType<File>           cover;
  private final OPDSAcquisitionFeedEntry   entry;

  /**
   * Construct a book snapshot.
   *
   * @param in_cover        The cover file, if any
   * @param in_book         The actual book (typically an EPUB), if any
   * @param in_entry        The acquisition feed entry
   * @param in_adobe_rights The Adobe DRM rights associated with the book, if
   *                        any
   */

  public BookSnapshot(
    final OptionType<File> in_cover,
    final OptionType<File> in_book,
    final OPDSAcquisitionFeedEntry in_entry,
    final OptionType<AdobeAdeptLoan> in_adobe_rights)
  {
    this.cover = NullCheck.notNull(in_cover);
    this.book = NullCheck.notNull(in_book);
    this.entry = NullCheck.notNull(in_entry);
    this.adobe_rights = NullCheck.notNull(in_adobe_rights);
  }

  /**
   * @return The book file (typically an EPUB), if any
   */

  public OptionType<File> getBook()
  {
    return this.book;
  }

  /**
   * @return The cover image, if any
   */

  public OptionType<File> getCover()
  {
    return this.cover;
  }

  /**
   * @return The Adobe DRM rights, if any
   */

  public OptionType<AdobeAdeptLoan> getAdobeRights()
  {
    return this.adobe_rights;
  }

  /**
   * @return The acquisition feed entry
   */

  public OPDSAcquisitionFeedEntry getEntry()
  {
    return this.entry;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(64);
    b.append("[BookSnapshot book=");
    b.append(this.book);
    b.append(" cover=");
    b.append(this.cover);
    b.append(" entry=");
    b.append(this.entry.getClass().getCanonicalName());
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
