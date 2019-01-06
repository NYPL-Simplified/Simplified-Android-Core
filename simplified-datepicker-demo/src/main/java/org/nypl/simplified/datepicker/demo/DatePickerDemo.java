package org.nypl.simplified.datepicker.demo;

import android.app.Activity;
import android.os.Bundle;

import org.nypl.simplified.datepicker.DatePicker;

import java.util.Objects;

public final class DatePickerDemo extends Activity {

  private DatePicker picker;

  @Override
  protected void onCreate(final Bundle state) {
    super.onCreate(state);

    this.setContentView(R.layout.date_picker_demo);

    this.picker =
      Objects.requireNonNull(this.findViewById(R.id.date_picker), "R.id.date_picker");
  }
}
