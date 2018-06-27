package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.EULAType;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.slf4j.Logger;

import java.util.Objects;


/**
 * A splash screen activity that either shows a license agreement, or simply
 * starts up another activity without displaying anything if the user has
 * already agreed to the license.
 */

public class MainWelcomeActivity extends Activity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainWelcomeActivity.class);
  }

  /**
   * Construct an activity.
   */

  public MainWelcomeActivity()
  {

  }

  @Override
  protected void onCreate(final Bundle state)
  {
    final String accountColor = Simplified.getCurrentAccount().getMainColor();
    setTheme(ThemeMatcher.Companion.noActionBarStyle(accountColor));

    super.onCreate(state);
    this.setContentView(R.layout.welcome);

    final AccountsRegistry registry = new AccountsRegistry(this, Simplified.getSharedPrefs());

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final DocumentStoreType docs = app.getDocumentStore();
    final OptionType<EULAType> eula_opt = docs.getEULA();


    final Button library_button =
      NullCheck.notNull((Button) findViewById(R.id.welcome_library));
    final Button instant_button =
      NullCheck.notNull((Button) findViewById(R.id.welcome_instant));

    library_button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View view) {


        if (eula_opt.isSome()) {
          final Some<EULAType> some_eula = (Some<EULAType>) eula_opt;
          final EULAType eula = some_eula.get();
          if (eula.eulaHasAgreed()) {
            MainWelcomeActivity.LOG.debug("EULA: agreed");

            final Account account = registry.getAccount(0);
            registry.addAccount(account, Simplified.getSharedPrefs());

            // show accounts list
            MainWelcomeActivity.this.openAccountsList();

          } else {
            MainWelcomeActivity.LOG.debug("EULA: not agreed");

            // show accounts list
            MainWelcomeActivity.this.openAccountsList();

          }
        } else {
          MainWelcomeActivity.LOG.debug("EULA: unavailable");


        }
      }
    });

    instant_button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View view) {

        Simplified.getSharedPrefs().putInt("current_account", 2);
        Simplified.getCatalogAppServices();
        Simplified.getSharedPrefs().putBoolean("welcome", true);
        MainWelcomeActivity.this.openCatalog();
      }
    });

  }

  private void openAccountsList()
  {
    final Intent i = new Intent(this, MainWelcomeAccountPickerActivity.class);
    this.startActivity(i);
    this.overridePendingTransition(0, 0);
    this.finish();
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
