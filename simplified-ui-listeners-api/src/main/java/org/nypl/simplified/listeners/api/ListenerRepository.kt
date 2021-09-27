package org.nypl.simplified.listeners.api

import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.disposables.Disposable
import kotlin.reflect.KClass

class ListenerRepository<E : Any, S : Any>(private var state: S) : ViewModel() {

  private val listeners: MutableMap<KClass<*>, FragmentListenerType<*>> =
    mutableMapOf()

  private val queue: UnicastWorkSubject<E> =
    UnicastWorkSubject.create()

  private var subscription: Disposable? =
    null

  /**
   * Register a handler for events coming up to this repository.
   */

  fun registerHandler(
    callback: (E, S) -> S
  ) {
    this.subscription =
      this.queue.subscribe { command ->
        this.state = callback(command, this.state)
      }
  }

  /**
   * Unregister a previously registered handler if any.
   */

  fun unregisterHandler() {
    this.subscription?.dispose()
    this.subscription = null
  }

  /**
   * Register the ability of this repository to handle some type of events which will be
   * wrapped by the provided closure.
   */

  fun <T : Any> registerListener(kClass: KClass<T>, wrap: (T) -> E) {
    this.listeners[kClass] = FragmentListenerType<T> { event -> this.queue.onNext(wrap(event)) }
  }

  /**
   * Get the listener available in this repository for the given type of events, if any.
   */

  fun <T : Any> getListener(kClass: KClass<T>): FragmentListenerType<T>? {
    @Suppress("UNCHECKED_CAST")
    return this.listeners.get(kClass) as? FragmentListenerType<T>
  }
}
