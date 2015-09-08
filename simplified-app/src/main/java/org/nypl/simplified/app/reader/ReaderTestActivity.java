package org.nypl.simplified.app.reader;

import android.app.Activity;
import android.os.Bundle;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.books.core.LogUtilities;
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

    final StringBuilder b = new StringBuilder();
    b.append("/storage/sdcard0");
    b.append("/Android/data/org.nypl.simplified.app/files/books/data/");
    b.append(
      "128b9c466faba55f8087f2a59cce25999cd1e2f59627357c398ba823f67397a8");
    b.append("/book.epub");

    final File epub_file = new File(b.toString());
    final BookID id = BookID.exactString("0");

    ReaderTestActivity.LOG.debug(
      "forking reader activity with {} : {}", id, epub_file);

    ReaderActivity.startActivity(this, id, epub_file);
    this.finish();
  }
}
