package densowave.com.DensoScannerSDK_demo;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FileUtils2 {
	
	private static final String FILE_PATH = Environment.getExternalStorageDirectory() + "/SP1Sample/";
	private static final String FILE_EXTENSION = ".csv";
	
	/**
	 * Check file name is valid
	 * @param text file name check
	 * @return true or false
	 */
	private boolean isValidName(String text)
	{
		Pattern pattern = Pattern.compile("^(?!(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\.[^.]*)?$)[^<>:\"/\\\\|?*\\x00-\\x1F]*[^<>:\"/\\\\|?*\\x00-\\x1F\\ .]$",
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.COMMENTS);
		Matcher matcher = pattern.matcher(text);
		boolean isMatch = matcher.matches();
		return isMatch;
	}


	/**
	 * Save the displayed list UII to the specified in CSV format.
	 * UIIリストを指定したCSVで保存する
	 *
	 * @param context  Context
	 * @param fileName file name to save.
	 * @param data  list data tags.
	 * @return success or failed.
	 */
//UII型で受け取っていたものをList<SisanInfo>型で受け取るようにした
	public boolean outputFile(Context context, String fileName, List<ShisanInfo> data) {

		File dir = new File(FILE_PATH);
		// Check path is exits
		if (!dir.exists()) {
			// Create folder
			dir.mkdirs();
		}
		// Check file name has extension
		if (!isValidName(fileName)) {
			return false;
		}
		if (!fileName.contains(FILE_EXTENSION)) {
			fileName = fileName + FILE_EXTENSION;
		}

		File fileOutput = new File(FILE_PATH + fileName);
		// Check file is exits
		if (!fileOutput.exists()) {
			try {
				fileOutput.createNewFile();
			} catch (IOException e) {

				return false;
			}
		}

		try {
			if (data.size() > 0) {
				// Start write file result and implement
				BufferedWriter bufferedWriterLogResult = new BufferedWriter(new FileWriter(fileOutput, true));
				for (int i = 0; i < data.size(); i++) {
					//ShisanInfoのリストの中身をCSV形式で吐き出す
					bufferedWriterLogResult.append(String.format("%s", data.get(i).getEpcID()+","+data.get(i).getAssetName()+","+data.get(i).getLocation()+","+data.get(i).getResult()));
					bufferedWriterLogResult.newLine();
				}
				// Close file log
				bufferedWriterLogResult.close();
				//MediaContent Providerに登録し、ギャラリーに反映させる？
				MediaScannerConnection.scanFile(context, new String[]{fileOutput.toString()}, null, null);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public Uri getCSVUri(String fileName) {
		File file = new File(FILE_PATH + fileName);
		Uri uri = Uri.fromFile(file);

		return uri;
	}

	/**
	 * Read all data in file master.
	 * @param fileMater file master selected.
	 * @return dataFileMaster data in file Master.
	 */
	public HashSet<String> readFileMaster (Context context,Uri fileMater) {
		HashSet<String> dataFileMaster = new HashSet<>();
		String line;
		try {
			InputStreamReader input = new InputStreamReader(context.getContentResolver().openInputStream(fileMater), "Shift-JIS");
			BufferedReader bufferedReader = new BufferedReader(input);
			while ((line = bufferedReader.readLine()) != null) {
				if (!"".equals(line)) {
					dataFileMaster.add(line.replace(",", ""));
				}
			}
			
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return dataFileMaster;
	}
	
	/**
	 * Implement write file result output.
	 * @param context Context
	 * @param fileName File name result output.
	 * @param listOrigin List data in file master.
	 * @param listRead List data when read Tags.
	 * @return success or fail.
	 */
	public boolean writeFileOutputResult (Context context, String fileName, HashSet<String> listOrigin, HashSet<String> listRead) {
		
		// Get data uncounted for list data master and list data read.
		ArrayList<String> listUncounted = splitArray(new ArrayList<>(listOrigin), new ArrayList<>(listRead));
		// Get data unlisted for list data master and list data read.
		ArrayList<String> listUnlisted = splitArray(new ArrayList<>(listRead), new ArrayList<>(listOrigin));
		File dir = new File(FILE_PATH);
		// Check path is exits
		if (!dir.exists()) {
			// Create folder
			dir.mkdirs();
		}
		
		File fileOutput = new File(FILE_PATH + fileName);
		// Check file is exits
		if (!fileOutput.exists()) {
			try {
				fileOutput.createNewFile();
			} catch (IOException e) {
				return false;
			}
		}
		
		try {
			// Start write file result and implement
			BufferedWriter bufferedWriterLogResult = new BufferedWriter(new FileWriter(fileOutput, true));
			bufferedWriterLogResult.append(String.format("%s", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date())));
			bufferedWriterLogResult.newLine();
			// Write data uncounted
			bufferedWriterLogResult.append(String.format("%s", "# uncounted"));
			bufferedWriterLogResult.newLine();
			for (int i = 0; i < listUncounted.size(); i++) {
				bufferedWriterLogResult.append(String.format("%s", listUncounted.get(i)));
				bufferedWriterLogResult.newLine();
			}
			// Write data unlisted
			bufferedWriterLogResult.append(String.format("%s", "# unlisted"));
			bufferedWriterLogResult.newLine();
			for (int i=0; i < listUnlisted.size(); i++) {
				bufferedWriterLogResult.append(String.format("%s", listUnlisted.get(i)));
				bufferedWriterLogResult.newLine();
			}
			// Close file log
			bufferedWriterLogResult.close();
			MediaScannerConnection.scanFile(context, new String[] {fileOutput.toString()}, null, null);
		} catch (Exception e) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Convert data from origin for list data uncounted and unlisted.
	 * @param listFirst List data first to process.
	 * @param listSecond List data second to process.
	 * @return list result is uncounted or unlisted.
	 */
	private ArrayList<String> splitArray (ArrayList<String> listFirst, ArrayList<String> listSecond) {
		ArrayList<String> listResult = new ArrayList<>();
		
		for (int i=0; i<listFirst.size(); i++) {
			boolean isCheck = false;
			for (int j=0; j<listSecond.size(); j++) {
				// if item in list first but not in list second
				if (listFirst.get(i).equalsIgnoreCase(listSecond.get(j))) {
					isCheck = true;
				}
			}
			if (!isCheck) {
				listResult.add(listFirst.get(i));
			}
		}
		
		return listResult;
	}
}
