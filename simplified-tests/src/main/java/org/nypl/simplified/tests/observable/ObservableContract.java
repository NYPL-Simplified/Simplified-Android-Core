package org.nypl.simplified.tests.observable;

import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.NullCheck;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.observable.Observable;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.nypl.simplified.observable.ObservableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The contract for Observables.
 */

public abstract class ObservableContract {

  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(ObservableContract.class));
  }

  @Test
  public final void testSubscribe() throws Exception {

    final ObservableType<Integer> o = Observable.create();
    Assert.assertEquals(0, o.count());

    final AtomicInteger calls = new AtomicInteger();
    final HashSet<Integer> values = new HashSet<>();

    final ProcedureType<Integer> receiver = new ProcedureType<Integer>() {
      @Override
      public void call(final Integer x) {
        LOG.debug("call: {}", x);
        calls.incrementAndGet();
        values.add(x);
      }
    };

    final ObservableSubscriptionType<Integer> s0 = o.subscribe(receiver);
    final ObservableSubscriptionType<Integer> s1 = o.subscribe(receiver);
    final ObservableSubscriptionType<Integer> s2 = o.subscribe(receiver);
    Assert.assertEquals(3, o.count());

    for (int index = 0; index < 10; ++index) {
      o.send(index);
    }

    Assert.assertEquals(30, calls.get());
    for (int index = 0; index < 10; ++index) {
      Assert.assertTrue("Values must contain " + index, values.contains(index));
    }

    s0.unsubscribe();
    s1.unsubscribe();

    calls.set(0);
    values.clear();

    for (int index = 0; index < 10; ++index) {
      o.send(index);
    }

    Assert.assertEquals(10, calls.get());
    for (int index = 0; index < 10; ++index) {
      Assert.assertTrue("Values must contain " + index, values.contains(index));
    }
  }
}
