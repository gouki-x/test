package densowave.com.DensoScannerSDK_demo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.densowave.scannersdk.Common.CommException;
import com.densowave.scannersdk.Common.CommManager;
import com.densowave.scannersdk.Common.CommScanner;

public class TaskService extends Service {

    private static CommScanner commScanner;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // to do something
        this.commScanner = (CommScanner)intent.getSerializableExtra(MainActivity.serviceKey);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Disconnect SP1
        close();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Disconnect SP1
        close();
    }

    /**
     * Disconnect SP1
     */
    private void close() {
        if (commScanner != null) {
            try {
                commScanner.close();
            } catch (CommException e) {
            }
        }
        // Cancel connection request
        CommManager.endAccept();
    }
}
