package com.example.mentorly.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("Message")
public class Message extends ParseObject {
    public static final String TO_ID_KEY = "toId";
    public static final String FROM_ID_KEY = "fromId";
    public static final String BODY_KEY = "body";
    public static final String SAVED_BY_KEY = "savedBy";

    public ParseUser getToId() {
        return getParseUser(TO_ID_KEY);
    }

    public ParseUser getFromId() {
        return getParseUser(FROM_ID_KEY);
    }

    public String getBody() {
        return getString(BODY_KEY);
    }

    public void setToId(ParseUser toId) {
        put(TO_ID_KEY, toId);
    }

    public void setFromId(ParseUser fromId) {
        put(FROM_ID_KEY, fromId);
    }

    public void setBody(String body) {
        put(BODY_KEY, body);
    }

}

