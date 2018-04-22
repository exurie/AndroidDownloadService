package com.example.chen.downloadservice;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by chen on 2018/4/13.
 */

public class DownLoadTask extends AsyncTask<String,Integer,Integer> {
    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCELED=3;

    private DownloadListener listener;
    private boolean isCanceled=false;
    private boolean isPaused=false;
    private int lastProgress=0;
    private static final String TAG = "DownLoadTask";
    public DownLoadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        String downloadUrl=strings[0];
        long downloadLength=0;

        RandomAccessFile saveFile=null;
        String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/")+1);
        String savedPath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

        File file=new File(savedPath+"/"+fileName);//存储在本地的文件
        InputStream is=null;
        if(file.exists()){
            downloadLength=file.length();
        }
        try {
            //文件的总大小
            long contentLength=getContentLength(downloadUrl);
            if(contentLength==0){
                return TYPE_FAILED;
            }else if(downloadLength==contentLength){//如果相等说明下载完成了
                return TYPE_SUCCESS;
            }

            OkHttpClient client=new OkHttpClient();
            Request request=new Request.Builder()
                    .addHeader("RANGE","bytes="+downloadLength+"-")
                    .url(downloadUrl)
                    .build();
            Response response=client.newCall(request).execute();
            is=response.body().byteStream();
            saveFile=new RandomAccessFile(file,"rw");
            saveFile.seek(downloadLength);
            byte[] bytes=new byte[1024];
            int len;//每次读取的长度
            int total=0;//目前下载的总长度
            while ((len=is.read(bytes))!=-1){
                if(isCanceled==true)//取消下载
                    return TYPE_CANCELED;
                else if(isPaused==true)//暂停下载
                    return TYPE_PAUSED;
                else
                    total+=len;
                saveFile.write(bytes,0,len);
                //计算当前下载百分比
                int progress= (int) ((downloadLength+total)*100/contentLength);
                //Log.d(TAG, "doInBackground: progress"+progress);
                publishProgress(progress);
                //Log.d(TAG, "doInBackground: 开始更新下载进度");
            }
            response.close();
            //返回成功下载
            return TYPE_SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(is!=null)
                    is.close();
                if(saveFile!=null)
                    saveFile.close();
                if(isCanceled==true){
                    file.delete();
                    Log.d(TAG, "doInBackground: 删除文件");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response=client.newCall(request).execute();
        long contentLength=0;
        if(response!=null && response.isSuccessful()){
            contentLength=response.body().contentLength();
            response.close();
        }
        return contentLength;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if(values[0]>lastProgress){
            lastProgress=values[0];
            listener.onProgress(values[0]);
        }
    }

    public void pauseDownload(){
        this.isPaused=true;
    }

    public void cancelDownload(){
        this.isCanceled=true;
    }
}
