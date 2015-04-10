package org.nypl.simplified.app;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.io7m.jnull.NullCheck;

public final class ReaderActivity extends Activity
{
  private static final String FILE_ID;

  static {
    FILE_ID = "org.nypl.simplified.app.ReaderActivity.file";
  }

  public static void startActivity(
    final Activity from,
    final File file)
  {
    NullCheck.notNull(file);
    final Bundle b = new Bundle();
    b.putSerializable(ReaderActivity.FILE_ID, file);
    final Intent i = new Intent(from, ReaderActivity.class);
    i.putExtras(b);
    from.startActivity(i);
  }
}
