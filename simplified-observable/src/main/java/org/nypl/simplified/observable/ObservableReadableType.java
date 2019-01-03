package org.nypl.simplified.observable;

import com.io7m.jfunctional.ProcedureType;

/**
 * A parameterized version of {@link java.util.Observable}.
 *
 * @param <T> The type of observable values
 */

public interface ObservableReadableType<T> {

  /**
   * Subscribe to the observable. The given {@code receiver} method will receive values of
   * type {@code T}.
   *
   * @param receiver The receiver method
   * @return A subscription
   */

  ObservableSubscriptionType<T> subscribe(
      ProcedureType<T> receiver);

  /**
   * @return The number of subscribers to the observable
   */

  int count();
}
