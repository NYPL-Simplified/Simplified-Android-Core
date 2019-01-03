package org.nypl.simplified.observable;

/**
 * A parameterized version of {@link java.util.Observable}.
 *
 * @param <T> The type of observable values
 */

public interface ObservableType<T> extends ObservableReadableType<T> {

  /**
   * Broadcast the given value to all observers.
   *
   * @param value The value to broadcast
   */

  void send(T value);
}
