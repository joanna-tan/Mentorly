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


public class AddEventDialogFragment extends DialogFragment {

    private static final String TAG = "AddPictureDialog";
    private Button btnSubmit;
    private EditText etEventBody;
    private EditText etEventTitle;

    public AddEventDialogFragment() {
    }

    public static AddEventDialogFragment newInstance() {
        AddEventDialogFragment frag = new AddEventDialogFragment();
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_event_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSubmit = view.findViewById(R.id.btnSubmitEvent);
        etEventTitle = view.findViewById(R.id.etEventTitle);
        etEventBody = view.findViewById(R.id.etEventBody);


        etEventTitle.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        //get the text from mEditText and return it to the parent fragment
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etEventTitle.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Title cannot be empty!", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendBackResult();
                }
            }
        });
    }

    // Defines the listener interface
    public interface AddEventDialogFragmentListener {
        void onFinishAddEventDialog(String title, String description);
    }

    // Call this method to send the data back to the parent fragment
    public void sendBackResult() {
        AddEventDialogFragmentListener listener = (AddEventDialogFragmentListener) getTargetFragment();
        if (etEventBody.getText().toString().isEmpty()) {
            listener.onFinishAddEventDialog(etEventTitle.getText().toString(), null);
        }
        else {
            listener.onFinishAddEventDialog(etEventTitle.getText().toString(), etEventBody.getText().toString());
        }
        dismiss();
    }
}