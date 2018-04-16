package com.qrphoto.qr;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qrphoto.qr.qwcode.camera.CameraManager;
import com.qrphoto.qr.qwcode.decode.InactivityTimer;
import com.zbar.lib.ZbarManager;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class QrActivity extends FragmentActivity implements SurfaceHolder.Callback {
    private RelativeLayout mContainer;
    private RelativeLayout mCropLayout;
    private CaptureActivityHandler handler;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.50f;
    private boolean vibrate;
    private int x = 0;
    private int y = 0;
    private int cropWidth = 0;
    private int cropHeight = 0;
    private String mSubstring;
    private boolean success = false;
    private ImageView mCapture_scan_line;
    private TextView mText;
    private TextView mTitle;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public void setCropWidth(int cropWidth) {
        this.cropWidth = cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public void setCropHeight(int cropHeight) {
        this.cropHeight = cropHeight;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        mText = (TextView) findViewById(R.id.text);
        mTitle = (TextView) findViewById(R.id.title);
        mContainer = (RelativeLayout) findViewById(R.id.capture_containter);
        mCropLayout = (RelativeLayout) findViewById(R.id.capture_crop_layout);
        mCapture_scan_line = (ImageView) findViewById(R.id.capture_scan_line);
        initData();
    }

    public void initData() {
        String title = getIntent().getStringExtra("title");
        String text = getIntent().getStringExtra("text");
        if (title!=null){
            mTitle.setText(title);
        }
        if (text!=null){
            mText.setText(text);
        }
        CameraManager.init(this);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        Animation animation = AnimationUtils.loadAnimation(QrActivity.this, R.anim.qr_scale);
        mCapture_scan_line.startAnimation(animation);

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_preview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void handleDecode(String result) {
        playBeepSoundAndVibrate();
        inactivityTimer.onActivity();
        Intent intent=new Intent();
        intent.putExtra("result",result);
        setResult(RESULT_OK,intent);
        finish();
    }


    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);

            Point point = CameraManager.get().getCameraResolution();
            int width = point.y;
            int height = point.x;

            int x = mCropLayout.getLeft() * width / mContainer.getWidth();
            int y = mCropLayout.getTop() * height / mContainer.getHeight();

            int cropWidth = mCropLayout.getWidth() * width
                    / mContainer.getWidth();
            int cropHeight = mCropLayout.getHeight() * height
                    / mContainer.getHeight();

            setX(x);
            setY(y);
            setCropWidth(cropWidth);
            setCropHeight(cropHeight);

        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    public Handler getHandler() {
        return handler;
    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    public class CaptureActivityHandler extends Handler {

        DecodeThread decodeThread = null;
        private String state;

        public CaptureActivityHandler() {
            decodeThread = new DecodeThread();
            decodeThread.start();
            state = "SUCCESS";
            CameraManager.get().startPreview();
            restartPreviewAndDecode();
        }

        @Override
        public void handleMessage(Message message) {

            if (message.what == R.id.edu_auto_focus) {
                if (state == "PREVIEW") {
                    CameraManager.get().requestAutoFocus(this, R.id.edu_auto_focus);
                }

            } else if (message.what == R.id.edu_restart_preview) {
                restartPreviewAndDecode();

            } else if (message.what == R.id.edu_decode_succeeded) {
                state = "SUCCESS";
                handleDecode((String) message.obj);// 解析成功，回调

            } else if (message.what == R.id.edu_decode_failed) {
                state = "PREVIEW";
                CameraManager.get().requestPreviewFrame(decodeThread.getHandler(),
                        R.id.edu_decode);

            }

        }

        public void quitSynchronously() {
            state = "DONE";
            CameraManager.get().stopPreview();
            removeMessages(R.id.edu_decode_succeeded);
            removeMessages(R.id.edu_decode_failed);
            removeMessages(R.id.edu_decode);
            removeMessages(R.id.edu_auto_focus);
        }

        private void restartPreviewAndDecode() {
            if ("SUCCESS".equals(state)) {
                state = "PREVIEW";
                CameraManager.get().requestPreviewFrame(decodeThread.getHandler(),
                        R.id.edu_decode);
                CameraManager.get().requestAutoFocus(this, R.id.edu_auto_focus);
            }
        }

    }

    public class DecodeThread extends Thread {

        private Handler handler;
        private final CountDownLatch handlerInitLatch;

        DecodeThread() {
            handlerInitLatch = new CountDownLatch(1);
        }

        Handler getHandler() {
            try {
                handlerInitLatch.await();
            } catch (InterruptedException ie) {
                // continue?
            }
            return handler;
        }

        @Override
        public void run() {
            Looper.prepare();
            handler = new DecodeHandler();
            handlerInitLatch.countDown();
            Looper.loop();
        }

    }

    public class DecodeHandler extends Handler {

        @Override
        public void handleMessage(Message message) {
            if (message.what == R.id.edu_decode) {
                decode((byte[]) message.obj, message.arg1, message.arg2);

            } else if (message.what == R.id.edu_quit) {
                Looper.myLooper().quit();

            }
        }

        private void decode(byte[] data, int width, int height) {
            byte[] rotatedData = new byte[data.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++)
                    rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
            int tmp = width;// Here we are swapping, that's the difference to #11
            width = height;
            height = tmp;

            ZbarManager manager = new ZbarManager();
            String result = manager.decode(rotatedData, width, height, true,
                    getX(), getY(), getCropWidth(),
                    getCropHeight());

            if (result != null) {
                if (null != getHandler()) {
                    Message msg = new Message();
                    msg.obj = result;
                    msg.what = R.id.edu_decode_succeeded;
                    getHandler().sendMessage(msg);
                }
            } else {
                if (null != getHandler()) {
                    getHandler().sendEmptyMessage(R.id.edu_decode_failed);
                }
            }
        }
    }
}
