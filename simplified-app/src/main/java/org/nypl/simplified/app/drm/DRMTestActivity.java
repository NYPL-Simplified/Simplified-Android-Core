package org.nypl.simplified.app.drm;

import java.io.File;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import android.app.Activity;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class DRMTestActivity extends Activity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(DRMTestActivity.class);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    try {
      final RMSDKProvider p =
        RMSDKProvider.openProvider(
          "42f40c40374851a5b4a3d8375cb98924",
          "NYPL Reader",
          new File("/data/local/tmp/simplified"),
          new File("/data/local/tmp/simplified"),
          new File("/data/local/tmp/simplified"));
    } catch (final DRMUnsupportedException e) {
      DRMTestActivity.LOG.error("unsupported drm: ", e);
    }
  }
}
