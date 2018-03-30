package org.nypl.simplified.books.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.jetbrains.annotations.NotNull;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.drm.core.AdobeLoanID;
import org.nypl.simplified.opds.core.annotation.BookAnnotation;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileLocking;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultToException;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A file-based book database.
 */

public final class BookDatabase implements BookDatabaseType
{
  private static final int LOCK_WAIT_MAXIMUM_MILLISECONDS;

  private static final Logger LOG;

  static {
    LOCK_WAIT_MAXIMUM_MILLISECONDS = 1000;
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BookDatabase.class));
  }

  private final File                                   directory;
  private final OPDSJSONParserType                     parser;
  private final OPDSJSONSerializerType                 serializer;
  private Map<BookID, BookDatabaseEntrySnapshot> snapshots;

  private BookDatabase(
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final File in_directory)
  {
    this.directory = NullCheck.notNull(in_directory);
    this.parser = NullCheck.notNull(in_json_parser);
    this.serializer = NullCheck.notNull(in_json_serializer);
    this.snapshots = new HashMap<BookID, BookDatabaseEntrySnapshot>(64);

    BookDatabase.LOG.debug("opened database {}", this.directory);
  }

  private static OptionType<File> makeCover(
    final HTTPType http,
    final BookID id,
    final OptionType<URI> cover_opt)
    throws IOException
  {
    if (cover_opt.isSome()) {
      final Some<URI> some = (Some<URI>) cover_opt;
      final URI cover_uri = some.get();

      final File cover_file_tmp = File.createTempFile("cover", ".jpg");
      cover_file_tmp.deleteOnExit();
      BookDatabase.makeCoverDownload(http, id, cover_file_tmp, cover_uri);
      return Option.some(cover_file_tmp);
    }

    return Option.none();
  }

  private static void makeCoverDownload(
    final HTTPType http,
    final BookID id,
    final File cover_file_tmp,
    final URI cover_uri)
    throws IOException
  {
    final String sid = id.getShortID();
    BookDatabase.LOG.debug("[{}]: fetching cover at {}", sid, cover_uri);

    final OptionType<HTTPAuthType> no_auth = Option.none();
    final HTTPResultOKType<InputStream> r =
      http.get(no_auth, cover_uri, 0L).matchResult(
        new HTTPResultToException<InputStream>(cover_uri));

    try {
      final FileOutputStream fs = new FileOutputStream(cover_file_tmp);
      try {
        final InputStream in = NullCheck.notNull(r.getValue());
        try {
          final byte[] buffer = new byte[8192];
          while (true) {
            final int rb = in.read(buffer);
            if (rb == -1) {
              break;
            }
            fs.write(buffer, 0, rb);
          }
        } finally {
          in.close();
        }

        fs.flush();
      } finally {
        fs.close();
      }
    } finally {
      r.close();
    }

    BookDatabase.LOG.debug("[{}]: fetched cover {}", sid, cover_uri);
  }

  /**
   * Open a database at the given directory.
   *
   * @param in_json_serializer A JSON serializer
   * @param in_json_parser     A JSON parser
   * @param in_directory       The directory
   *
   * @return A reference to the database
   */

  public static BookDatabaseType newDatabase(
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final File in_directory)
  {
    return new BookDatabase(in_json_serializer, in_json_parser, in_directory);
  }

  /**
   * Given a path to an epub file, return the path to the associated Adobe
   * rights file, if any.
   *
   * @param file The epub file
   *
   * @return The Adobe rights file, if any
   */

  public static OptionType<File> getAdobeRightsFileForEPUB(final File file)
  {
    NullCheck.notNull(file);

    final File parent = file.getParentFile();
    final File adobe = new File(parent, "rights_adobe.xml");
    if (adobe.isFile()) {
      return Option.some(adobe);
    }
    return Option.none();
  }

  @Override public void databaseCreate()
    throws IOException
  {
    DirectoryUtilities.directoryCreate(this.directory);
  }

  @Override public void databaseDestroy()
    throws IOException
  {
    if (this.directory.isDirectory()) {
      final List<BookDatabaseEntryType> es = this.getBookDatabaseEntries();
      for (final BookDatabaseEntryType e : es) {
        e.entryDestroy();
      }
      FileUtilities.fileDelete(this.directory);
    }
  }

  private List<BookDatabaseEntryType> getBookDatabaseEntries()
  {
    final List<BookDatabaseEntryType> xs =
      new ArrayList<BookDatabaseEntryType>(32);

    if (this.directory.isDirectory()) {
      final File[] book_list = this.directory.listFiles(
        new FileFilter()
        {
          @Override public boolean accept(
            final @Nullable File path)
          {
            return NullCheck.notNull(path).isDirectory();
          }
        });

      for (final File f : book_list) {
        final BookID id = BookID.exactString(NullCheck.notNull(f.getName()));
        xs.add(
          new BookDatabaseEntry(
            this.serializer, this.parser, this.directory, id));
      }
    }

    return xs;
  }

  @Override
  public BookDatabaseEntryType databaseOpenEntryForWriting(final BookID book_id)
  {
    return new BookDatabaseEntry(
      this.serializer, this.parser, this.directory, NullCheck.notNull(book_id));
  }

  @Override public BookDatabaseEntryReadableType databaseOpenEntryForReading(
    final BookID book_id)
    throws IOException
  {
    final File f = new File(this.directory, book_id.toString());
    if (f.isDirectory()) {
      return new BookDatabaseEntry(
        this.serializer,
        this.parser,
        this.directory,
        NullCheck.notNull(book_id));
    }

    throw new FileNotFoundException(f.toString());
  }

  @Override public void databaseNotifyAllBookStatus(
    final BooksStatusCacheType cache,
    final ProcedureType<Pair<BookID, BookDatabaseEntrySnapshot>> on_load,
    final ProcedureType<Pair<BookID, Throwable>> on_failure)
  {
    if (this.directory.isDirectory()) {
      final File[] book_list = this.directory.listFiles(
        new FileFilter()
        {
          @Override public boolean accept(
            final @Nullable File path)
          {
            return NullCheck.notNull(path).isDirectory();
          }
        });

      for (final File f : book_list) {
        final BookID id = BookID.exactString(NullCheck.notNull(f.getName()));
        final BookDatabaseEntry e = new BookDatabaseEntry(
          this.serializer, this.parser, this.directory, id);

        try {
          final BookDatabaseEntrySnapshot s = e.entryGetSnapshot();
          final BookStatusType status = BookStatus.fromSnapshot(id, s);
          cache.booksStatusUpdate(status);
          final Pair<BookID, BookDatabaseEntrySnapshot> p = Pair.pair(id, s);
          on_load.call(p);
        } catch (final IOException x) {
          BookDatabase.LOG.error(
            "[{}]: error creating snapshot: ", id.getShortID(), x);
          final Pair<BookID, Throwable> p = Pair.pair(id, (Throwable) x);
          on_failure.call(p);
        }
      }
    }
  }

  @Override
  public OptionType<BookDatabaseEntrySnapshot> databaseGetEntrySnapshot(
    final BookID book)
  {
    NullCheck.notNull(book);
    synchronized (this.snapshots) {
      if (this.snapshots.containsKey(book)) {
        return Option.some(this.snapshots.get(book));
      }
      return Option.none();
    }
  }

  @Override public Set<BookID> databaseGetBooks()
  {
    final Set<BookID> hs = new HashSet<BookID>(32);
    if (this.directory.isDirectory()) {
      final File[] book_list = this.directory.listFiles(
        new FileFilter()
        {
          @Override public boolean accept(
            final @Nullable File path)
          {
            return NullCheck.notNull(path).isDirectory();
          }
        });

      for (final File f : book_list) {
        final BookID id = BookID.exactString(NullCheck.notNull(f.getName()));
        hs.add(id);
      }
    }

    return hs;
  }

  /**
   * A single book directory.
   *
   * All operations on the directory are thread-safe but not necessarily
   * process-safe.
   */

  private final class BookDatabaseEntry implements BookDatabaseEntryType
  {
    private final File                   directory;
    private final File                   file_adobe_rights_tmp;
    private final File                   file_adobe_rights;
    private final File                   file_adobe_meta;
    private final File                   file_adobe_meta_tmp;
    private final File                   file_book;
    private final File                   file_cover;
    private final File                   file_lock;
    private final File                   file_meta;
    private final File                   file_meta_tmp;
    private final File                   file_bookmarks;
    private final File                   file_bookmarks_tmp;
    private final BookID                 id;
    private final OPDSJSONParserType     parser;
    private final OPDSJSONSerializerType serializer;
    private final Logger                 log;

    private BookDatabaseEntry(
      final OPDSJSONSerializerType in_json_serializer,
      final OPDSJSONParserType in_json_parser,
      final File parent,
      final BookID book_id)
    {
      this.parser = NullCheck.notNull(in_json_parser);
      this.serializer = NullCheck.notNull(in_json_serializer);
      this.id = NullCheck.notNull(book_id);

      NullCheck.notNull(parent);

      this.directory = new File(parent, book_id.toString());
      this.file_lock = new File(parent, book_id.toString() + ".lock");

      this.file_cover = new File(this.directory, "cover.jpg");
      this.file_meta = new File(this.directory, "meta.json");
      this.file_meta_tmp = new File(this.directory, "meta.json.tmp");
      this.file_book = new File(this.directory, "book.epub");
      this.file_adobe_rights = new File(this.directory, "rights_adobe.xml");
      this.file_adobe_rights_tmp =
        new File(this.directory, "rights_adobe.xml.tmp");
      this.file_adobe_meta = new File(this.directory, "meta_adobe.json");
      this.file_adobe_meta_tmp =
        new File(this.directory, "meta_adobe.json.tmp");
      this.file_bookmarks = new File(this.directory, "bookmarks.json");
      this.file_bookmarks_tmp = new File(this.directory, "bookmarks.json.tmp");

      this.log =
        NullCheck.notNull(LoggerFactory.getLogger(BookDatabaseEntry.class));
    }

    @Override
    public BookDatabaseEntrySnapshot entryCopyInBookFromSameFilesystem(
      final File file)
      throws IOException
    {
      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
        new PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException>()
        {
          @Override public BookDatabaseEntrySnapshot call(
            final Unit x)
            throws IOException
          {
            BookDatabaseEntry.this.copyInBookFromSameFilesystemLocked(file);
            return BookDatabaseEntry.this.updateSnapshotLocked();
          }
        });
    }

    @Override public BookDatabaseEntrySnapshot entryCopyInBook(final File file)
      throws IOException
    {
      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
        new PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException>()
        {
          @Override public BookDatabaseEntrySnapshot call(
            final Unit x)
            throws IOException
          {
            BookDatabaseEntry.this.copyInBookLocked(file);
            return BookDatabaseEntry.this.updateSnapshotLocked();
          }
        });
    }

    private void copyInBookLocked(final File file)
      throws IOException
    {
      FileUtilities.fileCopy(file, this.file_book);
    }

    private void copyInBookFromSameFilesystemLocked(
      final File file)
      throws IOException
    {
      FileUtilities.fileRename(file, this.file_book);
    }

    @Override public BookDatabaseEntrySnapshot entryCreate(
      final OPDSAcquisitionFeedEntry e)
      throws IOException
    {
      NullCheck.notNull(e);
      DirectoryUtilities.directoryCreate(this.directory);
      return this.entrySetFeedData(e);
    }

    @Override public void entryDestroy()
      throws IOException
    {
      FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
        new PartialFunctionType<Unit, Unit, IOException>()
        {
          @Override public Unit call(
            final Unit x)
            throws IOException
          {
            BookDatabaseEntry.this.destroyLocked();
            BookDatabaseEntry.this.deleteSnapshot();
            return Unit.unit();
          }
        });
    }

    @Override public BookDatabaseEntrySnapshot entryDeleteBookData()
      throws IOException
    {
      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
        new PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException>()
        {
          @Override public BookDatabaseEntrySnapshot call(
            final Unit x)
            throws IOException
          {
            BookDatabaseEntry.this.destroyBookDataLocked();
            return BookDatabaseEntry.this.updateSnapshotLocked();
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
        final File[] files = this.directory.listFiles();
        for (final File f : files) {
          FileUtilities.fileDelete(f);
        }
      }

      FileUtilities.fileDelete(this.directory);
      FileUtilities.fileDelete(this.file_lock);
    }

    @Override public boolean entryExists()
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

    @Override public OptionType<File> entryGetCover()
      throws IOException
    {
      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
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

    @Override public BookDatabaseEntrySnapshot entrySetCover(
      final OptionType<File> in_cover)
      throws IOException
    {
      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
        new PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException>()
        {
          @Override public BookDatabaseEntrySnapshot call(
            final Unit x)
            throws IOException
          {
            BookDatabaseEntry.this.setCoverLocked(in_cover);
            return BookDatabaseEntry.this.updateSnapshotLocked();
          }
        });
    }

    @Override public BookDatabaseEntrySnapshot entrySetAdobeRightsInformation(
      final OptionType<AdobeAdeptLoan> loan)
      throws IOException
    {
      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
        new PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException>()
        {
          @Override public BookDatabaseEntrySnapshot call(
            final Unit x)
            throws IOException
          {
            BookDatabaseEntry.this.setAdobeRightsInformationLocked(loan);
            return BookDatabaseEntry.this.updateSnapshotLocked();
          }
        });
    }

    @Override public BookDatabaseEntrySnapshot entrySetBookmark(final BookAnnotation bm)
        throws IOException {
      final List<BookAnnotation> bookmarks = entryGetBookmarksList();
      bookmarks.add(bm);
      return entrySetBookmarksList(bookmarks);
    }

    @NotNull
    @Override public List<BookAnnotation> entryGetBookmarksList()
        throws IOException {
      return FileLocking.withFileThreadLocked(
          this.file_lock,
          (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
          new PartialFunctionType<Unit, List<BookAnnotation>, IOException>()
          {
            @Override public List<BookAnnotation> call(
                final Unit x)
                throws IOException
            {
              return BookDatabaseEntry.this.getBookmarksLocked();
            }
          });
    }

    @Override public BookDatabaseEntrySnapshot entrySetBookmarksList(
        final @NotNull List<BookAnnotation> bookmarks)
        throws IOException
    {
      return FileLocking.withFileThreadLocked(
          this.file_lock,
          (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
          new PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException>()
          {
            @Override public BookDatabaseEntrySnapshot call(
                final Unit x)
                throws IOException
            {
              BookDatabaseEntry.this.setBookmarksListLocked(bookmarks);
              //TODO check if any updates are required to updateSnapshotLocked()
              return BookDatabaseEntry.this.updateSnapshotLocked();
            }
          });
    }

    @NotNull private List<BookAnnotation> getBookmarksLocked()
        throws IOException {
      final FileInputStream is = new FileInputStream(this.file_bookmarks);

      //TODO Turn input stream into List<BookAnnotation>

      try {
        //FIXME untested
        final Gson gson = new Gson();
        final JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
        final List<BookAnnotation> bookmarks = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
          BookAnnotation bookmark = gson.fromJson(reader, BookAnnotation.class);
          bookmarks.add(bookmark);
        }
        reader.endArray();
        reader.close();

        return bookmarks;
      } finally {
        is.close();
      }
    }

    private void setBookmarksListLocked(
        final List<BookAnnotation> bookmarks)
        throws IOException
    {
      this.log.debug("updating bookmarks list {}", this.file_bookmarks);
      final OutputStream stream = new FileOutputStream(this.file_bookmarks_tmp);

      try {
        //FIXME test and consider converting to jackson libraries across the board
        ObjectMapper mapper = new ObjectMapper();
        final ObjectNode node = mapper.valueToTree(bookmarks);
        this.serializer.serializeToStream(node, stream);
      } finally {
        stream.flush();
        stream.close();
      }
      FileUtilities.fileRename(this.file_bookmarks_tmp, this.file_bookmarks);
    }

    @Override public BookDatabaseEntrySnapshot entryUpdateAll(
      final OPDSAcquisitionFeedEntry e,
      final BooksStatusCacheType books_status,
      final HTTPType http)
      throws IOException
    {
      final String sid = this.id.getShortID();

      this.entryCreate(e);


//        BookDatabase.LOG.debug("[{}]: fetching cover", sid);
//        final OptionType<File> cover =
//          BookDatabase.makeCover(http, this.id, e.getThumbnail());
//        this.entrySetCover(cover);

      BookDatabase.LOG.debug("[{}]: getting snapshot", sid);
      final BookDatabaseEntrySnapshot snap = this.entryGetSnapshot();
      BookDatabase.LOG.debug("[{}]: determining status", sid);
      final BookStatusType status = BookStatus.fromSnapshot(this.id, snap);

      BookDatabase.LOG.debug("[{}]: updating status", sid);
      books_status.booksStatusUpdateIfMoreImportant(status);

      BookDatabase.LOG.debug(
        "[{}]: finished synchronizing book entry", sid);
      return snap;
    }

    private void setAdobeRightsInformationLocked(
      final OptionType<AdobeAdeptLoan> loan)
      throws IOException
    {
      if (loan.isSome()) {
        final AdobeAdeptLoan data = ((Some<AdobeAdeptLoan>) loan).get();

        FileUtilities.fileWriteBytesAtomically(
          this.file_adobe_rights,
          this.file_adobe_rights_tmp,
          data.getSerialized().array());

        final ObjectMapper jom = new ObjectMapper();
        final ObjectNode o = jom.createObjectNode();
        o.put("loan-id", data.getID().getValue());
        o.put("returnable", data.isReturnable());

        final ByteArrayOutputStream bao = new ByteArrayOutputStream();
        JSONSerializerUtilities.serialize(o, bao);

        FileUtilities.fileWriteUTF8Atomically(
          this.file_adobe_meta,
          this.file_adobe_meta_tmp,
          bao.toString("UTF-8"));
      } else {
        FileUtilities.fileDelete(this.file_adobe_meta);
        FileUtilities.fileDelete(this.file_adobe_meta_tmp);
        FileUtilities.fileDelete(this.file_adobe_rights);
        FileUtilities.fileDelete(this.file_adobe_rights_tmp);
      }
    }

    private OptionType<AdobeAdeptLoan> getAdobeAdobeRightsInformationLocked()
      throws IOException
    {
      if (this.file_adobe_rights.isFile()) {
        final byte[] serialized = FileUtilities.fileReadBytes(
          this.file_adobe_rights);

        final ObjectMapper jom = new ObjectMapper();
        final JsonNode jn = jom.readTree(this.file_adobe_meta);
        final ObjectNode o = JSONParserUtilities.checkObject(null, jn);

        final AdobeLoanID loan_id =
          new AdobeLoanID(JSONParserUtilities.getString(o, "loan-id"));
        final boolean returnable =
          JSONParserUtilities.getBoolean(o, "returnable");

        final AdobeAdeptLoan loan =
          new AdobeAdeptLoan(loan_id, ByteBuffer.wrap(serialized), returnable);
        return Option.some(loan);
      }

      return Option.none();
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

    @Override public OPDSAcquisitionFeedEntry entryGetFeedData()
      throws IOException
    {
      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
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

    @Override public BookDatabaseEntrySnapshot entrySetFeedData(
      final OPDSAcquisitionFeedEntry in_entry)
      throws IOException
    {
      final ObjectNode d = this.serializer.serializeFeedEntry(in_entry);

      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
        new PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException>()
        {
          @Override public BookDatabaseEntrySnapshot call(
            final Unit x)
            throws IOException
          {
            BookDatabaseEntry.this.setDataLocked(d);
            return BookDatabaseEntry.this.updateSnapshotLocked();
          }
        });
    }

    private void deleteSnapshot()
    {
      synchronized (BookDatabase.this.snapshots) {
        final String sid = this.id.getShortID();
        BookDatabase.LOG.debug("[{}]: deleting snapshot", sid);
        BookDatabase.this.snapshots.remove(this.id);
      }
    }

    private BookDatabaseEntrySnapshot updateSnapshotLocked()
      throws IOException
    {
      final BookDatabaseEntrySnapshot e = this.getSnapshotLocked();
      synchronized (BookDatabase.this.snapshots) {
        final String sid = this.id.getShortID();
        BookDatabase.LOG.debug("[{}]: updating snapshot {}", sid, e);
        BookDatabase.this.snapshots.put(this.id, e);
        return e;
      }
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
      this.log.debug("updating data {}", this.file_meta);

      final OutputStream os = new FileOutputStream(this.file_meta_tmp);

      try {
        this.serializer.serializeToStream(d, os);
      } finally {
        os.flush();
        os.close();
      }

      FileUtilities.fileRename(this.file_meta_tmp, this.file_meta);
    }

    @Override public File entryGetDirectory()
    {
      return this.directory;
    }

    @Override public BookID entryGetBookID()
    {
      return this.id;
    }

    @Override public BookDatabaseEntrySnapshot entryGetSnapshot()
      throws IOException
    {
      return FileLocking.withFileThreadLocked(
        this.file_lock,
        (long) BookDatabase.LOCK_WAIT_MAXIMUM_MILLISECONDS,
        new PartialFunctionType<Unit, BookDatabaseEntrySnapshot, IOException>()
        {
          @Override public BookDatabaseEntrySnapshot call(
            final Unit x)
            throws IOException
          {
            return BookDatabaseEntry.this.updateSnapshotLocked();
          }
        });
    }

    private BookDatabaseEntrySnapshot getSnapshotLocked()
      throws IOException
    {
      final OPDSAcquisitionFeedEntry in_entry = this.getDataLocked();
      final OptionType<File> in_cover = this.getCoverLocked();
      final OptionType<File> in_book = this.getBookLocked();
      final OptionType<AdobeAdeptLoan> in_adobe_rights =
        this.getAdobeAdobeRightsInformationLocked();
      return new BookDatabaseEntrySnapshot(
        this.id, in_cover, in_book, in_entry, in_adobe_rights);
    }
  }
}
