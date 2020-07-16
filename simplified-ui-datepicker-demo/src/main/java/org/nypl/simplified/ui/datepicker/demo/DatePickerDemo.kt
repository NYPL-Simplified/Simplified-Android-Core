package org.nypl.simplified.ui.datepicker.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.joda.time.LocalDate
import org.nypl.simplified.ui.datepicker.DatePicker

class DatePickerDemo : AppCompatActivity() {

  private lateinit var picker: DatePicker
  private lateinit var pickerLimited: DatePicker

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setTheme(R.style.SimplifiedTheme_NoActionBar_Indigo)
    this.setContentView(R.layout.date_picker_demo)

    this.picker =
      this.findViewById(R.id.datePicker0)
    this.pickerLimited =
      this.findViewById(R.id.datePickerLimited)

    this.pickerLimited.setRangeLimits(
      LocalDate.parse("2001-02-03"),
      LocalDate.parse("2003-04-05")
    )
  }
}
