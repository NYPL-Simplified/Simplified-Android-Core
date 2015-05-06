package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.R;

import android.app.Activity;
import android.content.res.Resources;
import android.widget.Button;

import com.io7m.jnull.NullCheck;

public final class CatalogBookDownloadErrorDismissButton extends Button
{
  public CatalogBookDownloadErrorDismissButton(
    final Activity in_activity)
  {
    super(in_activity);

    final Resources rr = NullCheck.notNull(in_activity.getResources());
    this.setText(NullCheck.notNull(rr
      .getString(R.string.catalog_book_error_dismiss)));
    this.setTextSize(12.0f);
    this.setBackground(rr.getDrawable(R.drawable.simplified_button));
    this
      .setTextColor(rr.getColorStateList(R.drawable.simplified_button_text));
  }
}
