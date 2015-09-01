package org.nypl.simplified.app.reader;

import android.app.Activity;
import android.os.Bundle;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BookID;
import org.slf4j.Logger;

import java.io.File;

/**
 * A secret activity to fork the reader with a specific book ID and EPUB file.
 */

public final class ReaderTestActivity extends Activity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderTestActivity.class);
  }

  /**
   * Construct an activity.
   */

  public ReaderTestActivity()
  {

  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final File epub_file =
      new File("/storage/sdcard0//Android/data/org.nypl.simplified.app/files/books/data/d652c5bb62a8a4d6ebfb791766ead779668c51175e6b66757daa147bec20b8ce/book.epub");
    final BookID id = BookID.exactString("0");

    ReaderTestActivity.LOG.debug(
      "forking reader activity with {} : {}", id, epub_file);

    ReaderActivity.startActivity(this, id, epub_file);
    this.finish();
  }
}
