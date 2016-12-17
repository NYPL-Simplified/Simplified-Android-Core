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
import java.util.Scanner;

/**
 * Created by aferditamuriqi on 8/29/16.
 */

public class AccountsRegistry implements Serializable {


  private JSONArray accounts;

  /**
   * @return accounts
   */
  public JSONArray getAccounts() {
    return this.accounts;
  }

  /**
   * @param prefs shared prefs
   * @return current accounts
   */
  public JSONArray getCurrentAccounts(final Prefs prefs) {
    JSONArray current_accounts = new JSONArray();

    current_accounts.put(this.getAccount(0).getJsonObject());
    current_accounts.put(this.getAccount(2).getJsonObject());

    try {
      current_accounts = new JSONArray(prefs.getString("current_accounts"));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return  current_accounts;
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

  private static String convertStreamToString(final InputStream is) {
    final Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
