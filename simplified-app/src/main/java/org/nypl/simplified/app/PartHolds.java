package org.nypl.simplified.app;

import com.io7m.jnull.NullCheck;

public final class PartHolds implements PartType
{
  private final MainActivity activity;

  public PartHolds(
    final MainActivity act)
  {
    this.activity = NullCheck.notNull(act);
  }

  @Override public <A, E extends Exception> A partMatch(
    final PartMatcherType<A, E> m)
    throws E
  {
    return m.matchHolds(this);
  }

  @Override public void partSwitchTo()
  {
    final HoldsFragment f = new HoldsFragment();
    this.activity.fragControllerSetContentFragmentWithBackOptionalReturn(
      this.activity.fragControllerGetContentFragmentCurrent(),
      f);
  }
}
