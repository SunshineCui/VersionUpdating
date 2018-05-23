package com.billy.versionupdating.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;


/**
 * 下载文件工具类   支持多线程  断点续传
 * Created by Billy_Cui on 2018/5/22
 * Describe:https://blog.csdn.net/tianzhaoai/article/details/56673071
 */
public class DownLoadUtils {

    private static final String SP_NAME = "download_file";
    private static final String CURR_LENGTH = "curr_length";
    private static final int DEFAULT_THREAD_COUNT = 3;//默认下载线程数
    //以下为线程状态
    private static final String DOWNLOAD_INIT = "1";
    private static final String DOWNLOAD_ING = "2";
    private static final String DOWNLOAD_PAUSE = "3";

    private Context mContext;

    private String loadUrl;//网络获取的url
    private String filePath;//下载到本地的path
    private int threadCount = DEFAULT_THREAD_COUNT;//下载线程数

    private int fileLength;//文件总大小
    //使用volatile防止多线程不安全
    private volatile int currLength;//当前总共下载的大小
    private volatile int runningThreadCount;//正在运行的线程数
    private DownThread[] mThreads;
    private String stateDownload = DOWNLOAD_INIT;//当前线程状态

    private DownLoadListener mDownLoadListener;
    private SharedPreferences mSp;

    private DownLoadUtils(Builder builder) {
        mContext = builder.mContext;
        loadUrl = builder.loadUrl;
        filePath = builder.filePath;
        mDownLoadListener = builder.mDownLoadListener;
        runningThreadCount = 0;
    }


    public interface DownLoadListener {
        //返回当前下载进度的百分比
        void getProgress(int progress);

        void onComplete();

        void onFailure(String message);
    }

