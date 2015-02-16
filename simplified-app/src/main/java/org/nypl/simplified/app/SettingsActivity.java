package org.nypl.simplified.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class SettingsActivity extends PartActivity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final LayoutInflater inflater =
      (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    final ViewGroup ca =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.content_area));
    final ViewGroup s =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.settings,
        ca,
        false));

    ca.addView(s);
    ca.requestLayout();
  }

  @Override public void onBackPressed()
  {
    super.onBackPressed();
    this.finish();
    this.overridePendingTransition(0, 0);
  }

  @Override public <A, E extends Exception> A matchPartActivity(
    final PartActivityMatcherType<A, E> m)
    throws E
  {
    return m.settings(this);
  }
}
