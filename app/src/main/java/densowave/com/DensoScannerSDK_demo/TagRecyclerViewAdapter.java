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
import java.util.ListIterator;

/**

 * Class which controls the generation of RecyclerView which lists up tags

 * The RecyclerView to which this class is applied has fixed width and height, and it shall be able to scroll when the tags does not fit in the display area

 * In order to add a large amount of data, only a certain range of data can be displayed in the list

 * When scrolling to the top and bottom of the list, if the position is not the start/end point of the entire data, then data in the range centered on that position is acquired

 * The height of the RecyclerView to which this class is applied should be: [(height of one line + height of boundary line) * default number of tag lines to be displayed]

 * TODO: Solve the problem of scrolling straight to the top when scrolling from the bottom and reading data in the range including the start point of the entire data 

 * TODO: Solve the problem of scrolling a large amount by just one tag when scrolling down and switching the display range (Check when adding multiple tags to one line)

 */

public class TagRecyclerViewAdapter extends RecyclerView.Adapter<TagRecyclerViewAdapter.TagViewHolder> {

    // Default number of tag lines for displaying 

    // Statically specify it as a constant by looking at the actual layout

    //private final int DEFAULT_TAG_LINE_NUMBER = 15;

    // Length of display range of tag

    private final int TAG_SHOW_RANGE_LENGTH = 100;

    private ArrayList<TagData> tagDataSet;
    private int storedTagCount = 0;
    private int showTagRangeStart = 0;
    private int defaultTagLineNumber = 15;

    // Context to acquire the color
    
    public int getDefaultTagLineNumber () {
        return defaultTagLineNumber;
    }
    
