package jp.sio.testapp.powersurvey.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import jp.sio.testapp.powersurvey.L;
import jp.sio.testapp.powersurvey.R;
import jp.sio.testapp.powersurvey.Repository.LocationLog;

import java.util.Timer;
import java.util.TimerTask;

/**
 * UEB測位を行うためのService
 * 測位回数、測位間隔、タイムアウト、SuplEndWaitTimeあたりが渡されればいいか？
 * Created by NTT docomo on 2017/05/22.
 */

public class TrackingService extends Service implements LocationListener {

    private LocationManager locationManager;
    private LocationLog locationLog;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private Handler resultHandler;
    private Handler intervalHandler;
    private Handler stopHandler;
    private Timer stopTimer;
    private Timer intervalTimer;
    private StopTimerTask stopTimerTask;
    private IntervalTimerTask intervalTimerTask;

    //設定値の格納用変数
    private final String locationType = "TRACKING";
    private long settingWaitStartTime;
    private long settingTrackingTime;
    private long settingIntervalTime;
    private boolean settingIsCold;
    private int settingDelAssistdatatime;
    private boolean settingIsOutputLog;
    //測位中の測位回数
    private int runningCount;

    //測位完了までの時間 TTFF
    private double ttff;

    //測位成功の場合:true 測位失敗の場合:false を設定
    private boolean isLocationFix;

    //測位開始時間、終了時間
    private long locationStartTime;
    private long locationStopTime;

    public class TrackingService_Binder extends Binder {
        public TrackingService getService() {
            return TrackingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        L.d("Tracking onCreate");

        resultHandler = new Handler();
        intervalHandler = new Handler();
        stopHandler = new Handler();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        super.onStartCommand(intent, flags, startid);
        L.d("onStartCommand");

        //サービスがKillされるのを防止する処理
        //サービスがKillされにくくするために、Foregroundで実行する
        startForeground();

        //画面が消灯しないようにする処理
        //画面が消灯しないようにPowerManagerを使用
        // *** 消費電流測定用としてTrackingでは使わないようにしてみる ***/
        //powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //PowerManagerの画面つけっぱなし設定SCREEN_BRIGHT_WAKE_LOCK、非推奨の設定値だが試験アプリ的にはあったほうがいいので使用
        //wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getString(R.string.locationTracking));
        //wakeLock.acquire();

        //設定値の取得
        // *1000は sec → msec の変換
        settingWaitStartTime = intent.getLongExtra(getBaseContext().getString(R.string.settingWaitStart), 0) * 1000;
        settingTrackingTime = intent.getLongExtra(getBaseContext().getString(R.string.settingTrackingTime), 0) * 1000;
        settingIntervalTime = intent.getLongExtra(getBaseContext().getString(R.string.settingInterval), 0) * 1000;
        settingIsCold = intent.getBooleanExtra(getBaseContext().getString(R.string.settingIsCold), true);
        settingDelAssistdatatime = intent.getIntExtra(getResources().getString(R.string.settingDelAssistdataTime), 0) * 1000;
        settingIsOutputLog = intent.getBooleanExtra(getBaseContext().getString(R.string.settingIsOutputLog),true);
        runningCount = 0;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationStart();

        return START_STICKY;
    }

    /**
     * 測位を開始する時の処理
     */
    public void locationStart() {

        L.d("locationStart");

        if (settingIsCold) {
            coldLocation(locationManager);
        }
        locationStartTime = System.currentTimeMillis();
        //MyLocationUsecaseで起動時にPermissionCheckを行っているのでここでは行わない
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        L.d("requestLocationUpdates");

        //測位停止Timerの設定
        /*
        L.d("SetStopTimer");
        stopTimerTask = new StopTimerTask();
        stopTimer = new Timer(true);
        stopTimer.schedule(stopTimerTask, settingTimeout);
        */
    }

