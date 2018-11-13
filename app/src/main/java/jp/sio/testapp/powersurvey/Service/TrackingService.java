package jp.sio.testapp.powersurvey.Service;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import jp.sio.testapp.powersurvey.L;
import jp.sio.testapp.powersurvey.Presenter.PowerSurveyPresenter;
import jp.sio.testapp.powersurvey.R;
import jp.sio.testapp.powersurvey.Repository.LocationLog;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * UEB測位を行うためのService
 * 測位回数、測位間隔、タイムアウト、SuplEndWaitTimeあたりが渡されればいいか？
 * Created by NTT docomo on 2017/05/22.
 */

public class TrackingService extends Service implements LocationListener {

    private LocationManager locationManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private Handler resultHandler;
    private Handler intervalHandler;
    private Handler stopHandler;

    private Timer waitStartTimer;
    private Timer trackingTimer;
    private Timer intervalTimer;

    private  WaitStartTimerTask waitStartTimerTask;
    private  TrackingTimerTask trakcingTimerTask;
    private  IntervalTimerTask intervalTimerTask;

    private AlarmManager waitStartAlarm;
    private AlarmManager trackingAlarm;
    private AlarmManager intervalAlarm;
    private PendingIntent waitStartPending;
    private PendingIntent trackingPending;
    private PendingIntent intervalPending;

    //設定値の格納用変数
    private  final String locationType = "TRACKING";
    private  int settingWaitStartTime;
    private  int settingTrackingTime;
    private  int settingIntervalTime;
    private  boolean settingIsCold;
    private  int settingDelAssistdatatime;
    private  boolean settingIsOutputLog;
    //測位中の測位回数
    private  int runningCount;

    //測位完了までの時間 TTFF
    private  double ttff;

    //アシストデータ削除処理用 最初の1回目のTrackingフラグ
    private  boolean is1stTracking = true;

    //測位成功の場合:true 測位失敗の場合:false を設定
    private  boolean isLocationFix;

    //測位開始時間、終了時間
    private  long locationStartTime;
    private  long locationStopTime;

    private  String receiveCategory;

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

        String extra = "";
        if(intent != null) {
            extra = intent.getStringExtra("method");
            if(extra == null){
                extra = "";
            }
        }

