package org.nypl.simplified.app.catalog;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.nypl.simplified.app.R;

/**
 * View that displays a clock icon with short time string below,
 * for use in read/get buttons.
 */
public class CatalogButtonExpiryView extends LinearLayout
{
  private DateTime date;

  /**
   * Construct a new expiry view
   *
   * @param context The context (probably the activity the view will be used in)
   */
  public CatalogButtonExpiryView(
    final Context context)
  {
    super(context);
    final LayoutInflater inflater = LayoutInflater.from(context);
    inflater.inflate(R.layout.catalog_button_expiry, this, true);
  }

  /**
   * Get the date that the view will display
   *
   * @return date
   */
  public DateTime getDate()
  {
    return this.date;
  }

  /**
   * Set the date that the view will display.
   *
   * @param in_date The date that the view should display.
   */
  public void setDate(
    final DateTime in_date)
  {
    this.date = in_date;
    final TextView text_view = (TextView) this.findViewById(R.id.catalog_button_expiry_date);
    if (in_date != null) {
      text_view.setText(CatalogBookAvailabilityStrings.getIntervalStringShort(
        this.getContext().getResources(),
        DateTime.now(),
        in_date
      ));
    }
  }
}
