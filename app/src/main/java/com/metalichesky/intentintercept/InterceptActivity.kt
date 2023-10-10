package com.metalichesky.intentintercept

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.ClipboardManager
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.LeadingMarginSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import com.metalichesky.intentintercept.databinding.ActivityInterceptBinding
import java.net.URISyntaxException

//TODO add icon -which icon - app icons???
//TODO add bitmaps/images (from intent extras?)
//TODO add getCallingActivity() - will only give details for startActivityForResult();
/**
 * Should really be called IntentDetailsActivity but this may cause problems with launcher
 * shortcuts and the enabled/disabled state of interception.
 */
class InterceptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInterceptBinding

    private var shareActionProvider: ShareActionProvider? = null
    private val density by lazy(LazyThreadSafetyMode.NONE) {
        getDisplayDensity()
    }
    private val standardIntentSizeDp = 10
    private val standardIntentSizeSp by lazy(LazyThreadSafetyMode.NONE) {
        (standardIntentSizeDp * density).toInt()
    }

    /**
     * String representation of intent as uri
     */
    private var originalIntent: String? = null

    /**
     * Bugfix #14: extras that are lost in the intent <-> string conversion
     */
    private var additionalExtras: Bundle? = null
    private var editableIntent: Intent? = null

    // support for onActivityResult
    private var lastResultCode: Int? = null
    private var lastResultIntent: Intent? = null

    private val textWatchers = mutableListOf<IntentUpdateTextWatcher>()
    private var textWatchersActive = true
        set(value) {
            textWatchers.forEach { it.isActive = value }
            field = value
        }

    private val intentDetailsHelper = IntentDetailsHelper(this)
    private val intentDetailsString: Spanned?
        get() {
            val intent = editableIntent ?: return null
            return intentDetailsHelper.getIntentDetails(
                intent = intent,
                lastResultCode = lastResultCode,
                lastResultIntent = lastResultIntent
            )
        }

    private var lastToast: Toast? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterceptBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        rememberIntent(intent)
        val isVisible = savedInstanceState != null && savedInstanceState.getBoolean(INTENT_EDITED)
        showInitialIntent(isVisible)
    }

    private fun rememberIntent(original: Intent) {
        originalIntent = original.getUri()
        val copy = cloneIntent(originalIntent)
        val originalExtras = original.extras
        if (originalExtras != null && copy != null) {
            // bugfix #14: collect extras that are lost in the intent <-> string conversion
            val additionalExtrasBundle = Bundle(originalExtras)
            for (key: String in originalExtras.keySet()) {
                if (copy.hasExtra(key)) {
                    additionalExtrasBundle.remove(key)
                }
            }
            if (!additionalExtrasBundle.isEmpty) {
                additionalExtras = additionalExtrasBundle
            }
        }
    }

    /**
     * creates a clone of originalIntent and displays it for editing
     *
     * @param isResetButtonVisible
     */
    private fun showInitialIntent(
        isResetButtonVisible: Boolean
    ) {
        editableIntent = cloneIntent(originalIntent)
        editableIntent?.component = null
        setupTextWatchers()
        setupButtons()
        showAllIntentData(null)
        showResetIntentButton(isVisible = isResetButtonVisible)
        showResendIntentButton(isIntentChanged = false)
    }

    /**
     * textViewToIgnore is not updated so current selected char in that textview will not change
     */
    @Suppress("DEPRECATION")
    private fun showAllIntentData(textViewToIgnore: TextView?) {
        showTextViewIntentData(textViewToIgnore)
        binding.intentCategoriesLayout.removeAllViews()
        val categories = editableIntent?.categories
        if (categories != null) {
            binding.intentCategoriesHeader.isVisible = true
            for (category: String in categories) {
                val categoryTextView = TextView(this)
                categoryTextView.text = category
                categoryTextView.setTextAppearanceCompat(this, R.style.TextFlags)
                binding.intentCategoriesLayout.addView(categoryTextView)
            }
        } else {
            binding.intentCategoriesHeader.isVisible = false
            // addTextToLayout("NONE", Typeface.NORMAL, categoriesLayout);
        }
        binding.intentFlagsLayout.removeAllViews()
        val flagsStrings = editableIntent?.flags() ?: emptyList()
        if (flagsStrings.isNotEmpty()) {
            for (thisFlagString: String in flagsStrings) {
                addTextToLayout(thisFlagString, Typeface.NORMAL, binding.intentFlagsLayout)
            }
        } else {
            addTextToLayout(
                getString(R.string.no_items),
                Typeface.NORMAL,
                binding.intentFlagsLayout
            )
        }
        binding.intentExtrasLayout.removeAllViews()
        try {
            val intentBundle = editableIntent?.extras
            if (intentBundle != null) {
                val extraKeys = intentBundle.keySet()
                var count = 0
                for (extraKey: String in extraKeys) {
                    count++
                    val extraItem = intentBundle[extraKey]
                    if (extraItem != null) {
                        val extraItemTypeName = extraItem.javaClass.name
                        addTextToLayout(count.toString(), Typeface.BOLD, binding.intentExtrasLayout)
                        addTextToLayout(
                            getString(R.string.extra_item_type_name_title) + BLANK + extraItemTypeName,
                            Typeface.ITALIC,
                            standardIntentSizeDp,
                            binding.intentExtrasLayout
                        )
                        addTextToLayout(
                            getString(R.string.extra_item_key_title) + BLANK + extraKey,
                            Typeface.ITALIC,
                            standardIntentSizeDp,
                            binding.intentExtrasLayout
                        )
                        if (extraItem is ArrayList<*>) {
                            addTextToLayout(
                                getString(R.string.extra_item_type_name_list),
                                Typeface.ITALIC,
                                binding.intentExtrasLayout
                            )
                            for (thisArrayListObject: Any in extraItem) {
                                addTextToLayout(
                                    thisArrayListObject.toString(),
                                    Typeface.ITALIC, standardIntentSizeDp,
                                    binding.intentExtrasLayout
                                )
                            }
                        } else {
                            addTextToLayout(
                                getString(R.string.extra_item_value_title) + BLANK + extraItem,
                                Typeface.ITALIC, standardIntentSizeDp,
                                binding.intentExtrasLayout
                            )
                        }
                    }
                }
            } else {
                addTextToLayout(
                    getString(R.string.no_items), Typeface.NORMAL,
                    binding.intentExtrasLayout
                )
            }
        } catch (e: Exception) {
            // TODO Should make this red to highlight error
            addTextToLayout(
                getString(R.string.error_extracting_extras),
                Typeface.NORMAL,
                binding.intentExtrasLayout
            )
            e.printStackTrace()
        }
        refreshUI()
    }

    /**
     * textViewToIgnore is not updated so current selected char in that textview will not change
     */
    private fun showTextViewIntentData(textViewToIgnore: TextView?) {
        textWatchersActive = false
        binding.actionEdit.takeIf { it != textViewToIgnore }?.setText(editableIntent?.action)
        binding.dataEdit.takeIf { it != textViewToIgnore && editableIntent?.dataString != null }
            ?.setText(editableIntent?.dataString)
        binding.typeEdit.takeIf { it != textViewToIgnore }?.setText(editableIntent?.type)
        binding.uriEdit.takeIf { it != textViewToIgnore }?.setText(editableIntent?.getUri())
        textWatchersActive = true
    }

    private fun checkAndShowMatchingActivities() {
        binding.intentMatchinActivitiesLayout.removeAllViews()
        binding.intentMatchingActivitiesHeader.text =
            getString(R.string.intent_matching_activities_title)

        val editableIntent = editableIntent ?: return
        val pm = packageManager
        val resolveInfo = pm.queryIntentActivities(
            editableIntent, 0
        )

        // Remove Intent Intercept from matching activities
        val numberOfMatchingActivities = resolveInfo.size - 1
        if (numberOfMatchingActivities < 1) {
            binding.resendIntentButton.isEnabled = false
            addTextToLayout(
                getString(R.string.no_items),
                Typeface.NORMAL,
                binding.intentMatchinActivitiesLayout
            )
        } else {
            binding.resendIntentButton.isEnabled = true
            for (i in 0..numberOfMatchingActivities) {
                val info = resolveInfo[i]
                val activityInfo = info.activityInfo
                if (activityInfo.packageName != packageName) {
                    addTextToLayout(
                        getActivityInfoString(pm, activityInfo),
                        Typeface.NORMAL,
                        binding.intentMatchinActivitiesLayout
                    )
                }
            }
        }
    }

    private fun getActivityInfoString(
        packageManager: PackageManager,
        activityInfo: ActivityInfo
    ): String {
        val label = activityInfo.loadLabel(packageManager)
        val packageName = activityInfo.packageName
        val name = activityInfo.name
        return "$label ($packageName - $name)"
    }

    private fun addTextToLayout(
        text: String,
        typeface: Int,
        paddingLeft: Int,
        layout: LinearLayout
    ) {
        val textView = TextView(this)
        val styleParagraph =
            LeadingMarginSpan.Standard(0, standardIntentSizeSp)
        val styledText = SpannableString(text)
        styledText.setSpan(
            styleParagraph, 0, styledText.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        textView.text = styledText
        textView.setTextAppearanceCompat(this, R.style.TextFlags)
        textView.setTypeface(null, typeface)
        textView.setTextIsSelectable(true)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins((paddingLeft * density).toInt(), 0, 0, 0)
        layout.addView(textView, params)
    }

    private fun addTextToLayout(text: String, typeface: Int, layout: LinearLayout) {
        addTextToLayout(text, typeface, 0, layout)
    }

    private fun setupTextWatchers() {
        textWatchers.clear()

        binding.actionEdit.addTextChangedListener(IntentUpdateTextWatcher {
            editableIntent?.action = binding.actionEdit.text.toString()

            onUpdateIntent(binding.actionEdit)
        }.also { textWatchers.add(it) })

        binding.dataEdit.addTextChangedListener(IntentUpdateTextWatcher {
            // setData clears type so we save it
            val savedType = editableIntent?.type
            val uri = try {
                Uri.parse(binding.dataEdit.text.toString())
            } catch (ex: Exception) {
                showToast("Wrong uri")
                null
            }
            editableIntent?.setDataAndType(uri, savedType)

            onUpdateIntent(binding.dataEdit)
        }.also { textWatchers.add(it) })

        binding.typeEdit.addTextChangedListener(IntentUpdateTextWatcher {
            // setData clears type so we save it
            val savedData = editableIntent?.data
            editableIntent?.setDataAndType(savedData, binding.typeEdit.text.toString())

            onUpdateIntent(binding.typeEdit)
        }.also { textWatchers.add(it) })

        binding.uriEdit.addTextChangedListener(IntentUpdateTextWatcher {
            // no error yet so continue
            editableIntent = cloneIntent(binding.uriEdit.text.toString())

            onUpdateIntent(binding.uriEdit)
            // this time must update all content since extras/flags may have been changed
            showAllIntentData(binding.uriEdit)
        }.also { textWatchers.add(it) })
    }

    private fun setupButtons() {
        binding.resetIntentButton.setOnClickListener {
            onResetIntent()
        }
        binding.resendIntentButton.setOnClickListener {
            onSendIntent()
        }
    }

    private fun onUpdateIntent(textView: TextView) {
        try {
            showTextViewIntentData(textView)
            showResetIntentButton(isVisible = true)
            showResendIntentButton(isIntentChanged = true)
            refreshUI()
        } catch (e: Exception) {
            showToast("Error updating intent: ${e.message}")
        }
    }

    private fun showResendIntentButton(isIntentChanged: Boolean) {
        if (isIntentChanged) {
            binding.resendIntentButton.setText(R.string.button_title_send_edited_intent)
        } else {
            binding.resendIntentButton.setText(R.string.button_title_resend_intent)
        }
    }

    private fun showResetIntentButton(isVisible: Boolean) {
        binding.resetIntentButton.isVisible = isVisible
    }

    private fun onSendIntent() {
        try {
            val title = binding.resendIntentButton.text
            startActivityForResult(Intent.createChooser(editableIntent, title), INTENT_REQUEST_CODE)
        } catch (e: Exception) {
            showToast("Error starting intent: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun onResetIntent() {
        // this would break onActivityResult
        // startActivity(this.originalIntent); // reload this with original data
        // finish();
        textWatchersActive = false
        showInitialIntent(false)
        textWatchersActive = true
        refreshUI()
    }

    private fun copyIntentDetails() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.text = intentDetailsString
        showToast(getString(R.string.message_intent_details_copied_to_clipboard))
    }

    private fun refreshUI() {
        checkAndShowMatchingActivities()
        shareActionProvider?.setShareIntent(createShareIntent())
    }

    private fun createShareIntent(): Intent {
        val share = Intent(Intent.ACTION_SEND)
        share.type = getString(R.string.mime_type_text_plain)
        share.putExtra(Intent.EXTRA_TEXT, intentDetailsString)
        return share
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val actionItem = menu.findItem(R.id.menu_share)
        shareActionProvider = MenuItemCompat.getActionProvider(actionItem) as? ShareActionProvider
        if (shareActionProvider == null) {
            shareActionProvider = ShareActionProvider(this)
            MenuItemCompat.setActionProvider(actionItem, shareActionProvider)
        }
        shareActionProvider?.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME)
        refreshUI()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_copy) {
            copyIntentDetails()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        textWatchersActive = false
    }

    override fun onResume() {
        super.onResume()
        // inhibit new activity animation when resetting intent details
        overridePendingTransition(0, 0)
        textWatchersActive = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            INTENT_EDITED,
            binding.resetIntentButton.isVisible
        )
    }

    // support for onActivityResult
    // OriginatorActivity -> IntentIntercept -> resendIntentActivity
    // Forward result of sub-activity {resendIntentActivity}
    // to caller of this activity {OriginatorActivity}.
    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall", "UnsafeIntentLaunch")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        processActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun processActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        lastResultCode = resultCode
        lastResultIntent = data
        super.onActivityResult(requestCode, resultCode, data)
        setResult(resultCode, data)
        refreshUI()
        val uri = data?.data
        showToast(getString(
                R.string.last_result_message,
                getString(R.string.last_result_header_title),
                requestCode.toString(),
                uri
            ))
    }

    private fun cloneIntent(intentUri: String?): Intent? {
        if (intentUri != null) {
            try {
                val clone = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
                // bugfix #14: restore extras that are lost in the intent <-> string conversion
                additionalExtras?.let { clone.putExtras(it) }
                return clone
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun showToast(text: String, length: Int = Toast.LENGTH_LONG) {
        lastToast?.cancel()
        lastToast = Toast.makeText(
            this,
            text,
            length
        )
        lastToast?.show()

    }
}

@Suppress("DEPRECATION")
private fun TextView.setTextAppearanceCompat(context: Context, resId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        setTextAppearance(resId)
    } else {
        setTextAppearance(context, resId)
    }
}

@Suppress("DEPRECATION")
private fun Context.getDisplayDensity(): Float {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics = DisplayMetrics()
        display?.getRealMetrics(metrics)
        metrics.density
    } else {
        val metrics = DisplayMetrics()
        val wm = getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(metrics)
        metrics.density
    }
}

private class IntentUpdateTextWatcher constructor(
    var isActive: Boolean = true,
    private val callback: Callback
) : TextWatcher {
    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
    ) {
        if (isActive) {
            callback.onUpdateIntent(s.toString())
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
    override fun afterTextChanged(s: Editable) = Unit

    fun interface Callback {
        fun onUpdateIntent(modifiedContent: String?)
    }
}

private const val INTENT_EDITED = "intent_edited"
private const val INTENT_REQUEST_CODE = 1