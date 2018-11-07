package jp.sio.testapp.powersurvey.Activity;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import jp.sio.testapp.powersurvey.Presenter.SettingPresenter;
import jp.sio.testapp.powersurvey.R;

/**
 * Settingの画面
 * 処理はSettingUsecaseへ渡す
 */
public class SettingActivity extends AppCompatActivity {

    SettingPresenter settingPresenter;

    private EditText editTextWaitStartTime;
    private EditText editTextTrackingTime;
    private EditText editTextIntervalTime;
    private EditText editTextDelAssistDataTime;
    private RadioButton radioButtonTrakcing;
    private CheckBox checkBoxisCold;
    private CheckBox checkBoxisOutputLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //縦方向に固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        settingPresenter = new SettingPresenter(this);

        editTextWaitStartTime = (EditText)findViewById(R.id.editTextWaitStartTime);
        editTextTrackingTime = (EditText)findViewById(R.id.editTextTrackingTime);
        editTextIntervalTime = (EditText)findViewById(R.id.editTextIntervalTime);
        editTextDelAssistDataTime = (EditText)findViewById(R.id.editTextDelAssistDataTime);
        radioButtonTrakcing = (RadioButton)findViewById(R.id.rbTracking);
        checkBoxisCold = (CheckBox)findViewById(R.id.checkboxIsDelAssistData);
        checkBoxisOutputLog = (CheckBox)findViewById(R.id.checkboxIsOutputLog);
    }

    @Override
    protected void onStart(){
        super.onStart();
        settingPresenter.loadSetting();
    }
    @Override
    protected void onResume(){
        settingPresenter.loadSetting();
        super.onResume();
    }

    public void setWaitStartTime(long waittime){
        editTextWaitStartTime.setText(Long.toString(waittime));
    }
    public void setTrackingTime(long trackingtime){
        editTextTrackingTime.setText(Long.toString(trackingtime));
    }
    public void setIntervalTime(long interval){
        editTextIntervalTime.setText(Long.toString(interval));
    }
    public void setDelAssistDataTime(int delAssistDataTime){
        editTextDelAssistDataTime.setText(Integer.toString(delAssistDataTime));
    }
    public void enableRadioButtonTracking(){
        radioButtonTrakcing.setChecked(true);
    }
    public void enableIsCold(){
        checkBoxisCold.setChecked(true);
    }
    public void disableIsCold(){
        checkBoxisCold.setChecked(false);
    }
    public void enableIsOutputLog(){
        checkBoxisOutputLog.setChecked(true);
    }
    public void disableIsOutputLog(){
        checkBoxisOutputLog.setChecked(false);
    }

    public long getWaitStartTime(){
        String waittime;
        waittime = editTextWaitStartTime.getText().toString();
        return Long.parseLong(waittime);
    }
    public long getTrackingTime(){
        String trackingtime;
        trackingtime = editTextTrackingTime.getText().toString();
        return Long.parseLong(trackingtime);
    }
    public long getIntervalTime(){
        String interval;
        interval = editTextIntervalTime.getText().toString();
        return Long.parseLong(interval);
    }
    public int getDelAssistDataTime(){
        String delassistdatatime;
        delassistdatatime = editTextDelAssistDataTime.getText().toString();
        return Integer.parseInt(delassistdatatime);
    }
    public boolean isRadioButtonTracking(){
        return radioButtonTrakcing.isChecked();
    }
    public boolean isColdCheck(){
        return checkBoxisCold.isChecked();
    }
    public boolean isOutputLogCheck(){
        return checkBoxisOutputLog.isChecked();
    }

    @Override
    protected void onDestroy(){
        //TODO: 戻るボタンを押されたときにSetting
        settingPresenter.commitSetting();
        super.onDestroy();
    }
    @Override
    protected void onPause(){
        settingPresenter.commitSetting();
        super.onPause();
    }
}
