package densowave.com.DensoScannerSDK_demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Fragment of the dialog that selects the numbers
 * Specify the contents displayed in the dialog in public field
 */
public class NumberPickerFragment extends DialogFragment {

    /**
     * Listener which accepts the notifications of selected numerical values
     */
    public interface PickListener {

        /**
         * Processing when numerical value is selected
         * @param pickedValue The selected numerical value
         */
        void onPicked(int pickedValue);
    }

    public String title = "";
    public int minValue = 0;
    public int maxValue = 0;
    public int startValue = 0;  // Value selected when the dialog is opened
    public PickListener listener = null;

    private View inflatedView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(title);

        // Add the View of NumberPicker
        LayoutInflater inflater = getActivity().getLayoutInflater();
        inflatedView = inflater.inflate(R.layout.dialog_number_picker, null);
        NumberPicker picker = (NumberPicker) inflatedView.findViewById(R.id.number_picker);
        picker.setMinValue(minValue);
        picker.setMaxValue(maxValue);
        picker.setValue(startValue);
        builder.setView(inflatedView);

        // Add button
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener == null) {
                    return;
                }
                NumberPicker picker = (NumberPicker) inflatedView.findViewById(R.id.number_picker);
                listener.onPicked(picker.getValue());
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        return builder.create();
    }
}
