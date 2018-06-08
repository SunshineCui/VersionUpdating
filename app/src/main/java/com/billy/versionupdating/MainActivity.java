package com.billy.versionupdating;

import android.app.ProgressDialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.billy.versionupdating.dialog.CommonProgressDialog;
import com.billy.versionupdating.utils.AppUtil;
import com.billy.versionupdating.utils.DialogUtils;
import com.billy.versionupdating.utils.DownLoadUtils;
import com.billy.versionupdating.utils.SDCardUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity implements DownLoadUtils.DownLoadListener ,View.OnClickListener{

    private static final String TAG = "MainActivity";


    private String filePath = Environment.getExternalStorageDirectory() + "/" + "111.apk";

    public TextView mTextView;
    private DownLoadUtils mLoadUtils;
    private String mLoadUrl;
    private CommonProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.textView);

        //应该网络获取
        int newVersionCode = 26;
        int versionCode = AppUtil.getVersionCode(this);
//        String loadUrl = "http://openbox.mobilem.360.cn/index/d/sid/3429345";//安装包下载地址 11.5M
//        String loadUrl = "http://imtt.dd.qq.com/16891/47D8294B06AD9F31AC31BBC5574A1897.apk?fsname=com.syezon.wifi_3.6.5_248.apk&csr=1bbd";//6.6M
        //22M
        mLoadUrl = "http://imtt.dd.qq.com/16891/7AB0BD682763263FE543D4CCE6A66433.apk?fsname=com.qq.reader_6.6.2.689_110.apk&csr=1bbd";
        Log.d(TAG, "onCreate ");
        if (newVersionCode > versionCode && SDCardUtils.isSDCardExist()) {
            //需要更新
            mLoadUtils = new DownLoadUtils.Builder().mContext(this).loadUrl(mLoadUrl).filePath(filePath).mDownLoadListener(this).build();
        }
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
                mProgressDialog = progressDialog();
                mLoadUtils.download();
                mProgressDialog.show();
            }
        });
    }


    public CommonProgressDialog progressDialog() {
        CommonProgressDialog cpd = new CommonProgressDialog(this);
        cpd.setCancelable(false);
        cpd.setCanceledOnTouchOutside(false);
        cpd.setTitle("正在下载");
        cpd.setCustomTitle(LayoutInflater.from(this).inflate(R.layout.title_dialog, null));
        cpd.setMessage("正在下载");
        cpd.setIndeterminate(true);
        cpd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        return cpd;
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
