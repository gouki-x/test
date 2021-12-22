package densowave.com.DensoScannerSDK_demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**

 * Fragment of the dialog which selects the string

 * Specify the contents displayed in the dialog in public field

 */

public class StringSelectorFragment extends DialogFragment {

    /**

     * Listener which accepts the selected string notification

     */

    public interface SelectListener {

        /**

         * Process when selecting the string

         * @param index Index of the selected string

         * @param item The selected string

         */

        void onSelected(int index, String item);
    }

    public String title = "";
    public String[] items = new String[0];
    public SelectListener listener = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener == null) {
                    return;
                }
                listener.onSelected(which, items[which]);
            }
        });
        return builder.create();
    }
}
