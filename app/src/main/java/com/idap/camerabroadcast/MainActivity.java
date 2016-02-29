package com.idap.camerabroadcast;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        FragmentManager manager = getFragmentManager();
        if(manager.findFragmentByTag(FragmentVideoBroadcast.TAG) == null){

            Fragment f = FragmentVideoBroadcast.newInstance();

            manager.beginTransaction()
                    .replace(android.R.id.content, f, FragmentVideoBroadcast.TAG)
                    .commit();


        }

    }
}
