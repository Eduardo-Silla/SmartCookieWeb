package com.cookiegames.smartcookie.settings.fragment

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.Preference
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.preference.UserPreferences
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * The general settings of the app.
 */
class ExtensionsSettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var userPreferences: UserPreferences

    private lateinit var proxyChoices: Array<String>

    override fun providePreferencesXmlResource() = R.xml.preference_extensions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        switchPreference(
                preference = DARK_MODE,
                isChecked = userPreferences.darkModeExtension,
                onCheckChange = { userPreferences.darkModeExtension = it }
        )

        /*switchPreference(
                preference = TRANSLATE,
                isChecked = userPreferences.darkModeExtension,
                onCheckChange = { userPreferences.darkModeExtension = it }
        )*/

    }




    companion object {
        private const val DARK_MODE = "dark_mode"
        private const val TRANSLATE = "translate"
    }
}
