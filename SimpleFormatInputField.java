package com.twentyfourpay.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.twentyfourpay.events.HideSendButtonEvent;
import com.twentyfourpay.events.ShowSendCodeButtonEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Razvan Cirstei on 24-Aug-17.
 */

public class SimpleFormatInputField extends AppCompatEditText {

    private String mPattern;
    private String mOldStringBuf;
    private SimpleFormatInputField mSelf;
    private volatile boolean mSelfUpdateSemaphore;
    private int mCursorPosition;


    public SimpleFormatInputField(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        mSelf = this;
    }

    /**
     * Setting the pattern for the field. Doing this will also disable selection and copy/paste.
     * The maximum number of characters is also limited to the size of the pattern.
     *
     * @param pattern Insert a pattern with no more than one consecutive non-alphanumeric character
     */
    public void setPattern(@NonNull String pattern) {
        this.mPattern = pattern;
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(pattern.length());
        this.setFilters(filters);
        disableCopyPaste();
        init();
        selfSetText(getFormattedText(mSelf.getText().toString()));
    }

    public String getPattern() {
        return mPattern;
    }

    public void setText(String text) {
        mOldStringBuf = text;
        super.setText(text);
    }

    public void selfSetText(String text) {
        mSelfUpdateSemaphore = true;
        mOldStringBuf = text;
        super.setText(text);
    }

    private void init() {

        mSelf.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 10){
                    EventBus.getDefault().post(new ShowSendCodeButtonEvent());
                }
                else{
                    EventBus.getDefault().post(new HideSendButtonEvent());
                }
                if (!mSelfUpdateSemaphore) {
                    mCursorPosition = start;
                    doFormat(start);
                } else {
                    mSelfUpdateSemaphore = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void doFormat(int cursorPosition) {
        String currentString = mSelf.getText().toString();

        if (currentString.length() == mOldStringBuf.length() - 1) {
            // one char was deleted
            if (!isNumberLetter(mOldStringBuf.charAt(cursorPosition))) {
                currentString = currentString.substring(0, cursorPosition - 1) + currentString.substring(cursorPosition);
                mCursorPosition--;
            }
        } else {
            // one char was added
            if (cursorPosition < mOldStringBuf.length() && !isNumberLetter(mOldStringBuf.charAt(cursorPosition))) {
                mCursorPosition++;
            } else if (cursorPosition == mOldStringBuf.length() && cursorPosition < mPattern.length()
                    && !isNumberLetter(mPattern.charAt(cursorPosition))) {
                mCursorPosition++;
            }
            mCursorPosition++;
        }

        String textToSet = getFormattedText(currentString);
        selfSetText(textToSet);

        if (mCursorPosition > 0 && mCursorPosition - 1 < textToSet.length()
                && !isNumberLetter(textToSet.charAt(mCursorPosition - 1))) {
            mCursorPosition--;
        }

        mSelf.setSelection(Math.min(mCursorPosition, textToSet.length()));
    }

    private String getFormattedText(String buffer) {
        String text = buffer.replaceAll("[^A-Za-z0-9]", "");
        StringBuilder stringBuilder = new StringBuilder();
        int j = 0;
        for (int i = 0; i < mPattern.length(); i++) {
            if (j >= text.length())
                break;
            if (j == text.length() - 1 && !isNumberLetter(text.charAt(text.length() - 1)))
                break;
            char c = mPattern.charAt(i);
            if (c == 'a') {
                stringBuilder.append(text.charAt(j));
                j++;
            } else {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    private boolean isNumberLetter(char c) {
        return Character.isDigit(c) || Character.isLetter(c);
    }

    private void disableCopyPaste() {
        this.setCustomSelectionActionModeCallback(new ActionMode.Callback() {

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }
        });
    }
}
