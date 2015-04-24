package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class ReaderTOCActivity extends Activity
{
  public static final int     TOC_SELECTION_REQUEST_CODE;
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderTOCActivity.class);
    TOC_SELECTION_REQUEST_CODE = 23;
  }

  public static void startActivityForResult(
    final Activity from)
  {
    NullCheck.notNull(from);
    final Intent i = new Intent(Intent.ACTION_PICK);
    i.setClass(from, ReaderTOCActivity.class);
    from.startActivityForResult(
      i,
      ReaderTOCActivity.TOC_SELECTION_REQUEST_CODE);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    ReaderTOCActivity.LOG.debug("onCreate");

    final Intent intent = new Intent();
    this.setResult(Activity.RESULT_OK, intent);
    this.finish();
  }
}
