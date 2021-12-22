package densowave.com.DensoScannerSDK_demo;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.densowave.scannersdk.Common.CommException;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Dto.RFIDScannerSettings;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;
import com.densowave.scannersdk.RFID.RFIDException;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class RapidReadActivity extends BaseActivity implements RFIDDataDelegate {

    private int tagCount = 0;

    private Date readStartDate = null;
    private long readTimeMilliseconds = 0;
    private Runnable readTimeViewRefreshAction = null;
    private Runnable readTagsPerSecondViewRefreshAction = null;

    private Handler handler = new Handler();

    private ReadState readState = ReadState.STANDBY;

    // Whether it is connected to the scanner during generating time

    // Even when the connection is lost while on this screen, if it was connected to scanner during generating time, display the communication error

    private boolean scannerConnectedOnCreate = false;

    private boolean disposeFlg = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rapid_read);

        scannerConnectedOnCreate = super.isCommScanner();

        // Register the listener

        if (scannerConnectedOnCreate) {
            try {
                super.getCommScanner().getRFIDScanner().setDataDelegate(this);
            } catch (Exception e) {
                super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            }
        } else {
            super.showMessage(getString(R.string.E_MSG_NO_CONNECTION));
        }
        settingSessionInit(true);

        // Service is started in the back ground.

        super.startService();
    }

    @Override
    protected void onDestroy() {
        if (scannerConnectedOnCreate && disposeFlg) {
            super.disconnectCommScanner();
        }
        settingSessionInit(false);
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        disposeFlg = true;
        super.onRestart();
    }

    @Override
    public void onUserLeaveHint() {
        // Stop reading in the background

        runReadAction(ReadAction.STOP);

        if (scannerConnectedOnCreate && readState == ReadState.READING) {
            disposeFlg = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // Transition to the home screen when the return button is pressed 

            case KeyEvent.KEYCODE_BACK:
                // Transition after reading is terminated

                runReadAction(ReadAction.STOP);

                // Transition the screen after the listener is released for registration.

                if (scannerConnectedOnCreate) {
                    super.getCommScanner().getRFIDScanner().setDataDelegate(null);
                }

                disposeFlg = false;

                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**

     * Process when receiving data

     *

     * @param rfidDataReceivedEvent Reception event

     */

    @Override
    public void onRFIDDataReceived(CommScanner scanner, final RFIDDataReceivedEvent rfidDataReceivedEvent) {
        // Control between threads

        handler.post(new Runnable() {
            @Override
            public void run() {
                readData(rfidDataReceivedEvent);
                refreshReadTagsView();
            }
        });
    }

    /**

     * Process when clicking

     * All touch events in Activity are controlled by this process

     *

     * @param view The clicked View

     */

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_navigate_up:       // Transition to the upper tier

                navigateUp();
                break;
            case R.id.button_read_toggle:       // Switch the reading status

                runReadAction(readState.nextAction());
                break;
            case R.id.button_read_clear:        // Clear the read data

                clearData();
                refreshReadTagsView();
                refreshReadTimeView();
                refreshReadTagsPerSecondView();
                break;
        }
    }

    /**

     * Move to the upper level at the time of screen transition

     */

    private void navigateUp() {
        // Transition after reading is terminated

        runReadAction(ReadAction.STOP);

        // Transition the screen after the listener is released for registration.

        if (scannerConnectedOnCreate) {
            super.getCommScanner().getRFIDScanner().setDataDelegate(null);
        }

        disposeFlg = false;

        // Although there is such embedded navigation function "Up Button" in Android,

        // since it doesn't meet the requirement due to the restriction on UI, transition the the screen using button events.

        Intent intent = new Intent(getApplication(), MainActivity.class);
        startActivity(intent);

        // Stop the Activity because it becomes unnecessary since the parent Activity is returned to.

        finish();
    }

    /**

     * Read data from reception event

     *

     * @param event RFID reception event

     */

    private void readData(final RFIDDataReceivedEvent event) {
        tagCount += event.getRFIDData().size();
    }

    /**

     * Clear data

     */

    private void clearData() {
        tagCount = 0;
        readTimeMilliseconds = 0;
    }

    /**

     * Update display of the read tag

     * Immediately update when there is any change in the number of tags

     */

    private void refreshReadTagsView() {
        // Display number of tags separated by commas

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        String text = numberFormat.format(tagCount);

        TextView textView = (TextView) findViewById(R.id.text_read_tags_value);
        textView.setText(text);
    }

    /**

     * Update display of read time

     * Automatically update at regular interval from the start of reading

     */

    private void refreshReadTimeView() {
        // Display the total read time in milliseconds by dividing it into the notation [minute]:[second]:[millisecond (two digits)]

        int separatedMinutes = (int)(readTimeMilliseconds / 1000 / 60);
        int separatedSeconds = (int)(readTimeMilliseconds / 1000 % 60);
        int separatedMilliseconds = (int)(readTimeMilliseconds % 1000 / 10);

        TextView textView = (TextView) findViewById(R.id.text_read_time);
        textView.setText(String.format(Locale.US, "%02d:%02d:%02d",
                separatedMinutes, separatedSeconds, separatedMilliseconds));
    }

    /**

     * Update the display of the read tags every 1 second

     * Automatically update at regular interval from the start of reading

     */

    private void refreshReadTagsPerSecondView() {
        // Truncate the numbers after the decimal point

        int readTagsPerSecond = readTimeMilliseconds > 0 ?
                (int)((float)tagCount / readTimeMilliseconds * 1000) : 0;


        TextView textView = (TextView) findViewById(R.id.text_read_tags_per_second_value);
        textView.setText(String.valueOf(readTagsPerSecond));
    }

    /**

     * Execute read action

     * Execute only executable actions such as start while waiting, stopping while reading

     * @param action The read action

     */

    private void runReadAction(ReadAction action) {
        // Accept only executable action

        if (!readState.runnable(action)) {
            return;
        }

        // Execute the configured action

        switch (action) {
            case START:
                startRead();
                break;
            case STOP:
                stopRead();
                break;
        }

        // Since the action was executed, switch to the following states

        readState = ReadState.nextState(action);

        // Set the name of the action to be executed next to the reading switch button

        Button readToggle = (Button) findViewById(R.id.button_read_toggle);
        readToggle.setText(readState.nextAction().toResourceString(getResources()));
    }

    /**

     * Start reading

     */

    private void startRead() {
        // Disable the Clear button during reading

        Button readClearButton = (Button) findViewById(R.id.button_read_clear);
        readClearButton.setEnabled(false);
        readClearButton.setTextColor(getColor(R.color.text_default_disabled));

        // Start measuring

        // Find the difference from this measurement start time and display it

        // When resuming the measurement, measure the read time from the start

        readStartDate = new Date();
        readStartDate.setTime(readStartDate.getTime() - readTimeMilliseconds);

        // Automatically update display of time read every 10 milliseconds

        readTimeViewRefreshAction = new Runnable() {
            @Override
            public void run() {
                // Keep the difference from the measurement start time

                Date nowDate = new Date();
                readTimeMilliseconds = nowDate.getTime() - readStartDate.getTime();

                // Update the display

                refreshReadTimeView();

                // Update again after ten milliseconds

                handler.postDelayed(this, 10);
            }
        };
        handler.post(readTimeViewRefreshAction);

        // Automatically update the display of tags read every second

        readTagsPerSecondViewRefreshAction = new Runnable() {
            @Override
            public void run() {
                // Update the display

                refreshReadTagsPerSecondView();

                // Update again after one second

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(readTagsPerSecondViewRefreshAction);

        // Tag reading starts

        if (scannerConnectedOnCreate) {
            try {
                super.getCommScanner().getRFIDScanner().openInventory();
            } catch (RFIDException e) {
                super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
                e.printStackTrace();
            }
        }
    }

    /**

     * Stop reading

     */

    private void stopRead() {
        // Tag reading ends

        if (scannerConnectedOnCreate) {
            try {
                super.getCommScanner().getRFIDScanner().close();
            } catch (Exception e) {
                super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
                e.printStackTrace();
            }
        }

        // Stop the automatic update of the time display

        if (readTimeViewRefreshAction != null) {
            handler.removeCallbacks(readTimeViewRefreshAction);
            readTimeViewRefreshAction = null;
        }
        if (readTagsPerSecondViewRefreshAction != null) {
            handler.removeCallbacks(readTagsPerSecondViewRefreshAction);
            readTagsPerSecondViewRefreshAction = null;
        }

        // Temporarily stop measuring

        if (readStartDate != null) {
            Date nowDate = new Date();
            readTimeMilliseconds = nowDate.getTime() - readStartDate.getTime();
            readStartDate = null;
        }

        // There may be an error in the measurement result from the automatic update time to the stop time, so display the measurement result at the stop time

        refreshReadTimeView();
        refreshReadTagsPerSecondView();

        // Enable Clear button as reading is completed

        Button readClearButton = (Button) findViewById(R.id.button_read_clear);
        readClearButton.setEnabled(true);
        readClearButton.setTextColor(getColor(R.color.text_default));
    }
    
    private void settingSessionInit(boolean isEnable) {
        try {
            RFIDScannerSettings settings = super.getCommScanner().getRFIDScanner().getSettings();
            settings.scan.sessionInit = isEnable;
            super.getCommScanner().getRFIDScanner().setSettings(settings);
        } catch (RFIDException e) {
            e.printStackTrace();
        }
    }

    /**

     * Read action

     */

    private enum ReadAction {
        START       // Start reading

        , STOP;     // Stop reading


        /**

         * Convert to resource string

         *

         * @param context Resource for getting the resource string

         * @return Resource string

         */

        String toResourceString(Resources context) {
            switch (this) {
                case START:
                    return context.getText(R.string.start).toString();
                case STOP:
                    return context.getText(R.string.stop).toString();
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    /**

     * Read status

     */

    private enum ReadState {
        STANDBY         // Standing by

        , READING;      // Reading


        /**

         * In this state, return the action to be executed next

         * @return The action to be executed next from this state

         */

        ReadAction nextAction() {
            // Next, execute the start while waiting or stop while reading

            return this == STANDBY ? ReadAction.START : ReadAction.STOP;
        }

        /**

         * Returns whether the specified action can be executed

         * @param action The read action

         * @return True if the specified action can be executed. Otherwise, False

         */

        boolean runnable(ReadAction action) {
            // The next action is determined for each state. Other actions can not be executed

            return action == this.nextAction();
        }

        /**

         * Returns the state after executing the specified action

         * @param action Read action

         * @return The state after executing the specified action

         */

        static ReadState nextState(ReadAction action) {
            // Enter reading state if it starts, or wait state if it stops

            return action == ReadAction.START ? READING : STANDBY;
        }
    }
}