    /**
     * 測位成功の場合の処理
     */
    public void locationSuccess(final Location location) {
        L.d("locationSuccess");
        //測位終了の時間を取得
        locationStopTime = System.currentTimeMillis();
        //測位タイムアウトのタイマーをクリア
        /*
        if (stopTimer != null) {
            stopTimer.cancel();
            stopTimer = null;
        }
        */
        //runningCount++;
        isLocationFix = true;
        ttff = (double) (locationStopTime - locationStartTime) / 1000;
        //測位結果の通知
        resultHandler.post(new Runnable() {
            @Override
            public void run() {
                L.d("resultHandler.post");
                sendLocationBroadCast(isLocationFix, location, locationStartTime, locationStopTime);
            }
        });
        L.d(location.getLatitude() + " " + location.getLongitude());
        /*
        try {
            Thread.sleep(settingSuplEndWaitTime);
        } catch (InterruptedException e) {
            L.d(e.getMessage());
            e.printStackTrace();
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        */

        //測位回数が設定値に到達しているかチェック
        /*
        if (runningCount == settingCount && settingCount != 0) {
            serviceStop();
        } else {
            //回数満了してなければ測位間隔Timerを設定して次の測位の準備
            L.d("SuccessのIntervalTimer");
            if (intervalTimer != null) {
                intervalTimer.cancel();
                intervalTimer = null;
            }
            intervalTimerTask = new IntervalTimerTask();
            intervalTimer = new Timer();
            L.d("Interval:" + settingInterval);
            intervalTimer.schedule(intervalTimerTask, settingInterval);
        }
        */
    }

    /**
     * 測位失敗の場合の処理
     * 今のところタイムアウトした場合のみを想定
     */
    public void locationFailed() {
        L.d("locationFailed");
        //測位終了の時間を取得
        locationStopTime = System.currentTimeMillis();
        runningCount++;
        isLocationFix = false;
        locationManager.removeUpdates(this);
        ttff = (double) (locationStopTime - locationStartTime) / 1000;

        if (stopTimer != null) {
            stopTimer = null;
        }
        //測位結果の通知
        resultHandler.post(new Runnable() {
            @Override
            public void run() {
                L.d("resultHandler.post");
                Location location = new Location(LocationManager.GPS_PROVIDER);
                sendLocationBroadCast(isLocationFix, location, locationStartTime, locationStopTime);
            }
        });
    }

    /**
     * 測位が終了してこのServiceを閉じるときの処理
     * 測位回数満了、停止ボタンによる停止を想定した処理
     */
    public void serviceStop() {
        L.d("serviceStop");
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager = null;
        }
        if (stopTimer != null) {
            stopTimer.cancel();
            stopTimer = null;
        }
        if (intervalTimer != null) {
            intervalTimer.cancel();
            intervalTimer = null;
        }
        //Serviceを終わるときにForegroundも停止する
        stopForeground(true);
        sendServiceEndBroadCast();

        if(wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        if (powerManager != null) {
            powerManager = null;
        }
        stopSelf();
    }

    @Override
    public void onLocationChanged(final Location location) {
        //locationChangeCount++;
        //L.d("onLocationChanged," + "locationChangeCount:" + locationChangeCount);
        locationSuccess(location);
        /*
        if(locationChangeCount == 1){
            locationSuccess(location);
        }
        */
    }

    @Override
    public void onDestroy() {
        L.d("onDestroy");
        serviceStop();
        super.onDestroy();
    }

    /**
     * アシストデータの削除
     */
    private void coldLocation(LocationManager lm) {
        sendColdBroadCast(getResources().getString(R.string.categoryColdStart));
        L.d("coldBroadcast:" + getResources().getString(R.string.categoryColdStart));
        lm.sendExtraCommand(LocationManager.GPS_PROVIDER, "delete_aiding_data", null);
        try {
            Thread.sleep(settingDelAssistdatatime);
        } catch (InterruptedException e) {
            L.d(e.getMessage());
            e.printStackTrace();
        }

        sendColdBroadCast(getResources().getString(R.string.categoryColdStop));
    }

