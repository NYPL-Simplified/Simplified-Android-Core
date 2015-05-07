

import java.util.ArrayList;
import java.util.List;

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

  public static <T> Pair<T, List<T>> stackPop(
    final List<T> s)
  {
    NullCheck.notNull(s);

    if (s.isEmpty()) {
      throw new IllegalArgumentException("Empty stack");
    }

    final List<T> r = new ArrayList<T>(s);
    final T last = NullCheck.notNull(r.remove(r.size() - 1));
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

  public static <T> List<T> stackPush(
    final List<T> s,
    final T e)
  {
    NullCheck.notNull(s);
    NullCheck.notNull(e);

    final List<T> r = new ArrayList<T>(s);
    r.add(e);
    return NullCheck.notNull(r);
  }
}
