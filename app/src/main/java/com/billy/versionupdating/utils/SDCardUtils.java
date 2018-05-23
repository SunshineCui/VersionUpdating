package com.billy.versionupdating.utils;

import android.os.Environment;


public class SDCardUtils {

    public static boolean isSDCardExist() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

}

