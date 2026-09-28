package io.cloudx.demo.demoapp.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.cloudx.demo.demoapp.R

/**
 * Screens listed under the More tab, in display order.
 */
enum class MoreDestination(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    APP_OPEN(R.string.app_open, R.drawable.baseline_open_in_new_24),
    NATIVE(R.string.native_ad, R.drawable.baseline_article_24),
    MEDIATION_DEBUGGER(R.string.menu_mediation_debugger, R.drawable.baseline_bug_report_24),
}
