package ru.stwtforever.fast.view;

import android.graphics.*;
import com.squareup.picasso.*;
import ru.stwtforever.fast.util.*;


public class BlurTransform implements Transformation {
    private int radius;
    private boolean fastMethod;

    public BlurTransform(int radius, boolean fastMethod) {
        this.radius = radius;
        this.fastMethod = fastMethod;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        if (radius <= 0) {
            return source;
        }
        Bitmap copy = source.copy(Bitmap.Config.ARGB_8888, true);
        if (copy != source) {
            source.recycle();
        }

        if (fastMethod) {
            ImageUtil.nativeStackBlur(copy, radius);
        } else {
            ImageUtil.stackBlur(copy, radius);
        }
        return copy;
    }

    @Override
    public String key() {
        return "stack_blur_" + radius;
    }


}
