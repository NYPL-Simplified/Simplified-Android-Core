package org.nypl.simplified.app;

import android.os.Bundle;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.Nullable;

/**
 * The activity that shows Helpstack's main activity
 */
public final class HelpActivity extends SimplifiedActivity
{

  /**
   * Construct help activity
   */
  public HelpActivity()
  {
  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return SimplifiedPart.PART_HELP;
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return true;
  }

  @Override protected void onCreate(
      final @Nullable Bundle state)
  {
    super.onCreate(state);
    final SimplifiedCatalogAppServicesType app =
        Simplified.getCatalogAppServices();
    final OptionType<HelpstackType> helpstack = app.getHelpStack();
    helpstack.map_(
        new ProcedureType<HelpstackType>()
        {
          @Override public void call(final HelpstackType hs)
          {
            hs.show(HelpActivity.this);
          }
        });
  }
}
