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
import org.nypl.simplified.cardcreator.R
import org.nypl.simplified.cardcreator.databinding.FragmentConfirmationBinding
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ConfirmationFragment : Fragment() {

  private val logger = LoggerFactory.getLogger(ConfirmationFragment::class.java)

  private var _binding: FragmentConfirmationBinding? = null
  private val binding get() = _binding!!

  private lateinit var type: String
  private lateinit var username: String
  private lateinit var barcode: String
  private lateinit var pin: String
  private var temporary: Boolean = true
  private lateinit var message: String

  private val STORAGE_REQUEST_CODE = 203

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentConfirmationBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    arguments?.let {
      val bundle = ConfirmationFragmentArgs.fromBundle(it)
      type = bundle.type
      username = bundle.username
      barcode = bundle.barcode
      pin = bundle.pin
      temporary = bundle.temporary
      message = bundle.message

      binding.nameCard.text = bundle.name
      binding.cardBarcode.text = "Card Number: ${bundle.barcode}"
      binding.cardPin.text = "PIN: ${bundle.pin}"
      binding.headerStatusDescTv.text = bundle.message

      val currentDate: String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
      binding.issued.text = "Issued: $currentDate"
    }

    // Go to next screen
    binding.nextBtn.setOnClickListener {
      logger.debug("User created card")
      returnResult()
    }

    // Go to previous screen
    binding.prevBtn.setOnClickListener {

      if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED
      ) {
        logger.debug("Requesting storage permission")
        ActivityCompat.requestPermissions(
          activity!!,
          arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
          STORAGE_REQUEST_CODE
        )
      } else {
        val card = binding.libraryCard
        card.isDrawingCacheEnabled = true
        val bitmap = card.drawingCache
        var f: File
        try {
          if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val file = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES)
            if (!file.exists()) {
              file.mkdirs()
            }
            f = File(file.absolutePath + File.separator + "library-card" + ".png")
            val outStream: FileOutputStream = FileOutputStream(f)
            bitmap.compress(Bitmap.CompressFormat.PNG, 10, outStream)
            outStream.close()
            addCardToGallery(f)
            Toast.makeText(activity!!, getString(R.string.card_saved), Toast.LENGTH_SHORT).show()
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  /**
   * Adds library card image to device photo gallery
   */
  private fun addCardToGallery(card: File) {
    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    val contentUri: Uri = Uri.fromFile(card)
    mediaScanIntent.data = contentUri
    activity!!.sendBroadcast(mediaScanIntent)
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
      STORAGE_REQUEST_CODE -> {
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          logger.debug("Storage permission granted")
          val card = binding.libraryCard
          card.isDrawingCacheEnabled = true
          val bitmap = card.drawingCache
          var f: File
          try {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
              val file = File(Environment.getExternalStorageDirectory(), "library_card")
              if (!file.exists()) {
                file.mkdirs()
              }
              f = File(file.absolutePath + File.separator + "library-card" + ".png")
              val outStream: FileOutputStream = FileOutputStream(f)
              bitmap.compress(Bitmap.CompressFormat.PNG, 10, outStream)
              outStream.close()
              Toast.makeText(activity!!, "card saved", Toast.LENGTH_SHORT).show()
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
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
    val data = Intent()
    data.putExtra("type", type)
    data.putExtra("username", username)
    data.putExtra("barcode", barcode)
    data.putExtra("pin", pin)
    data.putExtra("temporary", temporary)
    data.putExtra("message", message)
    activity!!.setResult(Activity.RESULT_OK, data)
    activity!!.finish()
  }
}
