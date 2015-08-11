package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.io.File;

/**
 * A snapshot of the state of a book.
 */

public final class BookSnapshot
{
  private final OptionType<File>         book;
  private final OptionType<File>         cover;
  private final OPDSAcquisitionFeedEntry entry;

  /**
   * Construct a book snapshot.
   *
   * @param in_cover The cover file, if any
   * @param in_book  The actual book (typically an EPUB), if any
   * @param in_entry The acquisition feed entry
   */

  public BookSnapshot(
    final OptionType<File> in_cover,
    final OptionType<File> in_book,
    final OPDSAcquisitionFeedEntry in_entry)
  {
    this.cover = NullCheck.notNull(in_cover);
    this.book = NullCheck.notNull(in_book);
    this.entry = NullCheck.notNull(in_entry);
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
