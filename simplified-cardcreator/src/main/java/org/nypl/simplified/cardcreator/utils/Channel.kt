package org.nypl.simplified.cardcreator.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class Channel<T>(
  private val channel: Channel<T> = Channel(Channel.BUFFERED)
) {

  suspend fun send(event: T) {
    channel.send(event)
  }

  fun receive(lifecycleOwner: LifecycleOwner, callback: suspend (T) -> Unit) {
    val observer = FlowObserver(lifecycleOwner, channel.receiveAsFlow(), callback)
    lifecycleOwner.lifecycle.addObserver(observer)
  }
}

class FlowObserver<T> (
  private val lifecycleOwner: LifecycleOwner,
  private val flow: Flow<T>,
  private val collector: suspend (T) -> Unit
) : LifecycleObserver {

  private var job: Job? = null

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onStart() {
    if (job == null) {
      job = lifecycleOwner.lifecycleScope.launch {
        flow.collect { collector(it) }
      }
    }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onStop() {
    job?.cancel()
    job = null
  }
}

inline fun <reified T> Flow<T>.observeWhenStarted(
  lifecycleOwner: LifecycleOwner,
  noinline collector: suspend (T) -> Unit
) {
  val observer = FlowObserver(lifecycleOwner, this, collector)
  lifecycleOwner.lifecycle.addObserver(observer)
}
