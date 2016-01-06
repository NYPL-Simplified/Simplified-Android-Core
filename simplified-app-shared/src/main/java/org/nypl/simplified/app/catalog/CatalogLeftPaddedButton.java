package org.nypl.simplified.app.catalog;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;

/**
 * A button that will have a left margin of 8dp. This is used to provide spaces
 * between buttons when they are programmatically added to a view group.
 */

public abstract class CatalogLeftPaddedButton extends LinearLayout
  implements CatalogBookButtonType
{

  private TextView text_view;

  /**
   * Construct a button that will have a left margin of 8dp.
   *
   * @param context The current context
   */

  public CatalogLeftPaddedButton(final Context context)
  {
    super(context);

    final SimplifiedCatalogAppServicesType ss =
      Simplified.getCatalogAppServices();
    final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
      (int) ss.screenDPToPixels(34));
    lp.leftMargin = (int) ss.screenDPToPixels(8);
    this.setLayoutParams(lp);
    this.text_view = new TextView(context);
    this.setGravity(Gravity.CENTER_VERTICAL);
    this.text_view.setGravity(Gravity.CENTER_VERTICAL);
    this.addView(this.text_view);
  }

  /**
   * Get the button's TextView
   *
   * @return text_view
   */

  final public TextView getTextView()
  {
    return this.text_view;
  }
}
