package jp.sio.testapp.powersurvey.Usecase;

import android.content.Context;

import jp.sio.testapp.powersurvey.L;
import jp.sio.testapp.powersurvey.Repository.SettingPref;

/**
 * Created by NTT docomo on 2017/05/24.
 * Settingの値を設定したり取得する
 * 設定の保存方法はここで吸収する
 * 今回はSharedPreferenceを使用してる
 */

public class SettingUsecase {
    private SettingPref settingPref;
    private Context context;

    public SettingUsecase(Context context){
        this.context = context;
        settingPref = new SettingPref(context);
        settingPref.createPref();
    }

    /**
     * 設定を初期化する
     */
    public void setDefaultSetting(){
        settingPref.setDefaultSetting();
    }

    /*********************ここからSetter**********************/
    public void setWaitStartTime(Long waittime){
        settingPref.setWaitStartTime(waittime);
    }
    public void setIntervalTime(long interval){
        settingPref.setIntervalTime(interval);
    }
    public void setTrackingTime(long trackingtime){
        settingPref.setTrackingTime(trackingtime);
    }
    public void setDelAssistDataTime(int delAssistDataTime){
        settingPref.setDelAssistDataTime(delAssistDataTime);
    }
    public void setIsCold(boolean iscold){
        settingPref.setIsCold(iscold);
    }
    public void setIsOutputLog(boolean isoutputlog){
        settingPref.setIsOutputLog(isoutputlog);
    }

    public void setLocationType(String locationType){
        L.d("Usecase:"+locationType);
        settingPref.setLocationType(locationType);
    }

     /*****************ここからGetter*******************/
    public String getLocationType(){
        return settingPref.getLocationType();
    }
    public long getWaitStartTime(){
        return settingPref.getWaitStartTime();
    }
    public long getTrackingTime(){
        return settingPref.getTrackingTime();
    }
    public long getIntervalTime(){
        return settingPref.getIntervalTime();
    }
    public boolean getIsCold(){
        return settingPref.getIsCold();
    }
    public boolean getIsOutputLog(){
        return settingPref.getIsOutputLog();
    }

    public int getDelAssistDataTime(){
        return settingPref.getDelAssistDataTime();
    }

    public void commitSetting(){
        settingPref.commitSetting();
    }
}
