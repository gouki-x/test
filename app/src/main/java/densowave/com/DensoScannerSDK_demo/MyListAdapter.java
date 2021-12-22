package densowave.com.DensoScannerSDK_demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class MyListAdapter extends ArrayAdapter<ShisanInfo> {
    private LayoutInflater myInflater;

    private TextView epcID;
    private TextView assetName;
    private TextView location;
    private TextView  result;


    public MyListAdapter(Context context, List<ShisanInfo> objects) {
        super(context, 0, objects);
        myInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position,
                        View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = myInflater.inflate(R.layout.layout, null);
        }
        ShisanInfo si = this.getItem(position);

        //epcID = (TextView)convertView.findViewById(R.id.epcId);
        assetName = (TextView)convertView.findViewById(R.id.assetName);
        location = (TextView)convertView.findViewById(R.id.location);
        result = (TextView)convertView.findViewById(R.id.result);

        //epcID.setText(si.getEpcID());
        assetName.setText(si.getAssetName());
        location.setText(si.getLocation());
        result.setText(si.getResult());

        return convertView;
    }

}