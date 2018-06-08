package com.billy.versionupdating;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.billy.versionupdating.dialog.CommonProgressDialog;
import com.billy.versionupdating.utils.DialogUtils;
import com.billy.versionupdating.utils.DownLoadUtils;
import com.billy.versionupdating.utils.VersionUpdatingManager;

import java.io.File;

public class MainActivity extends AppCompatActivity implements DownLoadUtils.DownLoadListener ,View.OnClickListener{

    private static final String TAG = "MainActivity";


    private String filePath = Environment.getExternalStorageDirectory() + "/" + "111.apk";

    public TextView mTextView;
    private DownLoadUtils mLoadUtils;
    private String mLoadUrl;
    private CommonProgressDialog mProgressDialog;
    private VersionUpdatingManager versionUpdatingManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.textView);
        versionUpdatingManager = new VersionUpdatingManager.Builder().mContext(this).build();
        super.onCreate(savedInstanceState);
        init();
    }

    private void installApp() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(filePath);
        //还没在AndroidManifest.xml 设置FileProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri contentUri = FileProvider.getUriForFile(this, "com.kangxin.doctor.FileProvider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        this.startActivity(intent);
    }

    private void init() {
        String content = "在MainActivity中调用这个类的网络操作方法，可能会导致activity的一些问题，谷歌从在android2.3版本以后，系统增加了一个类：StrictMode。这个类对网络的访问方式进行了一定的改变。 ";
        DialogUtils.showDialog(this,"版本更新", content, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                versionUpdatingManager.init();
            }
        });
    }


    public CommonProgressDialog progressDialog() {
        return CommonProgressDialog.getInstance(this);
    }

    @Override
    public void getProgress(int progress) {
        Log.d(TAG, "progress :" + progress);
        mTextView.setText("progress : " + progress);
        mProgressDialog.setProgress(progress);
    }

    @Override
    public void onComplete() {
        Log.d(TAG, "onComplete !");
        mProgressDialog.dismiss();
        installApp();
    }

    @Override
    public void onFailure(String message) {
        Log.d(TAG, "onFailure !" + message);
        DialogUtils.showDialog(this, "继续更新", "请确认网络可用,需要重联网络继续下载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mProgressDialog.dismiss();
                mProgressDialog = progressDialog();
                mLoadUtils.download();
                mProgressDialog.show();
            }
        });
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.b1) {
            //开始
            mLoadUtils.download();
            Log.d(TAG, "download !");
        } else if (v.getId() == R.id.b2) {
            //暂停
            mLoadUtils.onPause();
            Log.d(TAG, "onPause !");
        } else if (v.getId() == R.id.b3) {
            //重新开始
            mLoadUtils.onStart();
            Log.d(TAG, "onStart !");
        }
    }

    @Override
    protected void onDestroy() {
        mLoadUtils.onCancel();
        mLoadUtils = null;
        super.onDestroy();
    }
}
