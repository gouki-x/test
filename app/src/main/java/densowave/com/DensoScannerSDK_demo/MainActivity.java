package densowave.com.DensoScannerSDK_demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.densowave.scannersdk.Common.CommException;
import com.densowave.scannersdk.Common.CommManager;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.ScannerAcceptStatusListener;

/**
 * Main Activity that opens when starting
 * It corresponds to the Home screen
 */
public class MainActivity extends BaseActivity implements ScannerAcceptStatusListener {

    // The key value to exchange data with Service
    public static final String serviceKey = "serviceParam";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the TOP-Activity
        setTopActivity(true);

        setContentView(R.layout.activity_main);
    
        // Set the application program version
        TextView appVersionTextView = (TextView) findViewById(R.id.app_version);
        appVersionTextView.setText(getString(R.string.app_version_format, BuildConfig.VERSION_NAME));

//        if (!super.isCommScanner()) {
//            // Acquire the paired device
//            List<CommScanner> scanner_list = CommManager.getScanners();
//            for (int i = 0; i < scanner_list.size(); i++) {
//                if (scanner_list.get(i).getBTLocalName().indexOf("SP1") > -1) {
//                    super.setConnectedCommScanner(scanner_list.get(i));
//
//                    // SP1 connected
//                    try {
//                        super.connectCommScanner();
//                    } catch (CommException e) {
//                        this.showMessage(getString(R.string.E_MSG_NO_CONNECTION));
//                        super.setConnectedCommScanner(null);
//                    }
//                    break;
//                }
//            }
//        }

        // Service is started in the back ground.
        super.startService();

        // Setup takes time, so set up AudioTrack at the timing of application startup
        // Memory is not subject to much pressure, always keep AudioTrack during application execution
        BeepAudioTracks.setupAudioTracks(getResources());
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Connect it not connected when displaying the screen
        TextView connectionStatusTextView = (TextView) findViewById(R.id.connection_status);
        if (!super.isCommScanner()) {
            CommManager.addAcceptStatusListener(this);
            CommManager.startAccept();
            // Draw connection status if not connected
            connectionStatusTextView.setText(getString(R.string.waiting_for_connection));
        } else {
            if (commScanner != null) {
                connectionStatusTextView.setText(commScanner.getBTLocalName());
            } else {
                connectionStatusTextView.setText("");
            }
        }
    }

    @Override
    protected void onPause() {
        // Abort the connection request
        CommManager.endAccept();
        CommManager.removeAcceptStatusListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Release without forgetting AudioTrack

        BeepAudioTracks.releaseAudioTracks();

        if (super.isCommScanner()) {
            super.disconnectCommScanner();
        }
        // Abort the connection request
        CommManager.endAccept();

        super.onDestroy();
    }

    /**
     * When Home button is pressed
     */
    @Override
    public void onUserLeaveHint() {
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
                finish();
                return true;
        }
        return false;
    }

    /**
     * Processing when clicking
     * All the touch events in Activity are controlled by this
     * @param view Clicked View
     */
    public void onClick(View view) {
        // Transition to each Activity.
        Intent intent;
        switch (view.getId()) {
            case R.id.button_rapid_read:
                intent = new Intent(getApplication(), RapidReadActivity.class);
                break;
            case R.id.button_inventory:
                intent = new Intent(getApplication(), InventoryActivity.class);
                break;
            case R.id.button_barcode:
                intent = new Intent(getApplication(), BarcodeActivity.class);
                break;
            case R.id.button_settings:
                intent = new Intent(getApplication(), SettingsActivity.class);
                break;
            case R.id.button_locate_tag:
                intent = new Intent(getApplication(), LocateTagActivity.class);
                break;
            case R.id.button_pre_filters:
                intent = new Intent(getApplication(), PreFiltersActivity.class);
                break;
            case R.id.button_compare_master:
                intent = new Intent(getApplication(), CompareMasterActivity.class);
                break;
            case R.id.button_tansaku:
                intent = new Intent(getApplication(), InventoryActivity2.class);
                break;
            default:
                return;
        }
        // Abort the connection request
        CommManager.endAccept();

        startActivity(intent);
    }

    /**
     * SP1 connection event
     * @param commScanner
     */
    @Override
    public void OnScannerAppeared(CommScanner commScanner) {
        boolean successFlag = false;
        try {
            // Abort the connection request
            CommManager.endAccept();

            commScanner.claim();

            CommManager.removeAcceptStatusListener(this);

            super.setConnectedCommScanner(commScanner);
            successFlag = true;
        } catch (CommException e) {
            e.printStackTrace();
        }

        // Displey BTLocalName of the connected scanner in the screen
        // Run on UI Thread using runOnUIThread
        final boolean finalSuccessFlag = successFlag;
        final String btLocalName = commScanner.getBTLocalName();
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView connectionStatusTextView = (TextView) findViewById(R.id.connection_status);
                if (finalSuccessFlag) {
                    connectionStatusTextView.setText(btLocalName);
                } else {
                    connectionStatusTextView.setText(getString(R.string.connection_error));
                }
            }
        });
        // (STR) ADD CODE SHOW TOAST VERSION SP1 20181129
        if (commScanner.getVersion().contains("Ver. ")) {
            final String verSP1 = commScanner.getVersion().split("Ver. ")[1];
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run () {
                    float verF;
                    try {
                        verF = Float.valueOf(verSP1);
                    } catch (Exception e) {
                        verF = 0f;
                    }
                    final String text = String.format(getString(R.string.E_MSG_VERSION_SP1), verSP1);
                    if (verF < 1.02f) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run () {
                                MainActivity.super.showMessage(text);
                            }
                        });
                    }
                }
            });
            thread.start();
        }
        // (END) ADD CODE SHOW TOAST VERSION SP1 20181129
   }
   
}
