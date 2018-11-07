package jp.sio.testapp.powersurvey.Usecase;

import android.Manifest;
import android.os.Build;

import jp.sio.testapp.powersurvey.Activity.PowerSurveyActivity;

/**
 * Created by NTT docomo on 2017/05/26.
 */

public class PowerSurverUsecase {

    private PowerSurveyActivity activity;
    private static final int PERMISSION_REQUEST = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.WAKE_LOCK};

    public PowerSurverUsecase(PowerSurveyActivity activity){
        this.activity = activity;
    }

    public void hasPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Permissions granted at install time.
            activity.requestPermissions(REQUIRED_PERMISSIONS, PERMISSION_REQUEST);
        }
    }
}
