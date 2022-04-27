package org.nypl.simplified.cardcreator.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationEvent.CardConfirmed
import org.nypl.simplified.cardcreator.viewmodel.ConfirmationEvent.SaveCardPermissionsCheck
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream

class ConfirmationViewModel(
  initialState: ConfirmationData,
) : ViewModel() {
  private val logger = LoggerFactory.getLogger(ConfirmationViewModel::class.java)

  private val _events = MutableSharedFlow<ConfirmationEvent>()
  val events = _events.asSharedFlow()

  private val _state = MutableStateFlow(initialState)
  val state = _state.asStateFlow()

  fun confirmCard() { viewModelScope.launch { _events.emit(CardConfirmed) } }
  fun prepareToSaveCard() { viewModelScope.launch { _events.emit(SaveCardPermissionsCheck) } }

  // This could probably be its own utility component
  fun createAndStoreDigitalCard(bitmap: Bitmap) {
    viewModelScope.launch(Dispatchers.IO) {
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

          _events.emit(ConfirmationEvent.AddSavedCardToGallery(Uri.fromFile(f)))
        }
      } catch (e: Exception) {
        logger.error("Error creating digital card", e)
      }
    }
  }
}

sealed class ConfirmationEvent {
  object SaveCardPermissionsCheck : ConfirmationEvent()
  data class AddSavedCardToGallery(val fileUri: Uri) : ConfirmationEvent()
  object CardConfirmed : ConfirmationEvent()
}

data class ConfirmationData(
  val name: String,
  val barcode: String,
  val password: String,
  val message: String
)
