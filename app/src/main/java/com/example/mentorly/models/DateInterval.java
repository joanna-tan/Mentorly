package com.example.mentorly.models;

import android.icu.util.Calendar;
import android.os.Build;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import org.parceler.Parcel;

import java.util.Date;

@Parcel
public class DateInterval implements Parcelable {
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
    public void shiftDate() {
        Calendar startTime = Calendar.getInstance();
        startTime.setTime(start);
        startTime.add(Calendar.DAY_OF_MONTH, 1);
        start = startTime.getTime();

        Calendar endTime = Calendar.getInstance();
        endTime.setTime(end);
        endTime.add(Calendar.DAY_OF_MONTH, 1);
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
