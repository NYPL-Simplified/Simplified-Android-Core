package org.nypl.simplified.app.catalog;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.reader.ReaderActivity;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot;
import org.nypl.simplified.books.core.BookDatabaseReadableType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.prefs.Prefs;
import org.slf4j.Logger;

import java.io.File;

/**
 * A controller that opens a given book for reading.
 */

public final class CatalogBookRead implements OnClickListener
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookRead.class);
  }

  private final Activity activity;
  private final BookID   id;
  private final FeedEntryOPDS entry;

  /**
   * The parent activity.
   * @param in_activity   The activity
   * @param in_id         The book ID
   * @param in_entry      The OPDS feed entry
   */

  public CatalogBookRead(
    final Activity in_activity,
    final BookID in_id,
    final FeedEntryOPDS in_entry)
  {
    this.activity = NullCheck.notNull(in_activity);
    this.id = NullCheck.notNull(in_id);
    this.entry = NullCheck.notNull(in_entry);
  }

  @Override public void onClick(
    final @Nullable View v)
  {

    final Prefs prefs = new Prefs(this.activity);
    prefs.putBoolean("post_last_read", false);
    LOG.debug("CurrentPage prefs {}", prefs.getBoolean("post_last_read"));

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final BooksType books = app.getBooks();
    final BookDatabaseReadableType db = books.bookGetDatabase();
    final Activity a = this.activity;

    final OptionType<BookDatabaseEntrySnapshot> snap_opt =
      db.databaseGetEntrySnapshot(this.id);

    if (snap_opt.isSome()) {
      final Some<BookDatabaseEntrySnapshot> some_snap =
        (Some<BookDatabaseEntrySnapshot>) snap_opt;
      final BookDatabaseEntrySnapshot snap = some_snap.get();
      final OptionType<File> book_opt = snap.getBook();
      if (book_opt.isSome()) {
        final Some<File> some_book = (Some<File>) book_opt;
        final File book = some_book.get();
        ReaderActivity.startActivity(a, this.id, book, this.entry);
      } else {
        ErrorDialogUtilities.showError(
          a,
          CatalogBookRead.LOG,
          "Bug: book claimed to be downloaded but no book file exists in "
          + "storage",
          null);
      }
    } else {
      ErrorDialogUtilities.showError(
        a, CatalogBookRead.LOG, "Book no longer exists!", null);
    }
  }
}
