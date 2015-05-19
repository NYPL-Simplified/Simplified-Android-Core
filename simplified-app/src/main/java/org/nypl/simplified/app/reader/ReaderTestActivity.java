package org.nypl.simplified.app.reader;

import java.io.File;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BookID;
import org.slf4j.Logger;

import android.app.Activity;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class ReaderTestActivity extends Activity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderTestActivity.class);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final File epub_file =
      new File("/storage/sdcard0/epub30-test-0120-20140612.epub");
    final BookID id = BookID.exactString("0");

    ReaderTestActivity.LOG.debug(
      "forking reader activity with {} : {}",
      id,
      epub_file);

    ReaderActivity.startActivity(this, id, epub_file);
    this.finish();
  }
}