    /**
     * 开始下载
     */
    public void download() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mThreads == null) {
                        mThreads = new DownThread[threadCount];
                    }
                    //建立连接请求

                    URL url = new URL(loadUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setRequestMethod("GET");
                    //获取返回码
                    int code = connection.getResponseCode();
                    if (code == 200) {
                        //请求成功,根据文件大小开始分多线程下载
                        fileLength = connection.getContentLength();
                        //“rwd“——打开以便读取和写入，对于 “rw”，还要求对文件内容的每个更新都同步写入到底层存储设备。
                        RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
                        raf.setLength(fileLength);
                        raf.close();
                        //计算各个线程下载的数据段
                        int blockLength = fileLength / threadCount;
                        mSp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                        //获取上次取消下载的进度,若没有则返回0
                        currLength = mSp.getInt(CURR_LENGTH, 0);
                        for (int i = 0; i < threadCount; i++) {
                            //开始位置,获取上次取消下载的进度,默认返回i*blockLength,即第i个线程开始下载的位置
                            int startPosition = mSp.getInt(SP_NAME + (i + 1), i * blockLength);
                            //结束位置,-1是为了防止上一个线程和下一个线程重复下载衔接处数据
                            int endPosition = (i + 1) * blockLength - 1;
                            //将最后一个线程结束位置扩大,防止文件下载不完全,大了不影响,小了文件失效
                            if ((i + 1) == threadCount) {
                                endPosition = endPosition * 2;
                            }
                            mThreads[i] = new DownThread(i + 1, startPosition, endPosition);
                            mThreads[i].start();
                        }
                    } else if (code == -1) {
                        Message message = new Message();
                        message.what = FAILURE;
                        message.obj = "网络错误!";
                        handler.sendMessage(message);
//                        handler.sendEmptyMessage(FAILURE);
                    }
                } catch (Exception e) {
                    Message message = new Message();
                    message.what = FAILURE;
                    if (e instanceof UnknownHostException) {
                        message.obj = "无网络连接!";
                    } else if (e instanceof SocketTimeoutException) {
                        message.obj = "网络连接超时!";
                    } else {
                        message.obj = "网络错误!" + e.toString();
                    }
                    handler.sendMessage(message);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void onCancel() {
        if (mThreads != null) {
            //若线程处于等待状态，则while循环处于阻塞状态，无法跳出循环，必须先唤醒线程，才能执行取消任务
            if (stateDownload.equals(DOWNLOAD_PAUSE)) {
                onStart();
            }
            for (DownThread dt : mThreads) {
                dt.cannel();
            }
            mContext = null;
        }
    }

    /**
     * 暂停下载
     */
    public void onPause() {
        if (mThreads != null)
            stateDownload = DOWNLOAD_PAUSE;
    }

    /**
     * 继续下载
     */
    public void onStart() {
        if (mThreads != null)
            synchronized (DOWNLOAD_PAUSE) {
                stateDownload = DOWNLOAD_ING;
                DOWNLOAD_PAUSE.notifyAll();
            }
    }

    protected void onDestroy() {
        if (mThreads != null)
            mThreads = null;
    }


    private static final int SUCCESS = 0x00000101;
    private static final int FAILURE = 0x00000102;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mDownLoadListener != null) {
                if (msg.what == FAILURE) {
                    mDownLoadListener.onFailure((String) msg.obj);
                } else if (msg.what == SUCCESS) {
                    mDownLoadListener.onComplete();
                } else {
                    mDownLoadListener.getProgress(msg.what);
                }
            }
        }
    };

    //构造器
    public static final class Builder {
        private Context mContext;
        private String loadUrl;
        private String filePath;
        private DownLoadListener mDownLoadListener;

        public Builder() {
        }

        public Builder mContext(Context val) {
            mContext = val;
            return this;
        }

        public Builder loadUrl(String val) {
            loadUrl = val;
            return this;
        }

        public Builder filePath(String val) {
            filePath = val;
            return this;
        }

        public Builder mDownLoadListener(DownLoadListener val) {
            mDownLoadListener = val;
            return this;
        }

        public DownLoadUtils build() {
            return new DownLoadUtils(this);
        }
    }


    private class DownThread extends Thread {

        private boolean isGoOn = true;//是否继续下载
        private int threadId;
        private int startPosition;//开始下载点
        private int endPosition;//结束下载点
        private int currPosition;//当前线程的下载进度

        public DownThread(int threadId, int startPosition, int endPosition) {
            this.threadId = threadId;
            this.startPosition = startPosition;
            currPosition = startPosition;
            this.endPosition = endPosition;
            runningThreadCount++;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(loadUrl);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Range", "bytes=" + startPosition + "-" + endPosition);
                connection.setConnectTimeout(5000);
                //若请求头加上Range这个参数,则返回状态码为206,而不是200
                int responseCode = connection.getResponseCode();
                if (responseCode == 206) {
                    InputStream is = connection.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
                    //跳到指定位置开始写数据
                    raf.seek(startPosition);
                    int len;
                    byte[] buffer = new byte[1024 * 100];
                    while ((len = bis.read(buffer)) != -1) {
                        if (!isGoOn) {
                            break;
                        }
                        //流写入文件
                        raf.write(buffer, 0, len);
                        //回调当前进度
                        if (mDownLoadListener != null) {
                            currLength += len;
                            int progress = (int) ((float) currLength / (float) fileLength * 100);
                            handler.sendEmptyMessage(progress);
                        }
                        //写完后将当前指针后移,为取消下载时保存当前进度做准备
                        currPosition += len;
                        synchronized (DOWNLOAD_PAUSE) {
                            if (stateDownload.equals(DOWNLOAD_PAUSE)) {
                                DOWNLOAD_PAUSE.wait();
                            }
                        }
                    }
                    is.close();
                    bis.close();
                    raf.close();
                    //线程计数器 -1
                    runningThreadCount--;
                    //若取消下载,则直接返回
                    if (!isGoOn) {
                        //此处采用sharedPreferences保存每个线程的当前进度,和三个线程的总下载进度
                        if (currPosition < endPosition) {
                            mSp.edit().putInt(SP_NAME + threadId, currPosition).apply();
                            mSp.edit().putInt(CURR_LENGTH, currLength).apply();
                        }
                        return;
                    }
                    if (runningThreadCount == 0) {
                        handler.sendEmptyMessage(100);
                        handler.sendEmptyMessage(SUCCESS);
                        clear();
                    }
                } else {
                    //请求失败
                    clear();
                    Message message = new Message();
                    message.what = FAILURE;
                    message.obj = "子线程 请求失败";
                    handler.sendMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Message message = new Message();
                message.what = FAILURE;
                //SocketException;SocketTimeoutException 超时; ProtocolException 流意外结束
                message.obj = threadId+ "子线程 网络问题!";
                mSp.edit().putInt(SP_NAME + threadId, currPosition).apply();
                mSp.edit().putInt(CURR_LENGTH, currLength).apply();
                runningThreadCount = 0;
                //关闭所有子线程
                for (DownThread dt : mThreads) {
                    dt.cannel();
                }
                handler.sendMessage(message);
            }
        }
        //关闭方法
        public void cannel() {
            isGoOn = false;
        }
    }

    //清除此次下载数据
    private void clear() {
        mSp.edit().clear().apply();
        mThreads = null;
        currLength = 0;
    }

}
