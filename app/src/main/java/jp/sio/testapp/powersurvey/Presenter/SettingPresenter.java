package jp.sio.testapp.powersurvey.Presenter;

import android.app.Activity;

import jp.sio.testapp.powersurvey.Activity.SettingActivity;
import jp.sio.testapp.powersurvey.L;
import jp.sio.testapp.powersurvey.R;
import jp.sio.testapp.powersurvey.Usecase.SettingUsecase;

/**
 * Created by NTT docomo on 2017/05/24.
 * SettingActivityとSettingUsecaseの橋渡し
 */

public class SettingPresenter {
    SettingActivity activity;
    SettingUsecase settingusecase;

    public SettingPresenter(SettingActivity activity){
        this.activity = activity;
        settingusecase = new SettingUsecase(activity);
    }

    /**
     * 現在Activityに入力されている値を保存する
     */
    public void commitSetting(){
        String locationTyep = activity.getResources().getString(R.string.locationTracking);
        if(activity.isRadioButtonTracking()) {
            locationTyep = activity.getResources().getString(R.string.locationTracking);
        }
        settingusecase.setLocationType(locationTyep);

        settingusecase.setWaitStartTime(activity.getWaitStartTime());
        settingusecase.setTrackingTime(activity.getTrackingTime());
        settingusecase.setIntervalTime(activity.getIntervalTime());
        settingusecase.setIsCold(activity.isColdCheck());
        settingusecase.setIsOutputLog(activity.isOutputLogCheck());
        settingusecase.setDelAssistDataTime(activity.getDelAssistDataTime());
        settingusecase.commitSetting();
    }
    /**
     * 現在保存されている値をActivityに表示する
     */
    public void loadSetting(){
        String locationType = settingusecase.getLocationType();
        if(locationType.equals(activity.getResources().getString(R.string.locationTracking))) {
            activity.enableRadioButtonTracking();
        }
        activity.setWaitStartTime(settingusecase.getWaitStartTime());
        activity.setIntervalTime(settingusecase.getIntervalTime());
        activity.setTrackingTime(settingusecase.getTrackingTime());
        if(settingusecase.getIsCold()) {
            activity.enableIsCold();
        }else {
            activity.disableIsCold();
        }
        if(settingusecase.getIsOutputLog()) {
            activity.enableIsOutputLog();
        }else {
            activity.disableIsOutputLog();
        }

        activity.setDelAssistDataTime(settingusecase.getDelAssistDataTime());
    }
}