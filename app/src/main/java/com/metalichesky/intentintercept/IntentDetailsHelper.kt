package com.metalichesky.intentintercept

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.Spanned

class IntentDetailsHelper(
    private val context: Context
) {

    fun getIntentDetails(
        intent: Intent,
        lastResultCode: Int?,
        lastResultIntent: Intent?
    ): Spanned {
        val result = StringBuilder()
        // k3b so intent can be reloaded using
        // Intent.parseUri("Intent:....", Intent.URI_INTENT_SCHEME)
        result.append(intent.getUri())
            .append(NEW_SEGMENT)
        appendIntentDetails(result, intent, true)
            .append(NEW_SEGMENT)
        val pm = context.packageManager
        val resolveInfo = pm.queryIntentActivities(intent, 0)

        // Remove Intent Intercept from matching activities
        val numberOfMatchingActivities = resolveInfo.size - 1
        appendHeader(result, R.string.intent_matching_activities_title)
        if (numberOfMatchingActivities < 1) {
            appendHeader(result, R.string.no_items)
        } else {
            for (i in 0..numberOfMatchingActivities) {
                val info = resolveInfo[i]
                val activityinfo = info.activityInfo
                if (activityinfo.packageName != context.packageName) {
                    result.append(BOLD_START).append(activityinfo.loadLabel(pm))
                        .append(BOLD_END_BLANK).append(" (")
                        .append(activityinfo.packageName)
                        .append(" - ")
                        .append(activityinfo.name)
                        .append(")").append(NEWLINE)
                }
            }
        }

        // support for onActivityResult
        if (lastResultCode != null) {
            result.append(NEW_SEGMENT)
            appendHeader(result, R.string.last_result_header_title)
            appendNameValue(result, R.string.last_result_code_title, lastResultCode)
            if (lastResultIntent != null) {
                appendIntentDetails(result, lastResultIntent, false)
            }
        }
        return Html.fromHtml(result.toString())
    }

    private fun appendIntentDetails(
        result: StringBuilder,
        intent: Intent,
        detailed: Boolean
    ): StringBuilder {
        if (detailed) appendNameValue(result, R.string.intent_action_title, intent.action)
        appendNameValue(result, R.string.intent_data_title, intent.data)
        appendNameValue(result, R.string.intent_mime_type_title, intent.type)
        appendNameValue(result, R.string.intent_uri_title, intent.getUri())
        val categories = intent.categories
        if ((categories != null) && (categories.size > 0)) {
            appendHeader(result, R.string.intent_categories_title)
            for (category: String in categories) {
                result.append(category).append(NEWLINE)
            }
        }
        if (detailed) {
            appendHeader(result, R.string.intent_flags_title)
            val flagsStrings = intent.flags()
            if (flagsStrings.isNotEmpty()) {
                for (thisFlagString: String in flagsStrings) {
                    result.append(thisFlagString).append(NEWLINE)
                }
            } else {
                result.append(context.getString(R.string.no_items)).append(NEWLINE)
            }
        }
        try {
            val intentBundle = intent.extras
            if (intentBundle != null) {
                val keySet = intentBundle.keySet()
                appendHeader(result, R.string.intent_extras_title)
                var count = 0
                for (key: String in keySet) {
                    count++
                    val thisObject = intentBundle[key]
                    result.append(BOLD_START).append(count).append(BOLD_END_BLANK)
                    val thisClass = thisObject!!.javaClass.name
                    if (thisClass != null) {
                        result.append(context.getString(R.string.extra_item_type_name_title)).append(BLANK)
                            .append(thisClass).append(NEWLINE)
                    }
                    result.append(context.getString(R.string.extra_item_key_title)).append(BLANK)
                        .append(key).append(NEWLINE)
                    if ((thisObject is String || thisObject is Long
                                || thisObject is Int
                                || thisObject is Boolean
                                || thisObject is Uri)
                    ) {
                        result.append(context.getString(R.string.extra_item_value_title)).append(BLANK)
                            .append(thisObject.toString())
                            .append(NEWLINE)
                    } else if (thisObject is ArrayList<*>) {
                        result.append(context.getString(R.string.extra_item_type_name_list)).append(NEWLINE)
                        for (thisArrayListObject: Any in thisObject) {
                            result.append(thisArrayListObject.toString()).append(NEWLINE)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            appendHeader(result, R.string.intent_extras_title)
            result.append("<font color='red'>").append(context.getString(R.string.error_extracting_extras))
                .append("</font>").append(
                    NEWLINE
                )
            e.printStackTrace()
        }
        return result
    }

    private fun appendNameValue(result: StringBuilder, keyId: Int, value: Any?): StringBuilder {
        if (value != null) {
            result.append(BOLD_START).append(context.getString(keyId)).append(BOLD_END_BLANK)
                .append(value).append(NEWLINE)
        }
        return result
    }

    private fun appendHeader(result: StringBuilder, keyId: Int): StringBuilder {
        result.append(BOLD_START).append(context.getString(keyId)).append(BOLD_END_NL)
        return result
    }
}

fun Intent.getUri(): String {
    return toUri(Intent.URI_INTENT_SCHEME)
}


fun Intent.flags(): List<String> {
    val flagsStrings = mutableListOf<String>()
    val flags = flags
    val set = FLAGS_MAP.entries
    val i = set.iterator()
    while (i.hasNext()) {
        val thisFlag = i.next()
        if (flags and thisFlag.key != 0) {
            flagsStrings.add(thisFlag.value)
        }
    }
    return flagsStrings.toList()
}

const val BLANK = " "
private const val NEWLINE = "\n<br>"
private const val NEW_SEGMENT = "$NEWLINE------------$NEWLINE"
private const val BOLD_START = "<b><u>"
private const val BOLD_END_BLANK = "</u></b>$BLANK"
private const val BOLD_END_NL = "</u></b>$NEWLINE"
private val FLAGS_MAP: Map<Int, String> = buildMap {
    put(Intent.FLAG_GRANT_READ_URI_PERMISSION, "FLAG_GRANT_READ_URI_PERMISSION")
    put(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, "FLAG_GRANT_WRITE_URI_PERMISSION")
    put(Intent.FLAG_FROM_BACKGROUND, "FLAG_FROM_BACKGROUND")
    put(Intent.FLAG_DEBUG_LOG_RESOLUTION, "FLAG_DEBUG_LOG_RESOLUTION")
    put(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES, "FLAG_EXCLUDE_STOPPED_PACKAGES")
    put(Intent.FLAG_INCLUDE_STOPPED_PACKAGES, "FLAG_INCLUDE_STOPPED_PACKAGES")
    put(Intent.FLAG_ACTIVITY_NO_HISTORY, "FLAG_ACTIVITY_NO_HISTORY")
    put(Intent.FLAG_ACTIVITY_SINGLE_TOP, "FLAG_ACTIVITY_SINGLE_TOP")
    put(Intent.FLAG_ACTIVITY_NEW_TASK, "FLAG_ACTIVITY_NEW_TASK")
    put(Intent.FLAG_ACTIVITY_MULTIPLE_TASK, "FLAG_ACTIVITY_MULTIPLE_TASK")
    put(Intent.FLAG_ACTIVITY_CLEAR_TOP, "FLAG_ACTIVITY_CLEAR_TOP")
    put(Intent.FLAG_ACTIVITY_FORWARD_RESULT, "FLAG_ACTIVITY_FORWARD_RESULT")
    put(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP, "FLAG_ACTIVITY_PREVIOUS_IS_TOP")
    put(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS, "FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS")
    put(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT, "FLAG_ACTIVITY_BROUGHT_TO_FRONT")
    put(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED, "FLAG_ACTIVITY_RESET_TASK_IF_NEEDED")
    put(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY, "FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY")
    put(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET, "FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET")
    put(Intent.FLAG_ACTIVITY_NO_USER_ACTION, "FLAG_ACTIVITY_NO_USER_ACTION")
    put(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT, "FLAG_ACTIVITY_REORDER_TO_FRONT")
    put(Intent.FLAG_ACTIVITY_NO_ANIMATION, "FLAG_ACTIVITY_NO_ANIMATION")
    put(Intent.FLAG_ACTIVITY_CLEAR_TASK, "FLAG_ACTIVITY_CLEAR_TASK")
    put(Intent.FLAG_ACTIVITY_TASK_ON_HOME, "FLAG_ACTIVITY_TASK_ON_HOME")
    put(Intent.FLAG_RECEIVER_REGISTERED_ONLY, "FLAG_RECEIVER_REGISTERED_ONLY")
    put(Intent.FLAG_RECEIVER_REPLACE_PENDING, "FLAG_RECEIVER_REPLACE_PENDING")
    put(Intent.FLAG_RECEIVER_FOREGROUND, "FLAG_RECEIVER_FOREGROUND")
    put(0x08000000, "FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT")
    put(0x04000000, "FLAG_RECEIVER_BOOT_UPGRADE")
}
