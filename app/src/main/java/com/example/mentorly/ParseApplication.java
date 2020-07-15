package com.example.mentorly;

import android.app.Application;

import com.example.mentorly.models.Message;
import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //Register your parse models
        ParseObject.registerSubclass(Message.class);

        // set applicationId, and server server based on the values in the Heroku settings.
        // clientKey is not needed unless explicitly configured
        // any network interceptors must be added with the Configuration Builder given this syntax
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("joanna-mentorly")
                .clientKey("JMentorParseAndMoveFast")
                .server("https://joanna-mentorly.herokuapp.com/parse").build());

    }
}
