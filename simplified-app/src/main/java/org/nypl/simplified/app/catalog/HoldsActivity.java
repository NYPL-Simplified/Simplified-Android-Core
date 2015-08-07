package org.nypl.simplified.app.catalog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.SimplifiedPart;

/**
 * An activity that displays the holds for the current user.
 */

public final class HoldsActivity extends SimplifiedActivity
{
  /**
   * Construct a new activity.
   */

  public HoldsActivity()
  {

  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return SimplifiedPart.PART_HOLDS;
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return true;
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());

    final FrameLayout content_area = this.getContentFrame();
    final LinearLayout layout = NullCheck.notNull(
      (LinearLayout) inflater.inflate(
        R.layout.holds, content_area, false));
    content_area.addView(layout);
    content_area.requestLayout();

    this.navigationDrawerSetActionBarTitle();
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
  }

  @Override protected void onResume()
  {
    super.onResume();
  }
}
