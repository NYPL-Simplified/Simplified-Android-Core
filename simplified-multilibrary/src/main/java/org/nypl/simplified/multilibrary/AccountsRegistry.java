package org.nypl.simplified.multilibrary;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.prefs.Prefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by aferditamuriqi on 8/29/16.
 *
 */

public class AccountsRegistry implements Serializable {


  private JSONArray accounts;
  private JSONArray current_accounts;

  /**
   * @return accounts
   */
  public JSONArray getAccounts() {
    return this.accounts;
  }

  /**
   * @return
   */
  public JSONArray getCurrentAccounts() {
    return this.current_accounts;
  }

  /**
   * @param prefs shared prefs
   * @return current accounts
   */
  public JSONArray getCurrentAccounts(final Prefs prefs) {

    try {
      this.current_accounts = new JSONArray(prefs.getString("current_accounts"));
    } catch (JSONException e) {
      e.printStackTrace();
    }

    if (this.current_accounts == null || this.current_accounts.length() == 0)
    {
      this.current_accounts = new JSONArray();
//      this.current_accounts.put(this.getAccount(0).getJsonObject());
      this.current_accounts.put(this.getAccount(2).getJsonObject());
    }

    prefs.putString("current_accounts", this.current_accounts.toString());
    return  this.current_accounts;
  }

  /**
   * @param account
   * @param prefs
   */
  public void addAccount(final Account account, final Prefs prefs) {

    this.current_accounts.put(account.getJsonObject());
    prefs.putString("current_accounts", this.current_accounts.toString());
  }

  /**
   * @param account
   * @param prefs
   */
  public void removeAccount(final Account account, final Prefs prefs) {

    final JSONArray current_acc = this.getCurrentAccounts(prefs);


    this.current_accounts = new JSONArray();

    for (int index = 0; index < current_acc.length(); ++index) {

      try {

        final Account acc = new Account(current_acc.getJSONObject(index));

        if (account.getId() != acc.getId()) {

          this.current_accounts.put(acc.getJsonObject());

        }

      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    prefs.putString("current_accounts", this.current_accounts.toString());

  }


  /**
   * @param context The Android Context
   * @param prefs
   */
  public AccountsRegistry(final Context context, final Prefs prefs) {


    try {

      final AssetManager assets = context.getAssets();

      final InputStream stream = assets.open("Accounts.json");

      this.accounts = new JSONArray(convertStreamToString(stream));

      this.getCurrentAccounts(prefs);

    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }


  }


  /**
   * @param context The Android Context
   */
  public AccountsRegistry(final Context context) {

    try {

      final AssetManager assets = context.getAssets();

      final InputStream stream = assets.open("Accounts.json");

      this.accounts = new JSONArray(convertStreamToString(stream));


    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
  /**
   * @param id The account ID
   * @return Account
   */
  public Account getAccount(final Integer id) {

    for (int index = 0; index < this.getAccounts().length(); ++index) {

      try {

        final Account account = new Account(this.getAccounts().getJSONObject(index));
        if (account.getId() == id) {
          return account;
        }

      } catch (JSONException e) {
        e.printStackTrace();
      }

    }
    return null;
  }

  /**
   * @param id
   * @return
   */
  public Account getExistingAccount(final Integer id) {

    for (int index = 0; index < this.getCurrentAccounts().length(); ++index) {

      try {

        final Account account = new Account(this.getCurrentAccounts().getJSONObject(index));
        if (account.getId() == id) {
          return account;
        }

      } catch (JSONException e) {
        e.printStackTrace();
      }

    }
    return null;
  }



  private static String convertStreamToString(final InputStream is) {
    final Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }



}
