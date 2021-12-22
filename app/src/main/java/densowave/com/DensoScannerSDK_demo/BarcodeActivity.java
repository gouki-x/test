package densowave.com.DensoScannerSDK_demo;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Barcode.BarcodeScanner;
import com.densowave.scannersdk.Common.CommException;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;

import java.util.List;

public class BarcodeActivity extends BaseActivity implements BarcodeDataDelegate {
    /**
     * Loading action
     */
    private enum ReadAction {
        START       // Start reading
        , STOP;     // Stop reading

        /**
         * Convert to resource string
         * @return Resource string
         */
        String toResourceString(Resources resources) {
            switch(this) {
                case START:
                    return resources.getText(R.string.start).toString();
                case STOP:
                    return resources.getText(R.string.stop).toString();
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    private static final float BUTTON_READ_TOGGLE_LETTER_SPACING = 0.1f;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private BarcodeRecyclerViewAdapter adapter;
    private ReadAction nextReadAction = ReadAction.START;
    private Button readToggle;
    private BarcodeScanner barcodeScanner = null;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        initRecyclerView();
        initButton();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startSession();

        if (nextReadAction == ReadAction.STOP) {
            startBarcodeScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopBarcodeScan();
    }

    /**
     * Onclick processing
     * All touch events in Activity are controlled by this process
     * @param view The clicked View
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_navigate_up:
                navigateUp();
                break;
            case R.id.button_read_toggle:
                runReadAction();
                break;
            default:
                break;
        }
    }

    /**
     * Since it is called back after reading bar code,
     * Add the import result to list.
     */
    @Override
    public void onBarcodeDataReceived(CommScanner commScanner, BarcodeDataReceivedEvent barcodeDataReceivedEvent) {
        if (barcodeDataReceivedEvent != null){
            final List<BarcodeData> barcodeDataList = barcodeDataReceivedEvent.getBarcodeData();
            if (barcodeDataList != null && barcodeDataList.size() > 0) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (BarcodeData barcodeData : barcodeDataList) {
                            String text = barcodeData.getSymbologyDenso() + "(" + barcodeData.getSymbologyAim() + ")\n";
                            try {
                                text += new String(barcodeData.getData(), "Shift-JIS");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            adapter.addBarcode(text);
                        }
                        adapter.notifyDataSetChanged();
                        // Scroll to the place that displays the read data
                        recyclerView.smoothScrollToPosition(adapter.getStoredItemCount());
                    }
                });
            }
        }
    }

    /**
     * Initialize the RecyclerView
     */
    private void initRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.view_barcode_recycler);

        // Specify this to improve performance since the size of RecyclerView is not changed
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
    
        if (height < 1280) {
            params.height = (int) (310 * Resources.getSystem().getDisplayMetrics().density);
            recyclerView.setLayoutParams(params);
            recyclerView.setHasFixedSize(true);
        }
        recyclerView.setHasFixedSize(true);

        // Use LinearLayoutManager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Specify Adapter
        adapter = new BarcodeRecyclerViewAdapter(this, this.getWindowManager());
        recyclerView.setAdapter(adapter);

        // Initialize the view
        adapter.initView();
    }

    /**
     * Initialize the button
     */
    private void initButton() {
        // Character spacing (letterSpacing) is not applicable on Android Studio 3.1.2 editor
        // Perform settings on the code to clearly show the changes
        readToggle = (Button) findViewById(R.id.button_read_toggle);
        readToggle.setLetterSpacing(BUTTON_READ_TOGGLE_LETTER_SPACING);
        readToggle.setText(nextReadAction.toResourceString(getResources()));
    }

    /**
     * Execute the loading action
     */
    private void runReadAction() {
        // Execute the configured reading action
        switch (nextReadAction) {
            case START:
                // Clear tag
                adapter.clearTags();
                adapter.notifyDataSetChanged();

                // Start scanning barcode
                startBarcodeScan();

                break;

            case STOP:
                // Stop scanning barcode
                stopBarcodeScan();

                break;
        }

        // Set next reading action
        // Switch the previous reading action to STOP is it was STARTed, and to START if it was STOPped.
        nextReadAction = nextReadAction == ReadAction.START ? ReadAction.STOP : ReadAction.START;

        // For the buttons, set the name of the action to be executed next
        readToggle.setText(nextReadAction.toResourceString(getResources()));
    }

    /**
     * Move to the upper level at the time of screen transition
     */
    private void navigateUp() {
        onBackPressed();
    }

    /**
     * Get the instance of bar code scanner.
     */
    private void startSession() {
        if (commScanner != null) {
            barcodeScanner = commScanner.getBarcodeScanner();
        }
    }

    /**
     * Start scanning barcode
     */
    private void startBarcodeScan() {

        startSession();

        if (barcodeScanner != null) {
            try {
                // Set listener
                barcodeScanner.setDataDelegate(this);

                // For barcode settings, the default value excluding the barcode type (value set in the device) = not set anything
                //BarcodeScannerSettings barcodeScannerSettings = barcodeScanner.getSettings();
                //barcodeScanner.setSettings(barcodeScannerSettings);

                // Start scanning
                barcodeScanner.openReader();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stop scanning barcode
     */
    private void stopBarcodeScan() {
        if (barcodeScanner != null) {
            try {
                barcodeScanner.closeReader();
                barcodeScanner.setDataDelegate(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
