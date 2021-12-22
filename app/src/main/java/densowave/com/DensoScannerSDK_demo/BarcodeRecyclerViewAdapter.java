package densowave.com.DensoScannerSDK_demo;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class which controls the generation of RecyclerView which lists up tags
 * This RecyclerView has a certain width and height, and it can be scrolled when the tag does not fit in the display area.
 */
public class BarcodeRecyclerViewAdapter
        extends RecyclerView.Adapter<BarcodeRecyclerViewAdapter.BarcodeViewHolder> {

    // The number of non-stored tags used for default display
    // Statically specify it as a constant by looking at the actual layout
    // private static final int DEFAULT_NO_STORED_TAG_LINE_NUMBER = 10;

    private ArrayList<BarcodeData> barcodeDataSet;

    // Context to acquire the color
    private Context context;
    
    private int defaultTagLineNumber = 10;
    
    // Context to acquire the color
    
    public int getDefaultTagLineNumber () {
        return defaultTagLineNumber;
    }
    
    public void setDefaultTagLineNumber (WindowManager windowManager) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        if (height < 1280) {
            this.defaultTagLineNumber = 7;
        }
    }
    
    /**
     * Constructor
     * @param context Context to get color
     */
    public BarcodeRecyclerViewAdapter(Context context, WindowManager windowManager) {
        this.context = context;
        barcodeDataSet = new ArrayList<>();
    
        setDefaultTagLineNumber(windowManager);
    }

    // Generate view (called from Layout Manager)
    @Override
    public BarcodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.barcode_data, parent, false);
        return new BarcodeViewHolder(v);
    }

    // Replace View content (called from Layout Manager)
    @Override
    public void onBindViewHolder(BarcodeViewHolder holder, int position) {
        // - get element from your data set at this position
        // - replace the contents of the view with that element

        // Set the barcode data
        holder.setBarcodeData(barcodeDataSet.get(position));

        // To make it easy to see, change the background color alternately for each line
        int backgroundColorId = position % 2 == 0 ? R.color.tag_deep : R.color.tag_pale;
        holder.setBackgroundColor(context.getColor(backgroundColorId));
    }

    // Return the size of the data set (called from Layout Manager)
    @Override
    public int getItemCount() {
        return barcodeDataSet.size();
    }

    /**
     * Return the number of data set that hold the actual data
     */
    public int getStoredItemCount() {
        int count = 0;
        for (BarcodeData data : barcodeDataSet) {
            if (data.isStore()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Initialize the view
     */
    public void initView() {
        clearTags();
    }

    /**
     * Add tag
     * @param tagText Text of the added tag
     */
    public void addBarcode(String tagText) {
        // Configure if non-stored tags exist, add otherwise
        boolean existsNotStoredData = false;
        for (BarcodeData data : barcodeDataSet) {
            if (!data.isStore()) {
                data.setDataFromText(tagText);
                existsNotStoredData = true;
                break;
            }
        }
        if (!existsNotStoredData) {
            barcodeDataSet.add(new BarcodeData(tagText));
        }

        // If there is no non-stored data, no further processing will be done
        // The process to prevent non-stored tags from overflowing the display area
        if (!existsNotStoredData) {
            return;
        }

        // Count the number of tag rows
        int storedTagLineNumber = 0;
        for (BarcodeData data : barcodeDataSet) {
            if (data.isStore()) {
                storedTagLineNumber += data.getLineNumber();
            }
        }

        // If the number of stored tag rows is equal to or larger than the default number of tag rows, delete all non-stored tags
        Iterator<BarcodeData> iterator;
        if (storedTagLineNumber >= getDefaultTagLineNumber()) {
            iterator = barcodeDataSet.iterator();
            while (iterator.hasNext()) {
                if (!iterator.next().isStore()) {
                    iterator.remove();
                }
            }
            return;
        }

        // Perform adjustment so that Number of stored tag rows + Number of non-stored tag rows ==  Number of default tag rows
        int needNotStoredTagNumber = getDefaultTagLineNumber() - storedTagLineNumber;
        int notStoredTagCount = 0;
        iterator = barcodeDataSet.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().isStore()) {
                ++notStoredTagCount;
                if (notStoredTagCount > needNotStoredTagNumber) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Clear tag
     * The non-stored tag for displaying the boundary line is being returned to the state when it is placed
     */
    public void clearTags() {
        // Delete all tags at once
        barcodeDataSet.clear();

        // Add a certain number of non-stored tags for displaying the boundary line
        for (int i = 0; i < getDefaultTagLineNumber(); i++) {
            barcodeDataSet.add(new BarcodeData());
        }
    }

    /**
     * ViewHolder of the tag specified in RecyclerView
     */
    public static class BarcodeViewHolder extends RecyclerView.ViewHolder {

        private static final int singleLineHeightDp = 40;      // Row height (unit: dp)
        private static final int dividerHeightDp = 1;          // Boundary line height (unit: dp)
        private final float density;                    // Need to convert from dp density to actual size

        private TextView textBarcodeView;

        BarcodeViewHolder(View v) {
            super(v);
            textBarcodeView = (TextView) v.findViewById(R.id.text_barcode);

            // Acquire density
            DisplayMetrics displayMetrics = v.getResources().getDisplayMetrics();
            density = displayMetrics.density;
        }

        void setBarcodeData(BarcodeData data) {
            ViewGroup.LayoutParams layoutParams = textBarcodeView.getLayoutParams();
            if (data.isStore()) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                // When there is no data, display blank lines with fixed height
                layoutParams.height = (int) (density * (
                        singleLineHeightDp * data.getLineNumber() +
                                dividerHeightDp * (data.getLineNumber() - 1)));
            }
            textBarcodeView.setLayoutParams(layoutParams);

            // Set text to the tag
            textBarcodeView.setText(data.getText());
        }

        /**
         * Background color setting
         * @param backgroundColor Background color
         */
        void setBackgroundColor(int backgroundColor) {
            textBarcodeView.setBackgroundColor(backgroundColor);
        }
    }

    /**
     * Data related to tag
     */
    private static class BarcodeData {

        private String text = "";           // Text
        private int lineNumber = 1;         // Number of lines
        private boolean isStore = false;    // Whether it is stored or not

        /**
         * Create empty data
         */
        BarcodeData() {
            setEmptyData();
        }

        /**
         * Create data based on text
         * @param sourceText The source text
         */
        BarcodeData(String sourceText) {
            setDataFromText(sourceText);
        }

        /**
         * Set data to blank
         */
        void setEmptyData() {
            text = "";
            lineNumber = 1;
            isStore = false;
        }

        /**
         * Set data based on text
         * @param sourceText The source text
         */
        void setDataFromText(String sourceText) {
            text = sourceText;
            isStore = true;
        }

        /**
         * Get text
         * @return Text
         */
        String getText() {
            return text;
        }

        /**
         * Get number of lines
         * @return Number of lines
         */
        int getLineNumber() {
            return lineNumber;
        }

        /**
         * Whether it was stored
         * @return Whether it was stored
         */
        boolean isStore() {
            return isStore;
        }
    }
}
