package densowave.com.DensoScannerSDK_demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**

 * Fragment of the dialog which selects the string

 * Specify the contents displayed in the dialog in public field

 */

public class StringInputFragment extends DialogFragment {

    /**

     * Listener which accepts the entered string notification

     */

    public interface InputListener {

        /**

         * Process when accepting the entered string

         * @param string Entered string

         */

        void onInput(String string);
    }

    public Context context = null;      // Context for creating UI

    public InputType inputType = InputType.DEFAULT;
    public String title = "";
    public String startString = "";
    public InputListener listener = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(title);

        // Add View of EditText

        // Customize EditText depending on input type.

        final EditText editText = new EditText(context);
        if (inputType == InputType.UPPER_ALPHA_NUMERIC) {
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            editText.setFilters(new InputFilter[] {new UpperAlphaNumericFilter()});
        }
        editText.setText(startString);
        editText.setSelection(startString.length());
        builder.setView(editText);

        // Add button.

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener == null) {
                    return;
                }
                String string = editText.getText().toString();
                listener.onInput(string);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });

        // Display keyborad when displaying dialog.

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

     * Types of inputs

     */

    public enum InputType {
        DEFAULT                     // Can input all text.

        , UPPER_ALPHA_NUMERIC       // Can only input uppercase english and number.

    }

    /**

     * Filter which accepts only uppercase alphanumeric characters

     */

    private static class UpperAlphaNumericFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            // Only accept alphanumeric character.

            String sourceString = source.toString();
            if (!sourceString.matches("^[a-zA-Z0-9]+$")) {
                return "";
            }

            // Convert lower case to upper case.

            return sourceString.toUpperCase();
        }
    }
}
