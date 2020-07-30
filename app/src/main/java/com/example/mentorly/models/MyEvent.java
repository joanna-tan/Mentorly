package com.example.mentorly.models;

import java.util.Date;
import java.util.List;

public class MyEvent {

    private Date startDate;
    private Date endDate;
    private String eventDescription;
    private String eventTitle;
    private List<String> attendees;

    public MyEvent(Date startDate, Date endDate, String eventTitle, String eventDescription, List<String> attendees) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.eventDescription = eventDescription;
        this.eventTitle = eventTitle;
        this.attendees = attendees;
    }

    public MyEvent() {
    }

    public List<String> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<String> attendees) {
        this.attendees = attendees;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }
}
