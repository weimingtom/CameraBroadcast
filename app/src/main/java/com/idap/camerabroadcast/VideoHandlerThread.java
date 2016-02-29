package com.idap.camerabroadcast;

import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * Created by vladimirkondratenko on 2/29/16.
 */
public class VideoHandlerThread extends HandlerThread implements Camera.PreviewCallback {

    public static final int CAMERA_OPENED = 1;
    private final Handler uiHandler;
    private final Handler handler;

    public VideoHandlerThread(Handler uiHandler, String name) {
        super(name);
        this.uiHandler = uiHandler;
        start();
        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Camera camera = Camera.open();
                notifyCameraOpened(camera);
            }
        };
    }

    public void openCamera() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Camera camera = Camera.open();
                notifyCameraOpened(camera);
            }
        });
    }

    private void notifyCameraOpened(Camera camera) {
        Message message = uiHandler.obtainMessage(CAMERA_OPENED, camera);
        uiHandler.sendMessage(message);
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(FragmentVideoBroadcast.TAG, "onPreviewFrame, threadId: " + Thread.currentThread().getId());
    }
}
