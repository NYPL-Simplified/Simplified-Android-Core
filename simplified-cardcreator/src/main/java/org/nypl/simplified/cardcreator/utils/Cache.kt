package org.nypl.simplified.cardcreator.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.nypl.simplified.cardcreator.model.AccountInformation
import org.nypl.simplified.cardcreator.model.Address
import org.nypl.simplified.cardcreator.model.PersonalInformation

/**
 * Util class for handling saving/retrieving address across card creator process
 * This class acts as private key value pair caching system
 */
class Cache internal constructor(private val sharedPreferences: SharedPreferences) {
  constructor(context: Context) : this(
    context.getSharedPreferences("CARD_CREATOR", Context.MODE_PRIVATE)
  )

  companion object {

    /**
     * SharePreferences keys
     */
    private const val KEY_HOME_ADDRESS_LINE_1 = "home_line_1"
    private const val KEY_HOME_ADDRESS_LINE_2 = "home_line_2"
    private const val KEY_HOME_CITY = "home_city"
    private const val KEY_HOME_STATE = "home_state"
    private const val KEY_HOME_ZIP = "home_zip"
    private const val KEY_HOME_HAS_BEEN_VALIDATED = "home_validated"

    private const val KEY_SCHOOL_ADDRESS_LINE_1 = "school_line_1"
    private const val KEY_SCHOOL_ADDRESS_LINE_2 = "school_line_2"
    private const val KEY_SCHOOL_CITY = "school_city"
    private const val KEY_SCHOOL_STATE = "school_state"
    private const val KEY_SCHOOL_ZIP = "school_zip"
    private const val KEY_SCHOOL_HAS_BEEN_VALIDATED = "school_validated"

    private const val KEY_WORK_ADDRESS_LINE_1 = "work_line_1"
    private const val KEY_WORK_ADDRESS_LINE_2 = "work_line_2"
    private const val KEY_WORK_CITY = "work_city"
    private const val KEY_WORK_STATE = "work_state"
    private const val KEY_WORK_ZIP = "work_zip"
    private const val KEY_WORK_HAS_BEEN_VALIDATED = "work_validated"

    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_MIDDLE_NAME = "middle_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_BIRTH_DATE = "birth_date"
    private const val KEY_EMAIL = "email"

    private const val KEY_USERNAME = "username"
    private const val KEY_PIN = "pin"
    private const val KEY_JUVENILE_CARD = "juvenile_card"

    private const val EMPTY = ""
  }

  /**
   * Removes current address data
   */
  fun clear() {
    sharedPreferences.edit().clear().apply()
  }

  /**
   * Saves current home address data
   */
  fun setHomeAddress(address: Address) {
    sharedPreferences.edit {
      putString(KEY_HOME_ADDRESS_LINE_1, address.line1)
      putString(KEY_HOME_ADDRESS_LINE_2, address.line2)
      putString(KEY_HOME_CITY, address.city)
      putString(KEY_HOME_STATE, address.state)
      putString(KEY_HOME_ZIP, address.zip)
      putBoolean(KEY_HOME_HAS_BEEN_VALIDATED, address.hasBeenValidated)
    }
  }

  /**
   * Gets cached home address data
   */
  fun getHomeAddress(): Address {
    return Address(
      sharedPreferences.getString(KEY_HOME_ADDRESS_LINE_1, EMPTY)!!,
      sharedPreferences.getString(KEY_HOME_ADDRESS_LINE_2, EMPTY)!!,
      sharedPreferences.getString(KEY_HOME_CITY, EMPTY)!!,
      sharedPreferences.getString(KEY_HOME_STATE, EMPTY)!!,
      sharedPreferences.getString(KEY_HOME_ZIP, EMPTY)!!,
      true,
      sharedPreferences.getBoolean(KEY_HOME_HAS_BEEN_VALIDATED, false),
    )
  }

  /**
   * Saves current alternate address data
   */
  fun setSchoolAddress(address: Address) {
    sharedPreferences.edit {
      putString(KEY_SCHOOL_ADDRESS_LINE_1, address.line1)
      putString(KEY_SCHOOL_ADDRESS_LINE_2, address.line2)
      putString(KEY_SCHOOL_CITY, address.city)
      putString(KEY_SCHOOL_STATE, address.state)
      putString(KEY_SCHOOL_ZIP, address.zip)
      putBoolean(KEY_SCHOOL_HAS_BEEN_VALIDATED, address.hasBeenValidated)
    }
  }

