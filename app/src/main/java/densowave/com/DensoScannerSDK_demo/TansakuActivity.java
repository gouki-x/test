package densowave.com.DensoScannerSDK_demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class TansakuActivity extends AppCompatActivity {

    List<ShisanInfo> data = new ArrayList<ShisanInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tansaku);

        //ボタンの設定・登録
        final Button button_csv_read = (Button) findViewById(R.id.csv_read);
        button_csv_read.setOnClickListener(buttonClick);
        final Button button_csv_save = (Button) findViewById(R.id.csv_save);
        button_csv_save.setOnClickListener(buttonClick);
        final Button button_navigate_up = (Button) findViewById(R.id.button_navigate_up);
        button_navigate_up.setOnClickListener(buttonClick);
    }

    /**
     * Move to the upper level at the time of screen transition
     */
    private void navigateUp() {
        onBackPressed();
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
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
        //資産情報のリストの要素をすべてクリア
        data.clear();
        try {
            InputStream inputStream = getResources().getAssets().open("sample.csv");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferReader = new BufferedReader(inputStreamReader);
            String line = "";

            while ((line = bufferReader.readLine()) != null) {
                StringTokenizer stringTokenizer = new StringTokenizer(line, ",");
                //CSVをShisanInfoに代入　１，EPCID　２，資産番号　３，資産名称　４，取得年月日　５，前年度の場所（今回は使用していない）
                data.add(new ShisanInfo(stringTokenizer.nextToken(), stringTokenizer.nextToken(), stringTokenizer.nextToken()));
                //Toast.makeText(context, stringTokenizer.nextToken(), duration).show();
            }
            // リスト項目とListViewを対応付けるArrayAdapterを用意する
            MyListAdapter adapter = new MyListAdapter(this, data);
            // ListViewにArrayAdapterを設定する
            ListView listView = (ListView) findViewById(R.id.tansakuListView);
            listView.setAdapter(adapter);
            //ListViewにクリックイベントの検出を追加
            //listView.setOnClickListener(new AdapterView.OnItemClickListener());
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
            bufferReader.close();
        } catch (IOException e) {
            e.printStackTrace();
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


    private View.OnClickListener buttonClick = new View.OnClickListener() {
        /**
         * Onclick processing
         * All touch events in Activity are controlled by this process
         *
         * @param view The clicked View
         */
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.button_navigate_up:
                    navigateUp();
                    break;
                case R.id.csv_read:
                    readCSV();
                    break;
                case R.id.csv_save:
                    saveCSV();
                    break;
                default:
                    break;
            }
        }
    };

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //デバック用のトースト表示
        Context context = getApplicationContext();
        CharSequence text = "アイテムクリック";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

    }
}
