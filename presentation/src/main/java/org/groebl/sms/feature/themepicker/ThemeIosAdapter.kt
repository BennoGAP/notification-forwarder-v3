    package org.groebl.sms.feature.themepicker

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import org.groebl.sms.R
import org.groebl.sms.common.base.QkAdapter
import org.groebl.sms.common.base.QkViewHolder
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.dpToPx
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setTint
import org.groebl.sms.common.util.extensions.setVisible
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.theme_list_item.view.*
import kotlinx.android.synthetic.main.theme_palette_list_item.*
import kotlinx.android.synthetic.main.theme_palette_list_item.view.*
import javax.inject.Inject

class ThemeIosAdapter @Inject constructor(
    private val context: Context,
    private val colors: Colors
) : QkAdapter<List<Int>, QkViewHolder>() {

    val colorSelected: Subject<Int> = PublishSubject.create()

    var selectedColor: Int = -1
        set(value) {
            val oldPosition = data.indexOfFirst { it.contains(field) }
            val newPosition = data.indexOfFirst { it.contains(value) }

            field = value
            iconTint = colors.textPrimaryOnThemeForColor(value)

            oldPosition.takeIf { it != -1 }?.let { position -> notifyItemChanged(position) }
            newPosition.takeIf { it != -1 }?.let { position -> notifyItemChanged(position) }
        }

    private var iconTint = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.theme_palette_list_item, parent, false)
        view.palette.flexWrap = FlexWrap.WRAP
        view.palette.flexDirection = FlexDirection.ROW

        return QkViewHolder(view)
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val palette = getItem(position)

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val minPadding = (16 * 6).dpToPx(context)
        val size = if (screenWidth - minPadding > (56 * 5).dpToPx(context)) {
            56.dpToPx(context)
        } else {
            (screenWidth - minPadding) / 5
        }
        val swatchPadding = (screenWidth - size * 6) / 14 //столбцы

        holder.palette.removeAllViews()
        holder.palette.setPadding(swatchPadding, swatchPadding, swatchPadding, swatchPadding)

        (palette.subList(0, 6)) // ккакие цвета брать в строки, с 1 по 5
                .mapIndexed { index, color ->
                    LayoutInflater.from(context).inflate(R.layout.theme_list_item, holder.palette, false).apply {

                        // Send clicks to the selected subject
                        setOnClickListener { colorSelected.onNext(color) }

                        // Apply the color to the view
                        theme.setBackgroundTint(color)

                        // Control the check visibility and tint
                        check.setVisible(color == selectedColor)
                        check.setTint(iconTint)

                        // Update the size so that the spacing is perfectly even
                        layoutParams = (layoutParams as FlexboxLayout.LayoutParams).apply {
                            height = size
                            width = size
                            isWrapBefore = index % 6 == 0 //столбцы
                            setMargins(swatchPadding, swatchPadding, swatchPadding, swatchPadding)
                        }
                    }
                }
                .forEach { theme -> holder.palette.addView(theme) }
    }

}