package org.nypl.simplified.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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
import org.json.JSONObject;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The activity displaying the settings for the application.
 */

public final class MainSettingsAccountsActivity extends SimplifiedActivity
  implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

  private static final Logger LOG = LoggerFactory.getLogger(MainSettingsAccountsActivity.class);

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
      final TextView tv = NullCheck.notNull(current_account.findViewById(android.R.id.text1));
      final TextView tv2 = NullCheck.notNull(current_account.findViewById(android.R.id.text2));
      final int mainColor = ContextCompat.getColor(this, R.color.app_primary_color);
      tv.setText(account.getName());
      tv.setTextColor(mainColor);
      tv2.setText(account.getSubtitle());
      tv2.setTextColor(mainColor);

      final ImageView icon_view =
        NullCheck.notNull((ImageView) current_account.findViewById(R.id.cellIcon));

      try {
        icon_view.setImageBitmap(account.getLogoBitmap());
      } catch (IllegalArgumentException e) {
        icon_view.setImageResource(R.drawable.librarylogomagic);
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
          final TextView tv = NullCheck.notNull(v.findViewById(android.R.id.text1));
          final TextView tv2 = NullCheck.notNull(v.findViewById(android.R.id.text2));
          final int mainColor = ContextCompat.getColor(this.getContext(), R.color.app_primary_color);
          tv.setText(account.getName());
          tv.setTextColor(mainColor);
          tv2.setText(account.getSubtitle());
          tv2.setTextColor(mainColor);

          final ImageView icon_view =
            NullCheck.notNull(v.findViewById(R.id.cellIcon));

          try {
            icon_view.setImageBitmap(account.getLogoBitmap());
          } catch (IllegalArgumentException e) {
            icon_view.setImageResource(R.drawable.librarylogomagic);
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

      java.util.Collections.sort(all, new Comparator<Account>() {
        @Override
        public int compare(final Account a, final Account b) {
          // Check if we're one of the three "special" libraries that always come first.
          // This is a complete hack.
          if (a.getId() <= 2 || b.getId() <= 2) {
            // One of the libraries is special, so sort it first. Lower ids are "more
            // special" than higher ids and thus show up earlier.
            return a.getId() - b.getId();
          } else {
            // Neither library is special so we just go alphabetically.
            return a.getName().compareToIgnoreCase(b.getName());
          }
        }
      });

      int index = 0;
      for (Account account : all) {
        if (registry.getExistingAccount(account.getId()) == null) {
          menu.getMenu().add(Menu.NONE, account.getId(), index, account.getName());
        }
        ++index;
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
