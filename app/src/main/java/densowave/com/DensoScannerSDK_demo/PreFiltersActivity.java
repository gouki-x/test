package densowave.com.DensoScannerSDK_demo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.densowave.scannersdk.Dto.RFIDScannerFilter;
import com.densowave.scannersdk.RFID.RFIDException;

import java.util.Arrays;
import java.util.Locale;

public class PreFiltersActivity extends BaseActivity {

    // region Property


    // Filter maintained only between application program execution times
    // Do not save the filter unless it is the same as the filter of this activity when setting the filter
    // Save the value when the setting of the filter is successful, and keep the value displayed after the screen transition
    private static FilterBank tempFilterBank = null;
    private static FilterOffset tempFilterOffset = null;
    private static FilterPattern tempFilterPattern = null;

    // These filters do not necessarily have values
    // When the filter is null, display it as an null character string in the UI
    // Also, access this variable from getter/setter method
    private FilterBank _filterBank = null;
    private FilterOffset _filterOffset = null;
    private FilterPattern _filterPattern = null;

    // Filter setting value which is saved in body and is kept even after the application is ended
    // Write only when SetFilter is successful, and display the kept value in the text area when Load button is pressed
    private SharedPreferences sharedPref = null;

    // Whether it is connected to the scanner during generating time
    // Even when the connection is lost while on this screen, if it was connected to scanner during generating time, display the communication error
    private boolean scannerConnectedOnCreate = false;

    private boolean disposeFlg = true;

    // endregion

    // region Activity relation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_filters);

        scannerConnectedOnCreate = super.isCommScanner();

        // When SP1 is not found, display the error message.
        if (!scannerConnectedOnCreate) {
            super.showMessage(getString(R.string.E_MSG_NO_CONNECTION));
        }

        // Read SharedPreferences
        sharedPref = getPreferences(Context.MODE_PRIVATE);

        // Display the filter that succeeded in the last SetFilter
        loadFilterFromTemp();

        // Update the state of the button
        refreshSetFilterButton();
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

    @Override
    public void onUserLeaveHint() {
        if (scannerConnectedOnCreate) {
            disposeFlg = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                disposeFlg = false;

                finish();
                return true;
        }
        return false;
    }

    /**
     * Move to the upper level at the time of screen transition
     */

    private void navigateUp() {
        disposeFlg = false;

        // Although there is such embedded navigation function "Up Button" in Android,
        // since it doesn't meet the requirement due to the restriction on UI, transition the the screen using button events.
        Intent intent = new Intent(getApplication(), MainActivity.class);
        startActivity(intent);

        // Stop the Activity because it becomes unnecessary since the parent Activity is returned to.
        finish();
    }

    // endregion

    // region Handle click event

