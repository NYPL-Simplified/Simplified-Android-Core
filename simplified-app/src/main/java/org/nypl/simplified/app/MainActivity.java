package org.nypl.simplified.app;

import android.app.Activity;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class MainActivity extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.main);
  }
}
