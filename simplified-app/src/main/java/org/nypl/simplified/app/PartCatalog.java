package org.nypl.simplified.app;

import java.net.URI;

import android.app.Fragment;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

public final class PartCatalog implements PartType
{
  private final MainActivity activity;

  public PartCatalog(
    final MainActivity act)
  {
    this.activity = NullCheck.notNull(act);
  }

  @Override public <A, E extends Exception> A partMatch(
    final PartMatcherType<A, E> m)
    throws E
  {
    return m.matchCatalog(this);
  }

  @Override public void partSwitchTo()
  {
    final Simplified app = Simplified.get();
    final URI uri = app.getFeedInitialURI();
    final CatalogLoadingFragment clf =
      CatalogLoadingFragment.newInstance(uri);

    final OptionType<Fragment> current =
      this.activity.fragControllerGetContentFragmentCurrent();
    this.activity.fragControllerSetContentFragmentWithBackOptionalReturn(
      current,
      clf);
  }
}
