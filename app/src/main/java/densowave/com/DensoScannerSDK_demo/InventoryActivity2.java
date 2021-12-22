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
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Activity of Inventory screen
 * TODO: If it is possible when the display range is switched,  i want to prevent the time lag is generated
 */
public class InventoryActivity2 extends BaseActivity implements RFIDDataDelegate {

    private ListView listView;/*1027RecyclerViewからlistViewに変更*/
    private MyListAdapter adapter;/*1027 TagRecyclerViewAdapterからMyListAdapeterに変更*/
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
    private FileUtils2 fileUtils = new FileUtils2();
    private Button btnClear;
    private Button btnCsvRead;
    private Button btnCsvSave;
    //private Button btnFileOutput;
    private Context mContext;

    private List<ShisanInfo> data = new ArrayList<ShisanInfo>();
    private int storedTagCount = 0;
    private int totalTagCount;
    // 識別用のコード
    private final static int CHOSE_FILE_CODE = 12345;

    /**
     * Loading action
     */
    private enum ReadAction {
        START       // Start reading
        , STOP;     // Stop reading

        /**
         * Convert to resource string
         *
         * @param resources Resource for getting resource string
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
        setContentView(R.layout.activity_inventory2);

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
                listView = null;
                disposeFlg = false;

                finish();
                return true;
        }
        return false;
    }

    /**
     * CSVの読み込み
     * 現在はassetsフォルダから読み込んでいる
     */
    private void readCSV() {
        //デバック用のトースト表示
        Context context = getApplicationContext();
        CharSequence text = "CSVを読み込みました";
        int duration = Toast.LENGTH_SHORT;
        totalTagCount = 0; //読み込んだ資産数
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
        //資産情報のリストの要素をすべてクリア
        data.clear();

        //ファイルパスを選択するインテントの作成
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(Intent.createChooser(intent, "Open CSV"), CHOSE_FILE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent in) {
        //ファイルアプリからFile形式の文字列でCSVのファイルパスを受け取る
        if (requestCode == CHOSE_FILE_CODE && resultCode == RESULT_OK) {
            try {
                Context context = getApplicationContext();
                Uri fileUri = in.getData();
                InputStreamReader inputStreamReader = new InputStreamReader(context.getContentResolver().openInputStream(fileUri), "UTF-8");
                BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                String line = "";
                while ((line = bufferReader.readLine()) != null) {
                    StringTokenizer stringTokenizer = new StringTokenizer(line, ",");
                    //CSVをShisanInfoに代入　１，EPCID　２，資産番号　３，資産名称　４，取得年月日　５，前年度の場所（今回は使用していない）
                    data.add(new ShisanInfo(stringTokenizer.nextToken(), stringTokenizer.nextToken(), stringTokenizer.nextToken()));
                    //Toast.makeText(context, stringTokenizer.nextToken(), duration).show();
                    totalTagCount++;
                }
                //読み込んだ資産数をtext_total_shisan_valueに取り込む
                TextView textView = (TextView) findViewById(R.id.text_total_shisan_value);
                textView.setText(String.valueOf(totalTagCount));
                bufferReader.close();
                storedTagCount = 0;
                refreshTotalTags();
                listView.invalidateViews();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    /**
     * CSVの書き込み
     */
    private void saveCSV() {
        //デバック用のトースト表示
        Context context = getApplicationContext();
        CharSequence text = "CSVを保存しました";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        String csvText = "", line = "";
        for (ShisanInfo si : data) {
            line = si.getEpcID() + "," + si.getAssetName() + "," + si.getLocation() + "," + si.getResult() + "\n";
            Log.d("save", line);
            csvText += line;
        }

        String filename = "test.csv";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(csvText.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Processing when clicking
     * All the touch events in Activity are controlled by this
     *
     * @param view Clicked View
     */
    public void onClick(@NonNull View view) {
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
/*            case R.id.button_file_output_toggle:
                if (adapter.getStoredTagCount() > 0) {
                    checkPermission();
                } else {
                    // When list UII is empty, display the error message.
                    super.showMessage(getString(R.string.E_MSG_NOT_DATA_TO_OUTPUT));
                }
                break;*//*1027 File Outputボタン削除に伴いコメントアウト*/
            case R.id.csv_read:
                readCSV();
                break;
            case R.id.csv_save:
                //saveCSV();
                if (totalTagCount > 0) {
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
        listView = (ListView) findViewById(R.id.tansakuListView);

/*        // Specify this to improve performance since the size of RecyclerView is not changed
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
        recyclerView.setLayoutManager(layoutManager);*//*1027削除*/

        // Specify Adapter
        adapter = new MyListAdapter(this, data);
        listView.setAdapter(adapter);

        // Receive the scroll event and the touch event.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //ここに処理を書く
                //デバック用のトースト表示
                Context context = getApplicationContext();
                CharSequence text = "リストアイテム" + data.get(position).getLocation() + "をクリックしました";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }

        });

        /*recyclerView.addOnItemTouchListener(new OnItemTouchListener());*/
        /*1027削除*/

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
        btnCsvRead = (Button) findViewById(R.id.csv_read);
        btnCsvSave = (Button) findViewById(R.id.csv_save);
        //btnFileOutput = (Button) findViewById(R.id.button_file_output_toggle);
    }

    /**
     * Update TotalTags
     */
    private void refreshTotalTags() {
        // Update ‘Total tags’.
        TextView textView = (TextView) findViewById(R.id.text_total_tags_value);
        textView.setText(String.valueOf(storedTagCount));
    }

    /**
     * Update the display range if it is necessary
     * TODO: I want to optimize the implementation of the OnScrollListener class and the part that has been covered
     */
    /*private void refreshShowRangeIfNeeded() {
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
    }*//*1027削除*/

    /**
     * The event of the scroll in RecyclerView is processed
     */
    /*private class OnScrollListener extends RecyclerView.OnScrollListener {

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
    }*//*1027削除*/

    /**
     * The touch event in RecyclerView is processed
     */
    /*class OnItemTouchListener implements RecyclerView.OnItemTouchListener {

        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            // Do not allow to touch while updating the display range.
            return isRefreshingShowRange;
        }

        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }*//*1027削除*/

    /**
     * Execute the loading action
     */
    private void runReadAction() {
        // Execute the configured reading action
        switch (nextReadAction) {
            case START:
                updateStatus("340C00000000216100766600");
                ListView listView = (ListView) findViewById(R.id.tansakuListView);
                listView.invalidateViews();
                btnClear.setEnabled(false);
                btnClear.setTextColor(getColor(R.color.text_default_disabled));
                btnCsvRead.setEnabled(false);
                btnCsvRead.setTextColor(getColor(R.color.text_default_disabled));
                btnCsvSave.setEnabled(false);
                btnCsvSave.setTextColor(getColor(R.color.text_default_disabled));
                //btnFileOutput.setEnabled(false);
                //btnFileOutput.setTextColor(getColor(R.color.text_default_disabled));
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
                btnCsvRead.setEnabled(true);
                btnCsvRead.setTextColor(getColor(R.color.text_default));
                btnCsvSave.setEnabled(true);
                btnCsvSave.setTextColor(getColor(R.color.text_default));
                //btnFileOutput.setEnabled(true);
                //btnFileOutput.setTextColor(getColor(R.color.text_default));
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
                    //refreshShowRangeIfNeeded();/*1027削除*/

                    // Update TotalTags since the number of tags has been updated.
                    refreshTotalTags();

                    // Scroll to the lowest position when adding tag.
                    /*if (adapter.getItemCount() > 0) {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }*//*1027削除*/
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
                //adapter.addTag(data);/*1027削除*/
                listUII.add(data);
                updateStatus(data);
            }
        }
    }

    public void updateStatus(String inData) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).epcID.equals(inData)) {

                data.get(i).result = "OK";
                storedTagCount++;
            }
        }
        refreshTotalTags();
    }

    public void clearTags() {
        for (int i = 0; i < data.size(); i++) {
            data.get(i).result = "N/A";
        }
        storedTagCount = 0;
    }

    /**
     * Implement clear list when display
     */
    private void clearDataDisplay() {
        listUII = new ArrayList<>();
        clearTags();
        //adapter.notifyDataSetChanged();
        refreshTotalTags();
        ListView listView = (ListView) findViewById(R.id.tansakuListView);
        listView.invalidateViews();
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
            public void onClick(DialogInterface dialogInterface, int i) {
                if (fileUtils.outputFile(mContext, input.getText().toString(), data)) {
                    InventoryActivity2.super.showMessage(getString(R.string.E_MSG_FILE_OUTPUT_COMPLETE));
                } else {
                    InventoryActivity2.super.showMessage(getString(R.string.E_MSG_FILE_OUTPUT_INCOMPLETE));
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
     *
     * @param context     this context.
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
     *
     * @param requestCode  code success or not.
     * @param permissions  the permission had granted.
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
                        public void onClick(DialogInterface dialogInterface, int i) {
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