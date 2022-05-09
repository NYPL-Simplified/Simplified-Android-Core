package org.nypl.simplified.cardcreator.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.nypl.simplified.android.ktx.viewLifecycleAware
import org.nypl.simplified.cardcreator.CardCreatorActivity
import org.nypl.simplified.cardcreator.CardCreatorContract
import org.nypl.simplified.cardcreator.databinding.FragmentConfirmationBinding
import org.nypl.simplified.cardcreator.utils.Cache
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationEvent
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationViewModel
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationViewModelFactory
import org.slf4j.LoggerFactory

class ConfirmationFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ConfirmationFragment::class.java)

  private val args by lazy { ConfirmationFragmentArgs.fromBundle(requireArguments()) }

  private val viewModel by viewModels<ConfirmationViewModel> { ConfirmationViewModelFactory(args) }
  private var binding by viewLifecycleAware<FragmentConfirmationBinding>()

  private val storageRequestCode = 203
  private val dateFormat = "dd-MM-yyyy"

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentConfirmationBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.format = dateFormat
    binding.lifecycleOwner = viewLifecycleOwner
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    lifecycleScope.launch {
      viewModel.state.flowWithLifecycle(lifecycle).collect {
        it.events.firstOrNull()?.let { event -> handleEvent(event) }
      }
    }
  }

  private fun handleEvent(event: ConfirmationEvent) {
    when (event) {
      ConfirmationEvent.CardConfirmed -> returnResult()
      ConfirmationEvent.SaveCardPermissionsCheck -> checkAndRequestPermissions()
      is ConfirmationEvent.AddSavedCardToGallery -> addCardToGallery(event.fileUri)
    }
    viewModel.eventHasBeenHandled(event.id)
  }

  private fun checkAndRequestPermissions() {
    if (ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
      != PackageManager.PERMISSION_GRANTED
    ) {
      logger.debug("Requesting storage permission")
      ActivityCompat.requestPermissions(
        requireActivity(),
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        storageRequestCode
      )
    } else {
      saveLibraryCardImage()
    }
  }

  private fun saveLibraryCardImage() {
    val card = binding.libraryCard
    card.isDrawingCacheEnabled = true
    viewModel.createAndStoreDigitalCard(card.drawingCache)
  }

  /**
   * Adds library card image to device photo gallery
   */
  private fun addCardToGallery(fileUri: Uri) {
    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    mediaScanIntent.data = fileUri
    requireActivity().sendBroadcast(mediaScanIntent)
  }

  /**
   * Listen for result from storage permission request, this method is a callback provided by
   * Android for the requestPermissions() method
   *
   * @param requestCode - String user defined request code to identify the request
   * @param permissions - String Array of permissions requested
   * @param grantResults - Integer array of what the user has granted/denied
   */
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      storageRequestCode -> {
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          logger.debug("Storage permission granted")
          saveLibraryCardImage()
        } else {
          logger.debug("Storage permission NOT granted")
        }
        return
      }
    }
  }

  private fun returnResult() {
    val bundle = CardCreatorContract.resultBundle(
      viewModel.state.value.data.barcode,
      viewModel.state.value.data.password
    )
    val intent = Intent().apply {
      putExtras(bundle)
    }
    if (requireActivity().intent.getBooleanExtra("isLoggedIn", false)) {
      requireActivity().setResult(CardCreatorActivity.CHILD_CARD_CREATED, intent)
    } else {
      requireActivity().setResult(CardCreatorActivity.CARD_CREATED, intent)
    }
    Cache(requireContext()).clear()
    requireActivity().finish()
  }
}
