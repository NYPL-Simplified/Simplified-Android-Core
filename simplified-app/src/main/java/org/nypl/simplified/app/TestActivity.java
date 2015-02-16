package org.nypl.simplified.app;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.io7m.jnull.Nullable;

public final class TestActivity extends NavigableFragmentActivity
{
  private static final String TAG = "TestActivity";

  @Override protected NavigableFragment newInitialFragment(
    final int container)
  {
    Log.d(TestActivity.TAG, "newInitialFragment");

    final Resources rr = this.getResources();
    return ExampleFragment.newInstanceWithoutParent(container);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(TestActivity.TAG, "onCreate");
  }
}