    public void setDefaultTagLineNumber (WindowManager windowManager) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        if (height < 1280) {
            this.defaultTagLineNumber = 11;
        }
    }
    
    private Context context;

    /**

     * Constructor

     * @param context Context to get color

     */

    public TagRecyclerViewAdapter(Context context, WindowManager windowManager) {
        this.context = context;

        tagDataSet = new ArrayList<>();
        
        setDefaultTagLineNumber(windowManager);
        
        // Add a certain number of non-stored tags for displaying the boundary line

        for (int i = 0; i < getDefaultTagLineNumber(); i++) {
            tagDataSet.add(new TagData());
        }
    }

    // Generate view (called from Layout Manager)

    @Override
    public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view

        View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.inventory_tag, parent, false);
        return new TagViewHolder(v);
    }

    // Replace View content (called from Layout Manager)

    @Override
    public void onBindViewHolder(TagViewHolder holder, int position) {
        // - get element from your data set at this position

        // - replace the contents of the view with that element


        // Set tag data.

        int tagDataIndex = showTagRangeStart + position;
        holder.setTagData(tagDataSet.get(tagDataIndex));

        // To make it easy to see, change the background color alternately for each line

        int backgroundColorId = tagDataIndex % 2 == 0 ? R.color.tag_deep : R.color.tag_pale;
        holder.setBackgroundColor(context.getColor(backgroundColorId));
    }

    // Return the size of the data set (called from Layout Manager)

    @Override
    public int getItemCount() {
        int itemCount = tagDataSet.size() - showTagRangeStart;
        if (itemCount > TAG_SHOW_RANGE_LENGTH) {
            itemCount = TAG_SHOW_RANGE_LENGTH;
        }
        return itemCount;
    }

    /**

     * Add tag

     * @param tagText Text of the added tag

     */

    public void addTag(String tagText) {
        // Set there in case there are unstored tags and add in case there are no stored tags.

        if (storedTagCount < tagDataSet.size()) {
            tagDataSet.get(storedTagCount).setDataFromText(tagText);
        } else {
            tagDataSet.add(new TagData(tagText));
        }
        ++storedTagCount;

        // Count the number of tag rows

        int storedLine = 0;
        int noStoredLine = 0;
        for (TagData tagData : tagDataSet) {
            if (tagData.isStore()) {
                storedLine += tagData.lineNumber;
            } else {
                noStoredLine += tagData.lineNumber;
            }
        }

        // Delete extra unstored tag

        int overflowLine = (storedLine + noStoredLine) - getDefaultTagLineNumber();
        ListIterator<TagData> iterator = tagDataSet.listIterator(tagDataSet.size());
        while (iterator.hasPrevious()) {
            // Exit since there is no extra tag

            if (overflowLine <= 0) {
                break;
            }

            // In case it is stored, exit since there is no unstored tag before.

            TagData tagData = iterator.previous();
            if (tagData.isStore) {
                break;
            }

            // Delete because this is an extra unstored tag.

            iterator.remove();
            overflowLine -= tagData.lineNumber;
        }
    }

    /**

     * Clear tag

     * The non-stored tag for displaying the boundary line is returned to the state where it is placed

     */

    public void clearTags() {
        // Delete all tags at once

        tagDataSet.clear();

        // Add a certain number of non-stored tags for displaying the boundary line

        for (int i = 0; i < getDefaultTagLineNumber(); i++) {
            tagDataSet.add(new TagData());
        }

        // Set number of store tag to “0” since there is no tag which is being stored. 

        storedTagCount = 0;

        // Reset start position of display range to the default

        showTagRangeStart = 0;
    }

    /**

     * Get number of stored tags

     * @return Number of tags

     */

    public int getStoredTagCount() {
        return storedTagCount;
    }

    /**

     * Whether the update of display range is required

     * @param scrollPosition Scroll position

     * @return True if the update of display range is required

     */

    public boolean needsRefreshShowRange(int scrollPosition) {
        // Verify whether it is necessary to read before the display range.

        boolean canLoadPrev = showTagRangeStart > 0;
        if (canLoadPrev) {
            boolean atRangeStart = scrollPosition <= 0;
            if (atRangeStart) {
                return true;
            }
        }

        // Verify whether it is necessary to read after the display range.

        boolean canLoadNext = showTagRangeStart + TAG_SHOW_RANGE_LENGTH < tagDataSet.size() - 1;
        if (canLoadNext) {
            // It is necessary to update in case of the scroll position is already at the very back.

            // Enter here when the number of display tag lines fill the whole screen.

            if (scrollPosition == TAG_SHOW_RANGE_LENGTH - 1) {
                return true;
            }

            // Calculate the number of lines from the next data of the scroll position to the end of the display range.

            int lineNumberAtScrollPositionsNextToRangeEnd = 0;
            for (int indexOnRange = TAG_SHOW_RANGE_LENGTH - 1; indexOnRange > scrollPosition; indexOnRange--) {
                int indexOnAll = showTagRangeStart + indexOnRange;
                lineNumberAtScrollPositionsNextToRangeEnd += tagDataSet.get(indexOnAll).lineNumber;
            }

            boolean atRangeBottom = lineNumberAtScrollPositionsNextToRangeEnd <= getDefaultTagLineNumber();
            if (atRangeBottom) {
                return true;
            }
        }

        // It is unnecessary to update if the above conditions are not met.

        return false;
    }

    /**

     * Update display range if required

     * @param scrollPosition Scroll position

     * @return -1 if there is no new scroll position update

     */

    public int refreshShowRangeIfNeeded(int scrollPosition) {
        // Return “-1” if there is no update.

        boolean needsRefresh = needsRefreshShowRange(scrollPosition);
        if (!needsRefresh) {
            return -1;
        }

        // Start position of display range ...[Length of display range of tag / 2]...  Make sure it is in the scroll position.

        int oldShowTagRangeStart = showTagRangeStart;
        showTagRangeStart += scrollPosition - TAG_SHOW_RANGE_LENGTH / 2;
        if (showTagRangeStart < 0) {
            showTagRangeStart = 0;
        }

        // Find the new scroll position.

        int offset = showTagRangeStart - oldShowTagRangeStart;
        int newScrollPosition = scrollPosition - offset;
        return newScrollPosition;
    }

    /**

     * ViewHolder of the tag specified in RecyclerView

     * If the text is configured as it is, there is a problem that a space is created under TextView according to the number of lines

     * Solve the problem by adjusting line spacing on layout file

     * If there is any way to arrange the text of multiple lines at exactly the same spacing, it should be implemented

     */

    public static class TagViewHolder extends RecyclerView.ViewHolder {

        private final int singleLineHeightDp = 28;      // Row height (unit: dp)

        private final int dividerHeightDp = 1;          // Boundary line height (unit: dp)

        private final float density;                    // Need to convert from dp density to actual size


        private final TextView textTagView;

        /**

         * Constructor

         * @param view The View assigned by RecyclerView

         */

        TagViewHolder(View view) {
            super(view);

            // Acquire density

            DisplayMetrics displayMetrics = view.getResources().getDisplayMetrics();
            density = displayMetrics.density;

            textTagView = (TextView) view.findViewById(R.id.text_tag);
        }

        /**

         * Set tag data

         * @param data Tag data

         */

        void setTagData(TagData data) {
            // Change the height according to the number of lines so that all text can be displayed.

            // Being controlled by program side in order to ensure that the height of each View does not change even if there is a line break.

            ViewGroup.LayoutParams layoutParams = textTagView.getLayoutParams();
            layoutParams.height = (int)(density * (

                    singleLineHeightDp * data.getLineNumber() +

                            dividerHeightDp * (data.getLineNumber() - 1)));

            textTagView.setLayoutParams(layoutParams);

            // Set text to the tag

            textTagView.setText(data.getText());
        }

        /**

         * Set backgroung color

         * @param backgroundColor Background color

         */

        void setBackgroundColor(int backgroundColor) {
            textTagView.setBackgroundColor(backgroundColor);
        }
    }

    /**

     * Data related to tags

     */

    private static class TagData {
        private final int MAX_LINE_CHAR_NUM = 24;

        private String text = "";           // Text

        private int lineNumber = 1;         // Number of lines

        private boolean isStore = false;    // Whether it is stored or not


        /**

         * Create empty data

         */

        TagData() {
            setEmptyData();
        }

        /**

         * Create data based on text

         * @param sourceText The text to be the source

         */

        TagData(String sourceText) {
            setDataFromText(sourceText);
        }

        /**

         * Empty data

         */

        void setEmptyData() {
            text = "";
            lineNumber = 1;
            isStore = false;
        }

        /**

         * Set data based on text

         * @param sourceText The text to be the source

         */

        void setDataFromText(String sourceText) {
            // Wrap text at a certain number of characters.

            StringBuilder textBuilder = new StringBuilder(sourceText);
            int currentIndex = 0;
            lineNumber = 1;
            while (currentIndex + MAX_LINE_CHAR_NUM < textBuilder.length()) {
                textBuilder.insert(currentIndex + MAX_LINE_CHAR_NUM, "\n");
                ++lineNumber;
                currentIndex += MAX_LINE_CHAR_NUM + 1 /* \n == 1 */;

            }
            text = textBuilder.toString();
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

         * Whether it is stored or not

         * @return Whether it is stored or not

         */

        boolean isStore() {
            return isStore;
        }
    }
}
