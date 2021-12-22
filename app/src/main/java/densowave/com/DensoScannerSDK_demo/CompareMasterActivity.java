package densowave.com.DensoScannerSDK_demo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
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
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class CompareMasterActivity extends BaseActivity implements RFIDDataDelegate{
	
	private static final String FORMAT_DATETIME = "yyyyMMddHHmmss";
	private static final String FILE_EXTENSION = ".txt";
	private static final String FILE_NAME_FIRST = "Result_";
	
	private ReadAction nextReadAction = ReadAction.START;
	private TagRecyclerViewAdapter adapter;
	private Handler handler = new Handler();
	private boolean scannerConnectedOnCreate = false;
	private boolean disposeFlg = true;
	private boolean isSelectMaster = false;
	private TextView implementMatch;
	private TextView implementNotMatch;
	private TextView totalStock;
	private TextView fileNameMaster;
	private Button btnClear;
	private Button btnSelectMaster;
	private Button btnResultOutput;
	private HashSet<String> listDataMaster;
	private int countMatch = 0;
	private int countNotMatch = 0;
	private HashSet<String> listDataRead = new HashSet<>();
	private FileUtils fileUtils = new FileUtils();
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
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_compare_master);
		mContext = this;
		
		// Initialization
		initRecyclerView();
		initButton();
		
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
	 * @param keyCode code of key press
	 * @param event event press up or down
	 * @return true or false
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
				disposeFlg = false;
				
				finish();
				return true;
		}
		return false;
	}
	
	@Override
	public void onRFIDDataReceived (CommScanner commScanner, final RFIDDataReceivedEvent rfidDataReceivedEvent) {
		// Control between threads
		handler.post(new Runnable() {
			@Override
			public void run() {
				readData(rfidDataReceivedEvent);
				if (adapter != null) {
					// Reflect the data added to RecycleView.
					adapter.notifyDataSetChanged();
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
		for (int i = 0; i < rfidDataReceivedEvent.getRFIDData().size(); i++) {
			StringBuilder data = new StringBuilder();
			byte[] uii = rfidDataReceivedEvent.getRFIDData().get(i).getUII();
			for (byte loop: uii) {
				data.append(String.format("%02X ", loop).trim());
			}
			if (listDataRead.add(data.toString())) {
				// Update data matched and not matched.
				refreshTags(data.toString());
			}
		}
	}
	
	/**
	 * Initialize the Recycler View
	 */
	private void initRecyclerView() {
		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.view_tag_not_matched_recycler);
		
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
		
		implementMatch = (TextView) findViewById(R.id.text_implement_match);
		implementNotMatch = (TextView) findViewById(R.id.text_implement_not_match);
		totalStock = (TextView) findViewById(R.id.text_stock_total);
		fileNameMaster = (TextView) findViewById(R.id.text_master_file_name);
		
		btnClear = (Button) findViewById(R.id.button_clear_toggle);
		btnSelectMaster = (Button) findViewById(R.id.button_select_master_toggle);
		btnResultOutput = (Button) findViewById(R.id.button_result_output_toggle);
	}
	
	/**
	 * Processing when clicking
	 * All the touch events in Activity are controlled by this
	 *
	 * @param view Clicked View
	 */
	public void onClick (View view) {
		switch (view.getId()) {
			case R.id.button_navigate_up:
				navigateUp();
				break;
			case R.id.button_read_toggle:
				if (isSelectMaster) {
					runReadAction();
				} else {
					super.showMessage(getString(R.string.MSG_PLS_SELECT_MASTER));
				}
				break;
			case R.id.button_clear_toggle:
				clearData();
				break;
			case R.id.button_select_master_toggle:
				if (isSelectMaster) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setCancelable(false);
					builder.setTitle(R.string.compare_master_title);
					builder.setMessage(R.string.MSG_CHANGE_SELECT_MASTER);
					builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick (DialogInterface dialogInterface, int i) {
							listDataMaster = new HashSet<>();
							clearData();
							// Select File master
							showFilePathInputDialog();
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
				} else {
					checkPermission();
				}
				break;
			case R.id.button_result_output_toggle:
				if (countMatch != 0 || countNotMatch != 0) {
					// Set file name to output result data
					String fileNameOutput = FILE_NAME_FIRST + new SimpleDateFormat(FORMAT_DATETIME, Locale.getDefault()).format(new Date()) + FILE_EXTENSION;
					showDialogSaveFile(fileNameOutput);
				} else {
					super.showMessage(getString(R.string.MSG_PLS_SELECT_MASTER));
				}
				break;
		}
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
	 * Execute the loading action
	 */
	private void runReadAction() {
		// Execute the configured reading action
		switch (nextReadAction) {
			case START:
				btnClear.setEnabled(false);
				btnClear.setTextColor(getColor(R.color.text_default_disabled));
				btnResultOutput.setEnabled(false);
				btnResultOutput.setTextColor(getColor(R.color.text_default_disabled));
				btnSelectMaster.setEnabled(false);
				btnSelectMaster.setTextColor(getColor(R.color.text_default_disabled));
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
				btnSelectMaster.setEnabled(true);
				btnSelectMaster.setTextColor(getColor(R.color.text_default));
				btnResultOutput.setEnabled(true);
				btnResultOutput.setTextColor(getColor(R.color.text_default));
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
	 * Execute clear data on screen
	 */
	private void clearData () {
		countNotMatch = 0;
		countMatch = 0;
		listDataRead.clear();
		implementMatch.setText(String.valueOf(0));
		implementNotMatch.setText(String.valueOf(0));
		adapter.clearTags();
		adapter.notifyDataSetChanged();
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
				// Select File master
				showFilePathInputDialog();
			}
		} else {
			// Select File master
			showFilePathInputDialog();
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
					// Select File master
					showFilePathInputDialog();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setCancelable(false);
					builder.setTitle(R.string.compare_master_title);
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
	
	/**
	 * Show file path input dialog
	 */
	protected void showFilePathInputDialog() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
		intent.setType("*/*");
		startActivityForResult(intent,1);
	}
	
	/**
	 * Update tag matched and not matched with file master when read tags.
	 */
	private void refreshTags(String uii) {
		// Update ‘UII match and not match’.
		boolean checkMatch = false;
		boolean checkNotMatch = false;
		if (listDataMaster != null) {
			if (listDataMaster.contains(uii)) {
				checkMatch = true;
			} else {
				checkNotMatch = true;
				adapter.addTag(uii);
			}
		}
		
		if (checkMatch) {
			countMatch++;
		}
		
		if (checkNotMatch) {
			countNotMatch++;
		}
		
		implementMatch.setText(String.valueOf(countMatch));
		implementNotMatch.setText(String.valueOf(countNotMatch));
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		if ((requestCode == 1) && resultCode == RESULT_OK) {
			if (resultData != null) {
				isSelectMaster = true;
				ClipData clipData = resultData.getClipData();
				if (clipData == null) {
					// read 1 file
					Uri uri = resultData.getData();
					File file = new File(uri.getPath());
					String fileName = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/")+1, file.getAbsolutePath().lastIndexOf("."));
					if (fileName.contains("primary:")) {
						fileName = fileName.replace("primary:", "");
					}
					fileNameMaster.setText(fileName);
					listDataMaster = fileUtils.readFileMaster(mContext, uri);
					totalStock.setText(String.valueOf(listDataMaster.size()));
				}
			}
		} else {
			isSelectMaster = false;
		}
	}
	
	private void showDialogSaveFile (final String fileName) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setTitle(R.string.compare_master_title);
		builder.setMessage(String.format(getString(R.string.MSG_SAVE_FILE_OUTPUT), fileName));
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick (DialogInterface dialogInterface, int i) {
				if (fileUtils.writeFileOutputResult(mContext, fileName, listDataMaster, listDataRead)) {
					CompareMasterActivity.super.showMessage(getString(R.string.E_MSG_FILE_OUTPUT_COMPLETE));
				} else {
					CompareMasterActivity.super.showMessage(getString(R.string.E_MSG_FILE_OUTPUT_INCOMPLETE));
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
}
