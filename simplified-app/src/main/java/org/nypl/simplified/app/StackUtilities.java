package org.nypl.simplified.app;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;

/**
 * Simple stack utilities over immutable lists.
 */

public final class StackUtilities
{
  /**
   * Pop the last element of the given list, returning it and the new list.
   *
   * @param s
   *          The list
   * @return A pair consisting of the popped element and the new list
   */

  public static <T> Pair<T, ImmutableList<T>> stackPop(
    final List<T> s)
  {
    if (s.isEmpty()) {
      throw new IllegalArgumentException("Empty stack");
    }

    final Builder<T> b = ImmutableList.builder();
    final int undersize = s.size() - 1;
    for (int index = 0; index < undersize; ++index) {
      b.add(s.get(index));
    }
    final T last = NullCheck.notNull(s.get(undersize));
    final ImmutableList<T> r = NullCheck.notNull(b.build());
    return Pair.pair(last, r);
  }

  /**
   * Push the given element to the given list.
   * 
   * @param s
   *          The list
   * @param e
   *          The element
   * @return A new list
   */

  public static <T> ImmutableList<T> stackPush(
    final List<T> s,
    final T e)
  {
    final Builder<T> b = ImmutableList.builder();
    b.addAll(s);
    b.add(e);
    return NullCheck.notNull(b.build());
  }
}
