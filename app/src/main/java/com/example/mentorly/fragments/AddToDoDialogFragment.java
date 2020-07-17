package com.example.mentorly.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mentorly.R;


public class AddToDoDialogFragment extends DialogFragment {

    private EditText etToDoTitle;
    private EditText etToDoBody;
    private Button btnSubmit;

    public AddToDoDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Use `newInstance` instead as shown below
    }

    public static AddToDoDialogFragment newInstance(String title) {
        AddToDoDialogFragment frag = new AddToDoDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_to_do_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        etToDoTitle = view.findViewById(R.id.etToDoTitle);
        etToDoBody = view.findViewById(R.id.etToDoBody);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        // Fetch arguments from bundle and set title
        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);
        // Show soft keyboard automatically and request focus to field
        etToDoTitle.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        //get the text from mEditText and return it to the parent fragment
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etToDoTitle.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Title cannot be empty!", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendBackResult();

                }
            }
        });
    }

    // Defines the listener interface
    public interface AddToDoDialogListener {
        void onFinishEditDialog(String title, String body);
    }

    // Call this method to send the data back to the parent fragment
    public void sendBackResult() {
        // Notice the use of `getTargetFragment` which will be set when the dialog is displayed
        AddToDoDialogListener listener = (AddToDoDialogListener) getTargetFragment();
        if (etToDoBody.getText().toString().isEmpty()) {
            listener.onFinishEditDialog(etToDoTitle.getText().toString(), null);
        }
        else {
            listener.onFinishEditDialog(etToDoTitle.getText().toString(), etToDoBody.getText().toString());
        }
        dismiss();
    }


}