package org.nypl.simplified.listeners.api

fun interface FragmentListenerType<T : Any> {

  fun post(event: T)
}
