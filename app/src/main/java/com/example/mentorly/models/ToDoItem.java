package com.example.mentorly.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import java.util.Date;
import java.util.List;

@ParseClassName("ToDoItem")
public class ToDoItem extends ParseObject {
    public static final String TITLE_KEY = "title";
    public static final String BODY_KEY = "body";
    public static final String USERS_KEY = "users";
    public static final String DUE_DATE_KEY = "dueDate";

    public String getTitle() {
        return getString(TITLE_KEY);
    }

    public String getBody() {
        return getString(BODY_KEY);
    }

    public Date getDueDate() {
        return getDate(DUE_DATE_KEY);
    }

    public List getUsers() {
        return getList(USERS_KEY);
    }

    public void setTitle(String title) {
        put(TITLE_KEY, title);
    }

    public void setBody(String body) {
        put(BODY_KEY, body);
    }

    public void setUsers(List users) {
        put(USERS_KEY, users);
    }

    public void setDueDate(Date dueDate) {
        put(DUE_DATE_KEY, dueDate);
    }
}
