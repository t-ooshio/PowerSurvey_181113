package jp.sio.testapp.powersurvey.Presenter;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import java.text.SimpleDateFormat;
import java.util.List;

import jp.sio.testapp.powersurvey.Activity.PowerSurveyActivity;
import jp.sio.testapp.powersurvey.Activity.SettingActivity;
import jp.sio.testapp.powersurvey.L;
import jp.sio.testapp.powersurvey.R;
import jp.sio.testapp.powersurvey.Service.TrackingService;
import jp.sio.testapp.powersurvey.Usecase.PowerSurverUsecase;
import jp.sio.testapp.powersurvey.Usecase.SettingUsecase ;
import jp.sio.testapp.powersurvey.Repository.LocationLog;

/**
 * Created by NTT docomo on 2017/05/23.
 * ActivityとServiceの橋渡し
 * Activityはなるべく描画だけに専念させたいから分けるため
 */

public class PowerSurveyPresenter {
    private PowerSurveyActivity activity;
    private SettingUsecase settingUsecase;
    private PowerSurverUsecase powerSurverUsecase;
    private Intent settingIntent;
    private Intent locationserviceIntent;
    private ServiceConnection runService;
    private LocationLog locationLog;

    private String receiveCategory;
    private String categoryLocation;
    private String categoryColdStart;
    private String categoryColdStop;
    private String categoryServiceStop;

    private TrackingService trackingService;

    private String locationType;
    private long waitStartTime;
    private long trackingTime;
    private long intervalTime;
    private boolean isCold;
    private boolean isOutputLog;
    private int delassisttime;

    private String settingHeader;
    private String locationHeader;


    private ServiceConnection serviceConnectionTracking = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            trackingService = ((TrackingService.TrackingService_Binder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            activity.unbindService(runService);
            trackingService = null;

        }
    };

    private final LocationReceiver locationReceiver = new LocationReceiver();

    public PowerSurveyPresenter(PowerSurveyActivity activity){
        this.activity = activity;
        powerSurverUsecase = new PowerSurverUsecase(activity);
        settingUsecase = new SettingUsecase(activity);

        categoryLocation = activity.getResources().getString(R.string.categoryLocation);
        categoryColdStart = activity.getResources().getString(R.string.categoryColdStart);
        categoryColdStop = activity.getResources().getString(R.string.categoryColdStop);
        categoryServiceStop = activity.getResources().getString(R.string.categoryServiceEnd);
        settingHeader = activity.getResources().getString(R.string.settingHeader) ;
        locationHeader =activity. getResources().getString(R.string.locationHeader);

    }

    public void checkPermission(){
        powerSurverUsecase.hasPermissions();
    }

    public void mStart(){
        activity.offBtnStop();

        activity.showTextViewState(activity.getResources().getString(R.string.locationStop));
    }

    public void locationStart(){
        IntentFilter filter = null;
        getSetting();
        L.d(locationType + "," + waitStartTime + "," + trackingTime
                + "," + intervalTime + "," + delassisttime + "," + isCold
                + "," + isOutputLog);
        //ログファイルの生成
        locationLog = new LocationLog(activity);
        L.d("before_makeLogFile");
        L.d(settingHeader);
        locationLog.makeLogFile(settingHeader);
        locationLog.writeLog(
                locationType + "," + waitStartTime + "," + trackingTime
                        + "," + intervalTime + "," + delassisttime + "," + isCold
                        + "," + isOutputLog);
        locationLog.writeLog(locationHeader);

        activity.showTextViewSetting("測位方式:" + locationType + "\n" + "開始待ち時間:" + waitStartTime + "\n"
                + "TRACKING実行時間:" + trackingTime + "\n"
                +  "測位間隔:" + intervalTime + "\n" + "Cold:" + isCold + "\n"
                + "アシストデータ削除時間:" + delassisttime + "\n"
                + "ログ出力:" + isOutputLog + "\n" );


        if(locationType.equals(activity.getResources().getString(R.string.locationTracking))) {
            locationserviceIntent = new Intent(activity.getApplicationContext(), TrackingService.class);
            setSetting(locationserviceIntent);
            L.d("after runService");
            runService = serviceConnectionTracking;
            L.d("before runservice");
            filter = new IntentFilter(activity.getResources().getString(R.string.locationTracking));
        }else{
            showToast("予期せぬ測位方式");
        }
        L.d("before startService");
        activity.startService(locationserviceIntent);
        L.d("after startService");
        activity.registerReceiver(locationReceiver,filter);
        activity.bindService(locationserviceIntent,runService ,Context.BIND_AUTO_CREATE);

    }

