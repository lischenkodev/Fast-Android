package ru.melodin.fast.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import ru.melodin.fast.R

class BoundedLinearLayout : LinearLayout {

    private var mBoundedWidth: Int = 0
    private var mBoundedHeight: Int = 0

    var maxWidth: Int
        get() = mBoundedWidth
        set(width) {
            if (mBoundedWidth != width) {
                mBoundedWidth = width
                requestLayout()
            }
        }

    var maxHeight: Int
        get() = mBoundedHeight
        set(height) {
            if (mBoundedHeight != height) {
                mBoundedHeight = height
                requestLayout()
            }
        }

    constructor(context: Context) : super(context) {
        mBoundedWidth = 0
        mBoundedHeight = 0
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BoundedLinearLayout)
        mBoundedWidth = a.getDimensionPixelSize(R.styleable.BoundedLinearLayout_bounded_width, 0)
        mBoundedHeight = a.getDimensionPixelSize(R.styleable.BoundedLinearLayout_bounded_height, 0)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var measureSpec = widthMeasureSpec
        var heightMeasureSpec1 = heightMeasureSpec
        val measuredWidth = MeasureSpec.getSize(measureSpec)
        if (mBoundedWidth in 1 until measuredWidth) {
            val measureMode = MeasureSpec.getMode(measureSpec)
            measureSpec = MeasureSpec.makeMeasureSpec(mBoundedWidth, measureMode)
        }

        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec1)
        if (mBoundedHeight in 1 until measuredHeight) {
            val measureMode = MeasureSpec.getMode(heightMeasureSpec1)
            heightMeasureSpec1 = MeasureSpec.makeMeasureSpec(mBoundedHeight, measureMode)
        }
        super.onMeasure(measureSpec, heightMeasureSpec1)
    }
}
