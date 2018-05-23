package com.billy.versionupdating.utils;

import android.content.Context;
import android.content.DialogInterface;

/**
 * Created by Billy_Cui on 2018/5/22
 * Describe:
 */
public  class DialogUtils {

    public static void  showDialog(Context context ,String title ,String content,DialogInterface.OnClickListener listener){
        new android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("更新", listener)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }


//    public static void progressDialog(Context context,String newVewsion,String url ,String content){
//        CommonProgressDialog cpd = new CommonProgressDialog(context);
//        cpd.setCancelable(false);
//        cpd.setTitle("正在下载");
//        cpd.setCustomTitle(LayoutInflater.from(context).inflate(R.layout.title_dialog,null));
//        cpd.setMessage("正在下载");
//        cpd.setCancelable(true);
//        cpd.setIndeterminate(true);
//        cpd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//    }
}
