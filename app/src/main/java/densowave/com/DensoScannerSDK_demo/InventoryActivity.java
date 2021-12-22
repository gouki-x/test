package densowave.com.DensoScannerSDK_demo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Activity of Inventory screen
 * TODO: If it is possible when the display range is switched,  i want to prevent the time lag is generated
 */
public class InventoryActivity extends BaseActivity implements RFIDDataDelegate{

    private RecyclerView recyclerView;
    private TagRecyclerViewAdapter adapter;
    private ReadAction nextReadAction = ReadAction.START;
    private Handler handler = new Handler();

    // Whether the display range is being updated or not
    // TODO: Verify whether this variable is necessary and describe the reason if it is necessary
    private boolean isRefreshingShowRange = false;

    // Whether it is connected to the scanner during generating time
    // Even when the connection is lost while on this screen, if it was connected to scanner during generating time, display the communication error
    private boolean scannerConnectedOnCreate = false;

    private boolean disposeFlg = true;
    
    private ArrayList<String> listUII = new ArrayList<>();
    private static String FORMAT_DATETIME = "yyyyMMddHHmmss";
    private static String FILE_NAME_FIRST = "Inventory_";
    private FileUtils fileUtils = new FileUtils();
    private Button btnClear;
    private Button btnFileOutput;
    private Context mContext;
	
	/**
     * Loading action
     */
    private enum ReadAction {
        START       // Start reading
        , STOP;     // Stop reading

