package com.idap.camerabroadcast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;


/**
 * CameraPreview view.
 */
@SuppressLint("ViewConstructor")
@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView
        implements SurfaceHolder.Callback {

    private static final String TAG = "CameraPreview";


    private SurfaceHolder holder;
    private Camera camera;
    private OnPreviewStartedListener previewCallback;
    private List<Camera.Size> supportedPreviewSizes;
    private Camera.Size previewSize;

    public CameraPreview(Context context, Camera camera, OnPreviewStartedListener callback) {
        super(context);
        this.camera = camera;
        previewCallback = callback;

        supportedPreviewSizes = this.camera.getParameters().getSupportedPreviewSizes();
        for (Camera.Size str : supportedPreviewSizes)
            Log.e(TAG, str.width + "/" + str.height);

        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);

            camera.release();
            camera = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.e(TAG, "surfaceChanged => w=" + w + ", h=" + h);
        if (this.holder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            camera.setParameters(parameters);
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT) {
                camera.setDisplayOrientation(90);
            }
            camera.setPreviewDisplay(this.holder);
            camera.startPreview();
            previewCallback.onPreviewStarted(previewSize);

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(supportedPreviewSizes, 640, 480);
        }

        float ratio;
        if (previewSize.height >= previewSize.width)
            ratio = (float) previewSize.height / (float) previewSize.width;
        else
            ratio = (float) previewSize.width / (float) previewSize.height;

        // One of these methods should be used, second method squishes preview slightly
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
//        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, (int) (width * ratio));
//        setMeasuredDimension((int) (width * ratio), height);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }

        return optimalSize;
    }

    interface OnPreviewStartedListener {
        void onPreviewStarted(Camera.Size previewSize);
    }

}
