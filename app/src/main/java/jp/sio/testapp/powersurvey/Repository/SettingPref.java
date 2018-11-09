package jp.sio.testapp.powersurvey.Repository;

import android.content.Context;
import android.content.SharedPreferences;

import jp.sio.testapp.powersurvey.L;
import jp.sio.testapp.powersurvey.R;

import static android.content.Context.*;

/**
 * Created by NTT docomo on 2017/05/23.
 */

public class SettingPref {
    private SharedPreferences settingPref;
    private SharedPreferences.Editor editor;
    private Context context;

    //Default設定
    private final int defWaitStartTime = 300;
    private final int defTrackingTime = 120;
    private final int defIntervalTime = 300;
    private final boolean defIsCold = true;
    private final int defSuplEndWaitTime = 0;
    private final int defDelAssistDataTime = 3;
    private final boolean defIsOutputLog = true;
    private final String defLocationType = "TRACKING";
    //SharedPreference名
    private String PREFNAME = "PowerSurveySetting";

    /**
     * SettingをShearedPreferencesに保存・読み込みを行う
     */
    public SettingPref(Context context){
        this.context = context;
    }
    public void createPref(){
        settingPref = context.getSharedPreferences(PREFNAME, MODE_PRIVATE);
        editor = settingPref.edit();
    }

    public void setLocationType(String locationType){
        editor.putString(context.getString(R.string.settingLocationType),locationType);
        L.d("SettingPrefLocationType:" + locationType);
        commitSetting();
    }
    public void setWaitStartTime(int waittime){
        editor.putInt(context.getString(R.string.settingWaitStart),waittime);
        commitSetting();
    }
    public void setTrackingTime(int timeout){
        editor.putInt(context.getString(R.string.settingTrackingTime),timeout);
        commitSetting();
    }

    public void setIntervalTime(int interval){
        editor.putInt(context.getString(R.string.settingInterval),interval);
        commitSetting();
    }
    public void setIsCold(boolean isCold){
        editor.putBoolean(context.getString(R.string.settingIsCold),isCold);
        commitSetting();
    }
    public void setDelAssistDataTime(int delAssistDataTime){
        editor.putInt(context.getString(R.string.settingDelAssistdataTime),delAssistDataTime);
        commitSetting();
    }
    public void setIsOutputLog(boolean isOutputLog){
        editor.putBoolean(context.getString(R.string.settingIsOutputLog),isOutputLog);
        commitSetting();
    }

    public String getLocationType(){
        return settingPref.getString(context.getResources().getString(R.string.settingLocationType),defLocationType);
    }
    public int getWaitStartTime(){
        return settingPref.getInt(context.getString(R.string.settingWaitStart),defWaitStartTime);
    }
    public int getIntervalTime(){
        return settingPref.getInt(context.getString(R.string.settingInterval),defIntervalTime);
    }
    public int getTrackingTime(){
        return settingPref.getInt(context.getString(R.string.settingTrackingTime),defTrackingTime);
    }
    public boolean getIsCold(){
        return settingPref.getBoolean(context.getString(R.string.settingIsCold),defIsCold);
    }
    public boolean getIsOutputLog(){
        return settingPref.getBoolean(context.getString(R.string.settingIsOutputLog),defIsOutputLog);
    }

    public int getDelAssistDataTime(){
        return settingPref.getInt(context.getString(R.string.settingDelAssistdataTime),defDelAssistDataTime);
    }

    public void setDefaultSetting(){
        setLocationType(defLocationType);
        setWaitStartTime(defWaitStartTime);
        setTrackingTime(defTrackingTime);
        setIntervalTime(defIntervalTime);
        setIsCold(defIsCold);
        setDelAssistDataTime(defDelAssistDataTime);
        setIsOutputLog(defIsOutputLog);
        commitSetting();
    }

    public void commitSetting(){
        editor.apply();
        editor.commit();
        L.d("commitSetting");
    }
}