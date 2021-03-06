package com.cookiegames.smartcookie.settings.fragment

import com.cookiegames.smartcookie.R
import com.cookiegames.smartcookie.browser.ProxyChoice
import com.cookiegames.smartcookie.constant.SCHEME_BLANK
import com.cookiegames.smartcookie.constant.SCHEME_BOOKMARKS
import com.cookiegames.smartcookie.constant.SCHEME_HOMEPAGE
import com.cookiegames.smartcookie.di.injector
import com.cookiegames.smartcookie.dialog.BrowserDialog
import com.cookiegames.smartcookie.extensions.withSingleChoiceItems
import com.cookiegames.smartcookie.preference.UserPreferences
import com.cookiegames.smartcookie.search.SearchEngineProvider
import com.cookiegames.smartcookie.search.Suggestions
import com.cookiegames.smartcookie.search.engine.BaseSearchEngine
import com.cookiegames.smartcookie.search.engine.CustomSearch
import com.cookiegames.smartcookie.utils.FileUtils
import com.cookiegames.smartcookie.utils.ProxyUtils
import com.cookiegames.smartcookie.utils.ThemeUtils
import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.cookiegames.smartcookie.browser.JavaScriptChoice
import com.cookiegames.smartcookie.browser.SiteBlockChoice
import com.cookiegames.smartcookie.browser.SuggestionNumChoice
import javax.inject.Inject

/**
 * The general settings of the app.
 */
class GeneralSettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var searchEngineProvider: SearchEngineProvider
    @Inject lateinit var userPreferences: UserPreferences

    private lateinit var proxyChoices: Array<String>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_general)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        proxyChoices = resources.getStringArray(R.array.proxy_choices_array)

        clickableDynamicPreference(
            preference = SETTINGS_PROXY,
            summary = userPreferences.proxyChoice.toSummary(),
            onClick = ::showProxyPicker
        )

        clickableDynamicPreference(
            preference = SETTINGS_USER_AGENT,
            summary = choiceToUserAgent(userPreferences.userAgentChoice),
            onClick = ::showUserAgentChooserDialog
        )

        clickableDynamicPreference(
            preference = SETTINGS_DOWNLOAD,
            summary = userPreferences.downloadDirectory,
            onClick = ::showDownloadLocationDialog
        )

       clickableDynamicPreference(
                preference = SETTINGS_SUGGESTIONS_NUM,
                summary = userPreferences.suggestionChoice.toString(),
                onClick = ::showSuggestionNumPicker
        )

        clickableDynamicPreference(
            preference = SETTINGS_HOME,
            summary = homePageUrlToDisplayTitle(userPreferences.homepage),
            onClick = ::showHomePageDialog
        )

        clickableDynamicPreference(
            preference = SETTINGS_SEARCH_ENGINE,
            summary = getSearchEngineSummary(searchEngineProvider.provideSearchEngine()),
            onClick = ::showSearchProviderDialog
        )

        clickableDynamicPreference(
            preference = SETTINGS_SUGGESTIONS,
            summary = searchSuggestionChoiceToTitle(Suggestions.from(userPreferences.searchSuggestionChoice)),
            onClick = ::showSearchSuggestionsDialog
        )

        clickableDynamicPreference(
                preference = SETTINGS_BLOCK_JAVASCRIPT,
                summary = userPreferences.javaScriptChoice.toSummary(),
                onClick = ::showJavaScriptPicker
        )

        switchPreference(
            preference = SETTINGS_IMAGES,
            isChecked = userPreferences.blockImagesEnabled,
            onCheckChange = { userPreferences.blockImagesEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_FORCE_ZOOM,
                isChecked = userPreferences.forceZoom,
                onCheckChange = { userPreferences.forceZoom = it }
        )

        switchPreference(
            preference = SETTINGS_SAVEDATA,
            isChecked = userPreferences.saveDataEnabled,
            onCheckChange = { userPreferences.saveDataEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_JAVASCRIPT,
            isChecked = userPreferences.javaScriptEnabled,
            onCheckChange = { userPreferences.javaScriptEnabled = it }
        )

        switchPreference(
            preference = SETTINGS_COLOR_MODE,
            isChecked = userPreferences.colorModeEnabled,
            onCheckChange = { userPreferences.colorModeEnabled = it }
        )

        switchPreference(
                preference = SETTINGS_LAST_TAB,
                isChecked = userPreferences.closeOnLastTab,
                onCheckChange = { userPreferences.closeOnLastTab = it }
        )

    }

    private fun showSuggestionNumPicker(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.suggestion)
            val stringArray = resources.getStringArray(R.array.suggest)
            val values = SuggestionNumChoice.values().map {
                Pair(it, when (it) {
                    SuggestionNumChoice.THREE -> stringArray[0]
                    SuggestionNumChoice.FOUR -> stringArray[1]
                    SuggestionNumChoice.FIVE -> stringArray[2]
                    SuggestionNumChoice.SIX -> stringArray[3]
                    SuggestionNumChoice.SEVEN -> stringArray[4]
                    SuggestionNumChoice.EIGHT -> stringArray[5]
                    else -> stringArray[2]
                })
            }
            withSingleChoiceItems(values, userPreferences.suggestionChoice) {
                updateSearchNum(it, context as Activity, summaryUpdater)
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun updateSearchNum(choice: SuggestionNumChoice, activity: Activity, summaryUpdater: SummaryUpdater) {

        userPreferences.suggestionChoice = choice
        summaryUpdater.updateSummary(choice.toString())
    }

    private fun ProxyChoice.toSummary(): String {
        val stringArray = resources.getStringArray(R.array.proxy_choices_array)
        return when (this) {
            ProxyChoice.NONE -> stringArray[0]
            ProxyChoice.ORBOT -> stringArray[1]
            ProxyChoice.I2P -> stringArray[2]
            ProxyChoice.MANUAL -> "${userPreferences.proxyHost}:${userPreferences.proxyPort}"
        }
    }

    private fun showProxyPicker(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.http_proxy)
            val stringArray = resources.getStringArray(R.array.proxy_choices_array)
            val values = ProxyChoice.values().map {
                Pair(it, when (it) {
                    ProxyChoice.NONE -> stringArray[0]
                    ProxyChoice.ORBOT -> stringArray[1]
                    ProxyChoice.I2P -> stringArray[2]
                    ProxyChoice.MANUAL -> stringArray[3]
                })
            }
            withSingleChoiceItems(values, userPreferences.proxyChoice) {
                updateProxyChoice(it, context as Activity, summaryUpdater)
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun updateProxyChoice(choice: ProxyChoice, activity: Activity, summaryUpdater: SummaryUpdater) {
        val sanitizedChoice = ProxyUtils.sanitizeProxyChoice(choice, activity)
        if (sanitizedChoice == ProxyChoice.MANUAL) {
            showManualProxyPicker(activity, summaryUpdater)
        }

        userPreferences.proxyChoice = sanitizedChoice
        summaryUpdater.updateSummary(sanitizedChoice.toSummary())
    }

    private fun showManualProxyPicker(activity: Activity, summaryUpdater: SummaryUpdater) {
        val v = activity.layoutInflater.inflate(R.layout.dialog_manual_proxy, null)
        val eProxyHost = v.findViewById<TextView>(R.id.proxyHost)
        val eProxyPort = v.findViewById<TextView>(R.id.proxyPort)

        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limit the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        val maxCharacters = Integer.MAX_VALUE.toString().length
        eProxyPort.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxCharacters - 1))

        eProxyHost.text = userPreferences.proxyHost
        eProxyPort.text = userPreferences.proxyPort.toString()

        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.manual_proxy)
            setView(v)
            setPositiveButton(R.string.action_ok) { _, _ ->
                val proxyHost = eProxyHost.text.toString()
                val proxyPort = try {
                    // Try/Catch in case the user types an empty string or a number
                    // larger than max integer
                    Integer.parseInt(eProxyPort.text.toString())
                } catch (ignored: NumberFormatException) {
                    userPreferences.proxyPort
                }

                userPreferences.proxyHost = proxyHost
                userPreferences.proxyPort = proxyPort
                summaryUpdater.updateSummary("$proxyHost:$proxyPort")
            }
        }
    }

    private fun choiceToUserAgent(index: Int) = when (index) {
        1 -> resources.getString(R.string.agent_default)
        2 -> resources.getString(R.string.agent_desktop)
        3 -> resources.getString(R.string.agent_mobile)
        4 -> resources.getString(R.string.agent_custom)
        else -> resources.getString(R.string.agent_default)
    }

    private fun showUserAgentChooserDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(resources.getString(R.string.title_user_agent))
            setSingleChoiceItems(R.array.user_agent, userPreferences.userAgentChoice - 1) { _, which ->
                userPreferences.userAgentChoice = which + 1
                summaryUpdater.updateSummary(choiceToUserAgent(userPreferences.userAgentChoice))
                when (which) {
                    in 0..2 -> Unit
                    3 -> {
                        summaryUpdater.updateSummary(resources.getString(R.string.agent_custom))
                        showCustomUserAgentPicker(summaryUpdater)
                    }
                }
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }

    private fun showCustomUserAgentPicker(summaryUpdater: SummaryUpdater) {
        activity?.let {
            BrowserDialog.showEditText(it,
                R.string.title_user_agent,
                R.string.title_user_agent,
                userPreferences.userAgentString,
                R.string.action_ok) { s ->
                userPreferences.userAgentString = s
                summaryUpdater.updateSummary(it.getString(R.string.agent_custom))
            }
        }
    }

    private fun showDownloadLocationDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(resources.getString(R.string.title_download_location))
            val n: Int = if (userPreferences.downloadDirectory.contains(Environment.DIRECTORY_DOWNLOADS)) {
                0
            } else {
                1
            }

            setSingleChoiceItems(R.array.download_folder, n) { _, which ->
                when (which) {
                    0 -> {
                        userPreferences.downloadDirectory = FileUtils.DEFAULT_DOWNLOAD_PATH
                        summaryUpdater.updateSummary(FileUtils.DEFAULT_DOWNLOAD_PATH)
                    }
                    1 -> {
                        showCustomDownloadLocationPicker(summaryUpdater)
                    }
                }
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }


    private fun showCustomDownloadLocationPicker(summaryUpdater: SummaryUpdater) {
        activity?.let { activity ->
            val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null)
            val getDownload = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

            val errorColor = ContextCompat.getColor(activity
                , R.color.error_red)
            val regularColor = ThemeUtils.getTextColor(activity)
            getDownload.setTextColor(regularColor)
            getDownload.addTextChangedListener(DownloadLocationTextWatcher(getDownload, errorColor, regularColor))
            getDownload.setText(userPreferences.downloadDirectory)

            BrowserDialog.showCustomDialog(activity) {
                setTitle(R.string.title_download_location)
                setView(dialogView)
                setPositiveButton(R.string.action_ok) { _, _ ->
                    var text = getDownload.text.toString()
                    text = FileUtils.addNecessarySlashes(text)
                    userPreferences.downloadDirectory = text
                    summaryUpdater.updateSummary(text)
                }
            }
        }
    }

    private class DownloadLocationTextWatcher(
        private val getDownload: EditText,
        private val errorColor: Int,
        private val regularColor: Int
    ) : TextWatcher {

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (!FileUtils.isWriteAccessAvailable(s.toString())) {
                this.getDownload.setTextColor(this.errorColor)
            } else {
                this.getDownload.setTextColor(this.regularColor)
            }
        }
    }

    private fun homePageUrlToDisplayTitle(url: String): String = when (url) {
        SCHEME_HOMEPAGE -> resources.getString(R.string.action_homepage)
        SCHEME_BLANK -> resources.getString(R.string.action_blank)
        SCHEME_BOOKMARKS -> resources.getString(R.string.action_bookmarks)
        else -> url
    }

    private fun showHomePageDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.home)
            val n = when (userPreferences.homepage) {
                SCHEME_HOMEPAGE -> 0
                SCHEME_BLANK -> 1
                SCHEME_BOOKMARKS -> 2
                else -> 3
            }

            setSingleChoiceItems(R.array.homepage, n) { _, which ->
                when (which) {
                    0 -> {
                        userPreferences.homepage = SCHEME_HOMEPAGE
                        summaryUpdater.updateSummary(resources.getString(R.string.action_homepage))
                    }
                    1 -> {
                        userPreferences.homepage = SCHEME_BLANK
                        summaryUpdater.updateSummary(resources.getString(R.string.action_blank))
                    }
                    2 -> {
                        userPreferences.homepage = SCHEME_BOOKMARKS
                        summaryUpdater.updateSummary(resources.getString(R.string.action_bookmarks))
                    }
                    3 -> {
                        showCustomHomePagePicker(summaryUpdater)
                    }
                }
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }

    private fun showCustomHomePagePicker(summaryUpdater: SummaryUpdater) {
        val currentHomepage: String = if (!URLUtil.isAboutUrl(userPreferences.homepage)) {
            userPreferences.homepage
        } else {
            "https://www.google.com"
        }

        activity?.let {
            BrowserDialog.showEditText(it,
                R.string.title_custom_homepage,
                R.string.title_custom_homepage,
                currentHomepage,
                R.string.action_ok) { url ->
                if(url.contains("http")){
                    userPreferences.homepage = url
                    summaryUpdater.updateSummary(url)
                }
                else{
                    userPreferences.homepage = "https://" + url
                    summaryUpdater.updateSummary("https://" + url)
                }

            }
        }
    }

    private fun getSearchEngineSummary(baseSearchEngine: BaseSearchEngine): String {
        return if (baseSearchEngine is CustomSearch) {
            baseSearchEngine.queryUrl
        } else {
            getString(baseSearchEngine.titleRes)
        }
    }

    private fun convertSearchEngineToString(searchEngines: List<BaseSearchEngine>): Array<CharSequence> =
        searchEngines.map { getString(it.titleRes) }.toTypedArray()

    private fun showSearchProviderDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(resources.getString(R.string.title_search_engine))

            val searchEngineList = searchEngineProvider.provideAllSearchEngines()

            val chars = convertSearchEngineToString(searchEngineList)

            val n = userPreferences.searchChoice

            setSingleChoiceItems(chars, n) { _, which ->
                val searchEngine = searchEngineList[which]

                // Store the search engine preference
                val preferencesIndex = searchEngineProvider.mapSearchEngineToPreferenceIndex(searchEngine)
                userPreferences.searchChoice = preferencesIndex

                if (searchEngine is CustomSearch) {
                    // Show the URL picker
                    showCustomSearchDialog(searchEngine, summaryUpdater)
                } else {
                    // Set the new search engine summary
                    summaryUpdater.updateSummary(getSearchEngineSummary(searchEngine))
                }
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun showCustomSearchDialog(customSearch: CustomSearch, summaryUpdater: SummaryUpdater) {
        activity?.let {
            BrowserDialog.showEditText(
                it,
                R.string.search_engine_custom,
                R.string.search_engine_custom,
                userPreferences.searchUrl,
                R.string.action_ok
            ) { searchUrl ->
                userPreferences.searchUrl = searchUrl
                summaryUpdater.updateSummary(getSearchEngineSummary(customSearch))
            }

        }
    }

    private fun JavaScriptChoice.toSummary(): String {
        val stringArray = resources.getStringArray(R.array.blocked_sites)
        return when (this) {
            JavaScriptChoice.NONE -> stringArray[0]
            JavaScriptChoice.WHITELIST -> userPreferences.siteBlockNames
            JavaScriptChoice.BLACKLIST -> userPreferences.siteBlockNames
        }
    }

    private fun showJavaScriptPicker(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.block_javascript)
            val stringArray = resources.getStringArray(R.array.blocked_sites)
            val values = JavaScriptChoice.values().map {
                Pair(it, when (it) {
                    JavaScriptChoice.NONE -> stringArray[0]
                    JavaScriptChoice.WHITELIST -> stringArray[1]
                    JavaScriptChoice.BLACKLIST -> stringArray[2]
                })
            }
            withSingleChoiceItems(values, userPreferences.javaScriptChoice) {
                updateJavaScriptChoice(it, context as Activity, summaryUpdater)
            }
            setPositiveButton(R.string.action_ok, null)
        }
    }

    private fun updateJavaScriptChoice(choice: JavaScriptChoice, activity: Activity, summaryUpdater: SummaryUpdater) {
        if (choice == JavaScriptChoice.WHITELIST || choice == JavaScriptChoice.BLACKLIST) {
            showManualJavaScriptPicker(activity, summaryUpdater, choice)
        }

        userPreferences.javaScriptChoice = choice
        summaryUpdater.updateSummary(choice.toSummary())
    }

    private fun showManualJavaScriptPicker(activity: Activity, summaryUpdater: SummaryUpdater, choice: JavaScriptChoice) {
        val v = activity.layoutInflater.inflate(R.layout.site_block, null)
        val blockedSites = v.findViewById<TextView>(R.id.siteBlock)
        // Limit the number of characters since the port needs to be of type int
        // Use input filters to limit the EditText length and determine the max
        // length by using length of integer MAX_VALUE
        val maxCharacters = Integer.MAX_VALUE.toString().length

        blockedSites.text = userPreferences.javaScriptBlocked

        BrowserDialog.showCustomDialog(activity) {
            setTitle(R.string.block_javascript)
            setView(v)
            setPositiveButton(R.string.action_ok) { _, _ ->
                val proxyHost = blockedSites.text.toString()
                userPreferences.javaScriptBlocked = proxyHost
                if(choice.toString() == "BLACKLIST"){
                    summaryUpdater.updateSummary(getText(R.string.only_allow_sites).toString())
                }
                else{
                    summaryUpdater.updateSummary(getText(R.string.block_all_sites).toString())
                }

            }
        }
    }

    private fun searchSuggestionChoiceToTitle(choice: Suggestions): String =
        when (choice) {
            Suggestions.GOOGLE -> getString(R.string.powered_by_google)
            Suggestions.DUCK -> getString(R.string.powered_by_duck)
            Suggestions.BAIDU -> getString(R.string.powered_by_baidu)
            Suggestions.NAVER -> getString(R.string.powered_by_naver)
            Suggestions.NONE -> getString(R.string.search_suggestions_off)
        }

    private fun showSearchSuggestionsDialog(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showCustomDialog(activity) {
            setTitle(resources.getString(R.string.search_suggestions))

            val currentChoice = when (Suggestions.from(userPreferences.searchSuggestionChoice)) {
                Suggestions.GOOGLE -> 0
                Suggestions.DUCK -> 1
                Suggestions.BAIDU -> 2
                Suggestions.NAVER -> 3
                Suggestions.NONE -> 4
            }

            setSingleChoiceItems(R.array.suggestions, currentChoice) { _, which ->
                val suggestionsProvider = when (which) {
                    0 -> Suggestions.GOOGLE
                    1 -> Suggestions.DUCK
                    2 -> Suggestions.BAIDU
                    3 -> Suggestions.NAVER
                    4 -> Suggestions.NONE
                    else -> Suggestions.GOOGLE
                }
                userPreferences.searchSuggestionChoice = suggestionsProvider.index
                summaryUpdater.updateSummary(searchSuggestionChoiceToTitle(suggestionsProvider))
                Toast.makeText(context, getText(R.string.please_restart), Toast.LENGTH_LONG)
            }
            setPositiveButton(resources.getString(R.string.action_ok), null)
        }
    }

    companion object {
        private const val SETTINGS_PROXY = "proxy"
        private const val SETTINGS_FORCE_ZOOM = "force_zoom"
        private const val SETTINGS_IMAGES = "cb_images"
        private const val SETTINGS_SAVEDATA = "savedata"
        private const val SETTINGS_JAVASCRIPT = "cb_javascript"
        private const val SETTINGS_BLOCK_JAVASCRIPT = "block_javascript"
        private const val SETTINGS_COLOR_MODE = "cb_colormode"
        private const val SETTINGS_USER_AGENT = "agent"
        private const val SETTINGS_DOWNLOAD = "download"
        private const val SETTINGS_HOME = "home"
        private const val SETTINGS_SEARCH_ENGINE = "search"
        private const val SETTINGS_SUGGESTIONS = "suggestions_choice"
        private const val SETTINGS_SUGGESTIONS_NUM = "suggestions_number"
        private const val SETTINGS_LAST_TAB = "last_tab"
    }
}
