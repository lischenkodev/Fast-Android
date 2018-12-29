package ru.stwtforever.fast.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.squareup.picasso.Transformation;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.io.BytesOutputStream;

public class Utils {

    public static SimpleDateFormat dateFormatter;
    public static SimpleDateFormat dateMonthFormatter;
    public static SimpleDateFormat dateYearFormatter;
    public static SimpleDateFormat dateFullFormatter;

    static {
        dateFormatter = new SimpleDateFormat("HH:mm"); // 15:57
        dateMonthFormatter = new SimpleDateFormat("d MMM"); // 23 Окт
        dateYearFormatter = new SimpleDateFormat("d MMM, yyyy"); // 23 Окт, 2015
        dateFullFormatter = new SimpleDateFormat("dd.MM.yyyy, HH:mm");
    }

    public static void clearPreferences() {
        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("pm clear " + "com.procsec.fast");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showNoInternetToast() {
        Toast.makeText(AppGlobal.context, AppGlobal.context.getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();
    }

    public static void checkConnection() {
        if (!hasConnection()) showNoInternetToast();
    }

    public static void logD(String tag, String message) {
        Log.d(tag, message);
    }

    public static void copyText(String text) {
        ClipboardManager cm = (ClipboardManager) AppGlobal.context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(null, text));
    }

    public static float convertDpToPixel(float dp) {
        Resources resources = AppGlobal.context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    public static String parseSize(long sizeInBytes) {
        long unit = 1024;
        if (sizeInBytes < unit) return sizeInBytes + " B";
        int exp = (int) (Math.log(sizeInBytes) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp - 1) + ("i");
        return String.format(Locale.US, "%.1f %sB", sizeInBytes / Math.pow(unit, exp), pre);
    }

    public static String parseDate(long date) {
        Date currentDate = new Date();
        Date msgDate = new Date(date);

        if (currentDate.getYear() > msgDate.getYear()) {
            return dateYearFormatter.format(date);
        } else if (currentDate.getMonth() > msgDate.getMonth()
                || currentDate.getDate() > msgDate.getDate()) {
            return dateMonthFormatter.format(date);
        }

        return dateFormatter.format(date);
    }

    public static long getPeerId(int userId, int chatId, int groupId) {
        return groupId > 0 ? (-groupId)
                : chatId > 0 ? (2_000_000_000 + chatId)
                : userId;
    }

    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        display.getRealSize(size);
        return size;
    }

    public static byte[] serialize(Object source) {
        try {
            BytesOutputStream bos = new BytesOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);

            out.writeObject(source);
            out.close();
            return bos.getByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object deserialize(byte[] source) {
        if (ArrayUtil.isEmpty(source)) {
            return null;
        }

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(source);
            ObjectInputStream in = new ObjectInputStream(bis);

            Object o = in.readObject();

            in.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap loadBitmap(String url) {
        Bitmap bm = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.connect();
            is = conn.getInputStream();
            bis = new BufferedInputStream(is, 8192);
            bm = BitmapFactory.decodeStream(bis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bm;
    }

    public static int pxFromDp(int dp) {
        Resources resources = AppGlobal.context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    public static float convertPixelsToDp(float px) {
        Resources resources = AppGlobal.context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    public static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(AppGlobal.context);
    }

    public static int getThemeAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = AppGlobal.context.getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        @ColorInt int color = typedValue.data;

        return color;
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) AppGlobal.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected());
    }

    public static int getThemeAttrColor(int attr, float alpha) {
        final int color = getThemeAttrColor(attr);
        final int originalAlpha = Color.alpha(color);
        return ColorUtils.setAlphaComponent(color, Math.round(originalAlpha * alpha));
    }

    public static void showAlert(Context c, String title, String message) {
        AlertDialog.Builder adb = new AlertDialog.Builder(c);
        adb.setTitle(title);
        adb.setMessage(message);
        AlertDialog alert = adb.create();
        alert.show();
    }

    public static class RoundedTransformation implements Transformation {
        int pixels;

        public RoundedTransformation(int pixels) {
            this.pixels = pixels;
        }

        @Override
        public Bitmap transform(Bitmap source) {
            Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, source.getWidth(), source.getHeight());
            final RectF rectF = new RectF(rect);
            final float roundPx = pixels;

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(source, rect, rect, paint);

            if (source != output) {
                source.recycle();
            }

            return output;
        }

        @Override
        public String key() {
            return "round";
        }
    }

    public static void saveFileByUrl(String link) throws Exception {
        URL url = new URL(link);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoOutput(false);
        urlConnection.connect();

        File directory = new File(Environment.getExternalStorageDirectory() + "/Download");

        if (!directory.exists()) directory.mkdir();

        String name = link.substring(link.lastIndexOf("/") + 1, link.length());

        File file = new File(directory, name);

        int i = 1;
        while (file.exists()) {
            String fileName = link.substring(link.lastIndexOf("/") + 1);

            int dotIndex = fileName.lastIndexOf(".");

            String name_ = fileName.substring(0, dotIndex);
            String ext = fileName.substring(dotIndex);
            name_ += "-" + i;

            fileName = name_ + ext;

            file = new File(directory, fileName);
            i++;
        }

        FileOutputStream fileOutput = new FileOutputStream(file);
        InputStream inputStream = urlConnection.getInputStream();

        byte[] buffer = new byte[1024];
        int bufferLength;

        while ((bufferLength = inputStream.read(buffer)) > 0) {
            fileOutput.write(buffer, 0, bufferLength);
        }
        fileOutput.close();
    }
}