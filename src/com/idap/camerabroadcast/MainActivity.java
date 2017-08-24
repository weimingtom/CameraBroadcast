package com.idap.camerabroadcast;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        FragmentManager manager = this.getSupportFragmentManager();
        if(manager.findFragmentByTag(FragmentVideoBroadcast.TAG) == null){

            Fragment f = FragmentVideoBroadcast.newInstance();

            manager.beginTransaction()
                    .replace(android.R.id.content, f, FragmentVideoBroadcast.TAG)
                    .commit();


        }

    }
}
