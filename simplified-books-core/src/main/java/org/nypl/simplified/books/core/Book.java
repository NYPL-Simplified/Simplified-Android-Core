package org.nypl.simplified.books.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

public final class Book
{
  private static OptionType<File> checkCover(
    final File f)
  {
    final File cover = new File(f, "cover.jpg");
    if (cover.isFile()) {
      return Option.some(cover);
    }
    return Option.none();
  }

  private static OPDSAcquisitionFeedEntry loadFeedEntry(
    final File f)
    throws IOException
  {
    try {
      final File meta = new File(f, "meta.dat");
      final ObjectInputStream ois =
        new ObjectInputStream(new FileInputStream(meta));
      final OPDSAcquisitionFeedEntry e =
        (OPDSAcquisitionFeedEntry) ois.readObject();
      ois.close();
      return e;
    } catch (final ClassNotFoundException e) {
      throw new IOException(e);
    }
  }

  public static Book loadFromDirectory(
    final File f)
    throws IOException
  {
    NullCheck.notNull(f);

    final BookID in_book_id =
      BookID.fromString(NullCheck.notNull(NullCheck.notNull(f).getName()));
    final OPDSAcquisitionFeedEntry in_e = Book.loadFeedEntry(f);
    final OptionType<File> in_cover_opt = Book.checkCover(f);
    return new Book(in_book_id, in_e, f, in_cover_opt);
  }

  private final File                     book_dir;
  private final BookID                   book_id;
  private final OptionType<File>         cover;
  private final OPDSAcquisitionFeedEntry entry;

  public Book(
    final BookID in_book_id,
    final OPDSAcquisitionFeedEntry in_e,
    final File in_book_dir,
    final OptionType<File> in_cover_opt)
  {
    this.book_id = NullCheck.notNull(in_book_id);
    this.entry = NullCheck.notNull(in_e);
    this.book_dir = NullCheck.notNull(in_book_dir);
    this.cover = NullCheck.notNull(in_cover_opt);
  }

  public BookID getID()
  {
    return this.book_id;
  }
}
