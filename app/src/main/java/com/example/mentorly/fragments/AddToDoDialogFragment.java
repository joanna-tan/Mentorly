package com.example.mentorly.fragments;

import android.app.DatePickerDialog;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;

import com.example.mentorly.R;

import java.util.Date;


public class AddToDoDialogFragment extends DialogFragment {

    private EditText etToDoTitle;
    private EditText etToDoBody;
    private EditText etToDoDatePick;
    private Button btnSubmit;
    private Date dueDate;

    public AddToDoDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Use `newInstance` instead as shown below
    }

    public static AddToDoDialogFragment newInstance() {
        return new AddToDoDialogFragment();
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
        etToDoDatePick = view.findViewById(R.id.etToDoDate);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        // Show soft keyboard automatically and request focus to field
        etToDoTitle.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        //get the text from mEditText and return it to the parent fragment
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etToDoTitle.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "ToDo title cannot be empty", Toast.LENGTH_SHORT).show();
                }
                else if (etToDoDatePick.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Please select a due date", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendBackResult();

                }
            }
        });

        etToDoDatePick.setInputType(InputType.TYPE_NULL);
        etToDoDatePick.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);
                // date picker dialog
                DatePickerDialog picker = new DatePickerDialog(getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(year, monthOfYear, dayOfMonth);
                                dueDate = calendar.getTime();

                                etToDoDatePick.setText((monthOfYear + 1) + "/" + dayOfMonth + "/" + year);
                            }
                        }, year, month, day);
                picker.show();
            }
        });
    }

    // Defines the listener interface
    public interface AddToDoDialogListener {
        void onFinishEditDialog(String title, String body, Date date);
    }

    // Call this method to send To Do info back to the parent fragment
    public void sendBackResult() {
        AddToDoDialogListener listener = (AddToDoDialogListener) getTargetFragment();
        if (etToDoBody.getText().toString().isEmpty()) {
            listener.onFinishEditDialog(etToDoTitle.getText().toString(), null, dueDate);
        } else {
            listener.onFinishEditDialog(etToDoTitle.getText().toString(), etToDoBody.getText().toString(), dueDate);
        }
        dismiss();
    }
}