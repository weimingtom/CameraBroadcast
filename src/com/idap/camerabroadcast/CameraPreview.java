package com.idap.camerabroadcast;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.bytedeco.javacv.FrameRecorder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.List;


/**
 * Created by vladimirkondratenko on 2/29/16.
 */
public class CameraPreview extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraPreview";

    private String ffmpeg_link = "rtmp://101.101.106.18:1935/live/stream";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;
    private Frame yuvImage;
    private FFmpegFrameRecorder recorder;
    private int sampleAudioRateInHz = 44100;
    private int frameRate = 30;
    private FFmpegFrameFilter filter;
    private long startTime;
    private boolean recording;
    private AudioRecord audioRecord;
    private boolean runAudioThread;
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // supported preview sizes
        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        for (Camera.Size str : mSupportedPreviewSizes)
            Log.e(TAG, str.width + "/" + str.height);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // empty. surfaceChanged will take care of stuff
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);

            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.e(TAG, "surfaceChanged => w=" + w + ", h=" + h);
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or reformatting changes here
        // start preview with new settings
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT) {
                mCamera.setDisplayOrientation(90);
            }
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
//            startRecording();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, 640, 480);
        }

        float ratio;
        if (mPreviewSize.height >= mPreviewSize.width)
            ratio = (float) mPreviewSize.height / (float) mPreviewSize.width;
        else
            ratio = (float) mPreviewSize.width / (float) mPreviewSize.height;

        // One of these methods should be used, second method squishes preview slightly
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

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    public void startRecording() {
        initRecorder();


        try {
            recorder.start();
            setRecording(true);
            startTime = System.currentTimeMillis();
//            recording = true;
            audioThread.start();

        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    private void initRecorder() {
        yuvImage = new Frame(mPreviewSize.width, mPreviewSize.height, Frame.DEPTH_UBYTE, 2);
        Log.i(TAG, "create yuvImage");


        recorder = new FFmpegFrameRecorder(ffmpeg_link, mPreviewSize.width, mPreviewSize.height,1);

        recorder.setFormat("flv");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoBitrate(1500000);
        recorder.setSampleRate(sampleAudioRateInHz);
        recorder.setFrameRate(frameRate);


        filter = new FFmpegFrameFilter("transpose=clock", mPreviewSize.width, mPreviewSize.height);
        filter.setPixelFormat(avutil.AV_PIX_FMT_NV21);
        try {
            filter.start();
        } catch (FrameFilter.Exception e) {
            e.printStackTrace();
        }

        audioRecordRunnable = new AudioRecordRunnable();
        audioThread = new Thread(audioRecordRunnable);
        runAudioThread = true;
    }

    private synchronized  void setRecording(boolean recording){
        this.recording = recording;
    }

    private synchronized boolean isRecording(){
        return recording;
    }

    public void stopRecording(){

        recording = false;
        try {
            recorder.stop();
            recorder.release();
        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {


        if(!isRecording()){
            return;
        }

        if (yuvImage != null && isRecording()) {
            ((ByteBuffer) yuvImage.image[0].position(0)).put(data);

            Log.d(TAG, "onPreviewFrame");

            long t = 1000 * (System.currentTimeMillis() - startTime);


            recorder.setTimestamp(t);

            try {
                filter.push(yuvImage);
                Frame frame;
                while ((frame = filter.pull()) != null) {
                    recorder.record(frame);
                    Log.d(TAG, "frameRecorded");
                }
            } catch (FrameFilter.Exception | FrameRecorder.Exception e) {
                e.printStackTrace();
            }

        }
    }


    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            audioData = ShortBuffer.allocate(bufferSize);

            Log.d(TAG, "audioRecord.startRecording()");
            audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (runAudioThread) {
                //Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0) {
                    Log.v(TAG,"bufferReadResult: " + bufferReadResult);
                    // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                    // Why?  Good question...
                    if (recording) {
                        try {
                            recorder.recordSamples(audioData);
                            //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            Log.v(TAG,e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.v(TAG,"AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.v(TAG,"audioRecord released");
            }
        }
    }
}
