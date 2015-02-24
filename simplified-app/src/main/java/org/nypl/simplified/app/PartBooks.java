package org.nypl.simplified.app;

import com.io7m.jnull.NullCheck;

public final class PartBooks implements PartType
{
  private final MainActivity activity;

  public PartBooks(
    final MainActivity act)
  {
    this.activity = NullCheck.notNull(act);
  }

  @Override public <A, E extends Exception> A partMatch(
    final PartMatcherType<A, E> m)
    throws E
  {
    return m.matchBooks(this);
  }

  @Override public void partSwitchTo()
  {
    final BooksFragment f = new BooksFragment();
    this.activity.fragControllerSetContentFragmentWithBackOptionalReturn(
      this.activity.fragControllerGetContentFragmentCurrent(),
      f);
  }
}
