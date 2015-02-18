package org.nypl.simplified.app;

import android.app.Fragment;

public interface PartType
{
  Fragment getCurrentFragment();

  <A, E extends Exception> A matchPart(
    final PartMatcherType<A, E> m)
    throws E;
}
