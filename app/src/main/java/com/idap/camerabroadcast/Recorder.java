package com.idap.camerabroadcast;

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

public class Recorder implements Camera.PreviewCallback{

    private static final String TAG = "Recorder";
    private String ffmpeg_link = "rtmp://188.166.72.133:1934/live/stream";

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
    private Camera.Size mPreviewSize;



    public void startRecording(Camera.Size mPreviewSize) {
        this.mPreviewSize = mPreviewSize;
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

        setRecording(false);
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
