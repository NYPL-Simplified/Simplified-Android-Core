package org.nypl.simplified.stack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * An immutable stack.
 *
 * @param <T> The type of elements.
 */

public final class ImmutableStack<T> implements Serializable
{
  private static final long serialVersionUID = 1L;

  /**
   * @return A new empty stack
   * @param <T> The type of stack elements
   */

  public static <T> ImmutableStack<T> empty()
  {
    return new ImmutableStack<T>(new ArrayList<T>());
  }

  private final List<T> elements;

  private ImmutableStack(
    final List<T> in_e)
  {
    this.elements = NullCheck.notNull(in_e);
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final ImmutableStack<?> other = (ImmutableStack<?>) obj;
    return this.elements.equals(other.elements);
  }

  @Override public int hashCode()
  {
    return this.elements.hashCode();
  }

  /**
   * @return {@code true} iff the stack is empty
   */

  public boolean isEmpty()
  {
    return this.elements.isEmpty();
  }

  /**
   * Pop the last element of the current, returning it and a new stack without
   * that element.
   *
   * @return A pair consisting of the popped element and the new list
   * @throws IllegalStateException
   *           If the current stack is empty
   */

  public Pair<T, ImmutableStack<T>> pop()
  {
    if (this.elements.isEmpty()) {
      throw new IllegalStateException("Empty stack");
    }

    final List<T> r = new ArrayList<T>(this.elements);
    final T last = NullCheck.notNull(r.remove(r.size() - 1));
    return Pair.pair(last, new ImmutableStack<T>(r));
  }

  /**
   * Push the given element to the current stack, returning a new stack.
   *
   * @param x
   *          The element
   * @return A new stack
   */

  public ImmutableStack<T> push(
    final T x)
  {
    NullCheck.notNull(x);

    final List<T> r = new ArrayList<T>(this.elements);
    r.add(x);
    return new ImmutableStack<T>(r);
  }

  @Override public String toString()
  {
    return NullCheck.notNull(this.elements.toString());
  }
}
