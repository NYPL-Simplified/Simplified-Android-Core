package org.nypl.simplified.app;

import java.net.URI;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogActivity extends NavigableFragmentActivity
{
  private static final String TAG = "CatalogActivity";

  @Override protected NavigableFragment newInitialFragment(
    final int container)
  {
    Log.d(CatalogActivity.TAG, "newInitialFragment");

    final Resources rr = this.getResources();
    final URI u =
      NullCheck.notNull(URI.create(rr.getString(R.string.catalog_start_uri)));
    return CatalogLoadingFragment.newInstance(u, container);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(CatalogActivity.TAG, "onCreate");
  }
}
