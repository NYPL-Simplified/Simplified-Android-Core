package org.nypl.simplified.datepicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * A trivial three-part date picker.
 */

public final class DatePicker extends RelativeLayout {

  private final View root;
  private final NumberPicker year;
  private final NumberPicker month;
  private final NumberPicker day;

  /**
   * Construct a date picker.
   */

  public DatePicker(
      final Context context,
      final AttributeSet attrs) {
    super(context, attrs);
    this.root = inflate(context, R.layout.date_picker, this);
    this.year = this.root.findViewById(R.id.date_picker_year);
    this.month = this.root.findViewById(R.id.date_picker_month);
    this.day = this.root.findViewById(R.id.date_picker_day);
    this.init();
  }

  /**
   * Construct a date picker.
   */

  public DatePicker(
      final Context context,
      final AttributeSet attrs,
      final int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.root = inflate(context, R.layout.date_picker, this);
    this.year = this.root.findViewById(R.id.date_picker_year);
    this.month = this.root.findViewById(R.id.date_picker_month);
    this.day = this.root.findViewById(R.id.date_picker_day);
    this.init();
  }

  /**
   * @return The currently selected date
   */

  public LocalDate getDate()
  {
    return new LocalDate(this.year.getValue(), this.month.getValue(), this.day.getValue());
  }

  private void init() {
    final LocalDate date = LocalDate.now();

    this.year.setMinValue(1800);
    this.year.setMaxValue(9999);
    this.year.setValue(date.getYear() - 18);
    this.month.setMinValue(1);
    this.month.setMaxValue(12);
    this.month.setValue(date.getMonthOfYear());
    this.month.setDisplayedValues(calculateMonthNames());

    this.day.setMinValue(1);
    this.setMaximumDayValue();
    this.day.setValue(date.getDayOfMonth());

    this.month.setOnValueChangedListener(
        (picker, old_value, new_value) -> this.setMaximumDayValue());
  }

  private String[] calculateMonthNames() {
    final String[] values = new String[12];
    final DateTimeFormatter formatter = DateTimeFormat.forPattern("MMM");
    for (int month = 0; month < 12; ++month) {
      final LocalDate date = new LocalDate(2000, month + 1, 1);
      values[month] = formatter.print(date);
    }
    return values;
  }

  private void setMaximumDayValue() {
    final LocalDate date = new LocalDate(this.year.getValue(), this.month.getValue(), 1);
    final int max = date.dayOfMonth().getMaximumValue();
    this.day.setMaxValue(max);
    this.day.setValue(Math.min(this.day.getValue(), max));
  }
}
