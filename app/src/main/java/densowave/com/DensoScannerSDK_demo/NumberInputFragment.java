package densowave.com.DensoScannerSDK_demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Fragment of the dialog that inputs the numbers
 * Specify the contents displayed in the dialog in public field
 */
public class NumberInputFragment extends DialogFragment {

    /**
     * Listener which accepts the notifications of entered numerical values
     */
    public interface InputListener {

        /**
         * Processing when accepting the entered numerical values
         * @param number Entered numerical values  If there is no numerical value (in case of empty string), specify null
         */
        void onInput(Integer number);
    }

    public Context context = null;         // Context for creating the UI
    public String title = "";
    public Integer startNumber = null;      // Consider as null if nothing is specified
    public NumberInputFragment.InputListener listener = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(title);

        // Add the View of the EditText
        // If startNumber is null, specify the null character string because it was not specified
        final EditText editText = new EditText(context);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        String startNumberText = startNumber != null ? String.valueOf(startNumber) : "";
        editText.setText(startNumberText);
        editText.setSelection(startNumberText.length());
        builder.setView(editText);

        // Add button
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener == null) {
                    return;
                }
                String numberText = editText.getText().toString();
                Integer number = stringToClampedAllowInteger(numberText);
                listener.onInput(number);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });

        // Display the keyboard when the dialog is displayed
        Dialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager inputMethodManager =
                        (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    inputMethodManager.showSoftInput(editText, 0);
                }
            }
        });

        return dialog;
    }

    /**
     * Convert from a string to an integer value that is fixed within an allowable value
     * @param string String to convert
     * @return Return integer value that is fixed within an allowable value based on the string  Returns null if the string is empty string or not an integer value
     */
    private static Integer stringToClampedAllowInteger(String string) {
        // In case of a null character string, there is no corresponding integer value, so return null
        if (string.isEmpty()) {
            return null;
        }

        // If it is possible to convert to an integer value as it is, return that value
        Integer number = stringToInteger(string);
        if (number != null) {
            return number;
        }

        // Cut it down to the number of digits of the maximum value then convert it
        // Example: If the Int maximum value is 2147483647
        // (1) The value 112233445566 is cut down to 1122334455 and is smaller than the maximum value, so it is possible to convert the value
        // (2) The value 998877665544 is cut down to 9988776655 but it is larger than the maximum value, so it is impossible to convert the value
        string = string.substring(0, calcDigitNumber(Integer.MAX_VALUE));
        number = stringToInteger(string);
        if (number != null) {
            return number;
        }

        // If the maximum value is exceeded, cut off one more digit
        // Example: If the Int maximum value is 2147483647, the value 9988776655 is cut down to 998877665 and is smaller than the maximum value, so it is possible to convert the value
        string = string.substring(0, string.length() - 1);
        number = stringToInteger(string);
        return number;
    }

    /**
     * Convert from a string to an integer value
     * @param string String to convert
     * @return Return integer value based on string  Returns null if the string is not either empty string or integer value/if either one exceeds the allowable value
     */
    private static Integer stringToInteger(String string) {
        if (string.isEmpty()) {
            return null;
        }
        try {
            // In addition to not being an integer value, thow NumberFormatException if it exceeds the Int tolerance
            Integer number = Integer.parseInt(string);
            return number;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Request the number of digits of numerical number
     * @param number The numerical value
     * @return Number of digits of numerical number
     */
    private static int calcDigitNumber(int number) {
        int digitNumber = 0;
        for (int work = number; work != 0; work /= 10) {
            ++digitNumber;
        }
        return digitNumber;
    }
}