    /**
     * 測位回数満了などで測位を停止する処理
     */
    public void locationStop(){
        L.d("locationStop");

        //ServiceConnectionの削除
        /*
        L.d("ServiceConnectionの削除");
        if(runService != null) {
            if (isServiceRunning(activity, runService.getClass())) {
                L.d("unbindService");
                activity.unbindService(runService);
            }
        }
        */
        L.d("ServiceConnectionの削除");
        if(runService != null) {
            L.d("unbindService");
            try {
                activity.unbindService(runService);
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        }


        //Service1の停止
        L.d("Serviceの停止");
        if(locationserviceIntent != null) {
            try {
                activity.stopService(locationserviceIntent);
            }catch(SecurityException e){
                e.printStackTrace();
            }
        }

        //Receiverの消去
        L.d("Receiverの消去");
        try {
            if (locationReceiver != null) {
                activity.unregisterReceiver(locationReceiver);
            }
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }

        //logファイルの終了
        L.d("logファイルの終了");
        if(locationLog != null) {
            locationLog.endLogFile();
        }
    }

    /**
     * Setting表示開始
     */
    public void settingStart(){
        settingIntent = new Intent(activity.getApplicationContext(), SettingActivity.class);
        activity.startActivity(settingIntent);
    }

    /**
     * activityにToastを表示する
     * @param message
     */
    public void showToast(String message){
        activity.showToast(message);
    }

    /**
     * 測位結果を受けとるためのReceiver
     */
    public class LocationReceiver extends BroadcastReceiver {
        Boolean isFix;
        double lattude, longitude, ttff;
        long fixtimeEpoch;
        String fixtimeUTC;
        String locationStarttime, locationStoptime;


        Location location = new Location(LocationManager.GPS_PROVIDER);
        SimpleDateFormat fixTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZ");
        SimpleDateFormat simpleDateFormatHH = new SimpleDateFormat("HH:mm:ss.SSS");


        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            receiveCategory = bundle.getString(activity.getResources().getString(R.string.category));

            if (receiveCategory.equals(categoryLocation)) {
                location = bundle.getParcelable(activity.getResources().getString(R.string.TagLocation));
                isFix = bundle.getBoolean(activity.getResources().getString(R.string.TagisFix));
                locationStarttime = simpleDateFormatHH.format(bundle.getLong(activity.getResources().getString(R.string.TagLocationStarttime)));
                locationStoptime = simpleDateFormatHH.format(bundle.getLong(activity.getResources().getString(R.string.TagLocationStoptime)));
                if (isFix) {
                    lattude = location.getLatitude();
                    longitude = location.getLongitude();
                    fixtimeEpoch = location.getTime();
                    fixtimeUTC = fixTimeFormat.format(fixtimeEpoch);
                } else {
                    lattude = -1;
                    longitude = -1;
                    fixtimeEpoch = -1;
                    fixtimeUTC = "-1";
                }
                ttff = bundle.getDouble(activity.getResources().getString(R.string.Tagttff));
                L.d("onReceive");
                L.d(locationStarttime + "," + locationStoptime + "," + isFix + "," + lattude + "," + longitude + "," + ttff + ","
                        + fixtimeEpoch + "," + fixtimeUTC + "\n");
                locationLog.writeLog(
                        locationStarttime + "," + locationStoptime + "," + isFix + "," + location.getLatitude() + "," + location.getLongitude()
                                + "," + ttff + "," + location.getAccuracy() + "," + fixtimeEpoch + "," + fixtimeUTC);

                activity.showTextViewResult("測位成否：" + isFix + "\n" + "緯度:" + lattude + "\n" + "経度:" + longitude + "\n" + "TTFF：" + ttff
                        + "\n" + "fixTimeEpoch:" + fixtimeEpoch + "\n" + "fixTimeUTC:" + fixtimeUTC + "\n");

                activity.showTextViewState(activity.getResources().getString(R.string.locationWait));
            } else if (receiveCategory.equals(categoryColdStart)) {
                L.d("ReceiceColdStart");
                activity.showTextViewState(activity.getResources().getString(R.string.locationPositioning));
                showToast("アシストデータ削除中");
            } else if (receiveCategory.equals(categoryColdStop)) {
                L.d("ReceiceColdStop");
                showToast("アシストデータ削除終了");
            } else if (receiveCategory.equals(categoryServiceStop)) {
                L.d("ServiceStop");
                activity.showTextViewState(activity.getResources().getString(R.string.locationStop));
                showToast("測位サービス終了");
                activity.onBtnStart();
                activity.offBtnStop();
                activity.onBtnSetting();
            }
        }

        public void unreggister() {
            activity.unregisterReceiver(this);
        }
    }

    private void setSetting(Intent locationServiceIntent){
        L.d("before setSetting");
        locationServiceIntent.putExtra(activity.getResources().getString(R.string.settingWaitStart),waitStartTime);
        locationServiceIntent.putExtra(activity.getResources().getString(R.string.settingTrackingTime),trackingTime);
        locationServiceIntent.putExtra(activity.getResources().getString(R.string.settingInterval),intervalTime);
        locationServiceIntent.putExtra(activity.getResources().getString(R.string.settingIsCold),isCold);
        locationServiceIntent.putExtra(activity.getResources().getString(R.string.settingDelAssistdataTime),delassisttime);
        locationServiceIntent.putExtra(activity.getResources().getString(R.string.settingIsOutputLog),isOutputLog);
        L.d("after setSetting");
    }

    /**
     * 設定画面で設定した値を取得する
     */
    private void getSetting(){
        L.d("before getSetting");
        locationType = settingUsecase.getLocationType();
        waitStartTime = settingUsecase.getWaitStartTime();
        trackingTime = settingUsecase.getTrackingTime();
        intervalTime = settingUsecase.getIntervalTime();
        isCold = settingUsecase.getIsCold();
        delassisttime = settingUsecase.getDelAssistDataTime();
        isOutputLog = settingUsecase.getIsOutputLog();
        L.d("after getSetting");
    }

    /**
     * clsに渡したServiceが起動中か確認する
     * true:  起動している
     * false: 起動していない
     * @param context
     * @param cls
     * @return
     */
    private boolean isServiceRunning(Context context, Class<?> cls){
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningService = am.getRunningServices(Integer.MAX_VALUE);
        for(ActivityManager.RunningServiceInfo i :runningService){
            if(cls.getName().equals(i.service.getClassName())){
                L.d(cls.getName());
                return true;
            }
        }
        return false;
    }
}