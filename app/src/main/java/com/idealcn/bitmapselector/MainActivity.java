package com.idealcn.bitmapselector;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView mCountView;
    private TextView mDirView;
    private RelativeLayout mBottomContainer;
    private GridView mGridView;

    private List<String> imgs;
    private File mCurrentDir;
    private int mMaxCount;
    private List<FolderBean> folderBeanList = new ArrayList<>();

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCountView = (TextView) findViewById(R.id.count);
        mDirView = (TextView) findViewById(R.id.dir);
        mBottomContainer = (RelativeLayout) findViewById(R.id.bottom);
        mGridView = (GridView) findViewById(R.id.gridView);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "外部存储卡不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog = ProgressDialog.show(this, "正在加载中...", null);

        new Thread() {
            @Override
            public void run() {
                super.run();
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver resolver = MainActivity.this.getContentResolver();
                Cursor cursor = resolver.query(uri, null, MediaStore.Images.Media.MIME_TYPE + "=? or "
                                + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) continue;

                }

            }
        }.start();
    }
}
