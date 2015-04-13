package org.nypl.simplified.app;

import java.io.File;

import org.readium.sdk.android.Container;
import org.readium.sdk.android.EPub3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class ReaderActivity extends Activity
{
  private static final String FILE_ID;
  private static final String TAG = "RA";

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

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    Log.d(ReaderActivity.TAG, "opened");
    final Container container = EPub3.openBook("/storage/sdcard/book.epub");
    container.close();
    Log.d(ReaderActivity.TAG, "closed");
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
  }

  @Override protected void onResume()
  {
    super.onResume();
  }
}