        /**
         * Convert to resource string
         *
         * @param resources  Resource for getting resource string
         * @return Resource string
         */
        String toResourceString(Resources resources) {
            switch (this) {
                case START:
                    return resources.getText(R.string.start).toString();
                case STOP:
                    return resources.getText(R.string.stop).toString();
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Initialization
        initRecyclerView();
        initButton();
        mContext = this;

        scannerConnectedOnCreate = super.isCommScanner();

        if (scannerConnectedOnCreate) {
            try {
                super.getCommScanner().getRFIDScanner().setDataDelegate(this);
            } catch (Exception e) {
                // Failed to register data listener.
                super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            }
        } else {
            // When SP1 is not found, display the error message.
            super.showMessage(getString(R.string.E_MSG_NO_CONNECTION));
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
     * When Home button is pressed
     */
    @Override
    public void onUserLeaveHint() {
        // Tag reading end
        if (scannerConnectedOnCreate) {
            // When the tag reading is started
            if (nextReadAction == ReadAction.STOP) {
                // Tag loading end
                runReadAction();
                disposeFlg = false;
            }
        }
    }

    /**
     * Key event
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // When the Return button is pressed
            case KeyEvent.KEYCODE_BACK:
                // Tag reading end
                if (scannerConnectedOnCreate) {
                    // When the tag reading is started
                    if (nextReadAction == ReadAction.STOP) {
                        // Tag loading end
                        runReadAction();
                    }

                    // Remove delegate
                    super.getCommScanner().getRFIDScanner().setDataDelegate(null);
                }
                handler = null;
                adapter = null;
                recyclerView = null;
                disposeFlg = false;

                finish();
                return true;
        }
        return false;
    }

    /**
     * Processing when clicking
     * All the touch events in Activity are controlled by this
     *
     * @param view Clicked View
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_navigate_up:
                navigateUp();
                break;
            case R.id.button_read_toggle:
                runReadAction();
                break;
            case R.id.button_clear_toggle:
                clearDataDisplay();
                break;
            case R.id.button_file_output_toggle:
                if (adapter.getStoredTagCount() > 0) {
                    checkPermission();
                } else {
                    // When list UII is empty, display the error message.
                    super.showMessage(getString(R.string.E_MSG_NOT_DATA_TO_OUTPUT));
                }
                break;
        }
    }

    /**
     * Initialize the RecyclerView
     */
    private void initRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.view_tag_recycler);

        // Specify this to improve performance since the size of RecyclerView is not changed
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
    
        if (height < 1280) {
            params.height = (int) (335 * Resources.getSystem().getDisplayMetrics().density);
            recyclerView.setLayoutParams(params);
            recyclerView.setHasFixedSize(true);
        }
        recyclerView.setHasFixedSize(true);

        // Use LinearLayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // Specify Adapter
        adapter = new TagRecyclerViewAdapter(this, this.getWindowManager());
        recyclerView.setAdapter(adapter);

        // Receive the scroll event and the touch event.
        recyclerView.addOnScrollListener(new OnScrollListener());
        recyclerView.addOnItemTouchListener(new OnItemTouchListener());

        // Update the display.
        adapter.notifyDataSetChanged();
    }

    /**
     * Initialize the button
     */
    private void initButton() {
        // Character spacing (letterSpacing) is not applicable on Android Studio 3.1.2 editor
        // Perform settings on the code to clearly show the changes
        Button readToggle = (Button) findViewById(R.id.button_read_toggle);
        TypedValue letterSpacing = new TypedValue();
        getResources().getValue(R.dimen.button_read_toggle_letter_spacing, letterSpacing, true);
        readToggle.setLetterSpacing(letterSpacing.getFloat());
    
        btnClear = (Button) findViewById(R.id.button_clear_toggle);
        btnFileOutput = (Button) findViewById(R.id.button_file_output_toggle);
    }

    /**
     * Update TotalTags
     */
    private void refreshTotalTags() {
        // Update ‘Total tags’.
        TextView textView = (TextView) findViewById(R.id.text_total_tags_value);
        int storedTagCount = adapter.getStoredTagCount();
        textView.setText(String.valueOf(storedTagCount));
    }
    
    /**
     * Update the display range if it is necessary
     * TODO: I want to optimize the implementation of the OnScrollListener class and the part that has been covered
     */
    private void refreshShowRangeIfNeeded() {
        // Do not scroll again when scrolled in accordance with the updated display range
        if (isRefreshingShowRange) {
            return;
        }
        
        // Acquire the current scroll position.
        int currentScrollPosition = ((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        
        // Confirm whether it is necessary to update the display range, otherwise terminate the process.
        if (!adapter.needsRefreshShowRange(currentScrollPosition)) {
            return;
        }
        
        // Update the display range from here.
        isRefreshingShowRange = true;
        
        // Update the display range and scroll to a new position.
        int newScrollPosition = adapter.refreshShowRangeIfNeeded(currentScrollPosition);
        adapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(newScrollPosition);
        
        // Finish updating the display range.
        isRefreshingShowRange = false;
    }

    /**
     * The event of the scroll in RecyclerView is processed
     */
    private class OnScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            // Do not scroll again when scrolled in accordance with the updated display range
            if (isRefreshingShowRange) {
                return;
            }

            // Acquire the current scroll position.
            int currentScrollPosition = ((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

            // Confirm whether it is necessary to update the display range, otherwise terminate the process.
            if (!adapter.needsRefreshShowRange(currentScrollPosition)) {
                return;
            }

            // Update the display range from here.
            isRefreshingShowRange = true;

            // Update the display range.
            // Since notifyDataSetChanged cannot be called immediately on the same frame with onScrolled, it will be executed later.
            recyclerView.post(new RefreshShowRangeAction(currentScrollPosition));
        }

        // The thread to update the display range.
        // Declare your own class since it is required that property has to be in the action posted to RecyclerView
        private class RefreshShowRangeAction implements Runnable {
            private int currentScrollPosition;

            RefreshShowRangeAction(int currentScrollPosition) {
                this.currentScrollPosition = currentScrollPosition;
            }

            @Override
            public void run() {
                // Update the display range and scroll to a new position.
                int newScrollPosition = adapter.refreshShowRangeIfNeeded(currentScrollPosition);
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(newScrollPosition);

                // Finish updating the display range.
                isRefreshingShowRange = false;
            }
        }
    }

    /**
     * The touch event in RecyclerView is processed
     */
    class OnItemTouchListener implements RecyclerView.OnItemTouchListener {

        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            // Do not allow to touch while updating the display range.
            return isRefreshingShowRange;
        }

        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }

    /**
     * Execute the loading action
     */
    private void runReadAction() {
        // Execute the configured reading action
        switch (nextReadAction) {
            case START:
                btnClear.setEnabled(false);
                btnClear.setTextColor(getColor(R.color.text_default_disabled));
                btnFileOutput.setEnabled(false);
                btnFileOutput.setTextColor(getColor(R.color.text_default_disabled));
                // Tag reading starts
                if (scannerConnectedOnCreate) {
                    try {
                        super.getCommScanner().getRFIDScanner().openInventory();
                    } catch (Exception e) {
                        super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
                        e.printStackTrace();
                    }
                }
                break;

            case STOP:
                // Tag loading end
                if (scannerConnectedOnCreate) {
                    try {
                        super.getCommScanner().getRFIDScanner().close();
                    } catch (Exception e) {
                        super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
                        e.printStackTrace();
                    }
                }
                btnClear.setEnabled(true);
                btnClear.setTextColor(getColor(R.color.text_default));
                btnFileOutput.setEnabled(true);
                btnFileOutput.setTextColor(getColor(R.color.text_default));
                break;
        }

        // Set the next reading action
        // Switch the previous reading action to STOP is it was STARTed, and to START if it was STOPped.
        nextReadAction = nextReadAction == ReadAction.START ? ReadAction.STOP : ReadAction.START;

        // For the buttons, set the name of the action to be executed next
        Button readToggle = (Button) findViewById(R.id.button_read_toggle);
        readToggle.setText(nextReadAction.toResourceString(getResources()));
    }

    /**
     * Move to the upper level at the time of screen transition
     */
    private void navigateUp() {
        // Tag reading end
        if (scannerConnectedOnCreate) {
            // When the tag reading is started
            if (nextReadAction == ReadAction.STOP) {
                // Tag loading end
                runReadAction();
            }

            // Remove delegate
            super.getCommScanner().getRFIDScanner().setDataDelegate(null);
        }
        handler = null;
        adapter = null;
        disposeFlg = false;

        // Although there is such embedded navigation function "Up Button" in Android,
        // since it doesn't meet the requirement due to the restriction on UI, transition the the screen using button events.
        Intent intent = new Intent(getApplication(), MainActivity.class);
        startActivity(intent);

        // Stop the Activity because it becomes unnecessary since the parent Activity is returned to.
        finish();
    }

    /**
     * Processing when receiving data
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
                if (adapter != null) {
                    // Reflect the data added to RecycleView.
                    adapter.notifyDataSetChanged();

                    // Since the event is not issued from RecycleView.scrollPosition to OnScroll,
                    // Update the display range manually.
                    refreshShowRangeIfNeeded();

                    // Update TotalTags since the number of tags has been updated.
                    refreshTotalTags();

                    // Scroll to the lowest position when adding tag.
                    if (adapter.getItemCount() > 0) {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            }
        });
    }

    /**
     * Read data from reception event
     *
     * @param rfidDataReceivedEvent Reception event
     */
    public void readData(final RFIDDataReceivedEvent rfidDataReceivedEvent) {
        if (adapter != null) {
            for (int i = 0; i < rfidDataReceivedEvent.getRFIDData().size(); i++) {
                String data = "";
                byte[] uii = rfidDataReceivedEvent.getRFIDData().get(i).getUII();
                for (int loop = 0; loop < uii.length; loop++) {
                    data += String.format("%02X ", uii[loop]).trim();
                }
                adapter.addTag(data);
                listUII.add(data);
            }
        }
    }
    
    /**
     * Implement clear list when display
     */
    private void clearDataDisplay() {
        listUII = new ArrayList<>();
        adapter.clearTags();
        adapter.notifyDataSetChanged();
        refreshTotalTags();
    }
    
    /**
     * Choose directory to save the displayed list UII.
     */
    private void outputData() {
        String fileName = FILE_NAME_FIRST + new SimpleDateFormat(FORMAT_DATETIME, Locale.getDefault()).format(new Date());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(R.string.inventory);
        // Set up the input file name
        final EditText input = new EditText(this);
		InputFilter[] filters = new InputFilter[1];
		//Filter to 125 characters
		filters[0] = new InputFilter.LengthFilter(125);
		input.setFilters(filters);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(fileName);
        builder.setMessage(R.string.MSG_INPUT_FILE_NAME);
        builder.setView(input);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialogInterface, int i) {
                if (fileUtils.outputFile(mContext, input.getText().toString(), listUII)) {
                    InventoryActivity.super.showMessage(getString(R.string.E_MSG_FILE_OUTPUT_COMPLETE));
                } else {
                    InventoryActivity.super.showMessage(getString(R.string.E_MSG_FILE_OUTPUT_INCOMPLETE));
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                // Do nothing
            }
        });
        builder.show();
        Dialog dialog1 = builder.create();
        dialog1.setCanceledOnTouchOutside(false);
    }
    
    /**
     * Check permission for android 23 above
     */
    private void checkPermission() {
        String[] mAllPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!hasAllPermissions(this, mAllPermissions)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(mAllPermissions, 1);
            } else {
                outputData();
            }
        } else {
            outputData();
        }
    }
    
    /**
     * Check all permissions had granted.
     * @param context this context.
     * @param permissions all permissions check.
     * @return granted or not granted.
     */
    private static boolean hasAllPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Check result after request permission for app.
     * @param requestCode code success or not.
     * @param permissions the permission had granted.
     * @param grantResults granted result.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    outputData();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setCancelable(false);
                    builder.setTitle(R.string.inventory);
                    builder.setMessage(R.string.MSG_ENABLE_PERMISSION);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialogInterface, int i) {
                            goToSettings();
                        }
                    }).show();
                    Dialog dialog1 = builder.create();
                    dialog1.setCanceledOnTouchOutside(false);
                }
                break;
        }
    }
    
    /**
     * Go to settings device when don't allow permission.
     */
    private void goToSettings() {
        Intent appSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        appSettings.addCategory(Intent.CATEGORY_DEFAULT);
        appSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(appSettings);
    }
}