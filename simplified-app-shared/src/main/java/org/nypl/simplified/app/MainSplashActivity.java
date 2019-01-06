package org.nypl.simplified.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.EULAType;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A splash screen activity that either shows a license agreement, or simply
 * starts up another activity without displaying anything if the user has
 * already agreed to the license.
 */

public class MainSplashActivity extends AppCompatActivity
{
  private static final Logger LOG = LoggerFactory.getLogger(MainSplashActivity.class);

  /**
   * Construct an activity.
   */

  public MainSplashActivity()
  {

  }

  @Override
  protected void onCreate(final Bundle state)
  {
    this.setTheme(Simplified.getMainColorScheme().getActivityThemeResourceWithoutActionBar());

    super.onCreate(state);
    this.setContentView(R.layout.splash);

    final Timer timer = new Timer();
    timer.schedule(
      new TimerTask()
      {
        @Override
        public void run()
        {
          MainSplashActivity.this.finishSplash(false);
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
    final boolean show_eula)
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

        if (Simplified.getSharedPrefs().contains("welcome")) {
          this.openCatalog();
        }
        else {
          final AccountsRegistry registry = new AccountsRegistry(this, Simplified.getSharedPrefs());
          final Account account = registry.getAccount(0);

          final Account existing = registry.getExistingAccount(account.getId());
          if (existing == null) {
            registry.addAccount(account, Simplified.getSharedPrefs());
          }
          else if (existing.getId() != account.getId()) {
            registry.addAccount(account, Simplified.getSharedPrefs());
          }


          Simplified.getSharedPrefs().putInt("current_account", 0);
          Simplified.getCatalogAppServices();
          Simplified.getSharedPrefs().putBoolean("welcome", true);
          this.openCatalog();

        }

      } else {
        MainSplashActivity.LOG.debug("EULA: not agreed");
        if (show_eula) {
          this.openEULA();
        } else {
          this.openWelcome();
        }
      }
    } else {
      MainSplashActivity.LOG.debug("EULA: unavailable");
      this.openWelcome();
    }
  }

  private void openEULA()
  {
    final Intent i = new Intent(this, MainEULAActivity.class);
    this.startActivity(i);
    this.overridePendingTransition(0, 0);
  }

  private void openWelcome()
  {
    if (Simplified.getSharedPrefs().contains("welcome")) {
      this.openCatalog();
    } else {
      final Intent i = new Intent(this, MainWelcomeActivity.class);
      this.startActivity(i);
      this.overridePendingTransition(0, 0);
      this.finish();
    }

  }

  private void openCatalog()
  {
    final Intent i = new Intent(this, MainCatalogActivity.class);
    i.putExtra("reload", true);
    this.startActivity(i);
    this.overridePendingTransition(0, 0);
    this.finish();
  }

}
