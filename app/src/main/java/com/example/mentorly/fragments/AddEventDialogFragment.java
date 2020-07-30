package com.example.mentorly.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;

import com.example.mentorly.R;
import com.example.mentorly.models.DateInterval;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AddEventDialogFragment extends DialogFragment {

    private Button btnSubmit;
    private EditText etEventBody;
    private EditText etEventTitle;
    private EditText etDatePick;
    private EditText etTimePick;
    private EditText etEndTimePick;
    private TextView tvAutofillTime;

    // format: YYYY, MM, DD, HH, MM
    int[] startSelected;
    int[] endSelected;

    public AddEventDialogFragment() {
    }

    public static AddEventDialogFragment newInstance(List<DateInterval> eventDates) {
        AddEventDialogFragment frag = new AddEventDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("dates", (ArrayList<? extends Parcelable>) eventDates);
        frag.setArguments(args);
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
        final List<DateInterval> eventDates = getArguments().getParcelableArrayList("dates");

        btnSubmit = view.findViewById(R.id.btnSubmitEvent);
        etTimePick = view.findViewById(R.id.etSelectTime);
        etEndTimePick = view.findViewById(R.id.etSelectEndTime);
        etDatePick = view.findViewById(R.id.etSelectDate);
        etEventTitle = view.findViewById(R.id.etEventTitle);
        etEventBody = view.findViewById(R.id.etEventBody);
        tvAutofillTime = view.findViewById(R.id.tvAutofillEvent);

        // initialize data arrays
        startSelected = new int[5];
        endSelected = new int[5];

        // show input keyboard for event title
        etEventTitle.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // click to add a suggested time. probably take an input of all the event times & calculate some
        // non-conflict
        tvAutofillTime.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                DateInterval newInterval = suggestEventTime(eventDates);

                Calendar startTime = Calendar.getInstance();
                startTime.setTime(newInterval.getStart());
                startSelected[0] = startTime.get(Calendar.YEAR);
                startSelected[1] = startTime.get(Calendar.MONTH);
                startSelected[2] = startTime.get(Calendar.DAY_OF_MONTH);
                startSelected[3] = startTime.get(Calendar.HOUR_OF_DAY);
                startSelected[4] = startTime.get(Calendar.MINUTE);
                etTimePick.setText(updateTime(startSelected[3], startSelected[4]));

                Calendar endTime = Calendar.getInstance();
                endTime.setTime(newInterval.getEnd());
                endSelected[0] = endTime.get(Calendar.YEAR);
                endSelected[1] = endTime.get(Calendar.MONTH);
                endSelected[2] = endTime.get(Calendar.DAY_OF_MONTH);
                endSelected[3] = endTime.get(Calendar.HOUR_OF_DAY);
                endSelected[4] = endTime.get(Calendar.MINUTE);
                etEndTimePick.setText(updateTime(endSelected[3], endSelected[4]));

                etDatePick.setText((startSelected[1] + 1) + "/" + startSelected[2] + "/" + startSelected[0]);
            }
        });

        // set start time picker
        etTimePick.setInputType(InputType.TYPE_NULL);
        etTimePick.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {

                // show time picker for start
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                // time picker dialog
                TimePickerDialog picker = new TimePickerDialog(getContext(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                                startSelected[3] = hour;
                                startSelected[4] = minute;
                                etTimePick.setText(updateTime(hour, minute));
                            }
                        }, hour, minute, DateFormat.is24HourFormat(getActivity()));
                picker.show();
            }
        });

        // set end time picker
        etEndTimePick.setInputType(InputType.TYPE_NULL);
        etEndTimePick.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {

                // show the time picker for end time
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                // time picker dialog
                TimePickerDialog picker = new TimePickerDialog(getContext(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                                endSelected[3] = hour;
                                endSelected[4] = minute;
                                etEndTimePick.setText(updateTime(hour, minute));
                            }
                        }, hour, minute, DateFormat.is24HourFormat(getActivity()));
                picker.show();
            }
        });

        // set date picker
        etDatePick.setInputType(InputType.TYPE_NULL);
        etDatePick.setOnClickListener(new View.OnClickListener() {
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
                                startSelected[0] = year;
                                startSelected[1] = monthOfYear;
                                startSelected[2] = dayOfMonth;

                                endSelected[0] = year;
                                endSelected[1] = monthOfYear;
                                endSelected[2] = dayOfMonth;
                                etDatePick.setText((monthOfYear + 1) + "/" + dayOfMonth + "/" + year);
                            }
                        }, year, month, day);
                picker.show();
            }
        });

        //return event selections back to the calendar fragment
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etEventTitle.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Title cannot be empty!", Toast.LENGTH_SHORT).show();
                } else if (etDatePick.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Please select a date for the event", Toast.LENGTH_SHORT).show();
                } else if (etTimePick.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Please enter start time", Toast.LENGTH_SHORT).show();
                } else if (etEndTimePick.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Please enter an end time", Toast.LENGTH_SHORT).show();
                }
                // if the start hour is greater than end hour
                else if (startSelected[3] > endSelected[3] ||
                        startSelected[3] == endSelected[3] && startSelected[4] > endSelected[4]) {
                    Toast.makeText(getContext(), "Please select a valid end time", Toast.LENGTH_SHORT).show();

                    // Set the color of the end time to RED to indicate error
                    String endTime = etEndTimePick.getText().toString();
                    Spannable WordtoSpan = new SpannableString(endTime);
                    WordtoSpan.setSpan(new ForegroundColorSpan(Color.RED), 0, endTime.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    etEndTimePick.setText(WordtoSpan);
                } else {
                    sendBackResult();
                }
            }
        });
    }

    // given a list of start and end times, find the non-overlap
    @RequiresApi(api = Build.VERSION_CODES.N)
    private DateInterval suggestEventTime(List<DateInterval> eventTimes) {
        // suggest a time tomorrow, length of one hour
        Calendar newStartTime = Calendar.getInstance();
        Calendar newEndTime = Calendar.getInstance();

        Calendar now = Calendar.getInstance();
        now.setTime(new Date(System.currentTimeMillis()));

        if (eventTimes.size() == 1) {
            now.setTime(eventTimes.get(0).getEnd());
        } else {
            now.setTime(eventTimes.get(0).getEnd());
        }

        newStartTime.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH) + 1,
                now.get(Calendar.HOUR_OF_DAY), 0);
        newEndTime.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH) + 1,
                now.get(Calendar.HOUR_OF_DAY) + 1, 0);

        DateInterval newInterval = new DateInterval(newStartTime.getTime(), newEndTime.getTime());

        // change the event time by one day if an overlap occurs
        for (DateInterval interval : eventTimes) {
            if (newInterval.overlapsWith(interval)) {
                newInterval.shiftDate();
            }
        }

        return newInterval;
    }

    // Used to convert 24hr format to 12hr format with AM/PM values
    private String updateTime(int hours, int mins) {
        String timeSet = "";
        if (hours > 12) {
            hours -= 12;
            timeSet = "PM";
        } else if (hours == 0) {
            hours += 12;
            timeSet = "AM";
        } else if (hours == 12)
            timeSet = "PM";
        else
            timeSet = "AM";


        String minutes = "";
        if (mins < 10)
            minutes = "0" + mins;
        else
            minutes = String.valueOf(mins);

        // Append in a StringBuilder
        String aTime = new StringBuilder().append(hours).append(':')
                .append(minutes).append(" ").append(timeSet).toString();

        return aTime;
    }

    // Defines the listener interface
    public interface AddEventDialogFragmentListener {
        void onFinishAddEventDialog(String title, String description, int[] startSelected, int[] endSelected);

    }

    // Call this method to send the data back to the parent fragment
    public void sendBackResult() {
        AddEventDialogFragmentListener listener = (AddEventDialogFragmentListener) getTargetFragment();
        if (etEventBody.getText().toString().isEmpty()) {
            listener.onFinishAddEventDialog(etEventTitle.getText().toString(), null, startSelected, endSelected);
        } else {
            listener.onFinishAddEventDialog(etEventTitle.getText().toString(), etEventBody.getText().toString(), startSelected, endSelected);
        }
        dismiss();
    }
}