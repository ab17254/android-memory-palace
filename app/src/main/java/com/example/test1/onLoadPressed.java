package com.example.test1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.fragment.app.DialogFragment;

/**
 * Class to create which creates an DialogFragment
 * Used when reloading objects
 */

public class onLoadPressed extends DialogFragment {



    interface onOkListener {
        void onOkPressed(String value);
    }

    private onOkListener okListener;
    private EditText textField;

    /**
     * Sets the Ok listener which gets called when the ok button is tapped
     * @param okListener listener
     */
    void setListener(onOkListener okListener) {
        this.okListener = okListener;
    }

    /**
     * Creates an simple layout allowing the user to enter number values only into a text field
     * @return LinearLayout
     */
    private LinearLayout inputLayout() {
        Context context = getContext();
        LinearLayout layout = new LinearLayout(context);
        textField = new EditText(context);
        textField.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textField.setInputType(InputType.TYPE_CLASS_NUMBER);
        textField.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(8)
        });
        layout.addView(textField);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    /**
     *  Creates the input field from the layout
     * @param savedInstanceState
     * @return Dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(inputLayout()).setTitle("Load Anchor")
                .setPositiveButton("Load", (dialog, which) -> {
                    Editable text = textField.getText();
                    if (okListener != null && text != null && text.length() > 0) {
                        okListener.onOkPressed(text.toString());
                    }
                })
                .setNegativeButton("Cancel", ((dialog, which) -> {
                }));
        return builder.create();
    }

}
