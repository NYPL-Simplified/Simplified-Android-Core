package org.nypl.simplified.observable;

import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.NullCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observer;

/**
 * A parameterized version of {@link java.util.Observable}.
 *
 * @param <T> The type of observable values
 */

public final class Observable<T> implements ObservableType<T> {

  private static final Logger LOG = LoggerFactory.getLogger(Observable.class);

  private final ObservableWrapper<T> observable;

  private Observable() {
    this.observable = new ObservableWrapper<>();
  }

  private static final class ObservableWrapper<T> extends java.util.Observable {

    public void send(
        final T value) {
      this.setChanged();
      this.notifyObservers(value);
    }

    ObservableWrapper()
    {

    }
  }

  /**
   * Create a new observable that notifies observers with values of type {@code T}.
   *
   * @param <T> The type of observable events
   * @return A new observable
   */

  public static <T> ObservableType<T> create() {
    return new Observable<>();
  }

  @Override
  public ObservableSubscriptionType<T> subscribe(
      final ProcedureType<T> receiver) {

    NullCheck.notNull(receiver, "Receiver");

    /*
     * Note: This is specifically *not* a lambda because, depending on the translation
     * strategy the compiler uses for lambdas, the use of a lambda might result in registering
     * the "same" observer over and over. In other words, we cannot implicitly rely on two instances
     * of a lambda expression actually being different with regards to == or Object.equals().
     * See JLS section 15.27.4, Run-time Evaluation of Lambda Expressions:
     */

    final Observer observer = new Observer() {
      @Override
      public void update(
          final java.util.Observable observable,
          final Object o) {
        try {
          receiver.call((T) o);
        } catch (final Exception e) {
          LOG.error("observer raised exception: ", e);
        }
      }
    };

    this.observable.addObserver(observer);
    return new Subscription<>(this.observable, observer);
  }

  @Override
  public int count() {
    return this.observable.countObservers();
  }

  @Override
  public void send(final T value) {
    this.observable.send(NullCheck.notNull(value, "Value"));
  }

  private static final class Subscription<T> implements ObservableSubscriptionType<T> {

    private final ObservableWrapper<T> observable;
    private final Observer observer;

    Subscription(
        final ObservableWrapper<T> observable,
        final Observer observer) {
      this.observable = NullCheck.notNull(observable, "Observable");
      this.observer = NullCheck.notNull(observer, "Observer");
    }

    @Override
    public void unsubscribe() {
      this.observable.deleteObserver(this.observer);
    }
  }
}
