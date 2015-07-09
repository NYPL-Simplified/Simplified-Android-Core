package org.nypl.simplified.books.core;

import java.io.File;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

/**
 * A snapshot of the state of a book.
 */

public final class BookSnapshot
{
  private final OptionType<File>         book;
  private final OptionType<File>         cover;
  private final OPDSAcquisitionFeedEntry entry;

  public BookSnapshot(
    final OptionType<File> in_cover,
    final OptionType<File> in_book,
    final OPDSAcquisitionFeedEntry in_entry)
  {
    this.cover = NullCheck.notNull(in_cover);
    this.book = NullCheck.notNull(in_book);
    this.entry = NullCheck.notNull(in_entry);
  }

  public OptionType<File> getBook()
  {
    return this.book;
  }

  public OptionType<File> getCover()
  {
    return this.cover;
  }

  public OPDSAcquisitionFeedEntry getEntry()
  {
    return this.entry;
  }
}
