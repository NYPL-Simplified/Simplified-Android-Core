package org.nypl.simplified.observable;

/**
 * A subscription to an observer that produces values of type {@code T}.
 *
 * @param <T> The type of observable values
 */

public interface ObservableSubscriptionType<T> {

  /**
   * Unsubscribe from the observer.
   */

  void unsubscribe();

}
