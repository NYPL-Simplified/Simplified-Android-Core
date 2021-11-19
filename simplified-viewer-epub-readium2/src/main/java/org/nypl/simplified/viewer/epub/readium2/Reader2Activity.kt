package org.nypl.simplified.viewer.epub.readium2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.reactivex.disposables.Disposable
import org.librarysimplified.r2.views.SR2ReaderFragment
import org.librarysimplified.r2.views.SR2ReaderFragmentFactory
import org.librarysimplified.r2.views.SR2ReaderParameters
import org.librarysimplified.r2.views.SR2ReaderViewEvent
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookOpened
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationClose
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationOpenTOC
import org.librarysimplified.r2.views.SR2ReaderViewModel
import org.librarysimplified.r2.views.SR2ReaderViewModelFactory
import org.librarysimplified.r2.views.SR2TOCFragment
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accessibility.AccessibilityServiceType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException

/**
 * The main reader activity for reading an EPUB using Readium 2.
 */

class Reader2Activity : AppCompatActivity(R.layout.reader2) {

  companion object {

    private const val ARG_PARAMETERS =
      "org.nypl.simplified.viewer.epub.readium2.ReaderActivity2.parameters"

    private const val READER_FRAGMENT_TAG =
      "org.librarysimplified.r2.views.SR2ReaderFragment"

    private const val TOC_FRAGMENT_TAG =
      "org.librarysimplified.r2.views.SR2TOCFragment"

    /**
     * Start a new reader for the given book.
     */

    fun startActivity(
      context: Activity,
      parameters: Reader2ActivityParameters
    ) {
      val intent = Intent(context, Reader2Activity::class.java)
      val bundle = Bundle().apply {
        this.putSerializable(this@Companion.ARG_PARAMETERS, parameters)
      }
      intent.putExtras(bundle)
      context.startActivity(intent)
    }
  }

  private val logger =
    LoggerFactory.getLogger(Reader2Activity::class.java)

  private val services =
    Services.serviceDirectory()
  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)
  private val uiThread =
    services.requireService(UIThreadServiceType::class.java)

  private val parameters: Reader2ActivityParameters by lazy {
    requireNotNull(
      this.intent.getSerializableExtra(ARG_PARAMETERS)
        as? Reader2ActivityParameters?
    ) { "ReaderActivity2 Intent lacks parameters" }
  }

  private val readerParameters: SR2ReaderParameters by lazy {
    Reader2ParametersAdapter(
      application = this.application,
      currentProfile = this.profilesController.profileCurrent(),
      accessibilityService = this.services.requireService(AccessibilityServiceType::class.java)
    ).adapt(this.parameters)
  }

  private val viewModel: Reader2ViewModel by viewModels(
    factoryProducer = {
      val sr2Factory = { SR2ReaderViewModelFactory(application, readerParameters) }
      val sr2Model: SR2ReaderViewModel by viewModels(factoryProducer = sr2Factory)
      Reader2ViewModelFactory(applicationInfo, parameters, services, sr2Model)
    }
  )

  private lateinit var readerFragment: Fragment
  private lateinit var tocFragment: Fragment
  private var viewSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    try {
      this.profilesController.profileCurrent()
        .account(this.parameters.accountId)
    } catch (e: Exception) {
      this.logger.error("unable to locate account: ", e)
      this.finish()
      return
    }

    this.supportFragmentManager.fragmentFactory =
      SR2ReaderFragmentFactory(readerParameters)

    super.onCreate(savedInstanceState)

    if (savedInstanceState == null) {
      this.readerFragment =
        this.supportFragmentManager.fragmentFactory.instantiate(this.classLoader, SR2ReaderFragment::class.java.name)
      this.tocFragment =
        this.supportFragmentManager.fragmentFactory.instantiate(this.classLoader, SR2TOCFragment::class.java.name)

      this.supportFragmentManager.beginTransaction()
        .replace(R.id.reader2FragmentHost, this.readerFragment, READER_FRAGMENT_TAG)
        .add(R.id.reader2FragmentHost, this.tocFragment, TOC_FRAGMENT_TAG)
        .hide(this.tocFragment)
        .commit()
    } else {
      this.readerFragment =
        this.supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG) as SR2ReaderFragment
      this.tocFragment =
        this.supportFragmentManager.findFragmentByTag(TOC_FRAGMENT_TAG) as SR2TOCFragment
    }
  }

  override fun onStart() {
    super.onStart()

    this.viewSubscription =
      this.viewModel.viewEvents.subscribe(this::onViewEvent)
  }

  override fun onStop() {
    super.onStop()
    this.viewSubscription?.dispose()
  }

  /**
   * Handle incoming messages from the view fragments.
   */

  private fun onViewEvent(event: SR2ReaderViewEvent) {
    this.uiThread.checkIsUIThread()

    return when (event) {
      SR2ReaderViewNavigationClose ->
        this.tocClose()
      SR2ReaderViewNavigationOpenTOC ->
        this.tocOpen()
      is SR2BookOpened -> {
        // Nothing to do
      }
      is SR2BookLoadingFailed ->
        this.onBookLoadingFailed(event.exception)
    }
  }

  override fun onBackPressed() {
    if (this.tocFragment.isVisible) {
      this.tocClose()
    } else {
      super.onBackPressed()
    }
  }

  /**
   * Close the table of contents.
   */

  private fun tocClose() {
    this.uiThread.checkIsUIThread()

    this.logger.debug("TOC closing")
    this.supportFragmentManager.beginTransaction()
      .hide(this.tocFragment)
      .show(this.readerFragment)
      .commit()
  }

  /**
   * Open the table of contents.
   */

  private fun tocOpen() {
    this.uiThread.checkIsUIThread()

    this.logger.debug("TOC opening")
    this.supportFragmentManager.beginTransaction()
      .hide(this.readerFragment)
      .show(this.tocFragment)
      .commit()
  }

  /**
   * Loading a book failed.
   */

  private fun onBookLoadingFailed(
    exception: Throwable
  ) {
    this.uiThread.checkIsUIThread()

    val actualException =
      if (exception is ExecutionException) {
        exception.cause ?: exception
      } else {
        exception
      }

    AlertDialog.Builder(this)
      .setTitle(R.string.bookOpenFailedTitle)
      .setMessage(this.getString(R.string.bookOpenFailedMessage, actualException.javaClass.name, actualException.message))
      .setOnDismissListener { this.finish() }
      .create()
      .show()
  }
}
