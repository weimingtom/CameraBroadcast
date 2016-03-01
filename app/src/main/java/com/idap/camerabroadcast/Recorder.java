package com.idap.camerabroadcast;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.bytedeco.javacv.FrameRecorder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

@SuppressWarnings("deprecation")
public class Recorder implements Camera.PreviewCallback{

    private static final String TAG = "Recorder";
    private static final String TEST_LINK = "rtmp://188.166.72.133:1934/live/stream";
    private static final int SAMPLE_AUDIO_RATE_IN_HZ = 44100;
    private static final int FRAME_RATE = 30;

    private final  Camera.Size mPreviewSize;

    private Frame yuvImage;
    private FFmpegFrameRecorder recorder;
    private FFmpegFrameFilter filter;
    private long startTime;
    private boolean recording;
    private boolean runAudioThread;
    private Thread audioThread;
    private int screenOrientation;

    public Recorder(Camera.Size previewSize) {
        mPreviewSize = previewSize;
    }


    public void startRecording(int screenOrientation) {

        this.screenOrientation = screenOrientation;


        if(recorder != null){
            Log.d(TAG, "Recording already started");
            return;
        }

        initRecorder();


        try {
            recorder.start();
            setRecording(true);
            startTime = System.currentTimeMillis();
            recording = true;
            audioThread.start();

        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    private void initRecorder() {
        yuvImage = new Frame(mPreviewSize.width, mPreviewSize.height, Frame.DEPTH_UBYTE, 2);
        Log.i(TAG, "create yuvImage");


        recorder = new FFmpegFrameRecorder(TEST_LINK, mPreviewSize.width, mPreviewSize.height,1);

        recorder.setFormat("flv");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoBitrate(1500000);
        recorder.setSampleRate(SAMPLE_AUDIO_RATE_IN_HZ);
        recorder.setFrameRate(FRAME_RATE);


        filter = new FFmpegFrameFilter("transpose=clock", mPreviewSize.width, mPreviewSize.height);
        filter.setPixelFormat(avutil.AV_PIX_FMT_NV21);
        try {
            filter.start();
        } catch (FrameFilter.Exception e) {
            e.printStackTrace();
        }

        AudioRecordRunnable audioRecordRunnable = new AudioRecordRunnable();
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

        setRecording(false);
        try {
            recorder.stop();
            recorder.release();
            runAudioThread = false;
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
                if(screenOrientation == Configuration.ORIENTATION_PORTRAIT){
                    filter.push(yuvImage);
                    Frame frame;
                    while ((frame = filter.pull()) != null) {
                        recorder.record(frame);
                        Log.d(TAG, "frameRecorded");
                    }
                }else{
                    recorder.record(yuvImage);
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

            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_AUDIO_RATE_IN_HZ,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_AUDIO_RATE_IN_HZ,
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
            audioRecord.stop();
            audioRecord.release();
            Log.v(TAG,"audioRecord released");
        }
    }
}
