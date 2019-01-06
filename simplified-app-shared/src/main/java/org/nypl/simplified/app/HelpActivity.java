package org.nypl.simplified.app;

import android.content.res.Resources;
import android.os.Bundle;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.Nullable;

/**
 * The activity that shows Helpstack's main activity
 */

public final class HelpActivity extends NavigationDrawerActivity {

  /**
   * Construct help activity
   */

  public HelpActivity() {

  }

  @Override
  protected String navigationDrawerGetActivityTitle(final Resources resources) {
    return resources.getString(R.string.help);
  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return true;
  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    super.onCreate(state);

    final OptionType<HelpstackType> helpstack = Simplified.getHelpStack();
    helpstack.map_(hs -> {
      hs.show(HelpActivity.this);
      this.finish();
    });
  }

}
