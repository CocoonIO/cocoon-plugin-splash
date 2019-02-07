package com.ludei.splash.main.android;

import android.os.Bundle;

import org.apache.cordova.CordovaActivity;


public class MainActivity extends CordovaActivity {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }

}
