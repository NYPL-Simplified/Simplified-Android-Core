package org.nypl.simplified.ui.settings

import androidx.preference.PreferenceDataStore

/**
 * [NoOpSettingsDataStore] can be used when you want to use the preference api
 * but do not wish to store data in [SharedPreferences].
 *
 * All get operations return the given `defValue` while all put operations
 * do nothing.
 */

class NoOpSettingsDataStore : PreferenceDataStore() {
  override fun getBoolean(key: String?, defValue: Boolean) = defValue
  override fun putLong(key: String?, value: Long) {}
  override fun putInt(key: String?, value: Int) {}
  override fun getInt(key: String?, defValue: Int) = defValue
  override fun putBoolean(key: String?, value: Boolean) {}
  override fun putStringSet(key: String?, values: MutableSet<String>?) {}
  override fun getLong(key: String?, defValue: Long) = defValue
  override fun getFloat(key: String?, defValue: Float) = defValue
  override fun putFloat(key: String?, value: Float) {}
  override fun getStringSet(key: String?, defValues: MutableSet<String>?) = defValues ?: mutableSetOf()
  override fun getString(key: String?, defValue: String?) = defValue
  override fun putString(key: String?, value: String?) {}
}
