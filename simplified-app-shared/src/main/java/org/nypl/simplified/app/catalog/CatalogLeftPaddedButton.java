package org.nypl.simplified.app.catalog;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;

/**
 * A button that will have a left margin of 8dp. This is used to provide spaces
 * between buttons when they are programmatically added to a view group.
 */

public abstract class CatalogLeftPaddedButton extends Button
  implements CatalogBookButtonType
{
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
      android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.leftMargin = (int) ss.screenDPToPixels(8);
    this.setLayoutParams(lp);
  }
}
