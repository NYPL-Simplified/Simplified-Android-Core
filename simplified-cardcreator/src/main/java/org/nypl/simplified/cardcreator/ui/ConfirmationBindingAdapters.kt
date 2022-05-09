package org.nypl.simplified.cardcreator.ui

import android.widget.TextView
import androidx.databinding.BindingAdapter
import org.nypl.simplified.cardcreator.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@BindingAdapter("formatCardBarcode")
internal fun TextView.formatCardBarcode(barcode: String) {
  text = resources.getString(R.string.user_card_number, barcode)
}

@BindingAdapter("formatCardPin")
internal fun TextView.formatCardPin(pin: String) {
  text = resources.getString(R.string.user_password, pin)
}

@BindingAdapter("formatIssuedOn")
internal fun TextView.formatIssuedOn(format: String) {
  val currentDate: String = SimpleDateFormat(format, Locale.getDefault()).format(Date())
  text = resources.getString(R.string.issued_date, currentDate)
}
