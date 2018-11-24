package ru.stwtforever.fast.view;

import android.graphics.*;
import com.squareup.picasso.*;

/**
 * Created by Igor on 20.02.17.
 */

public class RoundTransform implements Transformation {
    private float factor;

    public RoundTransform(float factor) {
        this.factor = factor;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        if (factor == 0) {
            return source;
        }

        final int width = source.getWidth();
        final int height = source.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(0);

        int color = 0xff424242;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);

        Rect rect = new Rect(0, 0, width, height);
        RectF rectF = new RectF(rect);

        canvas.drawRoundRect(rectF, width * factor, height * factor, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);

        source.recycle();
        return output;
    }

    @Override
    public String key() {
        return "round_" + factor;
    }
}
