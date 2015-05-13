package org.nypl.simplified.tests.stack;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.stack.ImmutableStack;

import com.io7m.jfunctional.Pair;

@SuppressWarnings({ "null", "static-method" }) public final class ImmutableStackTest
{
  @Test public void testEmpty()
  {
    final ImmutableStack<Integer> s0 = ImmutableStack.empty();
    final ImmutableStack<Integer> s1 = ImmutableStack.empty();

    Assert.assertEquals(s0, s1);
    Assert.assertEquals(s0.hashCode(), s1.hashCode());
    Assert.assertEquals(s0.toString(), s1.toString());
  }

  @Test(expected = IllegalStateException.class) public void testEmptyBad()
  {
    final ImmutableStack<Integer> s0 = ImmutableStack.empty();
    s0.pop();
  }

  @Test public void testPushPop()
  {
    final ImmutableStack<Integer> s0 = ImmutableStack.empty();
    final ImmutableStack<Integer> s1 = s0.push(Integer.valueOf(23));
    final ImmutableStack<Integer> s2 = s0.push(Integer.valueOf(23));

    Assert.assertEquals(s1, s2);
    Assert.assertEquals(s1.hashCode(), s2.hashCode());
    Assert.assertEquals(s1.toString(), s2.toString());

    final Pair<Integer, ImmutableStack<Integer>> p2 = s2.pop();
    Assert.assertEquals(p2.getRight(), s0);
    Assert.assertEquals(p2.getLeft(), Integer.valueOf(23));
  }
}
