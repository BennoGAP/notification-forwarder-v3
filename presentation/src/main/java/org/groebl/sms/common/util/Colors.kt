/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.groebl.sms.common.util

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.core.content.res.getColorOrThrow
import org.groebl.sms.R
import org.groebl.sms.common.util.extensions.getColorCompat
import org.groebl.sms.model.Recipient
import org.groebl.sms.util.Preferences
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.math.pow

@Singleton
class Colors @Inject constructor(
    private val context: Context,
    private val prefs: Preferences
) {

    data class Theme(val theme: Int, private val colors: Colors) {
        val highlight by lazy { colors.highlightColorForTheme(theme) }
        val textPrimary by lazy { colors.textPrimaryOnThemeForColor(theme) }
        val textSecondary by lazy { colors.textSecondaryOnThemeForColor(theme) }
        val textTertiary by lazy { colors.textTertiaryOnThemeForColor(theme) }
    }

    val materialColors: List<List<Int>> = listOf(
        R.array.material_red,
        R.array.material_pink,
        R.array.material_purple,
        R.array.material_deep_purple,
        R.array.material_indigo,
        R.array.material_blue,
        R.array.material_light_blue,
        R.array.material_cyan,
        R.array.material_teal,
        R.array.material_green,
        R.array.material_light_green,
        R.array.material_lime,
        R.array.material_yellow,
        R.array.material_amber,
        R.array.material_orange,
        R.array.material_deep_orange,
        R.array.material_brown,
        R.array.material_gray,
        R.array.material_blue_gray)
            .map { res -> context.resources.obtainTypedArray(res) }
            .map { typedArray -> (0 until typedArray.length()).map(typedArray::getColorOrThrow) }

    val iosColors: List<List<Int>> = listOf(
            R.array.ios1_color,
            R.array.ios2_color,
            R.array.ios3_color,
            R.array.ios4_color)
            .map { res -> context.resources.obtainTypedArray(res) }
            .map { typedArray -> (0 until typedArray.length()).map(typedArray::getColorOrThrow) }

    val messagesColors: List<List<Int>> = listOf(
            R.array.Messages1_color,
            R.array.Messages2_color,
            R.array.Messages3_color,
            R.array.Messages4_color)
            .map { res -> context.resources.obtainTypedArray(res) }
            .map { typedArray -> (0 until typedArray.length()).map(typedArray::getColorOrThrow) }

    private val randomColors: List<Int> = context.resources.obtainTypedArray(R.array.random_colors)
            .let { typedArray -> (0 until typedArray.length()).map(typedArray::getColorOrThrow) }

    private val minimumContrastRatio = 2

    // Cache these values so they don't need to be recalculated
    private val primaryTextLuminance = measureLuminance(context.getColorCompat(R.color.textPrimaryDark))
    private val secondaryTextLuminance = measureLuminance(context.getColorCompat(R.color.textSecondaryDark))
    private val tertiaryTextLuminance = measureLuminance(context.getColorCompat(R.color.textTertiaryDark))

    fun theme(recipient: Recipient? = null): Theme {
        val pref = prefs.theme(recipient?.id ?: 0)
        val color = when {
            recipient == null || !prefs.autoColor.get() || pref.isSet -> pref.get()
            else -> generateColor(recipient)
        }
        return Theme(color, this)
    }

    fun themeObservable(recipient: Recipient? = null): Observable<Theme> {
        val pref = when {
            recipient == null -> prefs.theme()
            prefs.autoColor.get() -> prefs.theme(recipient.id, generateColor(recipient))
            else -> prefs.theme(recipient.id, prefs.theme().get())
        }
        return pref.asObservable()
                .map { color -> Theme(color, this) }
    }

    fun highlightColorForTheme(theme: Int): Int = FloatArray(3)
            .apply { Color.colorToHSV(theme, this) }
            .let { hsv -> hsv.apply { set(2, 0.75f) } } // 75% value
            .let { hsv -> Color.HSVToColor(85, hsv) } // 33% alpha

    fun textPrimaryOnThemeForColor(color: Int): Int = color
            .let { theme -> measureLuminance(theme) }
            .let { themeLuminance -> primaryTextLuminance / themeLuminance }
            .let { contrastRatio -> contrastRatio < minimumContrastRatio }
            .let { contrastRatio -> if (contrastRatio) R.color.textPrimary else R.color.textPrimaryDark }
            .let { res -> context.getColorCompat(res) }

    fun textSecondaryOnThemeForColor(color: Int): Int = color
            .let { theme -> measureLuminance(theme) }
            .let { themeLuminance -> secondaryTextLuminance / themeLuminance }
            .let { contrastRatio -> contrastRatio < minimumContrastRatio }
            .let { contrastRatio -> if (contrastRatio) R.color.textSecondary else R.color.textSecondaryDark }
            .let { res -> context.getColorCompat(res) }

    fun textTertiaryOnThemeForColor(color: Int): Int = color
            .let { theme -> measureLuminance(theme) }
            .let { themeLuminance -> tertiaryTextLuminance / themeLuminance }
            .let { contrastRatio -> contrastRatio < minimumContrastRatio }
            .let { contrastRatio -> if (contrastRatio) R.color.textTertiary else R.color.textTertiaryDark }
            .let { res -> context.getColorCompat(res) }

    /**
     * Measures the luminance value of a color to be able to measure the contrast ratio between two materialColors
     * Based on https://stackoverflow.com/a/9733420
     */
    private fun measureLuminance(color: Int): Double {
        val array = intArrayOf(Color.red(color), Color.green(color), Color.blue(color))
                .map { if (it < 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }

        return 0.2126 * array[0] + 0.7152 * array[1] + 0.0722 * array[2] + 0.05
    }

    private fun generateColor(recipient: Recipient): Int {
        val index = recipient.address.hashCode().absoluteValue % randomColors.size
        return randomColors[index]
    }

    @ColorRes
    fun colorForSim(context: Context, index: Int) =
        if (index == 1) {
            when (prefs.sim1Color.get()) {
                Preferences.SIM_COLOR_BLUE -> context.getColorCompat(R.color.sim1)
                Preferences.SIM_COLOR_GREEN -> context.getColorCompat(R.color.sim2)
                Preferences.SIM_COLOR_YELLOW -> context.getColorCompat(R.color.sim3)
                Preferences.SIM_COLOR_RED -> context.getColorCompat(R.color.sim4)
                Preferences.SIM_COLOR_PURPLE -> context.getColorCompat(R.color.sim5)
                Preferences.SIM_COLOR_MAGENTA -> context.getColorCompat(R.color.sim6)
                else -> context.getColorCompat(R.color.sim1)
            }
        } else if (index == 2) {
            when (prefs.sim2Color.get()) {
                Preferences.SIM_COLOR_BLUE -> context.getColorCompat(R.color.sim1)
                Preferences.SIM_COLOR_GREEN -> context.getColorCompat(R.color.sim2)
                Preferences.SIM_COLOR_YELLOW -> context.getColorCompat(R.color.sim3)
                Preferences.SIM_COLOR_RED -> context.getColorCompat(R.color.sim4)
                Preferences.SIM_COLOR_PURPLE -> context.getColorCompat(R.color.sim5)
                Preferences.SIM_COLOR_MAGENTA -> context.getColorCompat(R.color.sim6)
                else -> context.getColorCompat(R.color.sim2)
            }
        } else {
            when (prefs.sim3Color.get()) {
                Preferences.SIM_COLOR_BLUE -> context.getColorCompat(R.color.sim1)
                Preferences.SIM_COLOR_GREEN -> context.getColorCompat(R.color.sim2)
                Preferences.SIM_COLOR_YELLOW -> context.getColorCompat(R.color.sim3)
                Preferences.SIM_COLOR_RED -> context.getColorCompat(R.color.sim4)
                Preferences.SIM_COLOR_PURPLE -> context.getColorCompat(R.color.sim5)
                Preferences.SIM_COLOR_MAGENTA -> context.getColorCompat(R.color.sim6)
                else -> context.getColorCompat(R.color.sim3)
            }
        }
}
