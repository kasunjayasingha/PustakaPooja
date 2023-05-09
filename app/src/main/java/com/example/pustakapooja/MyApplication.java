package com.example.pustakapooja;

import android.app.Application;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

//application class runs before launcher activity
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    //convert timestamp to proper date format
    public static final String formatTimestamp(long timestamp){
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        //format timestamp to dd/mm/yyyy
        String date = DateFormat.format("dd/MM/yyyy", cal).toString();
        return date;
    }
}
