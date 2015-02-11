package org.nypl.simplified.app;

import android.app.Activity;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

/**
 * <p>
 * The type of activities that represent a major part of the application.
 * </p>
 * <p>
 * Each instance contains a layout that has buttons to navigate to each of the
 * other major parts.
 * </p>
 */

abstract class PartActivity extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.part);
  }
}
