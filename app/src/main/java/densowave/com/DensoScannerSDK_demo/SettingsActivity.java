package densowave.com.DensoScannerSDK_demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import com.densowave.scannersdk.Barcode.BarcodeException;
import com.densowave.scannersdk.Common.CommException;
import com.densowave.scannersdk.Const.CommConst.CommBattery;
import com.densowave.scannersdk.Dto.BarcodeScannerSettings;
import com.densowave.scannersdk.Dto.BarcodeScannerSettings.Scan.TriggerMode;
import com.densowave.scannersdk.Dto.CommScannerParams;
import com.densowave.scannersdk.Dto.CommScannerParams.BuzzerVolume;
import com.densowave.scannersdk.Dto.CommScannerParams.Notification.Sound.Buzzer;
import com.densowave.scannersdk.Dto.RFIDScannerSettings;
import com.densowave.scannersdk.Dto.RFIDScannerSettings.Scan.DoubleReading;
import com.densowave.scannersdk.Dto.RFIDScannerSettings.Scan.Polarization;
import com.densowave.scannersdk.Dto.RFIDScannerSettings.Scan.SessionFlag;
import com.densowave.scannersdk.RFID.RFIDException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**

 * Activity in Settings screen

 */

public class SettingsActivity extends BaseActivity {

    private boolean disposeFlg = true;

    private CommScannerParams commParams;
    private RFIDScannerSettings rfidSettings;
    private BarcodeScannerSettings barcodeSettings;
    
    private int checkboxTrue = 0;
    private int checkboxTrueID = 0;

    // Buzzer volume

    Map<String, BuzzerVolume> buzzerVolumeMap = new HashMap<String, BuzzerVolume>(3);
    // Trigger mode

    Map<String, TriggerMode> triggerModeMap = new HashMap<String, TriggerMode>(5);
    // Set the polarization

    Map<String, Polarization> polarizationMap = new HashMap<String, Polarization>(3);

    // RFID TRIGGER MODE

    Map<String, RFIDScannerSettings.Scan.TriggerMode> rfid_triggerModeMap = new HashMap<String, RFIDScannerSettings.Scan.TriggerMode>(5);


    // Whether it is connected to the scanner during generating time

    // Even when the connection is lost while on this screen, if it was connected to scanner during generating time, display the communication error

    private boolean scannerConnectedOnCreate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        scannerConnectedOnCreate = super.isCommScanner();

        boolean result = true;

        if (scannerConnectedOnCreate) {
            // Read SP1 information

            result = loadScannerInfo();
        } else {
            // When SP1 is not found, display the error message.

            super.showMessage(getString(R.string.E_MSG_NO_CONNECTION));
            result = false; // Disconnected

        }

        // Read the configuration value

        if (true == result) {
            setChannel();
            loadSettings();
        }

        // Service is started in the back ground.

