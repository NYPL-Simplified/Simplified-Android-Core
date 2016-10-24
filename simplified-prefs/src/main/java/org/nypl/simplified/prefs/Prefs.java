package org.nypl.simplified.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 *
 */
public class Prefs {

    private Context context;

    /**
     * @param in_context context
     */
    public Prefs(final Context in_context) {
        this.context = in_context;
    }

    /**
     *
     */
    public void clearAllPreferences() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply(); // important! Don't forget!
    }

    /**
     * @param key preference key
     * @return boolean
     */
    public boolean contains(final String key)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        return prefs.contains(key);
    }

    /**
     * @param key preference key
     * @return string value
     */
    public String getString(final String key) {
        String val = null;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        val = prefs.getString(key, "");
        return val;
    }

    /**
     * @param key preference key
     * @return boolean value
     */
    public boolean getBoolean(final String key) {
        boolean val = false;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        val = prefs.getBoolean(key, false);
        return val;
    }

    /**
     * @param key preference key
     * @return value
     */
    public int getInt(final String key) {
        int val = 0;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        val = prefs.getInt(key, 0);
        return val;
    }

    /**
     * @param key preference key
     */
    public void remove(final String key) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply(); // important! Don't forget!
    }

    /**
     * @param key preference key
     * @param value preference value
     */
    public void putBoolean(final String key, final boolean value) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply(); // important! Don't forget!
    }

    /**
     * @param key preference key
     * @param value preference value
     */
    public void putInt(final String key, final int value) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply(); // important! Don't forget!
    }

    /**
     * @param key preference key
     * @param value preference value
     * Store a preference via key -> value
     */
    public void putString(final String key, final String value) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply(); // important! Don't forget!
    }

}
