package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The activity displaying the settings for the application.
 */

public final class MainSettingsAccountsActivity extends SimplifiedActivity
  implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(MainSettingsAccountsActivity.class);
  }

  private @Nullable ArrayAdapter<Account> adapter_accounts;

  /**
   * Construct an activity.
   */
  public MainSettingsAccountsActivity() {

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

    this.setContentView(R.layout.accounts);
    final ListView dl =
      NullCheck.notNull((ListView) this.findViewById(R.id.account_list));
    final LinearLayout current_account =
      NullCheck.notNull((LinearLayout) this.findViewById(R.id.current_account));

    {
      final Account account = Simplified.getCurrentAccount();
      final TextView tv =
        NullCheck.notNull((TextView) current_account.findViewById(android.R.id.text1));
      tv.setText(account.getName());
      tv.setTextColor(R.color.text_black);
      final TextView tv2 =
        NullCheck.notNull((TextView) current_account.findViewById(android.R.id.text2));
      tv2.setText(account.getSubtitle());
      tv2.setTextColor(R.color.text_black);

      final ImageView icon_view =
        NullCheck.notNull((ImageView) current_account.findViewById(R.id.cellIcon));
      if (account.getId() == 0) {
        icon_view.setImageResource(R.drawable.account_logo_nypl);
      } else if (account.getId() == 1) {
        icon_view.setImageResource(R.drawable.account_logo_bpl);
      } else if (account.getId() == 2) {
        icon_view.setImageResource(R.drawable.account_logo_instant);
      } else if (account.getId() == 7) {
        icon_view.setImageResource(R.drawable.account_logo_alameda);
      } else if (account.getId() == 8) {
        icon_view.setImageResource(R.drawable.account_logo_hcls);
      } else if (account.getId() == 9) {
        icon_view.setImageResource(R.drawable.account_logo_mcpl);
      } else if (account.getId() == 10) {
        icon_view.setImageResource(R.drawable.account_logo_fcpl);
      } else if (account.getId() == 11) {
        icon_view.setImageResource(R.drawable.account_logo_anne_arundel);
      } else if (account.getId() == 12) {
        icon_view.setImageResource(R.drawable.account_logo_bgc);
      } else if (account.getId() == 13) {
        icon_view.setImageResource(R.drawable.account_logo_smcl);
      } else if (account.getId() == 14) {
        icon_view.setImageResource(R.drawable.account_logo_cl);
      } else if (account.getId() == 15) {
        icon_view.setImageResource(R.drawable.account_logo_ccpl);
      } else if (account.getId() == 16) {
        icon_view.setImageResource(R.drawable.account_logo_ccl);
      } else if (account.getId() == 17) {
        icon_view.setImageResource(R.drawable.account_logo_bcl);
      } else if (account.getId() == 18) {
        icon_view.setImageResource(R.drawable.account_logo_lapl);
      } else if (account.getId() == 19) {
        icon_view.setImageResource(R.drawable.account_logo_pcl);
      } else if (account.getId() == 20) {
        icon_view.setImageResource(R.drawable.account_logo_sccl);
      } else if (account.getId() == 21) {
        icon_view.setImageResource(R.drawable.account_logo_acls);
      } else if (account.getId() == 22) {
        icon_view.setImageResource(R.drawable.account_logo_rel);
      } else if (account.getId() == 23) {
        icon_view.setImageResource(R.drawable.account_logo_wcfl);
      }


      current_account.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View view) {


          final Bundle b = new Bundle();
          b.putInt("selected_account", account.getId());
          SimplifiedActivity.setActivityArguments(b, false);
          final Intent intent = new Intent();
          intent.setClass(
            MainSettingsAccountsActivity.this, MainSettingsAccountActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
          intent.putExtras(b);

          MainSettingsAccountsActivity.this.startActivity(intent);

        }
      });
    }

    final List<Account> dia = new ArrayList<Account>();
    final JSONArray registry =
      new AccountsRegistry(this, Simplified.getSharedPrefs()).getCurrentAccounts(Simplified.getSharedPrefs());
    for (int index = 0; index < registry.length(); ++index) {
      try {
        final Account account = new Account(registry.getJSONObject(index));
        if (Simplified.getCurrentAccount().getId() != account.getId()) {
          dia.add(new Account(registry.getJSONObject(index)));
        }
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
          } else if (account.getId() == 7) {
            icon_view.setImageResource(R.drawable.account_logo_alameda);
          } else if (account.getId() == 8) {
            icon_view.setImageResource(R.drawable.account_logo_hcls);
          } else if (account.getId() == 9) {
            icon_view.setImageResource(R.drawable.account_logo_mcpl);
          } else if (account.getId() == 10) {
            icon_view.setImageResource(R.drawable.account_logo_fcpl);
          } else if (account.getId() == 11) {
            icon_view.setImageResource(R.drawable.account_logo_anne_arundel);
          } else if (account.getId() == 12) {
            icon_view.setImageResource(R.drawable.account_logo_bgc);
          } else if (account.getId() == 13) {
            icon_view.setImageResource(R.drawable.account_logo_smcl);
          } else if (account.getId() == 14) {
            icon_view.setImageResource(R.drawable.account_logo_cl);
          } else if (account.getId() == 15) {
            icon_view.setImageResource(R.drawable.account_logo_ccpl);
          } else if (account.getId() == 16) {
            icon_view.setImageResource(R.drawable.account_logo_ccl);
          } else if (account.getId() == 17) {
            icon_view.setImageResource(R.drawable.account_logo_bcl);
          } else if (account.getId() == 18) {
            icon_view.setImageResource(R.drawable.account_logo_lapl);
          } else if (account.getId() == 19) {
            icon_view.setImageResource(R.drawable.account_logo_pcl);
          } else if (account.getId() == 20) {
            icon_view.setImageResource(R.drawable.account_logo_sccl);
          } else if (account.getId() == 21) {
            icon_view.setImageResource(R.drawable.account_logo_acls);
          } else if (account.getId() == 22) {
            icon_view.setImageResource(R.drawable.account_logo_rel);
          } else if (account.getId() == 23) {
            icon_view.setImageResource(R.drawable.account_logo_wcfl);
          }

          return v;
        }
      };

    dl.setAdapter(this.adapter_accounts);
    dl.setOnItemClickListener(this);
    dl.setOnItemLongClickListener(this);

  }


  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);

    if (item.getItemId() == R.id.add_account) {

      // show accounts available to add

      final PopupMenu menu = new PopupMenu(getApplicationContext(), this.findViewById(R.id.add_account));
      final AccountsRegistry registry = new AccountsRegistry(this, Simplified.getSharedPrefs());
      final JSONArray all_accounts = registry.getAccounts();

      final List<Account> all = new ArrayList<Account>();
      for (int index = 0; index < all_accounts.length(); ++index) {
        try {
          all.add(new Account(all_accounts.getJSONObject(index)));
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      for (Account account : all) {
        if (registry.getExistingAccount(account.getId()) == null) {
          menu.getMenu().add(Menu.NONE, account.getId(), account.getId(), account.getName());
        }
      }

      menu.show();

      menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(final MenuItem item) {

          final Account account = registry.getAccount(item.getItemId());
          registry.addAccount(account, Simplified.getSharedPrefs());

          MainSettingsAccountsActivity.this.adapter_accounts.add(account);
          MainSettingsAccountsActivity.this.adapter_accounts.notifyDataSetChanged();

          // reload activity , this is needed to determine if menu button needs to be hidden
          finish();
          overridePendingTransition(0, 0);
          startActivity(getIntent());
          overridePendingTransition(0, 0);

          return true;
        }

      });

      return true;
    }

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

    final Bundle b = new Bundle();
    b.putInt("selected_account", account.getId());
    SimplifiedActivity.setActivityArguments(b, false);
    final Intent intent = new Intent();
    intent.setClass(
      this, MainSettingsAccountActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    intent.putExtras(b);

    this.startActivity(intent);

  }

  @Override
  public boolean onItemLongClick(
    final AdapterView<?> parent,
    final View view,
    final int position,
    final long id) {

    final AccountsRegistry registry =
      new AccountsRegistry(this, Simplified.getSharedPrefs());
    final Account account = this.adapter_accounts.getItem(position);

    final CharSequence[] items = {"Delete " + account.getName()};
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);

//    builder.setTitle("Action:");
    builder.setItems(items, new DialogInterface.OnClickListener() {

      public void onClick(final DialogInterface dialog, final int item) {

        registry.removeAccount(account, Simplified.getSharedPrefs());
        MainSettingsAccountsActivity.this.adapter_accounts.remove(account);
        MainSettingsAccountsActivity.this.adapter_accounts.notifyDataSetChanged();

        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);

      }

    });

    builder.create().show();
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(
    final @Nullable Menu in_menu) {

    final AccountsRegistry registry = new AccountsRegistry(this, Simplified.getSharedPrefs());
    final JSONArray all_accounts = registry.getAccounts();
    final JSONArray current_accounts = registry.getCurrentAccounts();

    if (all_accounts.length() != current_accounts.length()) {
      final Menu menu_nn = NullCheck.notNull(in_menu);
      final MenuInflater inflater = this.getMenuInflater();
      inflater.inflate(R.menu.add_account, menu_nn);
    }

    return true;
  }

}
