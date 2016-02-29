package com.idap.camerabroadcast;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Video broadcast fragment
 */
public class FragmentVideoBroadcast extends Fragment{

    public static final String TAG = "FragmentVideoBroadcast";

    private static final String VIDEO_THREAD_NAME = "VIDEO_THREAD_NAME";
    private ViewGroup rootView;
    private VideoHandlerThread videoThread;
    private Handler uiHandler = new Handler(){
        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what){
                case  VideoHandlerThread.CAMERA_OPENED:
                    Camera camera = (Camera) msg.obj;
                    createSurfaceView(camera);
                    break;
            }
        }
    };
    private CameraPreview cameraPreview;

    private void createSurfaceView(Camera camera) {
        cameraPreview = new CameraPreview(getActivity(), camera);
        rootView.addView(cameraPreview);
    }

    public static FragmentVideoBroadcast newInstance() {

        Bundle args = new Bundle();
        FragmentVideoBroadcast fragment = new FragmentVideoBroadcast();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_broadcast, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        this.rootView = (ViewGroup) view;
        videoThread = new VideoHandlerThread(uiHandler, VIDEO_THREAD_NAME);
        videoThread.openCamera();

//        uiHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startRecording();
//            }
//        }, 2000);

    }

    public void startRecording(){
        cameraPreview.startRecording();
    }

    @Override
    public void onPause() {
        cameraPreview.stopRecording();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        videoThread.quit();
        super.onDestroy();
    }
}
