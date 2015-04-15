package org.nypl.simplified.app.reader;

import java.io.File;

import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class ReaderActivity extends Activity implements
  ReaderHTTPServerStartListenerType
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

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();
    final ReaderHTTPServerType hs = rs.getHTTPServer();
    hs.start(this);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
  }

  @Override protected void onResume()
  {
    super.onResume();
  }

  @Override public void onServerStartFailed(
    final ReaderHTTPServerType hs,
    final Throwable x)
  {
    ErrorDialogUtilities.showError(this, "Could not start http server", x);
    ReaderActivity.this.finish();
  }

  @Override public void onServerStartSucceeded(
    final ReaderHTTPServerType hs)
  {
    Log.d(ReaderActivity.TAG, "http server started");
  }
}
