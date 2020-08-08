package com.example.mentorly.models;

import android.icu.util.Calendar;
import android.os.Build;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import org.parceler.Parcel;

import java.util.Date;

@Parcel
public class DateInterval implements Parcelable {

    public static final int HOUR_END_OF_DAY = 18; // after 6pm is "end of day"
    public static final int HOUR_START_OF_DAY = 8; // 8am is "start of day"
    Date start;
    Date end;

    public DateInterval() {

    }

    protected DateInterval(android.os.Parcel in) {
    }

    public static final Creator<DateInterval> CREATOR = new Creator<DateInterval>() {
        @Override
        public DateInterval createFromParcel(android.os.Parcel in) {
            return new DateInterval(in);
        }

        @Override
        public DateInterval[] newArray(int size) {
            return new DateInterval[size];
        }
    };

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public DateInterval(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    // Determine overlap of two date intervals
    public boolean overlapsWith(DateInterval other) {
        return start.before(other.end) && other.start.before(end);
    }

    // Move the interval one hour forward
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void shiftHour() {
        Calendar startTime = Calendar.getInstance();
        startTime.setTime(start);
        startTime.add(Calendar.HOUR_OF_DAY, 1);
        start = startTime.getTime();

        Calendar endTime = Calendar.getInstance();
        endTime.setTime(start);
        endTime.add(Calendar.HOUR_OF_DAY, 1);
        end = endTime.getTime();
    }

    // Move the interval one hour forward
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void shiftHalfHour() {
        Calendar startTime = Calendar.getInstance();
        startTime.setTime(start);
        startTime.add(Calendar.MINUTE, 30);
        start = startTime.getTime();

        Calendar endTime = Calendar.getInstance();
        endTime.setTime(start);
        endTime.add(Calendar.HOUR_OF_DAY, 1);
        end = endTime.getTime();
    }

    // Move the interval to the next day at START_OF_DAY (i.e tomorrow at 8am)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void shiftToStartOfDay() {
        Calendar startTime = Calendar.getInstance();
        startTime.setTime(start);
        startTime.add(Calendar.DAY_OF_MONTH, 1);
        // set the start time to startOfDay
        startTime.set(Calendar.HOUR_OF_DAY, HOUR_START_OF_DAY);
        start = startTime.getTime();

        //end time is one hour after start time
        Calendar endTime = Calendar.getInstance();
        endTime.setTime(start);
        endTime.add(Calendar.HOUR_OF_DAY, 1);
        end = endTime.getTime();
    }

    // Return true if the time interval ends after HOUR_END_OF_DAY
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean isEndOfDay () {
        Calendar startTime = Calendar.getInstance();
        startTime.setTime(start);
        return startTime.get(Calendar.HOUR_OF_DAY) > HOUR_END_OF_DAY;
    }

    // Return true if the time interval starts before HOUR_START_OF_DAY
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean isBeforeStartOfDay () {
        Calendar startTime = Calendar.getInstance();
        startTime.setTime(start);
        return startTime.get(Calendar.HOUR_OF_DAY) < HOUR_START_OF_DAY;
    }

    // Reset the interval to the input start time to +1 hour
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void resetInterval(Date newStart) {
        Calendar startTime = Calendar.getInstance();
        startTime.setTime(newStart);
        start = startTime.getTime();

        // set the end time to one hour after new start
        Calendar endTime = Calendar.getInstance();
        endTime.setTime(start);
        endTime.add(Calendar.HOUR_OF_DAY, 1);
        end = endTime.getTime();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel parcel, int i) {
        parcel.writeValue(start);
        parcel.writeValue(end);
    }
}
