package org.nypl.simplified.ui.splash

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.boot.api.BootEvent
import org.slf4j.LoggerFactory

class BootFragment : Fragment(R.layout.splash_boot) {

  private lateinit var image: ImageView
  private lateinit var text: TextView
  private lateinit var progress: ProgressBar
  private lateinit var error: ImageView
  private lateinit var sendError: Button
  private lateinit var version: TextView
  private lateinit var exception: TextView

  private val logger = LoggerFactory.getLogger(BootFragment::class.java)
  private val subscriptions = CompositeDisposable()
  private val viewModel: BootViewModel by viewModels(
    ownerProducer = this::requireParentFragment
  )

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    image = view.findViewById(R.id.splashImage)
    progress = view.findViewById(R.id.splashProgress)
    error = view.findViewById(R.id.splashImageError)
    sendError = view.findViewById(R.id.splashSendError)
    version = view.findViewById(R.id.splashVersion)
    exception = view.findViewById(R.id.splashException)
    text = view.findViewById(R.id.splashText)

    image.setImageResource(viewModel.imageResource)
    image.visibility = View.VISIBLE
    progress.visibility = View.INVISIBLE
    error.visibility = View.INVISIBLE
    sendError.visibility = View.INVISIBLE
    version.text = viewModel.simplifiedVersion
    version.visibility = View.INVISIBLE
    text.visibility = View.INVISIBLE
  }

  override fun onStart() {
    super.onStart()
    viewModel.bootEvents
      .subscribe(this::onBootEvent)
      .let { subscriptions.add(it) }

    /*
     * Clicking the image makes the image invisible but makes the
     * progress bar visible.
     */

    image.setOnClickListener {
      this.popImageView()
    }

    sendError.setOnClickListener {
      viewModel.sendReport()
    }
  }

  override fun onStop() {
    super.onStop()
    subscriptions.clear()
  }

  private fun onBootEvent(event: BootEvent) {
    when (event) {
      is BootEvent.BootInProgress -> {
        text.text = event.message
      }
      is BootEvent.BootCompleted -> {
        setFragmentResult("", Bundle())
      }
      is BootEvent.BootFailed -> {
        onBootFailed(event)
      }
    }
  }

  private fun onBootFailed(event: BootEvent.BootFailed) {
    this.logger.error("boot failed: ", event.exception)
    if (image.alpha > 0.0) {
      this.popImageView()
    }
    // XXX: We need to do better than this.
    // Print a useful message rather than a raw exception message.
    error.visibility = View.VISIBLE
    sendError.visibility = View.VISIBLE
    progress.isIndeterminate = false
    progress.progress = 100
    exception.text = "${event.exception.message}"
    text.text = event.message
  }

  private fun popImageView() {
    progress.visibility = View.VISIBLE
    text.visibility = View.VISIBLE
    image.animation = AnimationUtils.loadAnimation(this.context, R.anim.zoom_fade)
    version.visibility = View.VISIBLE
  }
}
