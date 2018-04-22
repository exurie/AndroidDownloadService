package com.example.chen.downloadservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    private String downLoadURL=null;
    private DownLoadTask task=null;
    private Binder mBinder=new DownloadBinder();
    private DownloadListener listener=new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification(progress,"DownLoading..."));
            Log.d(TAG, "onProgress:更新下载进度progerss"+progress);
        }

        @Override
        public void onSuccess() {
            task=null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification(-1,"download finish"));
            Toast.makeText(DownloadService.this,"download finish",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            task=null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification(-1,"download failed"));
            Toast.makeText(DownloadService.this,"download failed",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            task=null;
            getNotificationManager().notify(1,getNotification(-1,"download paused"));
            Toast.makeText(DownloadService.this,"download paused",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            task=null;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification(-1,"download canceled"));
            Toast.makeText(DownloadService.this,"download canceled",Toast.LENGTH_SHORT).show();
        }
    };
    private static final String TAG = "DownloadService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: 创建服务");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: start service");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: 返回Binder");
        return mBinder;
    }

    private NotificationManager getNotificationManager(){
        return (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(int progress, String title){
        Intent intent=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder=null;
        //判断版本
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            String id="channel_id";
            String name="channel_name";
            NotificationChannel channel=new NotificationChannel(id,name,NotificationManager.IMPORTANCE_DEFAULT);
            getNotificationManager().createNotificationChannel(channel);

            builder=new NotificationCompat.Builder(this,id)
                    .setAutoCancel(true);
        }else {
            builder =new NotificationCompat.Builder(this).setAutoCancel(true);
        }

        builder.setContentIntent(pi);
        builder.setContentTitle(title).setSmallIcon(android.R.drawable.stat_notify_more);
        if(progress>0){
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }

    class DownloadBinder extends Binder {
        public void startDownload(String downloadUrl) {
            if (task == null) {
                task = new DownLoadTask(listener);
                task.execute(downloadUrl);
                startForeground(1, getNotification(0, "start download"));
                Toast.makeText(DownloadService.this, "start Download", Toast.LENGTH_SHORT).show();
            }
        }

        public void pauseDownload() {
            if (task != null) {
                task.pauseDownload();
            }
        }

        public void cancelDownload() {
            if (task != null) {
                task.cancelDownload();
            } else {
                if (downLoadURL != null) {
                    String filename = downLoadURL.substring(downLoadURL.lastIndexOf("/"));
                    String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(dir + filename);
                    if (file.exists()) {
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this, "cancel", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
