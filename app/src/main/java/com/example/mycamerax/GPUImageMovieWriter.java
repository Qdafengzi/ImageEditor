package com.example.mycamerax;

import android.annotation.TargetApi;
import android.opengl.EGL14;

import com.example.mycamerax.encoder.EglCore;
import com.example.mycamerax.encoder.MediaEncoder;
import com.example.mycamerax.encoder.MediaMuxerWrapper;
import com.example.mycamerax.encoder.MediaVideoEncoder;
import com.example.mycamerax.encoder.WindowSurface;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

@TargetApi(18)
public class GPUImageMovieWriter extends GPUImageFilter {
    private MediaMuxerWrapper mMuxer;
    private MediaVideoEncoder mVideoEncoder;
    private WindowSurface mCodecInput;

    private EGLSurface mEGLScreenSurface;
    private EGL10 mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EglCore mEGLCore;

    private boolean mIsRecording = false;

    private int frameRate = 30;
    public GPUImageErrorListener gpuImageErrorListener;

    public boolean drawVideo = false;

    public interface StartRecordListener {
        void onRecordStart();

        void onRecordError(Exception e);
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    @Override
    public void onInit() {
        super.onInit();
        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetCurrentDisplay();
        mEGLContext = mEGL.eglGetCurrentContext();
        mEGLScreenSurface = mEGL.eglGetCurrentSurface(EGL10.EGL_DRAW);
    }

    @Override
    public synchronized void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        // Draw on screen surface
        super.onDraw(textureId, cubeBuffer, textureBuffer);

        if (mIsRecording && drawVideo) {
            drawVideo = false;
            // create encoder surface
            if (mCodecInput == null) {
                if (mVideoEncoder == null || mVideoEncoder.getSurface() == null) {
                    return;
                }
                mEGLCore = new EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE);
                mCodecInput = new WindowSurface(mEGLCore, mVideoEncoder.getSurface(), false);
            }

            // Draw on encoder surface
            mCodecInput.makeCurrent();
            super.onDraw(textureId, cubeBuffer, textureBuffer);
            if (mCodecInput != null) {
                mCodecInput.swapBuffers();
            }
            mVideoEncoder.frameAvailableSoon();
        }

        // Make screen surface be current surface
        mEGL.eglMakeCurrent(mEGLDisplay, mEGLScreenSurface, mEGLScreenSurface, mEGLContext);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseEncodeSurface();
    }

    public synchronized void prepareRecording(final String outputPath, final int width, final int height) {
        runOnDraw(() -> {
            if (mIsRecording) {
                return;
            }

            try {
                mMuxer = new MediaMuxerWrapper(outputPath);

                // for video capturing
                mVideoEncoder = new MediaVideoEncoder(mMuxer, mMediaEncoderListener, width, height, frameRate);

                mMuxer.prepare();

            } catch (Exception e) {
//                if (!BuildConfig.DEBUG) {
//                    FirebaseCrashlytics.getInstance().recordException(e);
//                }
                e.printStackTrace();
                if (gpuImageErrorListener != null) {
                    gpuImageErrorListener.onError();
                }
            }
        });
    }

    public synchronized void stopRecording(final MainActivity.RecordListener recordListener) {
        runOnDraw(() -> {
            if (!mIsRecording) {
                return;
            }

            try {
                mMuxer.stopRecording(recordListener);
                mIsRecording = false;
                releaseEncodeSurface();
            } catch (Exception e) {
//                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
    }

    private void releaseEncodeSurface() {
        if (mEGLCore != null) {
            mEGLCore.makeNothingCurrent();
            mEGLCore.release();
            mEGLCore = null;
        }

        if (mCodecInput != null) {
            mCodecInput.release();
            mCodecInput = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder = null;
        }
        if (mMuxer != null) {
            mMuxer = null;
        }
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
        }

        @Override
        public void onMuxerStopped() {
        }
    };

    public synchronized void startRecording(StartRecordListener startRecordListener) {
        runOnDraw(() -> {
            try {
                if (mVideoEncoder != null) {
                    mMuxer.prepare();
                    mMuxer.startRecording();

                    mIsRecording = true;

                    if (startRecordListener != null) {
                        startRecordListener.onRecordStart();
                    }else {
                        XLogger.d("录制 错误");
                    }
                }else {
                    XLogger.d("录制 错误");
                }
            } catch (Exception e) {
//                if (!BuildConfig.DEBUG) {
//                    FirebaseCrashlytics.getInstance().recordException(e);
//                }
                if (startRecordListener != null) {
                    startRecordListener.onRecordError(e);
                }
                XLogger.d("录制出错："+e.getMessage());
            }
        });
    }

    public interface GPUImageErrorListener {
        void onError();
    }
}
