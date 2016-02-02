package org.nypl.labs.OpenEbooks.app;

import android.content.Intent;

import org.nypl.simplified.app.MainSplashActivity;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.books.core.AccountsControllerType;

/**
 * Subclass the splash activity to show tutorial screen after EULA
 */
public class OESplashActivity extends MainSplashActivity
{


  @Override
  protected void afterEULA()
  {
    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final AccountsControllerType accounts = app.getBooks();
    if (accounts.accountIsLoggedIn()) {
      super.afterEULA();
    } else {
      this.openTutorial();
    }
  }

  private void openTutorial()
  {
    final Intent i = new Intent(this, OEIntroActivity.class);
    this.startActivity(i);
    this.overridePendingTransition(0, 0);
    this.finish();
  }
}
