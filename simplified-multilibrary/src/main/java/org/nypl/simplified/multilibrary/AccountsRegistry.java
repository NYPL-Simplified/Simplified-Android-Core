package org.nypl.simplified.multilibrary;

import android.content.Context;
import android.content.res.AssetManager;

import com.io7m.junreachable.UnreachableCodeException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.prefs.Prefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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
      final JSONArray currentAccounts = new JSONArray(prefs.getString("current_accounts"));
      final JSONArray convertedCurrentAccounts = new JSONArray();
      for (int i = 0; i < currentAccounts.length(); i++) {
        for (int j = 0; j < this.accounts.length(); j++) {
          final JSONObject savedAcct = currentAccounts.getJSONObject(i);
          final JSONObject jsonAcct = this.accounts.getJSONObject(j);
          if (savedAcct.getInt("id_numeric") == jsonAcct.getInt("id_numeric")) {
            convertedCurrentAccounts.put(jsonAcct);
          }
        }
      }

      this.current_accounts = convertedCurrentAccounts;

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

      final JSONArray allEntries = new JSONArray(convertStreamToString(stream));
      final JSONArray productionEntries = new JSONArray();
      for (int i = 0; i < allEntries.length(); i++) {
        final JSONObject entry = allEntries.getJSONObject(i);
        if (entry.getBoolean("inProduction")) {
          productionEntries.put(entry);
        }
      }

      this.accounts = productionEntries;

      this.getCurrentAccounts(prefs);

    } catch (Exception e) {
      throw new UnreachableCodeException(e);
    }


  }


  /**
   * @param context The Android Context
   */
  public AccountsRegistry(final Context context) {

    try {

      final AssetManager assets = context.getAssets();

      final InputStream stream = assets.open("Accounts.json");

      final JSONArray allEntries = new JSONArray(convertStreamToString(stream));
      final JSONArray productionEntries = new JSONArray();
      for (int i = 0; i < allEntries.length(); i++) {
        final JSONObject entry = allEntries.getJSONObject(i);
        // if (entry.getBoolean("inProduction")) {
          productionEntries.put(entry);
        //}
      }

      this.accounts = productionEntries;

    } catch (Exception e) {
      throw new UnreachableCodeException(e);
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

    try {
      return new Account(this.getAccounts().getJSONObject(0));   //Default
    } catch (JSONException e) {
      throw new UnreachableCodeException(e);
    }
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
