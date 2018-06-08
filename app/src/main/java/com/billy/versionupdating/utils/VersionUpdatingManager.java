package com.billy.versionupdating.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;

import com.billy.versionupdating.BuildConfig;
import com.billy.versionupdating.dialog.CommonProgressDialog;

import java.io.File;

/**
 * Created by Billy_Cui on 2018/6/8
 * Describe:  版本更新 管理器
 */

public class VersionUpdatingManager implements DownLoadUtils.DownLoadListener {

    private String mLoadUrl = "http://imtt.dd.qq.com/16891/7AB0BD682763263FE543D4CCE6A66433.apk?fsname=com.qq.reader_6.6.2.689_110.apk&csr=1bbd";

    public Context mContext;
    private String filePath;
    private DownLoadUtils mLoadUtils;
    private CommonProgressDialog mProgressDialog;
    private String updataContent;

    private VersionUpdatingManager(Builder builder) {
        mContext = builder.mContext;
        if (builder.filePath != null) {
            filePath = builder.filePath;
        } else {
            filePath = mContext.getExternalCacheDir().getAbsolutePath() + "/" + "111.apk";
        }
        if (builder.updataContent != null){
            updataContent = builder.updataContent;
        }else {
            updataContent = "在MainActivity中调用这个类的网络操作方法，可能会导致activity的一些问题，谷歌从在android2.3版本以后，系统增加了一个类：StrictMode。这个类对网络的访问方式进行了一定的改变。 ";
        }
        if (builder.mLoadUrl != null) {
            mLoadUrl = builder.mLoadUrl;
        }
    }

    public void init() {

        DialogUtils.showDialog(mContext, "版本更新", updataContent, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                start();
            }
        });

    }

    public void start() {
        mLoadUtils = new DownLoadUtils.Builder().mContext(mContext).loadUrl(mLoadUrl).filePath(filePath).mDownLoadListener(this).build();
        mProgressDialog = CommonProgressDialog.getInstance(mContext);
        mLoadUtils.download();
        mProgressDialog.show();
    }

    @Override
    public void getProgress(int progress) {
        mProgressDialog.setProgress(progress);
    }

    @Override
    public void onComplete() {
        mProgressDialog.dismiss();
        installApp();
    }

    @Override
    public void onFailure(String message) {
        DialogUtils.showDialog(mContext, "继续更新", "请确认网络可用,需要重联网络继续下载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mLoadUtils.download();
            }
        });
    }

    private void installApp() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(filePath);
        //还没在AndroidManifest.xml 设置FileProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri contentUri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".myprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        mContext.startActivity(intent);
    }


    public void destroy() {

        if (mLoadUtils != null) {
            mLoadUtils.onCancel();
        }
        mContext = null;

    }


    public static final class Builder {
        private String filePath;
        private Context mContext;
        private String mLoadUrl;
        private String updataContent;


        public Builder() {
        }

        public Builder filePath(String val) {
            filePath = val;
            return this;
        }

        public Builder mContext(Context val) {
            mContext = val;
            return this;
        }

        public Builder updataContent(String val) {
            updataContent = val;
            return this;
        }


        public Builder mLoadUrl(String val) {
            mLoadUrl = val;
            return this;
        }


        public VersionUpdatingManager build() {
            return new VersionUpdatingManager(this);
        }
    }
}
