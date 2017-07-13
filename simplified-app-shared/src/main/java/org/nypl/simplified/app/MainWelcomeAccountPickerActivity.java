package org.nypl.simplified.app;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.nypl.simplified.app.catalog.MainCatalogActivity;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The activity displaying the settings for the application.
 */

public final class MainWelcomeAccountPickerActivity extends SimplifiedActivity
  implements AdapterView.OnItemClickListener {

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainWelcomeAccountPickerActivity.class);
  }

  private @Nullable ArrayAdapter<Account> adapter_accounts;

  /**
   * Construct an activity.
   */
  public MainWelcomeAccountPickerActivity() {

  }

  @Override
  protected SimplifiedPart navigationDrawerGetPart() {
    return SimplifiedPart.PART_SETTINGS;
  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return true;
  }

  @Override
  public void onCreate(final Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);


    final ActionBar bar = this.getActionBar();
    if (android.os.Build.VERSION.SDK_INT < 21) {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(true);
      bar.setIcon(R.drawable.ic_arrow_back);
    } else {
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(false);
    }

    this.setContentView(R.layout.accounts_picker);
    final ListView dl =
      NullCheck.notNull((ListView) this.findViewById(R.id.account_list));

    final List<Account> dia = new ArrayList<Account>();
    final JSONArray registry =
      new AccountsRegistry(this, Simplified.getSharedPrefs()).getAccounts();
    for (int index = 0; index < registry.length(); ++index) {
      try {
        dia.add(new Account(registry.getJSONObject(index)));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());

    this.adapter_accounts =
      new ArrayAdapter<Account>(this, R.layout.account_list_item, dia) {
        @Override
        public View getView(
          final int position,
          final @Nullable View reuse,
          final @Nullable ViewGroup parent) {
          final View v;
          if (reuse != null) {
            v = reuse;
          } else {
            v = inflater.inflate(R.layout.account_list_item, parent, false);
          }

          final Account account = NullCheck.notNull(dia.get(position));

          final TextView tv =
            NullCheck.notNull((TextView) v.findViewById(android.R.id.text1));
          tv.setText(account.getName());
          tv.setTextColor(R.color.text_black);
          final TextView tv2 =
            NullCheck.notNull((TextView) v.findViewById(android.R.id.text2));
          tv2.setText(account.getSubtitle());
          tv2.setTextColor(R.color.text_black);

          final ImageView icon_view =
            NullCheck.notNull((ImageView) v.findViewById(R.id.cellIcon));
          if (account.getId() == 0) {
            icon_view.setImageResource(R.drawable.account_logo_nypl);
          } else if (account.getId() == 1) {
            icon_view.setImageResource(R.drawable.account_logo_bpl);
          } else if (account.getId() == 2) {
            icon_view.setImageResource(R.drawable.account_logo_instant);
          } else if (account.getId() == 9) {
            icon_view.setImageResource(R.drawable.account_logo_mcpl);
          }

          return v;
        }
      };

    dl.setAdapter(this.adapter_accounts);
    dl.setOnItemClickListener(this);

  }


  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);

    switch (item.getItemId()) {

      case android.R.id.home: {
        onBackPressed();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }


  @Override
  public void onItemClick(
    final @Nullable AdapterView<?> parent,
    final @Nullable View view,
    final int position,
    final long id) {

    final Account account = this.adapter_accounts.getItem(position);
    final AccountsRegistry registry = new AccountsRegistry(this, Simplified.getSharedPrefs());
    final Account existing = registry.getExistingAccount(account.getId());
    if (existing != null) {
      if (existing.getId() != account.getId()) {
        registry.addAccount(account, Simplified.getSharedPrefs());
      }
    }
    else {
      registry.addAccount(account, Simplified.getSharedPrefs());
    }
    Simplified.getSharedPrefs().putInt("current_account", account.getId());
    Simplified.getCatalogAppServices();
    Simplified.getSharedPrefs().putBoolean("welcome", true);

    final Intent i = new Intent(this, MainCatalogActivity.class);
    i.putExtra("reload", true);
    this.startActivity(i);
    this.overridePendingTransition(0, 0);
    this.finish();

  }

}