  /**
   * Gets cached alternate address data
   */
  fun getSchoolAddress(): Address {
    return Address(
      sharedPreferences.getString(KEY_SCHOOL_ADDRESS_LINE_1, EMPTY)!!,
      sharedPreferences.getString(KEY_SCHOOL_ADDRESS_LINE_2, EMPTY)!!,
      sharedPreferences.getString(KEY_SCHOOL_CITY, EMPTY)!!,
      sharedPreferences.getString(KEY_SCHOOL_STATE, EMPTY)!!,
      sharedPreferences.getString(KEY_SCHOOL_ZIP, EMPTY)!!,
      false,
      sharedPreferences.getBoolean(KEY_SCHOOL_HAS_BEEN_VALIDATED, false),
    )
  }

  /**
   * Saves current alternate address data
   */
  fun setWorkAddress(address: Address) {
    sharedPreferences.edit {
      putString(KEY_WORK_ADDRESS_LINE_1, address.line1)
      putString(KEY_WORK_ADDRESS_LINE_2, address.line2)
      putString(KEY_WORK_CITY, address.city)
      putString(KEY_WORK_STATE, address.state)
      putString(KEY_WORK_ZIP, address.zip)
      putBoolean(KEY_WORK_HAS_BEEN_VALIDATED, address.hasBeenValidated)
    }
  }

  /**
   * Gets cached alternate address data
   */
  fun getWorkAddress(): Address {
    return Address(
      sharedPreferences.getString(KEY_WORK_ADDRESS_LINE_1, EMPTY)!!,
      sharedPreferences.getString(KEY_WORK_ADDRESS_LINE_2, EMPTY)!!,
      sharedPreferences.getString(KEY_WORK_CITY, EMPTY)!!,
      sharedPreferences.getString(KEY_WORK_STATE, EMPTY)!!,
      sharedPreferences.getString(KEY_WORK_ZIP, EMPTY)!!,
      false,
      sharedPreferences.getBoolean(KEY_WORK_HAS_BEEN_VALIDATED, false),
    )
  }

  /**
   * Gets cached personal information data
   */
  fun setPersonalInformation(personalInformation: PersonalInformation) {
    sharedPreferences.edit {
      putString(KEY_FIRST_NAME, personalInformation.firstName)
      putString(KEY_MIDDLE_NAME, personalInformation.middleName)
      putString(KEY_LAST_NAME, personalInformation.lastName)
      putString(KEY_BIRTH_DATE, personalInformation.birthDate)
      putString(KEY_EMAIL, personalInformation.email)
    }
  }

  /**
   * Sets cached personal information data
   */
  fun getPersonalInformation(): PersonalInformation {
    return PersonalInformation(
      sharedPreferences.getString(KEY_FIRST_NAME, EMPTY)!!,
      sharedPreferences.getString(KEY_MIDDLE_NAME, EMPTY)!!,
      sharedPreferences.getString(KEY_LAST_NAME, EMPTY)!!,
      sharedPreferences.getString(KEY_BIRTH_DATE, EMPTY)!!,
      sharedPreferences.getString(KEY_EMAIL, EMPTY)!!
    )
  }

  /**
   * Gets cached account information data
   */
  fun getAccountInformation(): AccountInformation {
    return AccountInformation(
      sharedPreferences.getString(KEY_USERNAME, EMPTY)!!,
      sharedPreferences.getString(KEY_PIN, EMPTY)!!
    )
  }

  /**
   * Sets cached account information data
   */
  fun setAccountInformation(username: String, pin: String) {
    sharedPreferences.edit {
      putString(KEY_USERNAME, username)
      putString(KEY_PIN, pin)
    }
  }

  /**
   * Getter/Setter of whether or not we are creating a juvenile card
   */
  var isJuvenileCard: Boolean? = null
    get() {
      if (field == null) {
        field = sharedPreferences.getBoolean(KEY_JUVENILE_CARD, false)
      }
      return field
    }
    set(value) {
      field = value
      sharedPreferences.edit { putBoolean(KEY_JUVENILE_CARD, value ?: false) }
    }
}
