package org.nypl.simplified.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

/**
 * A splash screen activity that either shows a license agreement, or simply
 * starts up another activity without displaying anything if the user has
 * already agreed to the license.
 */

public final class MainSplashActivity extends Activity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainSplashActivity.class);
  }

  /**
   * Construct an activity.
   */

  public MainSplashActivity()
  {

  }

  @Override protected void onCreate(final Bundle state)
  {
    super.onCreate(state);

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final OptionType<EULAType> eula_opt = app.getEULA();

    if (eula_opt.isSome()) {
      final Some<EULAType> some_eula = (Some<EULAType>) eula_opt;
      final EULAType eula = some_eula.get();
      if (eula.eulaHasAgreed()) {
        LOG.debug("EULA: agreed");
        this.openCatalog();
      } else {
        LOG.debug("EULA: not agreed");
        this.openEULA();
      }
    } else {
      LOG.debug("EULA: unavailable");
      this.openCatalog();
    }
  }

  private void openEULA()
  {
    final Intent i = new Intent(this, MainEULAActivity.class);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    this.startActivity(i);
    this.finish();
    this.overridePendingTransition(0, 0);
  }

  private void openCatalog()
  {
    final Intent i = new Intent(this, MainCatalogActivity.class);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    this.startActivity(i);
    this.finish();
    this.overridePendingTransition(0, 0);
  }
}
