package org.nypl.simplified.books.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileLocking;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A single book directory.
 *
 * All operations on the directory are thread-safe but not necessarily
 * process-safe.
 */

@SuppressWarnings("synthetic-access") public final class BookDatabaseEntry
  implements BookDatabaseEntryType
{
  /**
   * The number of milliseconds to wait whilst attempting to acquire a lock.
   */

  public static final int WAIT_MAXIMUM_MILLISECONDS;

  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BookDatabaseEntry.class));
    WAIT_MAXIMUM_MILLISECONDS = 100;
  }

  private final File                   directory;
  private final File                   file_book;
  private final File                   file_cover;
  private final File                   file_lock;
  private final File                   file_meta;
  private final File                   file_meta_tmp;
  private final BookID                 id;
  private final OPDSJSONParserType     parser;
  private final OPDSJSONSerializerType serializer;

  /**
   * Construct a book database entry.
   *
   * @param in_json_serializer A JSON serializer
   * @param in_json_parser     A JSON parser
   * @param parent             The parent directory
   * @param book_id            The book ID
   */

  public BookDatabaseEntry(
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final File parent,
    final BookID book_id)
  {
    this.parser = NullCheck.notNull(in_json_parser);
    this.serializer = NullCheck.notNull(in_json_serializer);
    this.directory = new File(
      NullCheck.notNull(parent), NullCheck.notNull(book_id).toString());

    this.id = NullCheck.notNull(book_id);
    this.file_lock = new File(this.directory, "lock");
    this.file_cover = new File(this.directory, "cover.jpg");
    this.file_meta = new File(this.directory, "meta.json");
    this.file_meta_tmp = new File(this.directory, "meta.json.tmp");
    this.file_book = new File(this.directory, "book.epub");
  }

  @Override public void copyInBookFromSameFilesystem(
    final File file)
    throws IOException
  {
    FileLocking.withFileThreadLocked(
      this.file_lock,
      (long) BookDatabaseEntry.WAIT_MAXIMUM_MILLISECONDS,
      new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit x)
          throws IOException
        {
          BookDatabaseEntry.this.copyInBookFromSameFilesystemLocked(file);
          return Unit.unit();
        }
      });
  }

  private void copyInBookFromSameFilesystemLocked(
    final File file)
    throws IOException
  {
    FileUtilities.fileRename(file, this.file_book);
  }

  @Override public void create()
    throws IOException
  {
    DirectoryUtilities.directoryCreate(this.directory);
  }

  @Override public void destroy()
    throws IOException
  {
    FileLocking.withFileThreadLocked(
      this.file_lock,
      (long) BookDatabaseEntry.WAIT_MAXIMUM_MILLISECONDS,
      new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit x)
          throws IOException
        {
          BookDatabaseEntry.this.destroyLocked();
          return Unit.unit();
        }
      });
  }

  @Override public void destroyBookData()
    throws IOException
  {
    FileLocking.withFileThreadLocked(
      this.file_lock,
      (long) BookDatabaseEntry.WAIT_MAXIMUM_MILLISECONDS,
      new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit x)
          throws IOException
        {
          BookDatabaseEntry.this.destroyBookDataLocked();
          return Unit.unit();
        }
      });
  }

  private void destroyBookDataLocked()
    throws IOException
  {
    FileUtilities.fileDelete(this.file_book);
  }

  private void destroyLocked()
    throws IOException
  {
    if (this.directory.isDirectory()) {
      FileUtilities.fileDelete(this.file_lock);
      FileUtilities.fileDelete(this.file_cover);
      FileUtilities.fileDelete(this.file_meta);
      FileUtilities.fileDelete(this.file_meta_tmp);
      FileUtilities.fileDelete(this.file_book);
    }

    FileUtilities.fileDelete(this.directory);
  }

  @Override public boolean exists()
  {
    return this.file_meta.isFile();
  }

  private OptionType<File> getBookLocked()
  {
    if (this.file_book.isFile()) {
      return Option.some(this.file_book);
    }
    return Option.none();
  }

  @Override public OptionType<File> getCover()
    throws IOException
  {
    return FileLocking.withFileThreadLocked(
      this.file_lock,
      (long) BookDatabaseEntry.WAIT_MAXIMUM_MILLISECONDS,
      new PartialFunctionType<Unit, OptionType<File>, IOException>()
      {
        @Override public OptionType<File> call(
          final Unit x)
          throws IOException
        {
          return BookDatabaseEntry.this.getCoverLocked();
        }
      });
  }

  @Override public void setCover(
    final OptionType<File> in_cover)
    throws IOException
  {
    FileLocking.withFileThreadLocked(
      this.file_lock,
      (long) BookDatabaseEntry.WAIT_MAXIMUM_MILLISECONDS,
      new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit x)
          throws IOException
        {
          BookDatabaseEntry.this.setCoverLocked(in_cover);
          return Unit.unit();
        }
      });
  }

  private OptionType<File> getCoverLocked()
  {
    if (this.file_cover.isFile()) {
      return Option.some(this.file_cover);
    }
    return Option.none();
  }

  private void setCoverLocked(
    final OptionType<File> in_cover)
    throws IOException
  {
    if (in_cover.isSome()) {
      final Some<File> some = (Some<File>) in_cover;
      FileUtilities.fileCopy(some.get(), this.file_cover);
      some.get().delete();
    } else {
      this.file_cover.delete();
    }
  }

  @Override public OPDSAcquisitionFeedEntry getData()
    throws IOException
  {
    return FileLocking.withFileThreadLocked(
      this.file_lock,
      (long) BookDatabaseEntry.WAIT_MAXIMUM_MILLISECONDS,
      new PartialFunctionType<Unit, OPDSAcquisitionFeedEntry, IOException>()
      {
        @Override public OPDSAcquisitionFeedEntry call(
          final Unit x)
          throws IOException
        {
          return BookDatabaseEntry.this.getDataLocked();
        }
      });
  }

  @Override public void setData(
    final OPDSAcquisitionFeedEntry in_entry)
    throws IOException
  {
    final ObjectNode d = this.serializer.serializeFeedEntry(in_entry);

    FileLocking.withFileThreadLocked(
      this.file_lock,
      (long) BookDatabaseEntry.WAIT_MAXIMUM_MILLISECONDS,
      new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit x)
          throws IOException
        {
          BookDatabaseEntry.this.setDataLocked(d);
          return Unit.unit();
        }
      });
  }

  private OPDSAcquisitionFeedEntry getDataLocked()
    throws IOException
  {
    final FileInputStream is = new FileInputStream(this.file_meta);
    try {
      return this.parser.parseAcquisitionFeedEntryFromStream(is);
    } finally {
      is.close();
    }
  }

  private void setDataLocked(
    final ObjectNode d)
    throws IOException
  {
    BookDatabaseEntry.LOG.debug("updating data {}", this.file_meta);

    final OutputStream os = new FileOutputStream(this.file_meta_tmp);

    try {
      this.serializer.serializeToStream(d, os);
    } finally {
      os.flush();
      os.close();
    }

    FileUtilities.fileRename(this.file_meta_tmp, this.file_meta);
  }

  @Override public File getDirectory()
  {
    return this.directory;
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public BookSnapshot getSnapshot()
    throws IOException
  {
    return FileLocking.withFileThreadLocked(
      this.file_lock,
      (long) BookDatabaseEntry.WAIT_MAXIMUM_MILLISECONDS,
      new PartialFunctionType<Unit, BookSnapshot, IOException>()
      {
        @Override public BookSnapshot call(
          final Unit x)
          throws IOException
        {
          return BookDatabaseEntry.this.getSnapshotLocked();
        }
      });
  }

  private BookSnapshot getSnapshotLocked()
    throws IOException
  {
    final OPDSAcquisitionFeedEntry in_entry = this.getDataLocked();
    final OptionType<File> in_cover = this.getCoverLocked();
    final OptionType<File> in_book = this.getBookLocked();
    return new BookSnapshot(in_cover, in_book, in_entry);
  }
}
