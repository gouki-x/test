package densowave.com.DensoScannerSDK_demo;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.densowave.scannersdk.Common.CommException;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Dto.CommScannerParams;
import com.densowave.scannersdk.Dto.RFIDScannerSettings;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;
import com.densowave.scannersdk.RFID.RFIDException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocateTagActivity extends BaseActivity implements RFIDDataDelegate {

    // region Property

    // The tags UII used for searching is kept static so that they are retained during the execution of the application.
    // Set and get this variable using getter/setter method.
    private static TagUII _searchTagUII = null;

    private Integer readPowerLevelOnReadSearchTag = null;

    private Float readPowerLevelOnSearch = null;
    private ReadPowerStage readPowerStageOnSearch = null;

    private MatchingMethod matchingMethod = MatchingMethod.FORWARD;

    private LocateTagState locateTagState = LocateTagState.STANDBY;

    private Handler readHandler = new Handler();
    private Handler messageHandler = new Handler();
    private RFIDScannerSettings.Scan.Polarization settingPolarization;
    private RFIDScannerSettings.Scan.SessionFlag settingSesionFlag;
    private int settingReadPowLevel = 0;
    Context context;
    // Whether it is connected to the scanner during generating time
    // Even when the connection is lost while on this screen, if it was connected to scanner during generating time, display the communication error
    private boolean scannerConnectedOnCreate = false;

    private boolean disposeFlg = true;

    ArrayList<Short> array_rssi = new ArrayList<Short>();
    Button searchTagToggle;
    CommScannerParams commParams;
    CommScannerParams.Notification.Sound.Buzzer buzzer_setting;


    // endregion

    // region Activity relation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_tag);

        scannerConnectedOnCreate = super.isCommScanner();

        // When SP1 is not found, display the error message.
        if (!scannerConnectedOnCreate) {
            super.showMessage(getString(R.string.E_MSG_NO_CONNECTION));
        }
        searchTagToggle = (Button) findViewById(R.id.button_search_tag_toggle);
        // Set up the UI.
        setupReadPowerLevelSpinner();

        // Reflect the tags UII used for searching which were saved last time
        loadSearchTagUII();
    
        // Since the UI has already been entered, Search button is enabled
        if (_searchTagUII != null) {
            // Enable UI operation in standby
            setEnableInteractiveUIWithoutNavigatorAndSearchTag(true);
            setEnableSearchTag(true);
        }

        // Register the listener
        if (scannerConnectedOnCreate) {
            try {
                super.getCommScanner().getRFIDScanner().setDataDelegate(this);
            } catch (Exception e) {
                super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            }
        }

        try {
            commParams = super.getCommScanner().getParams();
            buzzer_setting = commParams.notification.sound.buzzer;
        }catch (Exception e) {
            super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
        }
        saveSetting();
        context = this;
        // Service is started in the back ground.
        super.startService();
    }

    @Override
    protected void onDestroy() {
        BeepAudioTracks.stopAudioTracks();
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

    @Override
    public void onUserLeaveHint() {
        // Stop the reading and searching tags while a process is running in the background.
        runLocateTagAction(LocateTagAction.STOP);

        if (scannerConnectedOnCreate && locateTagState != LocateTagState.STANDBY) {
            disposeFlg = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Transition the screen after reading and searching tags are stopped.
                runLocateTagAction(LocateTagAction.STOP);

                // Transition the screen after the listener is released for registration.
                if (scannerConnectedOnCreate) {
                    super.getCommScanner().getRFIDScanner().setDataDelegate(null);
                }

                if(commParams != null){
                    commParams.notification.sound.buzzer = buzzer_setting;
                    setting();
                    try {
                        super.getCommScanner().setParams(commParams);
                    }catch (Exception e) {
                        super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
                    }
                }

                disposeFlg = false;

                finish();
                return true;
        }
        return false;
    }

    /**

     * Move to the upper level at the time of screen transition

     */

    private void navigateUp() {
        // Transition the screen after reading and searching tags are stopped.
        runLocateTagAction(LocateTagAction.STOP);

        // Transition the screen after the listener is released for registration.
        if (scannerConnectedOnCreate) {
            super.getCommScanner().getRFIDScanner().setDataDelegate(null);
        }

        if(commParams != null){

            commParams.notification.sound.buzzer = buzzer_setting;
            setting();
            try {
                super.getCommScanner().setParams(commParams);
            }catch (Exception e) {
                super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            }
        }

        disposeFlg = false;

        // Although there is such embedded navigation function "Up Button" in Android,
        // since it doesn't meet the requirement due to the restriction on UI, transition the the screen using button events.
        Intent intent = new Intent(getApplication(), MainActivity.class);
        startActivity(intent);

        // Stop the Activity because it becomes unnecessary since the parent Activity is returned to.
        finish();
    }

    // endregion

    // region Handle click event

    /**
     * Processing when clicking
     * All the touch events in Activity are controlled by this
     * @param view Clicked View
     */
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.button_navigate_up:
                navigateUp();
                break;
            case R.id.button_read_search_tag:
                runLocateTagAction(LocateTagAction.READ_SEARCH_TAG);
                break;
            case R.id.text_search_tag_uii_value:
                editTagUII();
                break;
            case R.id.radio_button_forward_match:
                matchingMethod = MatchingMethod.FORWARD;
                break;
            case R.id.radio_button_backward_match:
                matchingMethod = MatchingMethod.BACKWARD;
                break;
            case R.id.button_search_tag_toggle:
                LocateTagAction action = locateTagState == LocateTagState.STANDBY ?
                        LocateTagAction.SEARCH_TAG : LocateTagAction.STOP;
                runLocateTagAction(action);
                break;
        }
    }

    // endregion

    // region Handle received RFID event

    /**
     * Processing when receiving data
     *
     * @param rfidDataReceivedEvent Reception event
     */
    @Override
    public void onRFIDDataReceived(CommScanner scanner, final RFIDDataReceivedEvent rfidDataReceivedEvent) {
        // Control between threads
        readHandler.post(new Runnable() {
            @Override
            public void run() {
               if (locateTagState == LocateTagState.READING_SEARCH_TAG) {
                   readDataOnReadSearchTag(rfidDataReceivedEvent);
               }else if (locateTagState == LocateTagState.SEARCHING_TAG) {
                   readDataOnSearch(rfidDataReceivedEvent);
               }
            }
        });
    }

    /**
     * Data reception processing when reading the data for the search
     *
     * @param event RFID reception event
     */
    private void readDataOnReadSearchTag(final RFIDDataReceivedEvent event) {
        // When two or more tags are read, set the UII of the first tag.
        List<RFIDData> dataList = event.getRFIDData();
        RFIDData firstData = dataList.get(0);
        try {
            setSearchTagUII(TagUII.valueOf(firstData.getUII(), context));
        } catch (OverflowBitException e) {
            e.printStackTrace();
        }

        // Finish reading immediately after reading once
        stopReadSearchTag();
    }

    /**
     * Data reception processing when searching
     *
     * @param event RFID reception event
     */
    private void readDataOnSearch(final RFIDDataReceivedEvent event) {
        // Detect the data corresponding to the UII of search tag.

        List<RFIDData> dataList = event.getRFIDData();

        for (int i = 0; i < dataList.size(); i++) {
            RFIDData data = dataList.get(i);

            if(Arrays.equals(data.getUII(), getSearchTagUII().getBytes())) {
                ///if (checkMatchSearchTagUIIHexString(data.getUII())) {
                short rssi = (short) data.getRSSI();
                array_rssi.add(rssi);
                //Log.d("LocateTag rssi", "rssi=" + rssi);
                //Log.d("LocateTag polarization", "Polarization=" + data.getPolarization());


                //}
            }else{
                Log.d("LocateTag", getString(R.string.I_READ_UNSPECIFIED_UII));
            }
        }



        // Calculate the output value from RSSI and reflect to the display and the sound.
        // (Read output value [dBm])  =  (RSSI) / 10
        // Include RSSI to Short so that the value of 0x8000 or more becomes a negative value.
        // get average


        // Do nothing if there is no data that corresponds to the UII of the search tag.
        //if (array_rssi.size() == 0) {
         // return;
        //}

        // 1.5 seconds after reading, set the display to the initial state.
        // When the following reading is performed within 1.5 seconds, call removeCallbacksAndMessages and cancel.
        //messageHandler.removeCallbacksAndMessages(null);
        //setReadPowerLevelOnSearch(readPowerLevel);

    }

     Runnable r = new Runnable() {
         @Override
         public void run() {
             float readPowerLevel = 0;
             if(array_rssi.size() == 0) {
                 setReadPowerLevelOnSearch(null);
             }else {
                 for (int i = 0; i < array_rssi.size(); i++) {
                     readPowerLevel = readPowerLevel + array_rssi.get(i);
                 }
                 readPowerLevel = readPowerLevel / array_rssi.size() / 10.0f;
                 setReadPowerLevelOnSearch(readPowerLevel);
                 array_rssi.clear();

             }
             messageHandler.postDelayed(this,250);
         }
     };


    /**
     * Returns whether the UII hexadecimal string of the search tag matches that of the search target tag
     * Even if it does not match as a byte array, it matches if it matches as a string
     *
     * Example: UII of search tag: "A12" UII of search target tag: "A 123" Matching method: In case of matching from the beginning,
     * If matching by byte array, the UII of the search tag does not match [0A, 12] and the UII of the search target tag does not match [A1, 23],
     * But since it is a matching by string, it is assumed that the part "A12" at the head matches and therefore coincides
     *
     * @param targetTagUII  Search tag UII
     * @return Return true if the UII hexadecimal string of the search tag matches that of the search target tag, and false if it does not match
     */
    private boolean checkMatchSearchTagUIIHexString(byte[] targetTagUII) {
        // It is assumed to always be relevant since matching is not required when there is no UII of tag used for searching
        if (getSearchTagUII() == null) {
            return true;
        }

        // It is never relevant if the UII of the tags used for searching is longer than the UII to be searched.
        TagUII searchTagUII = getSearchTagUII();
        if (searchTagUII.getBytes().length > targetTagUII.length) {
            return false;
        }

        // It is assumed to relevant if the search string is at the beginning or the end of the string to be searched.
        String searchString = searchTagUII.getHexString();
        String targetString = bytesToHexString(targetTagUII);
        switch (matchingMethod) {
            case FORWARD:
                int searchStringFirstIndex = targetString.indexOf(searchString);
                return searchStringFirstIndex == 0;
            case BACKWARD:      // The search string is at the end of the string to be searched
                int searchStringLastIndex = targetString.lastIndexOf(searchString);
                return searchStringLastIndex == targetString.length() - searchString.length();
            default:
                return false;
        }
    }

    // endregion

    // region Interactive(UI and sound) action

    /**
     * Set up the spinner to select Read output value
     */
    private void setupReadPowerLevelSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.spinner_power_level_value_on_read_search_tag);

        // Set the list of options
        final int[] intElement = getResources().getIntArray(R.array.locate_tag_read_power_levels);
        final Integer[] elements = new Integer[intElement.length];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = intElement[i];
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                this, R.layout.support_simple_spinner_dropdown_item, elements);
        spinner.setAdapter(adapter);

        // Set the initial state to the minimum value
        readPowerLevelOnReadSearchTag = elements[0];
        spinner.setSelection(0);

        // To select, set the Read output value
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                readPowerLevelOnReadSearchTag = elements[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    /**
     * Edit UII
     */
    private void editTagUII() {
        StringInputContents contents = new StringInputContents();
        contents.title = getString(R.string.uii);
        contents.startString = getSearchTagUII() != null ? getSearchTagUII().getHexString() : "";
        contents.inputListener = new StringInputFragment.InputListener() {
            @Override
            public void onInput(String string) {
                // Set to blank for the null character string
                if (string.isEmpty()) {
                    searchTagToggle.setEnabled(false);
                    setSearchTagUII(null);
                    return;
                }

                // Verify the numerical values
                TagUII uii;
                try {
                    uii = TagUII.valueOf(string, context);
                } catch (NotHexException e) {
                    showMessage(getString(R.string.E_MSG_FILTER_INVALID_PATTERN));
                    return;
                } catch (OverflowBitException e) {
                    showMessage(getString(R.string.E_MSG_FILTER_OUT_OF_RANGE_PATTERN));
                    return;
                }
                searchTagToggle.setEnabled(true);
                // Perform the setting since the numerical value is a normal value
                setSearchTagUII(uii);
            }
        };
        showUpperAlphaInput(contents);
    }

    /**
     * Execute action after finding the tag
     * @param action The action after finding the tag
     */
    private void runLocateTagAction(LocateTagAction action) {
        switch (action) {
            case READ_SEARCH_TAG:
                if (locateTagState == LocateTagState.STANDBY) {
                    startReadSearchTag();
                }
                break;
            case SEARCH_TAG:
                if (locateTagState == LocateTagState.STANDBY) {
                    startSearchTag();
                }
                break;
            case STOP:
                if (locateTagState == LocateTagState.READING_SEARCH_TAG) {
                    stopReadSearchTag();
                } else if (locateTagState == LocateTagState.SEARCHING_TAG) {
                    stopSearchTag();
                }
                break;
        }
    }
    
    /**
     * Save setting Polarization, when destroy this activity resetting Polarization
     */
    private void saveSetting() {
        // Acquire only the configuration values from the existing ones
        try {
            RFIDScannerSettings settings = super.getCommScanner().getRFIDScanner().getSettings();
            if (settingPolarization == null && settingReadPowLevel == 0 && settingSesionFlag == null) {
                settingPolarization = settings.scan.polarization;
                settingReadPowLevel = settings.scan.powerLevelRead;
                settingSesionFlag = settings.scan.sessionFlag;
            }
        } catch (RFIDException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Save setting Polarization, when destroy this activity resetting Polarization
     */
    private void setting() {
        // Acquire only the configuration values from the existing ones
        try {
            RFIDScannerSettings settings = super.getCommScanner().getRFIDScanner().getSettings();
            settings.scan.polarization = settingPolarization;
            settings.scan.powerLevelRead = settingReadPowLevel;
            settings.scan.sessionFlag = settingSesionFlag;
            super.getCommScanner().getRFIDScanner().setSettings(settings);
        } catch (RFIDException e) {
            e.printStackTrace();
        }
    }
    

    /**
     * Start reading the search tag
     */
    private void startReadSearchTag() {
        locateTagState = LocateTagState.READING_SEARCH_TAG;

        // Disable UI operation other than the navigator while reading the tags used for searching
        setEnableInteractiveUIWithoutNavigatorAndSearchTag(false);
        setEnableSearchTag(false);

        // Set to the configured Read output value and open the inventory
        try {
            setScannerSettings(readPowerLevelOnReadSearchTag, RFIDScannerSettings.Scan.SessionFlag.S0, false);
            openScannerInventory();
        } catch (CommException | RFIDException e) {
            super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            e.printStackTrace();
        }
    }

    /**
     * Stop reading the search tag
     */
    private void stopReadSearchTag() {
        // Close the inventory
        try {
            closeScannerInventory();
        } catch (CommException | RFIDException e) {
            super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            e.printStackTrace();
        }

        // Enable UI operation in standby
        setEnableInteractiveUIWithoutNavigatorAndSearchTag(true);
        setEnableSearchTag(true);

        locateTagState = LocateTagState.STANDBY;
    }

    /**
     * Start searching tag
     */
    private void startSearchTag() {
        locateTagState = LocateTagState.SEARCHING_TAG;

        // Disable the UI operation other than Navigator and Stop button (tag searching switch button) during tag searching
        setEnableInteractiveUIWithoutNavigatorAndSearchTag(false);

        // Set the name of the action to be stopped to the tag searching switch button
        searchTagToggle.setText(LocateTagAction.STOP.toResourceString(getResources()));

        // Set the defined Read output value and session S0 and open the inventory
        //int searchReadPowerLevel = getResources().getInteger(R.integer.read_power_level_on_search_tag);
        int searchReadPowerLevel = 30;
        RFIDScannerSettings.Scan.SessionFlag searchSessionFlag =
                RFIDScannerSettings.Scan.SessionFlag.S0;

        TagUII uii;
        String pass = "00000000";
        short addr = 0;

        array_rssi.clear();

        uii = getSearchTagUII();

        try {
            setScannerSettings(searchReadPowerLevel, searchSessionFlag, false);
            //openScannerInventory();

            commParams.notification.sound.buzzer = CommScannerParams.Notification.Sound.Buzzer.DISABLE;

            try {
                super.getCommScanner().setParams(commParams);
            }catch (Exception e) {
                super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            }

            super.getCommScanner().getRFIDScanner().openRead(RFIDScannerSettings.RFIDBank.UII, addr, (short)uii.getBytes().length, stringToByte(pass), uii.getBytes());



        } catch (CommException | RFIDException e) {
            super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            e.printStackTrace();
        }

        messageHandler.post(r);

    }

    /**
     * Stop searching tag
     */
    private void stopSearchTag() {
        // Close the inventory
        try {
            closeScannerInventory();
        } catch (CommException | RFIDException e) {
            super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            e.printStackTrace();
        }

        // Enable UI operation in standby
        setEnableInteractiveUIWithoutNavigatorAndSearchTag(true);

        // Set the name of the tag searching action to the tag searching switch button
        Button searchTagToggle = (Button) findViewById(R.id.button_search_tag_toggle);
        searchTagToggle.setText(LocateTagAction.SEARCH_TAG.toResourceString(getResources()));

        // Eliminate the sound and the display of the Read output value
        setReadPowerLevelOnSearch(null);

        locateTagState = LocateTagState.STANDBY;

        messageHandler.removeCallbacks(r);
    }

    /**
     * Open inventory of scanner
     * @throws CommException Exception concerning CommScanner
     * @throws RFIDException Exception concerning RFIDScanner
     */
    private void openScannerInventory() throws CommException, RFIDException {
        if (!scannerConnectedOnCreate) {
            return;
        }

        super.getCommScanner().getRFIDScanner().openInventory();
    }

    /**
     * Close inventory of scanner
     * @throws CommException Exception concerning CommScanner
     * @throws RFIDException Exception concerning RFIDScanner
     */
    private void closeScannerInventory() throws CommException, RFIDException {
        if (!scannerConnectedOnCreate) {
            return;
        }

        super.getCommScanner().getRFIDScanner().close();
    }

    /**
     * Set up the scanner
     * @param readPowerLevel  Read output value
     * @param sessionFlag  Session flag  Do not set the session flag if null is specified
     * @throws CommException Exception concerning CommScanner
     * @throws RFIDException Exception concerning RFIDScanner
     */
    private void setScannerSettings(int readPowerLevel, @Nullable RFIDScannerSettings.Scan.SessionFlag sessionFlag, @Nullable Boolean sessionInit) throws CommException, RFIDException {
        if (!scannerConnectedOnCreate) {
            return;
        }

        // Acquire only the configuration values from the existing ones
        RFIDScannerSettings settings = super.getCommScanner().getRFIDScanner().getSettings();

        // Set the value
        settings.scan.powerLevelRead = readPowerLevel;

        // Set Polarization to Horizon
        settings.scan.polarization = RFIDScannerSettings.Scan.Polarization.H;

        if (sessionFlag != null) {
            settings.scan.sessionFlag = sessionFlag;
        }

        if (sessionInit != null){
            settings.scan.sessionInit = sessionInit;
        }


        super.getCommScanner().getRFIDScanner().setSettings(settings);
    }

    /**
     * Set enable/disable the UI operations other than navigator and tag search switching button
     * @param enabled Specify true if it is enabled, false if it is disabled
     */
    private void setEnableInteractiveUIWithoutNavigatorAndSearchTag(boolean enabled) {
        // Disable/Enable the relevant UI
        Spinner readPowerLevelSpinnerOnReadSearchTag = (Spinner) findViewById(R.id.spinner_power_level_value_on_read_search_tag);
        Button readSearchTagButton = (Button) findViewById(R.id.button_read_search_tag);
        TextView searchTagUIIValueTextView = (TextView) findViewById(R.id.text_search_tag_uii_value);
        RadioButton forwardMatchRadioButton = (RadioButton) findViewById(R.id.radio_button_forward_match);
        RadioButton backwardMatchRadioButton = (RadioButton) findViewById(R.id.radio_button_backward_match);
        View[] interactiveViews = {
                readPowerLevelSpinnerOnReadSearchTag, readSearchTagButton, searchTagUIIValueTextView,
                forwardMatchRadioButton, backwardMatchRadioButton
        };
        for (View interactiveView : interactiveViews) {
            interactiveView.setEnabled(enabled);
        }

        // Display default text such that the basic color becomes enabled/disabled
        int colorIdBasedDefault = enabled ? R.color.text_default : R.color.text_default_disabled;
        int colorBasedDefault = getColor(colorIdBasedDefault);
        TextView[] textViews = {
                readSearchTagButton, searchTagUIIValueTextView,
                forwardMatchRadioButton, backwardMatchRadioButton
        };
        for (TextView textView : textViews) {
            textView.setTextColor(colorBasedDefault);
        }

        // Display highlighted text such that the basic color becomes enabled/disabled
        int colorIdBasedEnhanced =  enabled ? R.color.text_default_enhanced : R.color.text_default_disabled;
        int colorBasedEnhanced = getColor(colorIdBasedEnhanced);
        TextView tagUIITextHeadView = (TextView) findViewById(R.id.text_search_tag_uii_head);
        tagUIITextHeadView.setTextColor(colorBasedEnhanced);
    }

    /**
     * Set enable/disable the tag search switching button
     * @param enabled Specify true if it is enabled, false if it is disabled
     */
    private void setEnableSearchTag(boolean enabled) {
        Button searchTagToggle = (Button) findViewById(R.id.button_search_tag_toggle);
        searchTagToggle.setEnabled(enabled);
        int colorId = enabled ? R.color.text_default : R.color.text_default_disabled;
        searchTagToggle.setTextColor(getColor(colorId));
    }

    /**
     * Set the display and the sound of the Read output value when searching
     * @param readPowerLevel  Read output value when searching
     */
    private void setReadPowerLevelOnSearch(@Nullable Float readPowerLevel) {
        // If the Read output value is also null last time, do nothing
        if (readPowerLevelOnSearch == null && readPowerLevel == null) {
            return;
        }
        readPowerLevelOnSearch = readPowerLevel;

        // Reflect the changed Read output value on the display
        String text = readPowerLevel != null ? String.format("%.2f", readPowerLevel) : "0.0";
        TextView textView = (TextView) findViewById(R.id.text_read_power_value_on_search);
        textView.setText(text);

        // If there is no change in the level of Read output value, do nothing
        ReadPowerStage readPowerStage = getReadPowerStage(readPowerLevel);
        if (readPowerStageOnSearch == readPowerStage) {
            return;
        }
        readPowerStageOnSearch = readPowerStage;

        // Reflects the stage of Read output value after change in Yen and sound
        setSearchCircle(readPowerStage);
        setSearchSound(readPowerStage);
    }

    /**
     * Set display of circle according to the stage of Read output value
     * @param readPowerStage The stage of the Read output value that will be the basis of the circular display  Do not display if null is specified
     */
    private void setSearchCircle(@Nullable ReadPowerStage readPowerStage) {
        // Do not display if the stage of Read output value is null
        ImageView imageView = (ImageView) findViewById(R.id.image_search_circle);
        if (readPowerStage != null) {
            imageView.setVisibility(ImageView.VISIBLE);
        } else {
            imageView.setVisibility(ImageView.INVISIBLE);
            return;
        }

        // Configure the display of Yen according to the stage of Read output value
        int circleResId;
        switch (readPowerStage) {
            case STAGE_1:
                circleResId = R.mipmap.locate_tag_circle_over_35;
                break;
            case STAGE_2:
                circleResId = R.mipmap.locate_tag_circle_48_to_36;
                break;
            case STAGE_3:
                circleResId = R.mipmap.locate_tag_circle_62_to_49;
                break;
            case STAGE_4:
                circleResId = R.mipmap.locate_tag_circle_74_to_63;
                break;
            case STAGE_5:
                circleResId = R.mipmap.locate_tag_circle_under_75;
                break;
            default:
                return;
        }
        imageView.setImageResource(circleResId);
    }

    /**
     * Seting the sound according to the stage of Read output value
     * @param readPowerStage  The stage of Read output value  Do not playback if null is specified
     */
    private void setSearchSound(@Nullable ReadPowerStage readPowerStage) {
        // Stop audio track if the stage of Read output value is null
        if (readPowerStage == null) {
            BeepAudioTracks.stopAudioTracks();
            return;
        }

        // Request the name of the audio track to play
        BeepAudioTracks.AudioTrackName playAudioTrackName;
        switch (readPowerStage) {
            case STAGE_1:
                playAudioTrackName = BeepAudioTracks.AudioTrackName.TRACK_1;
                break;
            case STAGE_2:
                playAudioTrackName = BeepAudioTracks.AudioTrackName.TRACK_2;
                break;
            case STAGE_3:
                playAudioTrackName = BeepAudioTracks.AudioTrackName.TRACK_3;
                break;
            case STAGE_4:
                playAudioTrackName = BeepAudioTracks.AudioTrackName.TRACK_4;
                break;
            case STAGE_5:
                playAudioTrackName = BeepAudioTracks.AudioTrackName.TRACK_5;
                break;
            default:
                return;
        }

        // Terminate the the audio tracks which are not played
        BeepAudioTracks.AudioTrackName[] audioTrackNames = BeepAudioTracks.AudioTrackName.values();
        for (BeepAudioTracks.AudioTrackName audioTrackName : audioTrackNames) {
            if (playAudioTrackName != audioTrackName) {
                BeepAudioTracks.stop(audioTrackName);
            }
        }

        // Play the relevant the audio track
        BeepAudioTracks.play(playAudioTrackName);
    }

    // endregion

    // region Set/Get/Load search tag UII with UI

    /**
     * Set UII of search tag
     * Reflect it in the corresponding TextView and Button
     * @param searchTagUII  UII of search tag If null is specified, it is kept as empty
     */
    private void setSearchTagUII(@Nullable TagUII searchTagUII) {
        _searchTagUII = searchTagUII;

        TextView textView = (TextView) findViewById(R.id.text_search_tag_uii_value);
        textView.setText(searchTagUII != null ? searchTagUII.getHexString() : null);
    }

    /**
     * Get UII of search tag
     * @return UII of search tag  Return null if there is no value
     */
    private TagUII getSearchTagUII() {
        return _searchTagUII;
    }

    /**
     * Read search tag UII that is always kept during the execution of the application
     */
    private void loadSearchTagUII() {
        setSearchTagUII(_searchTagUII);
    }

    // endregion

    // region Convert hex string and bytes

    /**
     * Convert from hexadecimal string to byte list
     * @param hexString The hexadecimal string
     * @return The byte list based on the hexadecimal string
     */
    private static byte[] hexStringToBytes(String hexString) {
        // Element 0 in case of null character
        if (hexString.length() == 0) {
            return new byte[0];
        }

        // Cut out hexadecimal character string in byte unit and store it in the list
        // 1 byte is equivalent to 2 hexadecimal characters, so cut out 2 characters at a time
        // In order to cut out 2 characters at a time irrespective of the length of the character string, add 0 to the beginning if the character string is odd length
        String workHexString = hexString.length() % 2 == 0 ? hexString : "0" + hexString;
        byte[] bytes = new byte[workHexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            // If leave Byte.parseByte as it is, overflow will occur when the value becomes larger than 0x80
            // By parsing it to a larger type then casting it to byte, a value larger than 0x80 can be input as a negative value
            String hex2Characters = workHexString.substring(i * 2, i * 2 + 2);

            short number = Short.parseShort(String.format("%s", hex2Characters), 16);
            bytes[i] = (byte) number;
        }
        return bytes;
    }

    /**
     * Convert from byte list to hexadecimal string
     * @param bytes Byte list
     * @return The hexadecimal string based on the byte list
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte byteNumber : bytes) {
            String hex2Characters = String.format("%02X", byteNumber);
            hexStringBuilder.append(hex2Characters);
        }
        return hexStringBuilder.toString();
    }

    // endregion

    // region Dialog relation

    /**
     * Display StringInput dialog into which only uppercase alphanumeric characters can be entered
     * @param contents The contents displayed in the dialog
     */
    private void showUpperAlphaInput(StringInputContents contents) {
        StringInputFragment fragment = new StringInputFragment();
        fragment.context = this;
        fragment.inputType = StringInputFragment.InputType.UPPER_ALPHA_NUMERIC;
        fragment.title = contents.title;
        fragment.startString = contents.startString;
        fragment.listener = contents.inputListener;
        fragment.show(getFragmentManager(), getString(R.string.fragment_anonymous));
    }

    /**
     * Contents for generating StringInput dialog
     */
    private static class StringInputContents {
        String title;
        String startString;
        StringInputFragment.InputListener inputListener;
    }

    // endregion

    // region Tag UII class

    /**
     * The class that shows UII of tag
     * The tag UII has restrictions of range and hexadecimal notation, so express it by wrapping
     */
    private static class TagUII {

        private static char[] hexCharacters =
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        // UII value (hexadecimal number)
        // UII extends to 256 bits ((0or1) * 256), so specify it by a character string
        private String hexString;

        // Byte list UII value
        // Put in the list from the highest byte
        // For example, if the character string is "ABC", it becomes {0xA, 0xBC}
        private byte[] bytes;

        /**
         * Return tag UII based on hexadecimal string
         * @param hexString The hexadecimal string
         * @return Tag UII based on the specified hexadecimal string
         * @throws NotHexException When the specified string is not in  hexadecimal form
         * @throws OverflowBitException When the number of bits in the specified string exceeds the defined number of bits
         */
        public static TagUII valueOf(String hexString, Context context) throws NotHexException, OverflowBitException {
            if (!checkHexString(hexString)) {
                throw new NotHexException(context);
            }
            if (hexString.length() > 64 /* Up to 256 bits can be input, and 64 hexadecimal digits correspond to 256 bits */) {

                throw new OverflowBitException(256, context);
            }
            return new TagUII(hexString);
        }

        /**
         * Return tag UII based on byte list
         * @param bytes Byte list
         * @return  Tag UII based on specified byte list
         * @throws OverflowBitException When the number of bits in the specified byte list exceeds the defined number of bits
         */
        static TagUII valueOf(byte[] bytes, Context context) throws OverflowBitException {
            if (bytes.length > 32 /* Up to 256 bits can be input. Since 1 byte is 8 bits, 32 bytes correspond to 256 bits */) {

                throw new OverflowBitException(256, context);
            }
            return new TagUII(bytes);
        }

        /**
         * Initialize from hexadecimal string
         * @param hexString The hexadecimal string
         */
        private TagUII(String hexString) {
            this.hexString = hexString;

            // Request byte list

            bytes = hexStringToBytes(hexString);
        }

        /**
         * Initialize from byte list
         * @param bytes Byte list
         */
        private TagUII(byte[] bytes) {
            this.bytes = bytes;

            // Find the hexadecimal string

            hexString = bytesToHexString(bytes);
        }

        /**
         * Return as a list of byte
         * @return The byte list
         */
        byte[] getBytes() {
            return bytes;
        }

        /**
         * Return as hexadecimal string
         * @return The hexadecimal string
         */
        String getHexString() {
            return hexString;
        }

        /**
         * Verify whether it is a hexadecimal string
         * @param string The string to be verified
         * @return True if it is a hexadecimal string. Otherwise, False
         */
        private static boolean checkHexString(String string) {
            for (int i = 0; i < string.length(); i++) {
                char character = string.charAt(i);
                if (!checkHexCharacter(character)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Verify whether it is a hexadecimal character
         * @param character The character to be verified
         * @return True if it is a hexadecimal character. Otherwise, False
         */
        private static boolean checkHexCharacter(char character) {
            for (char hexCharacter : hexCharacters) {
                if (character == hexCharacter) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * This exception is thrown if the number of bits overflows
     */
    private static class OverflowBitException extends Exception {

        /**
         * Initialize from number of bits
         * @param bitNumber The number of bits
         */
        OverflowBitException(int bitNumber, Context context) {
            super(String.format(Locale.getDefault(), context.getString(R.string.E_MSG_OVER_FLOW_BIT), bitNumber));
        }
    }

    /**
     * This exception is thrown if the number is not in hexadecimal form
     */
    private static class NotHexException extends Exception {

        /**
         * Initialize
         */
        NotHexException(Context context) {
            super(context.getString(R.string.E_MSG_NOT_HEXADECIMAL));
        }
    }

    // endregion

    // region Enums

    /**
     * Stage of Read output value (1 to 5 stage)
     */
    private enum ReadPowerStage {
        STAGE_1
        , STAGE_2
        , STAGE_3
        , STAGE_4
        , STAGE_5
    }

    /**
     * Returns the stage corresponding to the Read output value
     * @param readPowerLevel  Read output value
     * @return  The stage corresponding to the Read output value Return null if null is specified to argument
     */
    private ReadPowerStage getReadPowerStage(@Nullable Float readPowerLevel) {
        if (readPowerLevel == null) {
            return null;
        }

        int stage2MaxReadPowerLevel = getResources().getInteger(R.integer.stage2_max_read_power_level_on_search);
        int stage3MaxReadPowerLevel = getResources().getInteger(R.integer.stage3_max_read_power_level_on_search);
        int stage4MaxReadPowerLevel = getResources().getInteger(R.integer.stage4_max_read_power_level_on_search);
        int stage5MaxReadPowerLevel = getResources().getInteger(R.integer.stage5_max_read_power_level_on_search);
        if (readPowerLevel <= stage5MaxReadPowerLevel) {
            return ReadPowerStage.STAGE_5;
        } else if (readPowerLevel <= stage4MaxReadPowerLevel) {
            return ReadPowerStage.STAGE_4;
        } else if (readPowerLevel <= stage3MaxReadPowerLevel) {
            return ReadPowerStage.STAGE_3;
        } else if (readPowerLevel <= stage2MaxReadPowerLevel) {
            return ReadPowerStage.STAGE_2;
        } else {
            return ReadPowerStage.STAGE_1;
        }
    }

    /**
     * Method of matching tag
     */
    private enum MatchingMethod {
        FORWARD
        , BACKWARD
    }

    /**
     * The status after finding the tag
     */
    private enum LocateTagState {
        STANDBY                 // Standing by
        , READING_SEARCH_TAG    // Reading the tags used for searching
        , SEARCHING_TAG         // Searching the tags
    }

    /**
     * The action after finding the tag
     */
    private enum LocateTagAction {
        READ_SEARCH_TAG         // Read the tags used for searching
        , SEARCH_TAG            // Retrieve the tags
        , STOP;                 // Terminate the action

        /**
         * Convert to resource string
         *
         * @param resources  Resource for getting resource string
         * @return Resource string
         */
        String toResourceString(Resources resources) {
            switch (this) {
                case READ_SEARCH_TAG:
                    return resources.getText(R.string.read).toString();
                case SEARCH_TAG:
                    return resources.getText(R.string.search).toString();
                case STOP:
                    return resources.getText(R.string.stop).toString();
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    private byte[] stringToByte(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) Integer.parseInt(hex.substring(index * 2, (index + 1) * 2), 16);
        }
        return bytes;
    }

    // endregion
}
