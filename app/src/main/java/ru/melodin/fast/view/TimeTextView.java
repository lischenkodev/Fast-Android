package ru.melodin.fast.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.Px;
import androidx.appcompat.widget.AppCompatTextView;

import ru.melodin.fast.R;


public class TimeTextView extends AppCompatTextView {

    private TextPaint timePaint = new TextPaint();
    private CharSequence timeText;
    private Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
    private float timeWidth;
    private float timePadding = dp(8);
    private float timeHeight;
    private int timeColor;
    private int timeSize;
    private Typeface typeface;

    public TimeTextView(Context context) {
        super(context);
    }

    public TimeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TimeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.TimeTextView);
        timeColor = typedArray.getColor(R.styleable.TimeTextView_timeColor, Color.WHITE);
        timeSize = (int) typedArray.getDimension(R.styleable.TimeTextView_timeSize, dp(9));
        timeText = typedArray.getString(R.styleable.TimeTextView_timeText);
        typedArray.recycle();
    }

    public void setTimeTextColor(@ColorInt int color) {
        timeColor = color;
        requestLayout();
        invalidate();
    }

    public CharSequence getTimeText() {
        return timeText;
    }

    public void setTimeText(CharSequence text) {
        this.timeText = text;
        requestLayout();
        invalidate();
    }

    public void setTimeTextSize(@Px int size) {
        timeSize = size;
        requestLayout();
        invalidate();
    }

    public void setTimeTypeface(Typeface typeface) {
        this.typeface = typeface;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.timePaint.setAntiAlias(true);
        this.timePaint.setColor(timeColor);
        this.timePaint.setTextSize(timeSize);
        this.timePaint.setTypeface(typeface);

        this.timeWidth = this.timePaint.measureText(this.timeText.toString());
        this.timeHeight = this.timePaint.getTextSize();

        float multiwidth = this.getLayout().getLineWidth(this.getLineCount() - 1) + this.timeWidth + this.timePadding;
        if (this.getLineCount() == 1) {
            if (this.getMeasuredWidth() + this.timeWidth + this.timePadding < this.getMaxWidth()) {
                setMeasuredDimension((int) (this.getMeasuredWidth() + this.timeWidth + this.timePadding), this.getMeasuredHeight());
            } else {
                setMeasuredDimension(this.getMeasuredWidth(), (int) (this.getMeasuredHeight() + this.timeHeight));
            }
        } else {
            if (multiwidth <= this.getMaxWidth()) {
                setMeasuredDimension(this.getMeasuredWidth(), this.getMeasuredHeight());
            } else {
                setMeasuredDimension(this.getMeasuredWidth(), (int) (this.getMeasuredHeight() + this.timeHeight));
            }
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!TextUtils.isEmpty(timeText)) {
            canvas.drawText(this.timeText.toString(), 0, this.timeText.length(), ((getMeasuredWidth() - getPaddingRight()) - this.timeWidth), (((getMeasuredHeight() - getPaddingBottom() - dp(1))) - this.metrics.descent), this.timePaint);
        }
    }

    public float dp(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }
}
