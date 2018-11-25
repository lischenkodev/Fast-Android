package ru.stwtforever.fast;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ru.stwtforever.fast.helper.FontHelper;
import ru.stwtforever.fast.helper.PermissionHelper;
import ru.stwtforever.fast.util.Requests;

public class UpdateActivity extends AppCompatActivity {

    private boolean important;
    private int build;
    private String date, version, apk, changelog, name;

    private Button b_download;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getIntentData();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_screen);

        Toolbar tb = findViewById(R.id.toolbar);
        TextView tb_title = tb.findViewById(R.id.toolbar_title);

        TextView t_changelog = findViewById(R.id.changelog);
        TextView t_version = findViewById(R.id.version);

        b_download = findViewById(R.id.download);
        Button b_cancel = findViewById(R.id.cancel);

        if (important) {
            b_cancel.setVisibility(View.GONE);
        }

        FontHelper.setFont(tb_title, FontHelper.PS_REGULAR);
        FontHelper.setFont(new Button[]{b_download, b_cancel}, FontHelper.PS_REGULAR);

        b_download.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!PermissionHelper.isGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    PermissionHelper.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Requests.REQUEST_WRITE_PERMISSION);
                } else {
                    Toast.makeText(UpdateActivity.this, getString(R.string.update_downloading), Toast.LENGTH_LONG).show();
                    b_download.setEnabled(false);
                    downloadFile();
                }
            }

        });

        b_cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String s = String.format(getString(R.string.new_update_message), date, version, build);

        t_version.setText(s);
        if (!TextUtils.isEmpty(changelog))
            t_changelog.setText(Html.fromHtml(changelog));

        Window w = getWindow();

        int light_sb = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        int light_nb = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            w.getDecorView().setSystemUiVisibility(0);
            w.setStatusBarColor(0xffcccccc);
            w.setNavigationBarColor(0xffcccccc);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                w.setStatusBarColor(Color.WHITE);
                w.setNavigationBarColor(Color.WHITE);
                w.getDecorView().setSystemUiVisibility(light_sb | light_nb);
            } else {
                w.setStatusBarColor(Color.WHITE);
                w.setNavigationBarColor(0xffcccccc);
                w.getDecorView().setSystemUiVisibility(light_sb);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!important)
            super.onBackPressed();
    }

    private void getIntentData() {
        Bundle b = getIntent().getExtras();
        version = b.getString("version");
        name = b.getString("name");
        build = b.getInt("build");
        apk = b.getString("apk");
        changelog = b.getString("changelog");
        important = b.getBoolean("important");
        date = b.getString("date");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Requests.REQUEST_WRITE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(UpdateActivity.this, getString(R.string.update_downloading), Toast.LENGTH_LONG).show();
                b_download.setEnabled(false);
                downloadFile();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void downloadFile() {
        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/fvk/";

        String fileName = name + ".apk";
        destination += fileName;
        final String des = destination;
        final Uri uri = Uri.parse("file://" + destination);

        String url = apk;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(getString(R.string.loading));
        request.setTitle(getString(R.string.app_name) + " " + version);

        request.setDestinationUri(uri);

        final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    install.setDataAndType(uri,
                            manager.getMimeTypeForDownloadedFile(downloadId));
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(install);
                }

                Toast.makeText(UpdateActivity.this, String.format(getString(R.string.file_saved), des), Toast.LENGTH_LONG).show();

                unregisterReceiver(this);

                b_download.setEnabled(true);

                if (important) finish();
            }
        };

        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
}
