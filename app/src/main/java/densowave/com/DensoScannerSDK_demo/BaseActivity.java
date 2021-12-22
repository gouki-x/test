package densowave.com.DensoScannerSDK_demo;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.Toast;

import com.densowave.scannersdk.Common.CommException;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Common.CommStatusChangedEvent;
import com.densowave.scannersdk.Const.CommConst;
import com.densowave.scannersdk.Listener.ScannerStatusListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Common control class
 */
public class BaseActivity extends AppCompatActivity implements ScannerStatusListener {

    public static CommScanner commScanner;
    public static boolean scannerConnected = false;
    private Toast ts = null;

    /**
     * TOP-Activity
     */
    private boolean topActivity = false;

    /**
     * Activity stack management
     */
    private static List<BaseActivity> activityStack = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add to Activity stack
        activityStack.add(this);
    }

    @Override
    protected void onDestroy() {
        // Delete from Activity stack
        activityStack.remove(this);
        super.onDestroy();
    }

    /**
     * Set CommScanner which is connected
     * @param connectedCommScanner  Set CommScanner In case of CommScanner null which is connected, set the CommScanner which is being held to null.
     */
    public void setConnectedCommScanner(CommScanner connectedCommScanner) {
        if (connectedCommScanner != null) {
            scannerConnected = true;
            connectedCommScanner.addStatusListener(this);
        } else {
            scannerConnected = false;
            if (commScanner != null) {
                commScanner.removeStatusListener(this);
            }
        }
        commScanner = connectedCommScanner;
    }

    /**
     * Get CommScanner
     * Since it is not always connected even if the acquired CommScanner is not null,
     * Use isCommScanner in order to check whether the scanner is connected
     * @return
     */
    public CommScanner getCommScanner() {
        return commScanner;
    }

    /**
     * Determine CommScanner
     * If @return CommScanner is connected or disconnected, return true or false.
     */
    public boolean isCommScanner() {
        return scannerConnected;
    }

    /**
     * Disconnect SP1
     */
    public void disconnectCommScanner() {
        if (commScanner != null) {
            try {
                commScanner.close();
                commScanner.removeStatusListener(this);
                scannerConnected = false;
                commScanner = null;
            } catch (CommException e) {
                this.showMessage(e.getMessage());
            }
        }
    }

    /**
     * Display toast
     * @param msg
     */
    public synchronized void showMessage(String msg) {
        if (ts != null) {
            ts.cancel();
        }
        ts = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        ts.setGravity(Gravity.CENTER, 0, 0);
        ts.show();
    }

    /**
     * Start service in the background
     */
    public void startService() {
        if (isCommScanner()) {

            // Check if the service is already started or not
            ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> listServiceInfo = am.getRunningServices(Integer.MAX_VALUE);

            for (ActivityManager.RunningServiceInfo curr : listServiceInfo) {
                // If it is up running, do not start the service again
                if (curr.service.getClassName().equals(TaskService.class.getName())) {
                    return;
                }
            }

            // Start service
            Intent intent = new Intent(getApplication(), TaskService.class);
            startService(intent);
            ServiceParam serviceParam = new ServiceParam();
            serviceParam.commScanner = getCommScanner();
            intent.putExtra(MainActivity.serviceKey, serviceParam);
        }
    }

    /**
     * Set TOP-Activity
     * @param topActivity true:TOP-Activity false:non TOP-Activity
     */
    protected void setTopActivity(boolean topActivity){
        this.topActivity = topActivity;
    }

    /**
     * Event handling when the connection status of the scanner is changed
     * @param scanner Scanner
     * @param state Status
     */
    public void onScannerStatusChanged(CommScanner scanner, CommStatusChangedEvent state) {
        // When the scanner is disconnected, commScanner will not be connected
        // Because this event handling is called asynchronously, if commScanner is set as null immediately, it may cause a null exception during processing
        // To prevent this, keep the instances and monitor the connection status using flags
        CommConst.ScannerStatus scannerStatus = state.getStatus();
        if (scanner == commScanner && scannerStatus.equals(CommConst.ScannerStatus.CLOSE_WAIT)) {
            // When disconnection status is detected, terminate all Activity other than those on the TOP screen
            BaseActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(scannerConnected) {
                        // Disconnection message display
                        showMessage(getString(R.string.E_MSG_NO_CONNECTION));

                        scannerConnected = false;

                        for (int i = activityStack.size() - 1; i >= 0; i--) {
                            if (!activityStack.get(i).topActivity) {
                                // If the Activity is not on the TOP screen, delete Activity stack
                                activityStack.get(i).finish();
                            } else {
                                // If the Activity is on TOP screen, redraw Activity (onResume)
                                Intent intent = new Intent(BaseActivity.this, BaseActivity.this.getClass());
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                            }
                        }
                    }
                }
            });
        }
    }
}