        if(extra.equals("locationStart")){
            L.d("onStartCommand_locationStart");
            locationStart();
        }else if(extra.equals("locationStop")){
            L.d("onStartCommand_locationStop");
            locationStop();
        }else {


            //サービスがKillされるのを防止する処理
            //サービスがKillされにくくするために、Foregroundで実行する
            startForeground();

            //設定値の取得
            // *1000は sec → msec の変換
            settingWaitStartTime = intent.getIntExtra(this.getString(R.string.settingWaitStart), 0);
            settingTrackingTime = intent.getIntExtra(this.getString(R.string.settingTrackingTime), 0);
            settingIntervalTime = intent.getIntExtra(this.getString(R.string.settingInterval), 0);
            settingIsCold = intent.getBooleanExtra(this.getString(R.string.settingIsCold), true);
            settingDelAssistdatatime = intent.getIntExtra(this.getString(R.string.settingDelAssistdataTime), 0) * 1000;
            settingIsOutputLog = intent.getBooleanExtra(this.getString(R.string.settingIsOutputLog), true);
            runningCount = 0;

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            L.d("setWaitStartAlarm実行前");
            setWaitStartAlarm(settingWaitStartTime);
        }
        return START_STICKY;
    }

    /**
     * 測位を開始する時の処理
     */
    public  void locationStart() {

        L.d("locationStart");

        if (settingIsCold && is1stTracking) {
            coldLocation(locationManager);
        }
        locationStartTime = System.currentTimeMillis();
        //MyLocationUsecaseで起動時にPermissionCheckを行っているのでここでは行わない
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        L.d("requestLocationUpdates");
        is1stTracking = false;
        setTrackingAlarm(settingTrackingTime);

    }

    /**
     * 測位成功の場合の処理
     */
    public  void locationSuccess(final Location location) {
        L.d("locationSuccess");
        //測位終了の時間を取得
        locationStopTime = System.currentTimeMillis();
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
        L.d(location.getLatitude() + "," + location.getLongitude());

    }

    /**
     * 測位失敗の場合の処理
     * 今のところタイムアウトした場合のみを想定
     */
    public  void locationStop() {
        L.d("locationFailed");
        //測位終了の時間を取得
        locationStopTime = System.currentTimeMillis();
        runningCount++;
        isLocationFix = false;
        locationManager.removeUpdates(this);
        ttff = (double) (locationStopTime - locationStartTime) / 1000;

        if (trackingTimer != null) {
            trackingTimer = null;
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
        setIntervalAlarm(settingIntervalTime);
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
        if (waitStartTimer != null) {
            waitStartTimer.cancel();
            waitStartTimer = null;
        }

        if (trackingTimer != null) {
            trackingTimer.cancel();
            trackingTimer = null;
        }
        if (intervalTimer != null) {
            intervalTimer.cancel();
            intervalTimer = null;
        }

        if(wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        if (powerManager != null) {
            powerManager = null;
        }

        if(waitStartAlarm != null){
            waitStartAlarm.cancel(waitStartPending);
            waitStartPending.cancel();
        }

        if(trackingAlarm != null){
            trackingAlarm.cancel(waitStartPending);
            waitStartPending.cancel();
        }

        if(intervalAlarm != null){
            intervalAlarm.cancel(intervalPending);
            intervalPending.cancel();
        }

        //Serviceを終わるときにForegroundも停止する
        stopForeground(true);
        sendServiceEndBroadCast();

        stopSelf();
    }

    @Override
    public void onLocationChanged(final Location location) {
        //locationChangeCount++;
        //L.d("onLocationChanged," + "locationChangeCount:" + locationChangeCount);
        locationSuccess(location);
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
    private  void coldLocation(LocationManager lm) {
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
     * 開始待ちタイマー
     * 開始ボタンを押してから画面をOFFして諸々の端末の動作がおとなしくなるのを待つためのTimer
     */
    class WaitStartTimerTask extends TimerTask {

        @Override
        public void run() {
            intervalHandler.post(new Runnable() {
                @Override
                public void run() {
                    L.d("WaitStartTimerTask");
                    locationStart();

                }
            });
        }
    }

    /**
     * Tracking停止タイマー
     * Tracking測位を行う時間を満了したときの処理
     */
    class TrackingTimerTask extends TimerTask {

        @Override
        public void run() {
            stopHandler.post(new Runnable() {
                @Override
                public void run() {
                    L.d("TrackingTimerTask");
                    locationStop();
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

    private void setWaitStartAlarm(int waitStartTime){
        L.d("setWaitStartAlarm実行開始");
        Calendar calender = Calendar.getInstance();
        calender.setTimeInMillis(System.currentTimeMillis());
        calender.add(calender.SECOND,waitStartTime);

        L.d("Intent前");
        Intent intent = new Intent(getApplicationContext(),PowerSurveyPresenter.TimerReceiver.class);
        intent.setAction("waitStartTimerEnd");
        waitStartPending = PendingIntent.getBroadcast(this,0,intent,0);
        waitStartAlarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(waitStartAlarm != null){
            waitStartAlarm.set(AlarmManager.RTC_WAKEUP,calender.getTimeInMillis(), waitStartPending);
            L.d("setWaitStartAlarm");
        }
    }

    private void setTrackingAlarm(int trackingTime){
        L.d("setTrackingAlarm実行開始");
        Calendar calender = Calendar.getInstance();
        calender.setTimeInMillis(System.currentTimeMillis());
        calender.add(calender.SECOND,trackingTime);

        L.d("Intent前");
        Intent intent = new Intent(getApplicationContext(),PowerSurveyPresenter.TimerReceiver.class);
        intent.setAction("trackingTimerEnd");
        waitStartPending= PendingIntent.getBroadcast(this,0,intent,0);
        trackingAlarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(trackingAlarm != null){
            trackingAlarm.set(AlarmManager.RTC_WAKEUP,calender.getTimeInMillis(), waitStartPending);
            L.d("trackingTimer");
        }
    }
    private void setIntervalAlarm(int intervalTime){
        L.d("setIntervalAlarm実行開始");
        Calendar calender = Calendar.getInstance();
        calender.setTimeInMillis(System.currentTimeMillis());
        calender.add(calender.SECOND,intervalTime);

        L.d("Intent前");
        Intent intent = new Intent(getApplicationContext(),PowerSurveyPresenter.TimerReceiver.class);
        intent.setAction("intervalTImeEnd");
        intervalPending = PendingIntent.getBroadcast(this,0,intent,0);
        intervalAlarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(intervalAlarm != null){
            intervalAlarm.set(AlarmManager.RTC_WAKEUP,calender.getTimeInMillis(), intervalPending);
            L.d("IntervalTimer");
        }
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