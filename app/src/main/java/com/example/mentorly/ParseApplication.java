package com.example.mentorly;

import android.app.Application;

import com.example.mentorly.models.Message;
import com.example.mentorly.models.PairRequest;
import com.example.mentorly.models.ToDoItem;
import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //Register your parse models
        ParseObject.registerSubclass(Message.class);
        ParseObject.registerSubclass(PairRequest.class);
        ParseObject.registerSubclass(ToDoItem.class);

        // set applicationId, and server server based on the values in the Heroku settings.
        // clientKey is not needed unless explicitly configured
        // any network interceptors must be added with the Configuration Builder given this syntax
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("joanna-mentorly1")
                .clientKey("JMentorParseAndMoveFast")
                .server("https://joanna-mentorly1.herokuapp.com/parse").build());

    }
}
