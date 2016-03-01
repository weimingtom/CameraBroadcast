package com.idap.camerabroadcast;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;


/**
 * Video broadcast fragment
 */
@SuppressWarnings("deprecation")
public class FragmentVideoBroadcast extends Fragment{

    public static final String TAG = "FragmentVideoBroadcast";

    private static final String VIDEO_THREAD_NAME = "VIDEO_THREAD_NAME";
    private ViewGroup rootView;
    private VideoHandlerThread videoThread;
    private Handler uiHandler;
    private CameraPreview cameraPreview;
    private Recorder recorder;
    private Camera camera;

    CameraPreview.OnPreviewStartedListener previewStartedListener = new CameraPreview.OnPreviewStartedListener() {
        @Override
        public void onPreviewStarted(Camera.Size previewSize) {
            if(recorder == null){
                recorder = new Recorder(previewSize);
            }

            camera.setPreviewCallback(recorder);
            startRecording();
        }
    };

    private void createSurfaceView(Camera camera) {
        this.camera = camera;
        cameraPreview = new CameraPreview(getActivity(), camera, previewStartedListener);
        rootView.addView(cameraPreview);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHandler = new UiHandler(this);
        setRetainInstance(true);
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
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
//                startRecording();
            }
        }, 2000);


    }

    public void startRecording(){
        if(recorder != null){
            recorder.startRecording(getResources().getConfiguration().orientation);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if(recorder != null) {
            recorder.stopRecording();
        }
        videoThread.quit();
        super.onDestroy();
    }

    public  static class UiHandler extends Handler{

        private WeakReference<FragmentVideoBroadcast> fragment;

        public UiHandler(FragmentVideoBroadcast fragment) {
            this.fragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case  VideoHandlerThread.CAMERA_OPENED:
                    openCamera(msg);
                    break;
            }
        }

        private void openCamera(Message msg) {
            if(fragment != null && fragment.get() != null){
                Camera camera = (Camera) msg.obj;
                fragment.get().createSurfaceView(camera);
            }
        }


    }
}