    /**
     * Processing when clicking
     * All the touch events in Activity are controlled by this
     * @param view Clicked View
     */

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.button_navigate_up:
                navigateUp();
                break;
            case R.id.text_bank_value:
                editFilterBank();
                break;
            case R.id.text_offset_value:
                editFilterOffset();
                break;
            case R.id.text_pattern_value:
                editFilterPattern();
                break;
            case R.id.button_filter_set:
                setFilterToScanner();
                break;
            case R.id.button_filter_clear:
                clearFilterFromScanner();
                break;
            case R.id.button_filter_load:
                loadFilterFromPref();
                break;
        }
    }

    // endregion

    // region Edit filter info

    /**
     * Edit the Bank of filter
     */
    private void editFilterBank() {
        StringSelectorContents contents = new StringSelectorContents();
        contents.title = getString(R.string.bank);
        contents.items = getResources().getStringArray(R.array.strings_bank);
        contents.selectListener = new StringSelectorFragment.SelectListener() {
            @Override
            public void onSelected(int index, String item) {
                FilterBank bank = item.isEmpty() ? null : FilterBank.getFilterBank(item);
                setFilterBank(bank);
            }
        };
        showStringSelector(contents);
    }

    /**
     * Edit the Offset of filter
     */
    private void editFilterOffset() {
        NumberInputContents contents = new NumberInputContents();
        contents.title = getString(R.string.offset_bit);
        contents.startNumber = getFilterOffset() != null ? Long.valueOf(getFilterOffset().getNumber()).intValue() : null;
        contents.inputListener = new NumberInputFragment.InputListener() {
            @Override
            public void onInput(Integer number) {
                // Set to blank if there is no numerical value

                if (number == null) {
                    setFilterOffset(null);
                    return;
                }

                // Verify the numerical values

                FilterOffset filterOffset;
                try {
                    filterOffset = FilterOffset.valueOf(number);
                } catch (OutOfRangeException e) {
                    showMessage(getString(R.string.E_MSG_FILTER_OUT_OF_RANGE_OFFSET));
                    return;
                }

                // Perform the setting since the numerical value is a normal value

                setFilterOffset(filterOffset);
            }
        };
        showNumberInput(contents);
    }

    /**
     * Edit the Pattern of filter
     */
    private void editFilterPattern() {
        StringInputContents contents = new StringInputContents();
        contents.title = getString(R.string.pattern);
        contents.startString = getFilterPattern() != null ? getFilterPattern().getHexString() : "";
        contents.inputListener = new StringInputFragment.InputListener() {
            @Override
            public void onInput(String string) {
                // Set to blank for the null character string
                if (string.isEmpty()) {
                    setFilterPattern(null);
                    return;
                }

                // Verify the numerical values
                FilterPattern filterPattern;
                try {
                    filterPattern = FilterPattern.valueOf(string);
                } catch (NotHexException e) {
                    showMessage(getString(R.string.E_MSG_FILTER_INVALID_PATTERN));
                    return;
                } catch (OverflowBitException e) {
                    showMessage(getString(R.string.E_MSG_FILTER_OUT_OF_RANGE_PATTERN));
                    return;
                }

                // Perform the setting since the numerical value is a normal value
                setFilterPattern(filterPattern);
            }
        };
        showUpperAlphaInput(contents);
    }

    // endregion

    // region Set/Clear filter with scanner

    /**
     * Send filter to scanner
     * When Bank · Offset · Pattern is null, this method should not be called
     */

    private void setFilterToScanner() {
        if (!scannerConnectedOnCreate) {
            return;
        }

        // Create filter
        RFIDScannerFilter filter = new RFIDScannerFilter();
        filter.bank = getFilterBank().getBank();
        filter.bitOffset = getFilterOffset().getNumber();
        filter.bitLength = getFilterPattern().getBitLength();
        filter.filterData = getFilterPattern().getBytes();

        // If bank is UII, add 32 to the offset value
        if (filter.bank == RFIDScannerFilter.Bank.UII) {
            filter.bitOffset += 32;
        }

        RFIDScannerFilter[] filters = {filter};
        RFIDScannerFilter.RFIDLogicalOpe logicalOpe = RFIDScannerFilter.RFIDLogicalOpe.AND;

        // Set the filter
        try {
            super.getCommScanner().getRFIDScanner().setFilter(filters, logicalOpe);

            // Save the values in Preference and Temp only when the filter setting to the scanner is successful
            saveFilterToTemp();
            saveFilterToPref();

            super.showMessage(getString(R.string.I_MSG_SET_FILTER));
        } catch (RFIDException e) {
            super.showMessage(getString(R.string.E_MSG_SET_FILTER));
            e.printStackTrace();
        }
    }

    /**
     * Clear filter of scanner
     */
    private void clearFilterFromScanner() {
        if (!scannerConnectedOnCreate) {
            return;
        }

        try {
            super.getCommScanner().getRFIDScanner().clearFilter();

            // Reflect the clearing of the filter only when clearing the setting from the scanner
            clearFilterFromTemp();
            clearFilterFromView();

            super.showMessage(getString(R.string.I_MSG_CLEAR_FILTER));
        } catch (RFIDException e) {
            super.showMessage(getString(R.string.E_MSG_COMMUNICATION));
            e.printStackTrace();
        }
    }

    // endregion

    // region Load/Save/Clear filter with temp on app running

    /**
     * Read filter maintained only between application program execution times
     */
    private void loadFilterFromTemp() {
        setFilterBank(tempFilterBank);
        setFilterOffset(tempFilterOffset != null ? tempFilterOffset.clone() : null);
        setFilterPattern(tempFilterPattern != null ? tempFilterPattern.clone() : null);
    }

    /**
     * Load filter from SharedPreferences
     * Use when Load button is pressed
     */
    private void loadFilterFromPref() {
        String sharedFilterBank = sharedPref.getString(getString(R.string.pref_filter_bank), null);
        long sharedFilterOffset = sharedPref.getLong(getString(R.string.pref_filter_offset), -1);
        String sharedFilterPattern = sharedPref.getString(getString(R.string.pref_filter_pattern), null);

        if (sharedFilterBank != null && sharedFilterOffset >= 0 && sharedFilterPattern != null) {
            try {
                // Set FilterBank
                setFilterBank(FilterBank.getFilterBank(sharedFilterBank));
                // Set FilterOffset
                setFilterOffset(FilterOffset.valueOf(sharedFilterOffset));
                // Set FilterPattern
                setFilterPattern(FilterPattern.valueOf(sharedFilterPattern));
            } catch (NotHexException | OverflowBitException | OutOfRangeException e) {
                e.printStackTrace();
            }
        }
     }

    /**
     * Save as data maintained only between application program execution times
     */
    private void saveFilterToTemp() {
        // Do not do anything if the filter is the same as the kept one while the application is running
        if (filterEqualTemp()) {
            return;
        }

        // Save the filter as data to be kept only while the application is running
        FilterBank filterBank = getFilterBank();
        FilterOffset filterOffset = getFilterOffset();
        FilterPattern filterPattern = getFilterPattern();
        tempFilterBank = filterBank;
        tempFilterOffset = filterOffset != null ? filterOffset.clone() : null;
        tempFilterPattern = filterPattern != null ? filterPattern.clone() : null;
    }

    /**
     * Save filter in SharedPreferences
     * Basically it executes after Set success
     */
    private void saveFilterToPref() {
        sharedPref.edit()
                .putString(getString(R.string.pref_filter_bank), getFilterBank().getShowName())
                .putLong(getString(R.string.pref_filter_offset), getFilterOffset().getNumber())
                .putString(getString(R.string.pref_filter_pattern), getFilterPattern().getHexString())
                .apply();
    }

    /**
     * Discard the filter maintained only between application program execution times
     */
    private void clearFilterFromTemp() {
        tempFilterBank = null;
        tempFilterOffset = null;
        tempFilterPattern = null;
    }

    /**
     * Whether the filter of the activity is the same as the filter maintained only between application program execution times
     * @return Return true if the filter of the activity is the same the filter maintained only between application program execution times, and false otherwise
     */
    private boolean filterEqualTemp() {
        FilterOffset filterOffset = getFilterOffset();
        FilterPattern filterPattern = getFilterPattern();
        return getFilterBank() == tempFilterBank &&
                (filterOffset == null && tempFilterOffset == null ||
                        filterOffset != null && filterOffset.equals(tempFilterOffset)) &&
                (filterPattern == null && tempFilterPattern == null ||
                        filterPattern != null && filterPattern.equals(tempFilterPattern) );
    }

    // endregion

    // region Set/Get filter with UI

    /**
     * Set the Bank of filter
     * Reflect it in the corresponding TextView and Button
     * @param filterBank  Bank of filter  If null is specified, it is kept as empty
     */
    private void setFilterBank(@Nullable FilterBank filterBank) {
        _filterBank = filterBank;

        TextView textView = (TextView) findViewById(R.id.text_bank_value);
        textView.setText(filterBank != null ? filterBank.getShowName() : "");

        refreshSetFilterButton();
    }

    /**
     * Get the Bank of filter
     * @return Bank of filter Return null if there is no value
     */
    private FilterBank getFilterBank() {
        return _filterBank;
    }

    /**
     * Setting the Offset of filter
     * Reflect it in the corresponding TextView and Button
     * @param filterOffset  Offset of filter  If null is specified, it is kept as empty
     */
    private void setFilterOffset(@Nullable FilterOffset filterOffset) {
        _filterOffset = filterOffset;

        TextView textView = (TextView) findViewById(R.id.text_offset_value);
        textView.setText(filterOffset != null ? String.valueOf(filterOffset.getNumber()) : "");

        refreshSetFilterButton();
    }

    /**
     * Get the Offset of filter
     * @return  Offset of filter  Return null if there is no value
     */
    private FilterOffset getFilterOffset() {
        return _filterOffset;
    }

    /**
     * Setting the Pattern of filter
     * Reflect it in the corresponding TextView and Button
     * @param filterPattern  Pattern of filter  If null is specified, it is kept as empty
     */
    private void setFilterPattern(@Nullable FilterPattern filterPattern) {
        _filterPattern = filterPattern;

        TextView textView = (TextView) findViewById(R.id.text_pattern_value);
        textView.setText(filterPattern != null ? filterPattern.getHexString() : null);

        refreshSetFilterButton();
    }

    /**
     * Get the Pattern of filter
     * @return Pattern of filter  Return null if there is no value
     */
    private FilterPattern getFilterPattern() {
        return _filterPattern;
    }

    /**
     * Discard the filter displayed on the screen
     */
    private void clearFilterFromView() {
        setFilterBank(null);
        setFilterOffset(null);
        setFilterPattern(null);
    }

    // endregion

    // region Restrict UI as necessary

    /**
     * Update state of Set button
     */
    private void refreshSetFilterButton() {
        Button filterSetButton = (Button) findViewById(R.id.button_filter_set);

        boolean enabled = getFilterBank() != null && getFilterOffset() != null && getFilterPattern() != null;
        filterSetButton.setEnabled(enabled);

        int colorId = enabled ? R.color.text_default : R.color.text_default_disabled;
        filterSetButton.setTextColor(getColor(colorId));
    }

    // endregion

    // region Dialog relation

    /**
     * Display StringSelector dialog
     * @param contents The contents displayed in the dialog
     */
    private void showStringSelector(StringSelectorContents contents) {
        StringSelectorFragment fragment = new StringSelectorFragment();
        fragment.title = contents.title;
        fragment.items = contents.items;
        fragment.listener = contents.selectListener;
        fragment.show(getFragmentManager(), getString(R.string.fragment_anonymous));
    }

    /**
     * Display StringInput dialog into which only uppercase alphanumeric characters can be entered
     * @param contents The contents displayed in the dialog
     */
    private void showUpperAlphaInput(StringInputContents contents) {
        StringInputFragment fragment = new StringInputFragment();
        fragment.context = this;
        fragment.inputType = StringInputFragment.InputType.UPPER_ALPHA_NUMERIC;
        fragment.title = contents.title;
        fragment.startString = contents.startString;
        fragment.listener = contents.inputListener;
        fragment.show(getFragmentManager(), getString(R.string.fragment_anonymous));
    }

    /**
     * Display NumberInput dialog
     * @param contents The contents displayed in the dialog
     */
    private void showNumberInput(NumberInputContents contents) {
        NumberInputFragment fragment = new NumberInputFragment();
        fragment.context = this;
        fragment.title = contents.title;
        fragment.startNumber = contents.startNumber;
        fragment.listener = contents.inputListener;
        fragment.show(getFragmentManager(), getString(R.string.fragment_anonymous));
    }

    /**
     * Contents for generating StringSelector dialog
     */
    private static class StringSelectorContents {
        String title;
        String[] items;
        StringSelectorFragment.SelectListener selectListener;
    }

    /**
     * Contents for generating StringInput dialog
     */
    private static class StringInputContents {
        String title;
        String startString;
        StringInputFragment.InputListener inputListener;
    }

    /**
     * Contents for generating NumberInput dialog
     */
    private static class NumberInputContents {
        String title;
        Integer startNumber;
        NumberInputFragment.InputListener inputListener;
    }

    // endregion

    // region Each filter class

    /**
     * The class that shows Bank of filter
     * In order to make it easy to understand the conversion to the display name, express it by wrapping
     */
    private enum FilterBank {

        UII("UII"), TID("TID"), USER("USER");

        private final RFIDScannerFilter.Bank bank;
        private final String showName;

        /**
         * Return Bank of filter based on the display name
         * @param showName Display name
         * @return Bank of filter based on the display name
         * @throws IllegalArgumentException When the Bank of filter corresponding to the display name does not exist
         */
        public static FilterBank getFilterBank(String showName) throws IllegalArgumentException {
            for (FilterBank filterBank : FilterBank.values()) {
                if (filterBank.showName.equals(showName)) {
                    return filterBank;
                }
            }
            throw new IllegalArgumentException();
        }

        /**
         * Initialize from display name
         * @param showName Display name
         */
        FilterBank(String showName) {
            switch (showName) {
                case "UII":
                    bank = RFIDScannerFilter.Bank.UII;
                    break;
                case "TID":
                    bank = RFIDScannerFilter.Bank.TID;
                    break;
                case "USER":
                    bank = RFIDScannerFilter.Bank.USER;
                    break;
                default:
                    bank = null;
                    break;
            }
            this.showName = showName;
        }

        /**
         * Get display name
         * @return Display name
         */
        public String getShowName() {
            return showName;
        }

        /**
         * Get Bank of API
         * @return Bank of API
         */
        public RFIDScannerFilter.Bank getBank() {
            return bank;
        }
    }

    /**
     * The class that shows Offset of filter
     * Offset has a range restriction, so express it by wrapping
     */
    private static class FilterOffset implements Cloneable {

        private long number;

        /**
         * Return Offset of filter based on the number
         * @param number The number
         * @return Offset of filter based on the specified number
         * @throws OutOfRangeException When the specified number is out of the defined range
         */
        public static FilterOffset valueOf(long number) throws OutOfRangeException {
            if (number < 0 || number > 0x7FFFF) {
                throw new OutOfRangeException(0, 0x7FFFF);
            }
            return new FilterOffset(number);
        }

        /**
         * Initialize from number
         * @param value number
         */
        private FilterOffset(long value) {
            this.number = value;
        }

        @Override
        public int hashCode() {
            return Long.valueOf(number).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            FilterOffset other = (FilterOffset) obj;
            return number == other.number;
        }

        @Override
        public FilterOffset clone() {
            FilterOffset cloneInstance;
            try {
                cloneInstance = (FilterOffset) super.clone();
            } catch (CloneNotSupportedException e) {
                // Cloneable is specified for the class, this is not executed

                e.printStackTrace();
                return null;
            }
            cloneInstance.number = number;
            return cloneInstance;
        }

        /**
         * Get as number
         * @return number
         */
        public long getNumber() {
            return number;
        }
    }

    /**
     * Class that shows Pattern of filter
     * Pattern has restrictions of range and hexadecimal notation, so express it by wrapping
     */
    private static class FilterPattern implements Cloneable {

        private static char[] hexCharacters =
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        // Hexadecimal Pattern value
        // Pattern extends to 256 bits ((0or1) * 256), so specify it by a character string
        private String hexString;

        // Byte list pattern value
        // Put in the list from the highest byte
        // For example, if the character string is "ABC", it becomes {0xA, 0xBC}
        private byte[] bytes;

        // Bit length of Pattern value
        // The bit length corresponds to the length of the hexadecimal character string
        // For example, if the character string is “ABC”, it is 4 bits per hexadecimal character, so the length is 12
        private short bitLength;

        /**
         * Return Pattern of filter based on the hexadecimal string
         * @param hexString The hexadecimal string
         * @return Pattern of filter based on the specified hexadecimal string
         * @throws NotHexException When the specified string is not in  hexadecimal form
         * @throws OverflowBitException When the number of bits in the specified string exceeds the defined number of bits
         */
        public static FilterPattern valueOf(String hexString) throws NotHexException, OverflowBitException {
            if (!checkHexString(hexString)) {
                throw new NotHexException();
            }
            if (hexString.length() > 64 /* Up to 256 bits can be input, and 64 hexadecimal digits correspond to 256 bits */) {

                throw new OverflowBitException(256);
            }
            return new FilterPattern(hexString);
        }

        /**
         * Initialize from hexadecimal string
         * @param hexString The hexadecimal string
         */
        private FilterPattern(String hexString) {
            this.hexString = hexString;

            // Request byte list
            bytes = hexStringToBytes(hexString);

            // Request bit length
            bitLength = calcBitLength(hexString);
        }

        @Override
        public int hashCode() {
            return hexString.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            // If the strings are the same, all other parameters are the same, too
            FilterPattern other = (FilterPattern) obj;
            return hexString.equals(other.hexString);
        }

        @Override
        public FilterPattern clone() {
            FilterPattern cloneInstance;
            try {
                cloneInstance = (FilterPattern) super.clone();
            } catch (CloneNotSupportedException e) {
                // Cloneable is specified for the class, this is not executed
                e.printStackTrace();
                return null;
            }
            cloneInstance.bytes = Arrays.copyOf(bytes, bytes.length);
            cloneInstance.hexString = hexString;
            cloneInstance.bitLength = bitLength;
            return cloneInstance;
        }

        /**
         * Get as byte list
         * @return The byte list
         */
        byte[] getBytes() {
            return bytes;
        }

        /**
         * Get as hexadecimal string
         * @return The hexadecimal string
         */
        String getHexString() {
            return hexString;
        }

        /**
         * Get bit length
         * @return The bit length
         */
        short getBitLength() {
            return bitLength;
        }

        /**
         * Verify whether it is a hexadecimal string
         * @param string The string to be verified
         * @return True if it is a hexadecimal string. Otherwise, False
         */
        private static boolean checkHexString(String string) {
            for (int i = 0; i < string.length(); i++) {
                char character = string.charAt(i);
                if (!checkHexCharacter(character)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Verify whether it is a hexadecimal character
         * @param character The character to be verified
         * @return True if it is a hexadecimal character. Otherwise, False
         */
        private static boolean checkHexCharacter(char character) {
            for (char hexCharacter : hexCharacters) {
                if (character == hexCharacter) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Convert from hexadecimal string to byte list
         * @param hexString The hexadecimal string
         * @return The byte list based on the hexadecimal string
         */
        private static byte[] hexStringToBytes(String hexString) {
            // Element 0 in case of null character

            if (hexString.length() == 0) {
                return new byte[0];
            }

            // Cut out hexadecimal character string in byte unit and store it in the list
            // 1 byte is equivalent to 2 hexadecimal characters, so cut out 2 characters at a time
            // In order to cut out 2 characters at a time irrespective of the length of the character string, add 0 to the beginning if the character string is odd length
            String workHexString = hexString.length() % 2 == 0 ? hexString : "0" + hexString;
            byte[] bytes = new byte[workHexString.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                // If leave Byte.parseByte as it is, overflow will occur when the value becomes larger than 0x80
                // By parsing it to a larger type then casting it to byte, a value larger than 0x80 can be input as a negative value
                String hex2Characters = workHexString.substring(i * 2, i * 2 + 2);

                short number = Short.parseShort(String.format("%s", hex2Characters), 16);
                bytes[i] = (byte) number;
            }
            return bytes;
        }

        /**
         * Find bit length from byte list
         * @param hexString The string
         * @return Bit length of the string
         */
        private static short calcBitLength(String hexString){
            // 1 character has four bits

            return (short) (hexString.length() * 4);

        }
    }

    /**
     * This exception is thrown if the number is out of range
     */
    private static class OutOfRangeException extends Exception {

        /**
         * Initialize based on minimum and maximum values
         * @param minValue The minimum value
         * @param maxValue The maximum value
         */
        OutOfRangeException(int minValue, int maxValue) {
            super(String.format(Locale.getDefault(), "指定できる値は %d から %d までです。", minValue, maxValue));
        }
    }

    /**
     * This exception is thrown if the number of bits overflows
     */
    private static class OverflowBitException extends Exception {

        /**
         * Initialize from number of bits
         * @param bitNumber The number of bits
         */
        OverflowBitException(int bitNumber) {
            super(String.format(Locale.getDefault(), "指定できる値は %d bitまでです。", bitNumber));
        }
    }

    /**
     * This exception is thrown if the number is not in hexadecimal form
     */
    private static class NotHexException extends Exception {

        /**
         * Initialize
         */
        NotHexException() {
            super("指定できる値は16進数でなければいけません。");
        }
    }

    // endregion
}
