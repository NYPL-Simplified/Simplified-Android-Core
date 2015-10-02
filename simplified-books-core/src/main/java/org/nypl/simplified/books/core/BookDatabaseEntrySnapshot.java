package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.io.File;

/**
 * A snapshot of the most recent state of a book in the database.
 */

public final class BookDatabaseEntrySnapshot
{
  private final BookID                     id;
  private final OptionType<AdobeAdeptLoan> adobe_rights;
  private final OptionType<File>           book;
  private final OptionType<File>           cover;
  private final OPDSAcquisitionFeedEntry   entry;

  /**
   * Construct a book snapshot.
   *
   * @param in_id           The book ID
   * @param in_cover        The cover file, if any
   * @param in_book         The actual book (typically an EPUB), if any
   * @param in_entry        The acquisition feed entry
   * @param in_adobe_rights The Adobe DRM rights associated with the book, if
   *                        any
   */

  public BookDatabaseEntrySnapshot(
    final BookID in_id,
    final OptionType<File> in_cover,
    final OptionType<File> in_book,
    final OPDSAcquisitionFeedEntry in_entry,
    final OptionType<AdobeAdeptLoan> in_adobe_rights)
  {
    this.id = NullCheck.notNull(in_id);
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
   * @return The book ID
   */

  public BookID getBookID()
  {
    return this.id;
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
    final StringBuilder sb = new StringBuilder("BookDatabaseEntrySnapshot{");
    sb.append("adobe_rights=").append(this.adobe_rights);
    sb.append(", id=").append(this.id);
    sb.append(", book=").append(this.book);
    sb.append(", cover=").append(this.cover);
    sb.append(", entry=").append(this.entry.getAvailability());
    sb.append('}');
    return sb.toString();
  }
}
