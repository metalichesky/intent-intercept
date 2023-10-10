package com.metalichesky.intentintercept.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import androidx.annotation.ColorInt

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat

class UnderlinedTextView : AppCompatTextView {

    private val underlinePaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply {
            color = ContextCompat.getColor(context, DEFAULT_UNDERLINE_COLOR_ID)
        }
    }
    private var underlineHeight = DEFAULT_UNDERLINE_HEIGHT

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, underlineHeight + bottom)
    }

    fun setUnderlineHeight(underlineHeight: Int) {
        val underlineHeightFixed = underlineHeight.coerceAtLeast(0)
        if (underlineHeightFixed != this.underlineHeight) {
            this.underlineHeight = underlineHeightFixed
            setPadding(paddingLeft, paddingTop, paddingRight, this.underlineHeight + paddingBottom)
        }
    }

    fun setUnderlineColor(@ColorInt underlineColor: Int) {
        if (underlinePaint.color != underlineColor) {
            underlinePaint.color = underlineColor
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(
            0f,
            height.toFloat() - underlineHeight,
            width.toFloat(),
            height.toFloat(),
            underlinePaint
        )
    }

    companion object {
        const val DEFAULT_UNDERLINE_HEIGHT = 2
        const val DEFAULT_UNDERLINE_COLOR_ID = android.R.color.holo_blue_light
    }
}
