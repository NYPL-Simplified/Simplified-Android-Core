package org.nypl.simplified.app;

import android.app.Activity;
import android.content.Intent;
import android.net.MailTo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import org.nypl.drm.core.Assertions;
import org.nypl.simplified.app.catalog.MainCatalogActivity;

/**
 * An activity that shows a license agreement, and aborts if the user does not
 * agree to it.
 */

public final class MainEULAActivity extends Activity
{
  private WebView web_view;
  private Button  agree;
  private Button  disagree;

  /**
   * Construct an activity.
   */

  public MainEULAActivity()
  {

  }

  @Override protected void onCreate(final Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.eula);

    this.web_view =
      NullCheck.notNull((WebView) this.findViewById(R.id.eula_web_view));
    this.agree = NullCheck.notNull((Button) this.findViewById(R.id.eula_agree));
    this.disagree =
      NullCheck.notNull((Button) this.findViewById(R.id.eula_disagree));

    final WebSettings settings = this.web_view.getSettings();
    this.web_view.setWebViewClient(
      new WebViewClient()
      {
        @Override public boolean shouldOverrideUrlLoading(
          final WebView wv,
          final String url)
        {
          /**
           * Handle the EULA mailto link.
           */

          if (url.startsWith("mailto:")) {
            final MailTo mt = MailTo.parse(url);
            final Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(
              Intent.EXTRA_EMAIL, new String[] {
                mt.getTo() });
            i.putExtra(Intent.EXTRA_SUBJECT, mt.getSubject());
            i.putExtra(Intent.EXTRA_CC, mt.getCc());
            i.putExtra(Intent.EXTRA_TEXT, mt.getBody());
            MainEULAActivity.this.startActivity(i);
            return true;
          }

          return false;
        }
      });

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();
    final OptionType<EULAType> eula_opt = app.getEULA();

    Assertions.checkPrecondition(eula_opt.isSome(), "EULA must be provided");
    final Some<EULAType> some_eula = (Some<EULAType>) eula_opt;
    final EULAType eula = some_eula.get();
    this.web_view.loadUrl(eula.eulaGetReadableURL().toString());

    /**
     * Agreeing with the EULA opens the catalog.
     */

    this.agree.setOnClickListener(
      new View.OnClickListener()
      {
        @Override public void onClick(final View v)
        {
          MainEULAActivity.this.eulaAccepted(eula);
        }
      });

    /**
     * Disagreeing with the EULA closes the application.
     */

    this.disagree.setOnClickListener(
      new View.OnClickListener()
      {
        @Override public void onClick(final View v)
        {
          MainEULAActivity.this.eulaRejected(eula);
        }
      });
  }

  private void eulaRejected(final EULAType eula)
  {
    eula.eulaSetHasAgreed(false);
    MainEULAActivity.this.finish();
  }

  private void eulaAccepted(final EULAType eula)
  {
    eula.eulaSetHasAgreed(true);
    MainEULAActivity.this.openCatalog();
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
