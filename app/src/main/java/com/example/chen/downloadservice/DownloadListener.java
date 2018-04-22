package com.example.chen.downloadservice;

/**
 * Created by chen on 2018/4/13.
 */

public interface DownloadListener {
    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCanceled();
}