    /**
     * 測位停止タイマー
     * 測位タイムアウトしたときの処理
     */
    class StopTimerTask extends TimerTask {

        @Override
        public void run() {
            stopHandler.post(new Runnable() {
                @Override
                public void run() {
                    L.d("StopTimerTask");
                    locationFailed();
                }
            });
        }
    }

    /**
     * 測位間隔タイマー
     * 測位間隔を満たしたときの次の動作（次の測位など）を処理
     */
    class IntervalTimerTask extends TimerTask {

        @Override
        public void run() {
            intervalHandler.post(new Runnable() {
                @Override
                public void run() {
                    L.d("IntervalTimerTask");
                    locationStart();
                }
            });
        }
    }

    /**
     * 測位完了を上に通知するBroadcast 測位結果を入れる
     *
     * @param fix               測位成功:True 失敗:False
     * @param location          測位結果
     * @param locationStartTime 測位API実行の時間
     * @param locationStopTime  測位API停止の時間
     */
    protected void sendLocationBroadCast(Boolean fix, Location location, long locationStartTime, long locationStopTime) {
        L.d("sendLocation");
        Intent broadcastIntent = new Intent(getResources().getString(R.string.locationTracking));
        broadcastIntent.putExtra(getResources().getString(R.string.category), getResources().getString(R.string.categoryLocation));
        broadcastIntent.putExtra(getResources().getString(R.string.TagisFix), fix);
        broadcastIntent.putExtra(getResources().getString(R.string.TagLocation), location);
        broadcastIntent.putExtra(getResources().getString(R.string.Tagttff), ttff);
        broadcastIntent.putExtra(getResources().getString(R.string.TagLocationStarttime), locationStartTime);
        broadcastIntent.putExtra(getResources().getString(R.string.TagLocationStoptime), locationStopTime);

        sendBroadcast(broadcastIntent);
    }

    /**
     * Cold化(アシストデータ削除)の開始と終了を通知するBroadcast
     * 削除開始:categoryColdStart 削除終了:categoryColdStop
     *
     * @param category
     */
    protected void sendColdBroadCast(String category) {
        Intent broadcastIntent = new Intent(getResources().getString(R.string.locationTracking));

        if (category.equals(getResources().getString(R.string.categoryColdStart))) {
            L.d("ColdStart");
            broadcastIntent.putExtra(getResources().getString(R.string.category), getResources().getString(R.string.categoryColdStart));
        } else if (category.equals(getResources().getString(R.string.categoryColdStop))) {
            L.d("ColdStop");
            broadcastIntent.putExtra(getResources().getString(R.string.category), getResources().getString(R.string.categoryColdStop));
        }
        sendBroadcast(broadcastIntent);
    }

    /**
     * Serviceを破棄することを通知するBroadcast
     */
    protected void sendServiceEndBroadCast() {
        Intent broadcastIntent = new Intent(getResources().getString(R.string.locationTracking));
        broadcastIntent.putExtra(getResources().getString(R.string.category), getResources().getString(R.string.categoryServiceEnd));
        sendBroadcast(broadcastIntent);
    }
    /**
     * Foregroundの設定作成
     */
    private void startForeground(){
        //サービスがKillされるのを防止する処理
        //サービスがKillされにくくするために、Foregroundで実行する
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        String name = "MyLocation";
        String id = "casareal_foreground";
        String notifyDescription = "詳細情報(未実装)";
        NotificationChannel mChannel;
        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(notificationManager.getNotificationChannel(id) == null){
                mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(mChannel);
            }
        }
        notification = new NotificationCompat.Builder(this,id).build();

        startForeground(1, notification);

    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true; // 再度クライアントから接続された際に onRebind を呼び出させる場合は true を返す
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new TrackingService_Binder();
    }

    @Override
    public void onRebind(Intent intent) {
    }
}