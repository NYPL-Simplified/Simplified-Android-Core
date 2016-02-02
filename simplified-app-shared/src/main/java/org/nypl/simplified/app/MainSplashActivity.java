package org.nypl.simplified.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.EULAType;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A splash screen activity that either shows a license agreement, or simply
 * starts up another activity without displaying anything if the user has
 * already agreed to the license.
 */

public class MainSplashActivity extends Activity
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

  @Override
  protected void onCreate(final Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.splash);

    final Timer timer = new Timer();
    timer.schedule(
      new TimerTask()
      {
        @Override
        public void run()
        {
          MainSplashActivity.this.finishSplash(true);
        }
      }, 2000L);
  }

  @Override
  protected void onRestart()
  {
    super.onRestart();
    this.finishSplash(false);
  }

  private void finishSplash(
    final boolean showEULA)
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final DocumentStoreType docs = app.getDocumentStore();
    final OptionType<EULAType> eula_opt = docs.getEULA();

    if (eula_opt.isSome()) {
      final Some<EULAType> some_eula = (Some<EULAType>) eula_opt;
      final EULAType eula = some_eula.get();
      if (eula.eulaHasAgreed()) {
        MainSplashActivity.LOG.debug("EULA: agreed");
        this.afterEULA();
      } else {
        MainSplashActivity.LOG.debug("EULA: not agreed");
        if (showEULA) {
          this.openEULA();
        } else {
          this.finish();
        }
      }
    } else {
      MainSplashActivity.LOG.debug("EULA: unavailable");
      this.afterEULA();
    }
  }

  private void openEULA()
  {
    final Intent i = new Intent(this, MainEULAActivity.class);
    this.startActivity(i);
    this.overridePendingTransition(0, 0);
  }

  private void openCatalog()
  {
    final Intent i = new Intent(this, MainCatalogActivity.class);
    this.startActivity(i);
    this.overridePendingTransition(0, 0);
    this.finish();
  }

  protected void afterEULA()
  {
    this.openCatalog();
  }
}
