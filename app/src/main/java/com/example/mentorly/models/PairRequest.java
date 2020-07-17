package com.example.mentorly.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

@ParseClassName("PairRequest")
public class PairRequest extends ParseObject {
    public static final String USER_SENDING_KEY = "userSendingReq";
    public static final String USER_RECEIVING_KEY = "userReceivingReq";
    public static final String CHECK_ACCEPTED_KEY = "requestAccepted";
    public static final String CHECK_REJECTED_KEY = "requestRejected";

    public ParseUser getUserSending() {
        return getParseUser(USER_SENDING_KEY);
    }

    public ParseUser getUserReceiving() {
        return getParseUser(USER_RECEIVING_KEY);
    }

    public String getIsAccepted() {
        return getString(CHECK_ACCEPTED_KEY);
    }

    public String getIsRejected() {
        return getString(CHECK_REJECTED_KEY);
    }

    public void setUserSending(ParseUser userSending) {
        put(USER_SENDING_KEY, userSending);
    }

    public void setUserReceiving(ParseUser userReceiving) {
        put(USER_RECEIVING_KEY, userReceiving);
    }

    public void setIsAccepted (boolean isAccepted) {
        put(CHECK_ACCEPTED_KEY, isAccepted);
    }

    public void setIsRejected (boolean isRejected) {
        put(CHECK_REJECTED_KEY, isRejected);
    }
}