        super.startService();
    }

    @Override
    protected void onDestroy() {
        if (scannerConnectedOnCreate && disposeFlg) {
            super.disconnectCommScanner();
        }
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        disposeFlg = true;
        super.onRestart();
    }

    /**

     * When pressing home button

     */

    @Override
    public void onUserLeaveHint() {
        if (disposeFlg) {
            // Transmit and maintain SP1 configuration value

            save();
            disposeFlg = false;
        }
    }

    /**

     * Key event

     * @param keyCode

     * @param event

     * @return

     */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode){
            // When the Return button is pressed

            case KeyEvent.KEYCODE_BACK:
                // Transmit and maintain SP1 configuration value

                save();
                disposeFlg = false;

                // Stop the Activity because it becomes unnecessary since the parent Activity is returned to.

                finish();
                return true;
        }
        return false;
    }

    /**

     * Process when clicking

     * All touch events in Activity are controlled by this process

     * @param view The clicked View

     */

    public void onClick(View view) {
        // Declare the variable that cannot be put in switch

        NumberPickerContents numberPickerContents;
        StringSelectorContents stringSelectorContents;
        CheckBox checkBoxDisable = null;

        int id = view.getId();
        switch (id) {
            case R.id.button_navigate_up:
                navigateUp();
                break;
            case R.id.text_read_power_level_value:
                // Generate the Number Picker dialog

                numberPickerContents = new NumberPickerContents();

                numberPickerContents.id = id;
                numberPickerContents.prefResId = R.string.pref_read_power_level;
                numberPickerContents.title = getString(R.string.read_power_level);
                numberPickerContents.minValue =
                        getResources().getInteger(R.integer.read_power_level_min);
                numberPickerContents.maxValue =
                        getResources().getInteger(R.integer.read_power_level_max);
                numberPickerContents.startValue = numberPickerContents.minValue;

                showNumberPicker (numberPickerContents);
                break;
            case R.id.text_session_value:
                // Generate the StringSelector dialog

                stringSelectorContents = new StringSelectorContents();

                stringSelectorContents.id = id;
                stringSelectorContents.title = getString(R.string.session);
                stringSelectorContents.items = getResources().getStringArray(R.array.strings_session);

                showStringSelector (stringSelectorContents);
                break;

            /*case R.id.checkbox_session_init:
                break;*/

            case R.id.checkbox_report_unique_tags:
                break;

            case R.id.checkbox_channel5:
                checkBoxDisable = (CheckBox) findViewById(id);
                checkboxChannel(checkBoxDisable.isChecked());
                break;

            case R.id.checkbox_channel11:
                checkBoxDisable = (CheckBox) findViewById(id);
                checkboxChannel(checkBoxDisable.isChecked());
                break;

            case R.id.checkbox_channel17:
                checkBoxDisable = (CheckBox) findViewById(id);
                checkboxChannel(checkBoxDisable.isChecked());
                break;

            case R.id.checkbox_channel23:
                checkBoxDisable = (CheckBox) findViewById(id);
                checkboxChannel(checkBoxDisable.isChecked());
                break;

            case R.id.checkbox_channel24:
                checkBoxDisable = (CheckBox) findViewById(id);
                checkboxChannel(checkBoxDisable.isChecked());
                break;

            case R.id.checkbox_channel25:
                checkBoxDisable = (CheckBox) findViewById(id);
                checkboxChannel(checkBoxDisable.isChecked());
                break;

            case R.id.text_q_factor_value:
                // Generate the Number Picker dialog

                numberPickerContents = new NumberPickerContents();

                numberPickerContents.id = id;
                numberPickerContents.title = getString(R.string.q_factor);
                numberPickerContents.minValue =
                        getResources().getInteger(R.integer.q_factor_min);
                numberPickerContents.maxValue =
                        getResources().getInteger(R.integer.q_factor_max);
                numberPickerContents.startValue = numberPickerContents.minValue;

                showNumberPicker(numberPickerContents);
                break;

            case R.id.text_link_profile_value:
                // Generate the StringSelector dialog

                stringSelectorContents = new StringSelectorContents();

                stringSelectorContents.id = id;
                stringSelectorContents.title = getString(R.string.link_profile);
                stringSelectorContents.items =
                        getResources().getStringArray(R.array.strings_link_profile);

                showStringSelector(stringSelectorContents);
                break;

            case R.id.text_polarization_value:
                // Generate the StringSelector dialog

                stringSelectorContents = new StringSelectorContents();

                stringSelectorContents.id = id;
                stringSelectorContents.title = getString(R.string.polarization);
                stringSelectorContents.items =
                        getResources().getStringArray(R.array.strings_polarization);

                showStringSelector(stringSelectorContents);
                break;

            case R.id.checkbox_power_save:
                break;
            case R.id.text_rfid_trigger_mode_value:
                // Generate the StringSelector dialog

                stringSelectorContents = new StringSelectorContents();

                stringSelectorContents.id = id;
                stringSelectorContents.title = getString(R.string.trigger_mode);
                stringSelectorContents.items =
                        getResources().getStringArray(R.array.strings_rfid_trigger_mode);

                showStringSelector (stringSelectorContents);
                break;
            case R.id.checkbox_buzzer:
                break;

            case R.id.text_buzzer_volume_value:
                // Generate the StringSelector dialog

                stringSelectorContents = new StringSelectorContents();

                stringSelectorContents.id = id;
                stringSelectorContents.title = getString(R.string.buzzer_volume);
                stringSelectorContents.items =
                        getResources().getStringArray(R.array.strings_buzzer_volume);

                showStringSelector (stringSelectorContents);
                break;

            case R.id.text_trigger_mode_value:
                // Generate the StringSelector dialog

                stringSelectorContents = new StringSelectorContents();

                stringSelectorContents.id = id;
                stringSelectorContents.title = getString(R.string.trigger_mode);
                stringSelectorContents.items =
                        getResources().getStringArray(R.array.strings_trigger_mode);

                showStringSelector (stringSelectorContents);
                break;

            case R.id.checkbox_enable_all_1d_codes:
                break;

            case R.id.checkbox_enable_all_2d_codes:
                break;
        }
    }

    /**

     * Read information in scanner

     */

    private boolean loadScannerInfo() {
        // Acquire SP1 version

        try {
            String ver = super.getCommScanner().getVersion();
            TextView textView = (TextView) findViewById(R.id.text_scanner_version_value);
            textView.setText(ver.replace("Ver. ","")); // Remove the character string "Ver. “

        } catch (Exception e) {
            return false;
        }

        // Read the remaining SP1 battery

        CommBattery battery = null;
        try {
            battery = super.getCommScanner().getRemainingBattery();
        } catch (CommException e) {
            e.printStackTrace();
        }
        if(battery != null) {
            int batteryResId = 0;
            switch (battery) {
                case UNDER10:
                    batteryResId = R.mipmap.battery_1;
                    break;
                case UNDER40:
                    batteryResId = R.mipmap.battery_2;
                    break;
                case OVER40:
                    batteryResId = R.mipmap.battery_full;;
                    break;
            }
            ImageView imageView = (ImageView) findViewById(R.id.image_scanner_battery);
            imageView.setImageResource(batteryResId);
        } else {
            // showMessage(“Failed to acquire remaining battery");

            return false;
        }

        return true;
    }

    /**

     * Read setting value

     * Read setting value saved in the app from SharedPreferences and apply to each UI

     */

    private void loadSettings() {

        // Create the Map

        this.setMap();

        try {
            // Acquire the configuration value related to RFID 

            this.rfidSettings = super.getCommScanner().getRFIDScanner().getSettings();

            // Acquire the configuration value of the common parameters

            this.commParams = super.getCommScanner().getParams();

            // Acquire the configuration value related to the bar code

            this.barcodeSettings = super.getCommScanner().getBarcodeScanner().getSettings();

        } catch (CommException | RFIDException | BarcodeException e){
            this.rfidSettings = null;
            this.commParams = null;
            this.barcodeSettings = null;
            super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            e.printStackTrace();
            return;
        }

        // [Configuration value related to RFID]

        // read_power_level

        loadIntegerSetting(
                R.id.text_read_power_level_value,
                this.rfidSettings.scan.powerLevelRead);

        // session

        loadStringSetting(
                R.id.text_session_value,
                this.rfidSettings.scan.sessionFlag.toString());

        // sessionInit

        /*loadBooleanSetting(
                R.id.checkbox_session_init,
                this.rfidSettings.scan.sessionInit);*/

        // report unigue tags

        boolean unigue = (true == this.rfidSettings.scan.doubleReading.equals(DoubleReading.PREVENT1)) ? true : false;
        loadBooleanSetting(
                R.id.checkbox_report_unique_tags,
                unigue);

        // channel5 : 0x00000001

        boolean onoff = ((this.rfidSettings.scan.channel & 0x00000001) > 0) ? true : false;
        loadBooleanSetting(
                R.id.checkbox_channel5,
                onoff);

        // channel11 : 0x00000002

        onoff = ((this.rfidSettings.scan.channel & 0x00000002) > 0) ? true : false;
        loadBooleanSetting(
                R.id.checkbox_channel11,
                onoff);

        // channel17 : 0x00000004

        onoff = ((this.rfidSettings.scan.channel & 0x00000004) > 0) ? true : false;
        loadBooleanSetting(
                R.id.checkbox_channel17,
                onoff);

        // channel23 : 0x00000008

        onoff = ((this.rfidSettings.scan.channel & 0x00000008) > 0) ? true : false;
        loadBooleanSetting(
                R.id.checkbox_channel23,
                onoff);

        // channel24 : 0x00000010

        onoff = ((this.rfidSettings.scan.channel & 0x00000010) > 0) ? true : false;
        loadBooleanSetting(
                R.id.checkbox_channel24,
                onoff);

        // channel25 : 0x00000020

        onoff = ((this.rfidSettings.scan.channel & 0x00000020) > 0) ? true : false;
        loadBooleanSetting(
                R.id.checkbox_channel25,
                onoff);

        // q_factor

        loadIntegerSetting(
                R.id.text_q_factor_value,
                this.rfidSettings.scan.qParam);

        // link_profile

        loadStringSetting(
                R.id.text_link_profile_value,
                String.valueOf(this.rfidSettings.scan.linkProfile));

        // polarization

        String strPolarization = "";
        if (true == this.polarizationMap.containsValue(this.rfidSettings.scan.polarization)) {
            // Declare Iterator <Map.Entry<String, Polarization>>

            Iterator<Map.Entry<String, Polarization>> itr = this.polarizationMap.entrySet().iterator();

            // Acquire the key and value

            while(itr.hasNext()) {
                // Acquire the value using next

                Map.Entry<String, Polarization> item = itr.next();
                if (item.getValue().equals(this.rfidSettings.scan.polarization)) {
                    strPolarization = item.getKey();
                    break;
                }
            }
        }
        loadStringSetting(
                R.id.text_polarization_value,
                strPolarization);


        // power_save(RFID)

        loadBooleanSetting(
                R.id.checkbox_power_save,
                this.rfidSettings.scan.powerSave);


        // rfid trigger_mode

        String strRfid_TriggerMode = "";
        if (true == this.rfid_triggerModeMap.containsValue(this.rfidSettings.scan.triggerMode)) {
            // Declare Iterator<Map.Entry<String, TriggerMode>>

            Iterator<Map.Entry<String, RFIDScannerSettings.Scan.TriggerMode>> itr = this.rfid_triggerModeMap.entrySet().iterator();

            // Acquire the key and value

            while(itr.hasNext()) {
                // Acquire the value using next

                Map.Entry<String, RFIDScannerSettings.Scan.TriggerMode> item = itr.next();
                if (item.getValue().equals(this.rfidSettings.scan.triggerMode)) {
                    strRfid_TriggerMode = item.getKey();
                    break;
                }
            }
        }
        loadStringSetting(
                R.id.text_rfid_trigger_mode_value,
                strRfid_TriggerMode);

        // [Configuration value related to the common parameters]

        // buzzer

        onoff = (this.commParams.notification.sound.buzzer.equals(Buzzer.ENABLE)) ? true : false;
        loadBooleanSetting(
                R.id.checkbox_buzzer,
                onoff);

        // buzzer_volumes

        String strBuzzerVolume = "";
        if (true == this.buzzerVolumeMap.containsValue(this.commParams.buzzerVolume)) {
            // Declare Iterator<Map.Entry<String and BuzzerVolume>>

            Iterator<Map.Entry<String, BuzzerVolume>> itr = this.buzzerVolumeMap.entrySet().iterator();

            // Acquire the key and value

            while(itr.hasNext()) {
                // Acquire the value using next

                Map.Entry<String, BuzzerVolume> item = itr.next();
                if (item.getValue().equals(this.commParams.buzzerVolume)) {
                    strBuzzerVolume = item.getKey();
                    break;
                }
            }
        }
        loadStringSetting(
                R.id.text_buzzer_volume_value,
                strBuzzerVolume);

        // [Configuration value related to the barcode]

        // trigger_mode

        String strTriggerMode = "";
        if (true == this.triggerModeMap.containsValue(this.barcodeSettings.scan.triggerMode)) {
            // Declare Iterator<Map.Entry<String, TriggerMode>>

            Iterator<Map.Entry<String, TriggerMode>> itr = this.triggerModeMap.entrySet().iterator();

            // Acquire the key and value

            while(itr.hasNext()) {
                // Acquire the value using next

                Map.Entry<String, TriggerMode> item = itr.next();
                if (item.getValue().equals(this.barcodeSettings.scan.triggerMode)) {
                    strTriggerMode = item.getKey();
                    break;
                }
            }
        }
        loadStringSetting(
                R.id.text_trigger_mode_value,
                strTriggerMode);

        // enable_all_1d_codes

        onoff = this.checkEnable1dCodes(this.barcodeSettings);
        loadBooleanSetting(
                R.id.checkbox_enable_all_1d_codes,
                onoff);

        // enable_all_2d_codes

        onoff = this.checkEnable2dCodes(this.barcodeSettings);
        loadBooleanSetting(
                R.id.checkbox_enable_all_2d_codes,
                onoff);

    }

    /**

     * Create Map

     * @param

     */

    private void setMap(){

        // Buzzer volume Map

        this.buzzerVolumeMap.put(getResources().getString(R.string.buzzer_volume_low), BuzzerVolume.LOW);
        this.buzzerVolumeMap.put(getResources().getString(R.string.buzzer_volume_middle), BuzzerVolume.MIDDLE);
        this.buzzerVolumeMap.put(getResources().getString(R.string.buzzer_volume_loud), BuzzerVolume.LOUD);

        // Bar code trigger mode Map

        this.triggerModeMap.put(getResources().getString(R.string.trigger_mode_auto_off), TriggerMode.AUTO_OFF);
        this.triggerModeMap.put(getResources().getString(R.string.trigger_mode_momentary), TriggerMode.MOMENTARY);
        this.triggerModeMap.put(getResources().getString(R.string.trigger_mode_alternate), TriggerMode.ALTERNATE);
        this.triggerModeMap.put(getResources().getString(R.string.trigger_mode_continuous), TriggerMode.CONTINUOUS);
        this.triggerModeMap.put(getResources().getString(R.string.trigger_mode_trigger_release), TriggerMode.TRIGGER_RELEASE);

        // RFID trigger mode Map

        this.rfid_triggerModeMap.put(getResources().getString(R.string.trigger_mode_auto_off), RFIDScannerSettings.Scan.TriggerMode.AUTO_OFF);
        this.rfid_triggerModeMap.put(getResources().getString(R.string.trigger_mode_momentary), RFIDScannerSettings.Scan.TriggerMode.MOMENTARY);
        this.rfid_triggerModeMap.put(getResources().getString(R.string.trigger_mode_alternate), RFIDScannerSettings.Scan.TriggerMode.ALTERNATE);
        this.rfid_triggerModeMap.put(getResources().getString(R.string.rfid_trigger_mode_continuous1), RFIDScannerSettings.Scan.TriggerMode.CONTINUOUS1);
        this.rfid_triggerModeMap.put(getResources().getString(R.string.rfid_trigger_mode_continuous2), RFIDScannerSettings.Scan.TriggerMode.CONTINUOUS2);


        // Polarized wave setting Map

        this.polarizationMap.put(getResources().getString(R.string.polarization_vertical), Polarization.V);
        this.polarizationMap.put(getResources().getString(R.string.polarization_horizontal), Polarization.H);
        this.polarizationMap.put(getResources().getString(R.string.polarization_both), Polarization.Both);

    }

    /**

     * Enable/disable the reading of one-dimensional bar code

     * @param settings Bar code setting object

     */

    private boolean checkEnable1dCodes(BarcodeScannerSettings settings) {

        // EAN-13 UPC-A

        if (false == settings.decode.symbologies.ean13upcA.enabled) {
            return false;
        }
        // EAN-8

        if (false == settings.decode.symbologies.ean8.enabled) {
            return false;
        }
        // UPC-E

        if (false == settings.decode.symbologies.upcE.enabled) {
            return false;
        }
        // ITF

        if (false == settings.decode.symbologies.itf.enabled) {
            return false;
        }
        // STF

        if (false == settings.decode.symbologies.stf.enabled) {
            return false;
        }
        // Codabar

        if (false == settings.decode.symbologies.codabar.enabled) {
            return false;
        }
        // Code39

        if (false == settings.decode.symbologies.code39.enabled) {
            return false;
        }
        // Code93

        if (false == settings.decode.symbologies.code93.enabled) {
            return false;
        }
        // Code128

        if (false == settings.decode.symbologies.code128.enabled) {
            return false;
        }
        // GS1 Databar

        if (false == settings.decode.symbologies.gs1DataBar.enabled) {
            return false;
        }
        // GS1 Databar Limited

        if (false == settings.decode.symbologies.gs1DataBarLimited.enabled) {
            return false;
        }
        // GS1 Databar Expanded

        if (false == settings.decode.symbologies.gs1DataBarExpanded.enabled) {
            return false;
        }

        return true;
    }

    /**

     * Enable/disable the reading of two-dimensional bar code

     * @param settings Bar code setting object

     */

    private boolean checkEnable2dCodes(BarcodeScannerSettings settings) {
        // QR Code

        if (false == settings.decode.symbologies.qrCode.enabled) {
            return false;
        }
        // QR Code.Model1

        if (false == settings.decode.symbologies.qrCode.model1.enabled) {
            return false;
        }
        // QR Code.Model2

        if (false == settings.decode.symbologies.qrCode.model2.enabled) {
            return false;
        }
        // QR Code.Micro QR

        if (false == settings.decode.symbologies.microQr.enabled) {
            return false;
        }
        // iQR Code

        if (false == settings.decode.symbologies.iqrCode.enabled) {
            return false;
        }
        // iQR Code.Square

        if (false == settings.decode.symbologies.iqrCode.square.enabled) {
            return false;
        }
        // iQR Code.Rectangle

        if (false == settings.decode.symbologies.iqrCode.rectangle.enabled) {
            return false;
        }
        // Data Matrix

        if (false == settings.decode.symbologies.dataMatrix.enabled) {
            return false;
        }
        // Data Matrix.Square

        if (false == settings.decode.symbologies.dataMatrix.square.enabled) {
            return false;
        }
        // Data Matrix.Rectangle

        if (false == settings.decode.symbologies.dataMatrix.rectangle.enabled) {
            return false;
        }
        // PDF417

        if (false == settings.decode.symbologies.pdf417.enabled) {
            return false;
        }
        // Micro PDF417

        if (false == settings.decode.symbologies.microPdf417.enabled) {
            return false;
        }
        // Maxi Code

        if (false == settings.decode.symbologies.maxiCode.enabled) {
            return false;
        }
        // GS1 Composite

        if (false == settings.decode.symbologies.gs1Composite.enabled) {
            return false;
        }

        return true;
    }

    /**

     * Read integer setting value

     * Read setting value saved in the app from SharedPreferences and apply to the UI of the specified ID

     * @param id ID of View

     * @param value Setting value

     */

    private void loadIntegerSetting(int id, int value) {
        TextView textView = (TextView) findViewById(id);
        textView.setText(String.valueOf(value));
    }

    /**

     * Read string setting value

     * Read setting value saved in the app from SharedPreferences and apply to the UI of the specified ID

     * @param id ID of View

     * @param value Setting value

     */

    private void loadStringSetting(int id, String value) {
        TextView textView = (TextView) findViewById(id);
        textView.setText(value);
    }

    /**

     * Read truth setting value

     * Read setting value saved in the app from SharedPreferences and apply to the UI of the specified ID

     * @param id ID of View

     * @param value Setting value

     */

    private void loadBooleanSetting(int id, boolean value) {
        CheckBox checkBox = (CheckBox) findViewById(id);
        checkBox.setChecked(value);
        if (value && (id == R.id.checkbox_channel5 || id == R.id.checkbox_channel11 || id == R.id.checkbox_channel17
                || id == R.id.checkbox_channel23 || id == R.id.checkbox_channel24 || id == R.id.checkbox_channel25)) {
            checkboxTrue++;
            checkboxTrueID = id;
        }
        if (id == R.id.checkbox_channel25 && checkboxTrue == 1) {
            CheckBox checkBoxDisable = (CheckBox) findViewById(checkboxTrueID);
            checkBoxDisable.setEnabled(false);
        }
    }

    /**

     * Move to the upper level at the time of screen transition

     */

    private void navigateUp() {
        // Transmit and maintain SP1 configuration value

        save();
        disposeFlg = false;

        // Although there is such embedded navigation function "Up Button" in Android,

        // since it doesn't meet the requirement due to the restriction on UI, transition the the screen using button events.

        Intent intent = new Intent(getApplication(), MainActivity.class);
        startActivity(intent);

        // Stop the Activity because it becomes unnecessary since the parent Activity is returned to.

        finish();
    }

    /**

     * Get integer setting value

     * Get value of the UI of the specified ID

     * @param id ID of View

     * @return Integer setting value

     */

    private int getIntegerSetting(int id) {
        TextView textView = (TextView) findViewById(id);
        return Integer.valueOf(textView.getText().toString());
    }

    /**

     * Get string setting value

     * Get value of the UI of the specified ID

     * @param id ID of View

     * @return String setting value

     */

    private String getStringSetting(int id) {
        TextView textView = (TextView) findViewById(id);
        return textView.getText().toString();
    }

    /**

     * Save truth setting value

     * Get value of the UI of the specified ID

     * @param id ID of View

     * @return Truth setting value

     */

    private boolean getBooleanSetting(int id) {
        CheckBox checkBox = (CheckBox) findViewById(id);
         return checkBox.isChecked();
    }

    /**

     * Send RFIDScannerSettings setting value by command (Command: setSettings)

     */

    private void sendRFIDScannerSettings() throws CommException, RFIDException {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        // Set “powerLevelRead”

        this.rfidSettings.scan.powerLevelRead = this.getIntegerSetting(R.id.text_read_power_level_value);

        // Set “session”

        this.rfidSettings.scan.sessionFlag = SessionFlag.valueOf(this.getStringSetting(R.id.text_session_value));

        // Set “sessionInt”

        /*this.rfidSettings.scan.sessionInit = this.getBooleanSetting(R.id.checkbox_session_init);*/

        // Set “report unigue tags”

        DoubleReading doubleReading;
        if (true == this.getBooleanSetting(R.id.checkbox_report_unique_tags)) {
            doubleReading = DoubleReading.PREVENT1;
        } else {
            doubleReading = DoubleReading.Free;
        }
        this.rfidSettings.scan.doubleReading = doubleReading;

        // channel(channel5 ~ 25)

        long channel = 0L;
        // channel5

        if (true == this.getBooleanSetting(R.id.checkbox_channel5)) {
            channel += 1L;
        }
        // channel11

        if (true == this.getBooleanSetting(R.id.checkbox_channel11)) {
            channel += 2L;
        }
        // channel17

        if (true == this.getBooleanSetting(R.id.checkbox_channel17)) {
            channel += 4L;
        }
        // channel23

        if (true == this.getBooleanSetting(R.id.checkbox_channel23)) {
            channel += 8L;
        }
        // channel24

        if (true == this.getBooleanSetting(R.id.checkbox_channel24)) {
            channel += 16L;
        }
        // channel25

        if (true == this.getBooleanSetting(R.id.checkbox_channel25)) {
            channel += 32L;
        }
        this.rfidSettings.scan.channel = channel;

        // Set “q factor”

        this.rfidSettings.scan.qParam = (short)this.getIntegerSetting(R.id.text_q_factor_value);

        // Set “link profile”

        this.rfidSettings.scan.linkProfile = (short)this.getIntegerSetting(R.id.text_link_profile_value);

        // Set “polarization”

        if (true == polarizationMap.containsKey(this.getStringSetting(R.id.text_polarization_value))) {
            this.rfidSettings.scan.polarization = polarizationMap.get(this.getStringSetting(R.id.text_polarization_value));
        }

        // Set “powerSave” (RFID)

        this.rfidSettings.scan.powerSave = this.getBooleanSetting(R.id.checkbox_power_save);

        // Set “Trigger mode”

        if (true == rfid_triggerModeMap.containsKey(this.getStringSetting(R.id.text_rfid_trigger_mode_value))) {
            this.rfidSettings.scan.triggerMode = this.rfid_triggerModeMap.get(this.getStringSetting(R.id.text_rfid_trigger_mode_value));
        }

        // Send “SP1 setting value”

        super.getCommScanner().getRFIDScanner().setSettings(this.rfidSettings);
    }

    /**

     * Send CommScannerParams setting value by command (Command: setParams, saveParams)

     */

    private void sendCommScannerParams() throws CommException {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);



        // Set “buzzer”

        Buzzer buzzer;
        if (true == this.getBooleanSetting(R.id.checkbox_buzzer)) {
            buzzer = Buzzer.ENABLE;
        } else {
            buzzer = Buzzer.DISABLE;
        }
        this.commParams.notification.sound.buzzer = buzzer;

        // Set “buzzer Volume”

        if (true == buzzerVolumeMap.containsKey(this.getStringSetting(R.id.text_buzzer_volume_value))) {
            this.commParams.buzzerVolume = buzzerVolumeMap.get(this.getStringSetting(R.id.text_buzzer_volume_value));
        }

        // Send “SP1 setting value”

        super.getCommScanner().setParams(this.commParams);

        // Save “SP1 setting value”

        super.getCommScanner().saveParams();
    }

    /**

     * Send BarcodeScannerSettings setting value by command (Command: setSettings)

     */

    private void sendBarcodeScannerSettings() throws CommException, BarcodeException {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        // Set “Trigger mode”

        if (true == triggerModeMap.containsKey(this.getStringSetting(R.id.text_trigger_mode_value))) {
            this.barcodeSettings.scan.triggerMode = this.triggerModeMap.get(this.getStringSetting(R.id.text_trigger_mode_value));
        }

        // Set “enable_all_1d_codes”

        boolean enable1dFlg = this.getBooleanSetting(R.id.checkbox_enable_all_1d_codes);

        // Set “enable_all_2d_codes”

        boolean enable2dFlg = this.getBooleanSetting(R.id.checkbox_enable_all_2d_codes);

        // Send “SP1 setting value”

        this.setEnable1dCodes(this.barcodeSettings, enable1dFlg);
        this.setEnable2dCodes(this.barcodeSettings, enable2dFlg);
        super.getCommScanner().getBarcodeScanner().setSettings(this.barcodeSettings);
    }

    /**

     * Enable/disable the reading of one-dimensional bar code

     * @param settings Bar code setting object

     * @param enable1dFlg Enable/disable

     */

    private void setEnable1dCodes(BarcodeScannerSettings settings, boolean enable1dFlg) {
        // Always allow EAN code regardless if checked or not.

        settings.decode.symbologies.ean13upcA.enabled = true; // EAN-13 UPC-A

        settings.decode.symbologies.ean8.enabled = true; // EAN-8

        settings.decode.symbologies.upcE.enabled = enable1dFlg; // UPC-E

        settings.decode.symbologies.itf.enabled = enable1dFlg; // ITF

        settings.decode.symbologies.stf.enabled = enable1dFlg; // STF

        settings.decode.symbologies.codabar.enabled = enable1dFlg; // Codabar

        settings.decode.symbologies.code39.enabled = enable1dFlg; // Code39

        settings.decode.symbologies.code93.enabled = enable1dFlg; // Code93

        settings.decode.symbologies.code128.enabled = enable1dFlg; // Code128

        settings.decode.symbologies.msi.enabled = enable1dFlg; // MSI

        settings.decode.symbologies.gs1DataBar.enabled = enable1dFlg; // GS1 Databar

        settings.decode.symbologies.gs1DataBarLimited.enabled = enable1dFlg; // GS1 Databar Limited

        settings.decode.symbologies.gs1DataBarExpanded.enabled = enable1dFlg; // GS1 Databar Expanded

    }

    /**

     * Enable/disable the reading of two-dimensional bar code

     * @param settings Bar code setting object

     * @param enable2dFlg Enable/disable

     */

    private void setEnable2dCodes(BarcodeScannerSettings settings, boolean enable2dFlg) {
        settings.decode.symbologies.qrCode.enabled = enable2dFlg;   // QR Code

        settings.decode.symbologies.qrCode.model1.enabled = enable2dFlg;    // QR Code.Model1

        settings.decode.symbologies.qrCode.model2.enabled = enable2dFlg;    // QR Code.Model2

        settings.decode.symbologies.microQr.enabled = enable2dFlg; // QR Code.Micro QR

        settings.decode.symbologies.iqrCode.enabled = enable2dFlg; // iQR Code

        settings.decode.symbologies.iqrCode.square.enabled = enable2dFlg; // iQR Code.Square

        settings.decode.symbologies.iqrCode.rectangle.enabled = enable2dFlg; // iQR Code.Rectangle

        settings.decode.symbologies.dataMatrix.enabled = enable2dFlg; // Data Matrix

        settings.decode.symbologies.dataMatrix.square.enabled = enable2dFlg; // Data Matrix.Square

        settings.decode.symbologies.dataMatrix.rectangle.enabled = enable2dFlg; // Data Matrix.Rectangle

        settings.decode.symbologies.pdf417.enabled = enable2dFlg; // PDF417

        settings.decode.symbologies.microPdf417.enabled = enable2dFlg; // Micro PDF417

        settings.decode.symbologies.maxiCode.enabled = enable2dFlg; // Maxi Code

        settings.decode.symbologies.gs1Composite.enabled = enable2dFlg; // GS1 Composite

        settings.decode.symbologies.plessey.enabled = enable2dFlg;  // Plessey

        settings.decode.symbologies.aztec.enabled = enable2dFlg; // Aztec

    }

    /**

     * Display NumberPicker dialog

     * @param contents The contents displayed in the dialog

     */

    private void showNumberPicker(NumberPickerContents contents) {
        NumberPickerFragment fragment = new NumberPickerFragment();

        NumberPickerListener listener = new NumberPickerListener();
        listener.activity = this;
        listener.id = contents.id;

        fragment.title = contents.title;
        fragment.minValue = contents.minValue;
        fragment.maxValue = contents.maxValue;
        fragment.startValue = contents.startValue;
        fragment.listener = listener;

        fragment.show(getFragmentManager(), getString(R.string.fragment_anonymous));
    }

    /**

     * Display StringSelector dialog

     * @param contents The contents displayed in the dialog

     */

    private void showStringSelector(StringSelectorContents contents) {
        StringSelectorFragment fragment = new StringSelectorFragment();

        StringSelectorListener listener = new StringSelectorListener();
        listener.activity = this;
        listener.id = contents.id;

        fragment.title = contents.title;
        fragment.items = contents.items;
        fragment.listener = listener;

        fragment.show(getFragmentManager(), getString(R.string.fragment_anonymous));
    }

    /**

     * Contents for generating NumberPicker dialog

     */

    private static class NumberPickerContents {
        int id;
        int prefResId = -1;
        String title;
        int minValue;
        int maxValue;
        int startValue;
    }

    /**

     * Listener of NumberPicker dialog

     */

    private static class NumberPickerListener implements NumberPickerFragment.PickListener {
        Activity activity;  // Activity to search View and get Preferences

        int id;             // ID of View


        @Override
        public void onPicked(int pickedValue) {
            // Display the value selected in text.

            TextView textView = (TextView) activity.findViewById(id);
            textView.setText(String.valueOf(pickedValue));
        }
    }

    /**

     * Contents for generating StringSelector dialog

     */

    public static class StringSelectorContents {
        int id;
        String title;
        String[] items;
    }

    /**

     * Listener of StringSelector dialog

     */

    public static class StringSelectorListener implements StringSelectorFragment.SelectListener {
        Activity activity;  // Activity to search View and get Preferences

        int id;             // ID of View


        @Override
        public void onSelected(int index, String selectedItem) {
            // Display the value selected in text.

            TextView textView = (TextView) activity.findViewById(id);
            textView.setText(selectedItem);
        }
    }

    /**

     * Send/save SP1 setting value

     */

    private void save() {
        // Stop processing in case of there was no connection with SP1 when transit to scren “Setting”.

        if (!scannerConnectedOnCreate) {
            return;
        }

        // Transmit and maintain SP1 configuration value

        boolean failedSettings = this.rfidSettings == null || this.commParams == null || this.barcodeSettings == null;
        Exception exception = null;
        if (!failedSettings) {
            try {
                // Display “toast” to indicate “in process of setting” when connected to scanner. 

                // Can not display message in UI thread due to send method is working. 

                // Create another thread and display message toast there.

                // Consider the case can not write a setting even when it is connected to determine it is able to read a setting or not.

                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        Looper.prepare();
                        showMessage(getString(R.string.I_MSG_PROGRESS_SETTING));
                        Looper.loop();
                    }
                });
                thread.start();

                // Send a setting value of RFIDScannerSettings.

                sendRFIDScannerSettings();

                // Send a setting value of CommScannerParams.

                sendCommScannerParams();

                // Send a setting value of BarcodeScannerSettings.

                sendBarcodeScannerSettings();
            } catch (CommException | RFIDException | BarcodeException e) {
                failedSettings = true;
                exception = e;
            }
        }
        if (failedSettings) {
            super.showMessage(getString(R.string.E_MSG_SAVE_SETTINGS));
            if (exception != null) {
                exception.printStackTrace();
            }
            return;
        }

        // Exit screen after waiting since it takes time to send and receive SP1 command.

        try {
            Thread.sleep(1500);
        } catch (Exception e) {
        }

        // Message “save complete”

        if (this.commParams != null) {
            super.showMessage(getString(R.string.I_MSG_SAVE_SETTINGS));
        }
    }
    
    private void setChannel() {
        ConstraintLayout channel4 = (ConstraintLayout) findViewById(R.id.channel4);
        ConstraintLayout channel5 = (ConstraintLayout) findViewById(R.id.channel5);
        ConstraintLayout channel6 = (ConstraintLayout) findViewById(R.id.channel6);
    
        TextView txtChannel1 = (TextView) findViewById(R.id.text_channel5);
        TextView txtChannel2 = (TextView) findViewById(R.id.text_channel11);
        TextView txtChannel3 = (TextView) findViewById(R.id.text_channel17);
        TextView txtChannel4 = (TextView) findViewById(R.id.text_channel23);
        TextView txtChannel5 = (TextView) findViewById(R.id.text_channel24);
        
        String region = commScanner.getRegion();
        switch (region) {
            case "JP":
                break;
                
            case "IL":
                channel6.setVisibility(View.GONE);
                txtChannel1.setText(getString(R.string.channel1));
                txtChannel2.setText(getString(R.string.channel2));
                txtChannel3.setText(getString(R.string.channel3));
                txtChannel4.setText(getString(R.string.channel4));
                txtChannel5.setText(getString(R.string.channel5));
                break;
                
            case "EU":
                channel5.setVisibility(View.GONE);
                channel6.setVisibility(View.GONE);
                txtChannel1.setText(getString(R.string.channel4));
                txtChannel2.setText(getString(R.string.channel7));
                txtChannel3.setText(getString(R.string.channel10));
                txtChannel4.setText(getString(R.string.channel13));
                break;
                
            case "IN":
                channel4.setVisibility(View.GONE);
                channel5.setVisibility(View.GONE);
                channel6.setVisibility(View.GONE);
                txtChannel1.setText(getString(R.string.channel1));
                txtChannel2.setText(getString(R.string.channel2));
                txtChannel3.setText(getString(R.string.channel3));
                break;
                
            default:
                ConstraintLayout tableChannel = (ConstraintLayout) findViewById(R.id.tableChannel);
                tableChannel.setVisibility(View.GONE);
                break;
                
        }
    }
    
    private void checkboxChannel(boolean cbState) {
        CheckBox checkBoxDisable = null;
        if (checkboxTrue == 1 && cbState) {
            checkBoxDisable = (CheckBox) findViewById(checkboxTrueID);
            checkBoxDisable.setEnabled(true);
            checkboxTrue++;
        } else if (checkboxTrue != 1 && !cbState) {
            checkboxTrue--;
        } else {
            checkboxTrue++;
        }
        if (checkboxTrue == 1) {
            if (this.getBooleanSetting(R.id.checkbox_channel5)) {
                checkboxTrueID = R.id.checkbox_channel5;
            }
            if (this.getBooleanSetting(R.id.checkbox_channel11)) {
                checkboxTrueID = R.id.checkbox_channel11;
            }
            if (this.getBooleanSetting(R.id.checkbox_channel17)) {
                checkboxTrueID = R.id.checkbox_channel17;
            }
            if (this.getBooleanSetting(R.id.checkbox_channel23)) {
                checkboxTrueID = R.id.checkbox_channel23;
            }
            if (this.getBooleanSetting(R.id.checkbox_channel24)) {
                checkboxTrueID = R.id.checkbox_channel24;
            }
            if (this.getBooleanSetting(R.id.checkbox_channel25)) {
                checkboxTrueID = R.id.checkbox_channel25;
            }
            checkBoxDisable = (CheckBox) findViewById(checkboxTrueID);
            checkBoxDisable.setEnabled(false);
        }
    }
}