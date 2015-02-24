package org.nypl.simplified.app;

import com.io7m.jnull.NullCheck;

public final class PartSettings implements PartType
{
  private final MainActivity activity;

  public PartSettings(
    final MainActivity act)
  {
    this.activity = NullCheck.notNull(act);
  }

  @Override public <A, E extends Exception> A partMatch(
    final PartMatcherType<A, E> m)
    throws E
  {
    return m.matchSettings(this);
  }

  @Override public void partBegin()
  {
    final SettingsFragment sf = new SettingsFragment();
    this.activity.fragControllerSetContentFragmentWithBackOptionalReturn(
      this.activity.fragControllerGetContentFragmentCurrent(),
      sf);
  }
}
