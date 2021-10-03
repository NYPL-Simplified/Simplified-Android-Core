package org.nypl.simplified.cardcreator.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.CardCreatorContract
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentConfirmationBinding
import org.nypl.simplified.cardcreator.utils.Cache
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class ConfirmationFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ConfirmationFragment::class.java)
  private val bundle by lazy { ConfirmationFragmentArgs.fromBundle(requireArguments()) }

  private var _binding: FragmentConfirmationBinding? = null
  private val binding get() = _binding!!

  private val storageRequestCode = 203
  private val dateFormat = "dd-MM-yyyy"

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentConfirmationBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.nameCard.text = bundle.name
    binding.cardBarcode.text = getString(R.string.user_card_number, bundle.barcode)
    binding.cardPin.text = getString(R.string.user_password, bundle.password)
    binding.headerStatusDescTv.text = bundle.message

    val currentDate: String = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date())
    binding.issued.text = getString(R.string.issued_date, currentDate)

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      logger.debug("User created card")
      returnResult()
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {
      if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED
      ) {
        logger.debug("Requesting storage permission")
        ActivityCompat.requestPermissions(
          requireActivity(),
          arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
          storageRequestCode
        )
      } else {
        createDigitalCard()
      }
    }
  }

  /**
   * Create library card from ImageView
   */
  private fun createDigitalCard() {
    GlobalScope.launch(Dispatchers.Main) {
      val card = binding.libraryCard
      card.isDrawingCacheEnabled = true
      val bitmap = card.drawingCache
      val f: File
      try {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
          val file = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES)
          if (!file.exists()) {
            file.mkdirs()
          }
          f = File(file.absolutePath, "library-card.png")
          val outStream = FileOutputStream(f)
          bitmap.compress(Bitmap.CompressFormat.PNG, 10, outStream)
          outStream.close()
          addCardToGallery(f)

          Toast.makeText(requireContext(), getString(R.string.card_saved), Toast.LENGTH_SHORT).show()
        }
      } catch (e: Exception) {
        logger.error("Error creating digital card", e)
      }
    }
  }

  /**
   * Adds library card image to device photo gallery
   */
  private fun addCardToGallery(card: File) {
    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    val contentUri = Uri.fromFile(card)
    mediaScanIntent.data = contentUri
    requireActivity().sendBroadcast(mediaScanIntent)
  }

  /**
   * Listen for result from location permission request, this method is a callback provided by
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
          createDigitalCard()
        } else {
          logger.debug("Storage permission NOT granted")
        }
        return
      }
    }
  }

  /**
   * Returns result to caller with card details
   */
  private fun returnResult() {
    val bundle = CardCreatorContract.createResult(bundle.barcode, bundle.password)
      .apply {
        putString("username", bundle.username)
        putString("message", bundle.message)
      }
    val data = Intent().apply {
      putExtras(bundle)
    }
    if (requireActivity().intent.getBooleanExtra("isLoggedIn", false)) {
      requireActivity().setResult(Activity.RESULT_CANCELED, data)
    } else {
      requireActivity().setResult(Activity.RESULT_OK, data)
    }
    Cache(requireContext()).clear()
    requireActivity().finish()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
