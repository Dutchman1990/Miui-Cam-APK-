package com.android.camera.module;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.net.Uri;
import android.opengl.GLSurfaceView.Renderer;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.recyclerview.C0049R;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.camera.Camera;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraDisabledException;
import com.android.camera.CameraHardwareException;
import com.android.camera.CameraHolder;
import com.android.camera.CameraPreferenceActivity;
import com.android.camera.CameraScreenNail;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.ExifHelper;
import com.android.camera.LocationManager;
import com.android.camera.OnClickAttr;
import com.android.camera.PanoUtil;
import com.android.camera.PictureSize;
import com.android.camera.PictureSizeManager;
import com.android.camera.Thumbnail;
import com.android.camera.Util;
import com.android.camera.camera_adapter.CameraPadOne;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.draw_mode.DrawExtTexAttribute;
import com.android.camera.panorama.MorphoPanoramaGP;
import com.android.camera.panorama.MorphoPanoramaGP.InitParam;
import com.android.camera.panorama.NativeMemoryAllocator;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.Storage;
import com.android.camera.ui.GLTextureView;
import com.android.camera.ui.GLTextureView.EGLShareContextGetter;
import com.android.camera.ui.PanoMovingIndicatorView;
import com.android.camera.ui.V6ModeExitView;
import com.android.gallery3d.ui.GLCanvas;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public class MorphoPanoramaModule extends BaseModule implements PreviewCallback, PictureCallback {
    private static final String TAG = MorphoPanoramaModule.class.getSimpleName();
    private static final boolean USE_PREVIEW_IMAGE = Device.isPanoUsePreviewFrame();
    private static int sSaveOutputType = 1;
    private int MAX_DST_IMG_WIDTH = 30000;
    private final boolean USE_MULTI_THREAD = true;
    private boolean mAeLockSupported;
    private int mAppDeviceRotation = 0;
    private int mAppPanoramaDirection = 0;
    private int mAppPanoramaDirectionSettings = 0;
    private int mAttachPosOffsetX;
    private int mAttachPosOffsetY;
    protected boolean mAwbLockSupported;
    private byte[] mCameraPreviewBuff;
    private int mCameraState;
    private ArrayList<CaptureInfo> mCaptureInfoList = new ArrayList();
    private int mCntProcessd = 0;
    private int mCntReqShoot = 0;
    private int mDeviceOrientationAtCapture;
    private int[] mDirection = new int[1];
    private Bitmap mDispPreviewImage;
    private float mFrameRatio;
    private int[] mImageID = new int[1];
    private InitParam mInitParam;
    private boolean mIsShooting = false;
    private View mLeftIndicator;
    private Location mLocation;
    protected final Handler mMainHandler = new MainHandler();
    private MorphoPanoramaGP mMorphoPanoramaGP;
    private byte[] mMotionData = new byte[256];
    private int mMotionlessThres = 32768;
    private View mMoveReferenceLine;
    private int[] mMoveSpeed = new int[1];
    private PanoMovingIndicatorView mMovingDirectionView;
    private ImageView mPanoramaPreview;
    private ViewGroup mPanoramaViewRoot;
    private int mPictureHeight;
    private int mPictureWidth;
    private int mPrevDirection;
    private int mPreviewCount;
    private int mPreviewCroppingAdjustByAuto = 0;
    private final int mPreviewCroppingRatio = 10;
    private float mPreviewDisplayRatio;
    private int mPreviewHeight;
    private Bitmap mPreviewImage;
    private int mPreviewRefY;
    private int mPreviewSkipCount;
    private int mPreviewWidth;
    private boolean mRequestTakePicture = false;
    private View mRightIndicator;
    private SaveOutputImageTask mSaveOutputImageTask;
    private Object mSensorSyncObj = new Object();
    private SetupCameraThread mSetupCameraThread;
    private String mSnapshotFocusMode = "auto";
    private ConditionVariable mStartPreviewPrerequisiteReady = new ConditionVariable();
    private int[] mStatus = new int[1];
    private GLTextureView mStillPreview;
    private View mStillPreviewHintArea;
    private StillPreviewRender mStillPreviewRender;
    private int mStillPreviewTextureHeight;
    private int mStillPreviewTextureOffsetX;
    private int mStillPreviewTextureOffsetY;
    private int mStillPreviewTextureWidth;
    private ArrayList<StillImageData> mStillProcList;
    private StillProcTask mStillProcTask = null;
    private boolean mSwitchingCamera = false;
    private Object mSyncObj = new Object();
    private String mTargetFocusMode = "continuous-picture";
    private long mTimeTaken;
    private TextView mUseHint;
    private View mUseHintArea;
    private int mUseImage = 0;
    private boolean mUseSensorAWF = false;
    private int mUseSensorThres = 0;
    private int mUseThres = 10;
    private AsyncTask<Void, Void, Void> mWaitProcessorTask;
    private boolean mWaitingFirstFrame = false;

    class C01092 implements EGLShareContextGetter {
        C01092() {
        }

        public EGLContext getShareContext() {
            return MorphoPanoramaModule.this.getUIController().getGLView().getEGLContext();
        }
    }

    class CaptureInfo {
        int mId;
        int mStatus;

        public CaptureInfo(int i, int i2) {
            this.mId = i;
            this.mStatus = i2;
        }
    }

    private class MainHandler extends Handler {
        private MainHandler() {
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    MorphoPanoramaModule.this.getWindow().clearFlags(128);
                    return;
                case 2:
                    if (MorphoPanoramaModule.this.getUIController().getPreviewPage().isPreviewPageVisible()) {
                        MorphoPanoramaModule.this.mActivity.setBlurFlag(false);
                    }
                    MorphoPanoramaModule.this.ignoreTouchEvent(false);
                    return;
                case 3:
                    MorphoPanoramaModule.this.onCameraException();
                    return;
                case 4:
                    CameraSettings.changeUIByPreviewSize(MorphoPanoramaModule.this.mActivity, CameraSettings.getUIStyleByPreview(MorphoPanoramaModule.this.mPreviewWidth, MorphoPanoramaModule.this.mPreviewHeight));
                    MorphoPanoramaModule.this.getUIController().getPreviewFrame().setAspectRatio(CameraSettings.getPreviewAspectRatio(MorphoPanoramaModule.this.mPreviewWidth, MorphoPanoramaModule.this.mPreviewHeight));
                    MorphoPanoramaModule.this.initCommenConfig();
                    return;
                case 5:
                    MorphoPanoramaModule.this.mSetupCameraThread = null;
                    MorphoPanoramaModule.this.getUIController().onCameraOpen();
                    sendEmptyMessageDelayed(2, 100);
                    int uIStyleByPreview = CameraSettings.getUIStyleByPreview(MorphoPanoramaModule.this.mPreviewWidth, MorphoPanoramaModule.this.mPreviewHeight);
                    if (uIStyleByPreview != MorphoPanoramaModule.this.mUIStyle) {
                        MorphoPanoramaModule.this.mUIStyle = uIStyleByPreview;
                        MorphoPanoramaModule.this.changePreviewSurfaceSize();
                    }
                    MorphoPanoramaModule.this.initPreviewLayout();
                    MorphoPanoramaModule.this.computeFrameRatio();
                    MorphoPanoramaModule.this.showSmallPreview();
                    MorphoPanoramaModule.this.enableCameraControls(true);
                    MorphoPanoramaModule.this.mSwitchingCamera = false;
                    return;
                case 6:
                    MorphoPanoramaModule.this.setProgressUI(MorphoPanoramaModule.this.mIsShooting);
                    return;
                default:
                    return;
            }
        }
    }

    private class SaveOutputImageTask extends AsyncTask<Void, Integer, Integer> {
        boolean mSaveImage;
        long start_time;

        SaveOutputImageTask(Context context, boolean z) {
            this.mSaveImage = z;
        }

        protected Integer doInBackground(Void... voidArr) {
            Log.v(MorphoPanoramaModule.TAG, "doInBackground start");
            MorphoPanoramaModule.this.finishAttachStillImageTask();
            int end = MorphoPanoramaModule.this.mMorphoPanoramaGP.end();
            if (this.mSaveImage) {
                if (end != 0) {
                    Log.e(MorphoPanoramaModule.TAG, String.format("%s:end() -> 0x%x", new Object[]{MorphoPanoramaModule.TAG, Integer.valueOf(end)}));
                }
                Rect rect = new Rect();
                if ((MorphoPanoramaModule.sSaveOutputType & 2) > 0) {
                    rect.setEmpty();
                    if (MorphoPanoramaModule.this.mMorphoPanoramaGP.getBoundingRect(rect) != 0) {
                        Log.e(MorphoPanoramaModule.TAG, String.format("getBoundingRect() -> 0x%x", new Object[]{MorphoPanoramaModule.TAG, Integer.valueOf(end)}));
                    }
                    MorphoPanoramaModule.this.saveOutputJpeg(MorphoPanoramaModule.this.createNameString(MorphoPanoramaModule.this.mTimeTaken, 2), rect);
                }
                if ((MorphoPanoramaModule.sSaveOutputType & 1) > 0) {
                    rect.setEmpty();
                    if (MorphoPanoramaModule.this.mMorphoPanoramaGP.getClippingRect(rect) != 0) {
                        Log.e(MorphoPanoramaModule.TAG, String.format("getClippingRect() -> 0x%x", new Object[]{MorphoPanoramaModule.TAG, Integer.valueOf(end)}));
                    }
                    MorphoPanoramaModule.this.saveOutputJpeg(MorphoPanoramaModule.this.createNameString(MorphoPanoramaModule.this.mTimeTaken, 1), rect);
                }
            }
            Log.v(MorphoPanoramaModule.TAG, "doInBackground end");
            return null;
        }

        protected void onPostExecute(Integer num) {
            Log.v(MorphoPanoramaModule.TAG, "SaveOutputImageTask onPostExecute");
            MorphoPanoramaModule.this.mMorphoPanoramaGP.finish();
            MorphoPanoramaModule.this.mMorphoPanoramaGP = null;
            MorphoPanoramaModule.this.mPanoramaPreview.setImageBitmap(null);
            if (MorphoPanoramaModule.this.mDispPreviewImage != null) {
                MorphoPanoramaModule.this.mDispPreviewImage.eraseColor(0);
            }
            if (!MorphoPanoramaModule.this.mPaused) {
                MorphoPanoramaModule.this.setProgressUI(false);
                MorphoPanoramaModule.this.mActivity.getThumbnailUpdater().updateThumbnailView();
            }
            if (MorphoPanoramaModule.this.mCameraDevice != null) {
                if (MorphoPanoramaModule.this.mAeLockSupported) {
                    MorphoPanoramaModule.this.mParameters.setAutoExposureLock(false);
                }
                if (MorphoPanoramaModule.this.mAwbLockSupported) {
                    MorphoPanoramaModule.this.mParameters.setAutoWhiteBalanceLock(false);
                }
                MorphoPanoramaModule.this.mParameters.setFocusMode(MorphoPanoramaModule.this.mTargetFocusMode);
                MorphoPanoramaModule.this.mCameraDevice.setParameters(MorphoPanoramaModule.this.mParameters);
                MorphoPanoramaModule.this.resetToPreview();
                MorphoPanoramaModule.this.enableCameraControls(true);
            }
            MorphoPanoramaModule.this.mIsShooting = false;
            if (MorphoPanoramaModule.this.mCameraState != 0) {
                MorphoPanoramaModule.this.mCameraState = 1;
            }
            Log.d(MorphoPanoramaModule.TAG, String.format("[MORTIME] PanoramaFinish time = %d", new Object[]{Long.valueOf(System.currentTimeMillis() - this.start_time)}));
        }

        protected void onPreExecute() {
            this.start_time = System.currentTimeMillis();
            MorphoPanoramaModule.this.mMainHandler.sendEmptyMessageDelayed(6, 400);
        }
    }

    private class SetupCameraThread extends Thread {
        private volatile boolean mCancelled;

        private SetupCameraThread() {
        }

        public void cancel() {
            this.mCancelled = true;
        }

        public void run() {
            try {
                if (!this.mCancelled) {
                    if (MorphoPanoramaModule.this.mCameraId == -1) {
                        MorphoPanoramaModule.this.mCameraId = 0;
                    }
                    CameraDataAnalytics.instance().trackEventTime("open_camera_times_key");
                    MorphoPanoramaModule.this.mCameraDevice = Util.openCamera(MorphoPanoramaModule.this.mActivity, MorphoPanoramaModule.this.mCameraId);
                    MorphoPanoramaModule.this.mCameraDevice.setHardwareListener(MorphoPanoramaModule.this);
                    Log.v(MorphoPanoramaModule.TAG, "SetupCameraThread mCameraDevice=" + MorphoPanoramaModule.this.mCameraDevice);
                    MorphoPanoramaModule.this.mParameters = MorphoPanoramaModule.this.mCameraDevice.getParameters();
                    MorphoPanoramaModule.this.initializeCapabilities();
                    MorphoPanoramaModule.this.mStartPreviewPrerequisiteReady.block();
                    if (!this.mCancelled) {
                        MorphoPanoramaModule.this.setDisplayOrientation();
                        MorphoPanoramaModule.this.setupCaptureParams();
                        MorphoPanoramaModule.this.configureCamera(MorphoPanoramaModule.this.mParameters);
                        MorphoPanoramaModule.this.mMainHandler.sendEmptyMessage(4);
                        if (!this.mCancelled) {
                            MorphoPanoramaModule.this.startCameraPreview();
                            MorphoPanoramaModule.this.mMainHandler.sendEmptyMessage(5);
                            Log.v(MorphoPanoramaModule.TAG, "SetupCameraThread done");
                        }
                    }
                }
            } catch (CameraHardwareException e) {
                MorphoPanoramaModule.this.mSetupCameraThread = null;
                MorphoPanoramaModule.this.mOpenCameraFail = true;
                MorphoPanoramaModule.this.mMainHandler.sendEmptyMessage(3);
            } catch (CameraDisabledException e2) {
                MorphoPanoramaModule.this.mSetupCameraThread = null;
                MorphoPanoramaModule.this.mCameraDisabled = true;
                MorphoPanoramaModule.this.mMainHandler.sendEmptyMessage(3);
            }
        }
    }

    private class StillImageData {
        public int mId;
        public ByteBuffer mImage;
        public ByteBuffer mMotionData;

        StillImageData(int i, int i2, byte[] bArr, byte[] bArr2) {
            this.mId = i;
            this.mImage = PanoUtil.createByteBuffer(bArr);
            this.mMotionData = PanoUtil.createByteBuffer(bArr2);
        }
    }

    private class StillPreviewRender implements Renderer {
        private DrawExtTexAttribute mExtTexture;
        float[] mTransform;

        private StillPreviewRender() {
            this.mExtTexture = new DrawExtTexAttribute(true);
            this.mTransform = new float[16];
        }

        public void onDrawFrame(GL10 gl10) {
            CameraScreenNail cameraScreenNail = MorphoPanoramaModule.this.mActivity.getCameraScreenNail();
            GLCanvas gLCanvas = MorphoPanoramaModule.this.getUIController().getGLView().getGLCanvas();
            if (cameraScreenNail != null && gLCanvas != null) {
                synchronized (gLCanvas) {
                    gLCanvas.clearBuffer();
                    int width = gLCanvas.getWidth();
                    int height = gLCanvas.getHeight();
                    gLCanvas.getState().pushState();
                    gLCanvas.setSize(MorphoPanoramaModule.this.mStillPreview.getWidth(), MorphoPanoramaModule.this.mStillPreview.getHeight());
                    cameraScreenNail.getSurfaceTexture().getTransformMatrix(this.mTransform);
                    gLCanvas.draw(this.mExtTexture.init(cameraScreenNail.getExtTexture(), this.mTransform, MorphoPanoramaModule.this.mStillPreviewTextureOffsetX, MorphoPanoramaModule.this.mStillPreviewTextureOffsetY, MorphoPanoramaModule.this.mStillPreviewTextureWidth, MorphoPanoramaModule.this.mStillPreviewTextureHeight));
                    gLCanvas.setSize(width, height);
                    gLCanvas.getState().popState();
                    gLCanvas.recycledResources();
                }
            }
        }

        public void onSurfaceChanged(GL10 gl10, int i, int i2) {
        }

        public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        }
    }

    public class StillProcTask extends Thread {
        private int shootCount = 0;

        class C01111 implements Runnable {
            C01111() {
            }

            public void run() {
                MorphoPanoramaModule.this.stopPanoramaShooting(false);
            }
        }

        public void run() {
            StillImageData stillImageData;
            while (MorphoPanoramaModule.this.mIsShooting) {
                if (MorphoPanoramaModule.this.mStillProcList.size() > 0) {
                    stillImageData = (StillImageData) MorphoPanoramaModule.this.mStillProcList.remove(0);
                    if (MorphoPanoramaModule.USE_PREVIEW_IMAGE) {
                        Log.d(MorphoPanoramaModule.TAG, MorphoPanoramaModule.TAG + ": run attachStillImageRaw() start :" + stillImageData.mId);
                        if (MorphoPanoramaModule.this.mMorphoPanoramaGP.attachStillImageRaw(stillImageData.mImage, stillImageData.mId, stillImageData.mMotionData) != 0) {
                            Log.e(MorphoPanoramaModule.TAG, String.format("%s:attachStillImageExt() -> 0x%x", new Object[]{MorphoPanoramaModule.TAG, Integer.valueOf(r2)}));
                        }
                    } else {
                        Log.d(MorphoPanoramaModule.TAG, MorphoPanoramaModule.TAG + ": run attachStillImageExt() start :" + stillImageData.mId);
                        if (MorphoPanoramaModule.this.mMorphoPanoramaGP.attachStillImageExt(stillImageData.mImage, stillImageData.mId, stillImageData.mMotionData) != 0) {
                            Log.e(MorphoPanoramaModule.TAG, String.format("%s: attachStillImageExt() -> 0x%x", new Object[]{MorphoPanoramaModule.TAG, Integer.valueOf(r2)}));
                            MorphoPanoramaModule.this.mActivity.runOnUiThread(new C01111());
                        }
                        if (this.shootCount == 0) {
                            MorphoPanoramaModule.this.mMorphoPanoramaGP.attachSetJpegForCopyingExif(stillImageData.mImage);
                        }
                    }
                    this.shootCount++;
                    NativeMemoryAllocator.freeBuffer(stillImageData.mImage);
                    NativeMemoryAllocator.freeBuffer(stillImageData.mMotionData);
                    MorphoPanoramaModule morphoPanoramaModule = MorphoPanoramaModule.this;
                    morphoPanoramaModule.mCntProcessd = morphoPanoramaModule.mCntProcessd + 1;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (MorphoPanoramaModule.this.mCntReqShoot > MorphoPanoramaModule.this.mCntProcessd) {
                if (MorphoPanoramaModule.this.mStillProcList.size() > 0) {
                    stillImageData = (StillImageData) MorphoPanoramaModule.this.mStillProcList.remove(0);
                    NativeMemoryAllocator.freeBuffer(stillImageData.mImage);
                    NativeMemoryAllocator.freeBuffer(stillImageData.mMotionData);
                    morphoPanoramaModule = MorphoPanoramaModule.this;
                    morphoPanoramaModule.mCntProcessd = morphoPanoramaModule.mCntProcessd + 1;
                }
            }
        }
    }

    private void addImageAsApplication(String str, Rect rect, int i) {
        if (USE_PREVIEW_IMAGE) {
            ExifHelper.writeExif(str, i, LocationManager.instance().getCurrentLocation(), this.mTimeTaken);
        }
        Uri addImage = Storage.addImage(this.mActivity, str, i, this.mTimeTaken, this.mLocation, rect.width(), rect.height());
        CameraDataAnalytics.instance().trackEvent("capture_nums_panorama");
        CameraDataAnalytics.instance().trackEvent("camera_picture_taken_key");
        this.mActivity.getScreenHint().updateHint();
        if (addImage != null) {
            this.mActivity.addSecureUri(addImage);
            Thumbnail createThumbnailFromUri = Thumbnail.createThumbnailFromUri(this.mActivity.getContentResolver(), addImage, false);
            Util.broadcastNewPicture(this.mActivity, addImage);
            this.mActivity.getThumbnailUpdater().setThumbnail(createThumbnailFromUri, false);
        }
    }

    private void addStillImage(StillImageData stillImageData) {
        this.mStillProcList.add(stillImageData);
        if (this.mStillProcTask == null) {
            this.mStillProcTask = new StillProcTask();
            this.mStillProcTask.start();
        }
    }

    private void allocateDisplayBuffers(int i) {
        if (!(this.mPreviewImage == null || (this.mPreviewImage.getWidth() == this.mInitParam.preview_img_width && this.mPreviewImage.getHeight() == this.mInitParam.preview_img_height))) {
            this.mPreviewImage.recycle();
            this.mPreviewImage = null;
            this.mDispPreviewImage.recycle();
            this.mDispPreviewImage = null;
        }
        if (this.mPreviewImage == null) {
            float f;
            switch (i) {
                case 0:
                    this.mPreviewImage = Bitmap.createBitmap(this.mInitParam.preview_img_width, this.mInitParam.preview_img_height, Config.ARGB_8888);
                    f = ((float) this.mInitParam.preview_img_height) / ((float) this.mInitParam.preview_img_width);
                    this.mDispPreviewImage = Bitmap.createBitmap(Util.sWindowWidth, (((int) (((float) Util.sWindowWidth) * f)) * 80) / 100, Config.ARGB_8888);
                    this.mAttachPosOffsetX = ((((int) (((float) Util.sWindowWidth) * f)) * this.mPreviewHeight) / this.mPreviewWidth) / 2;
                    return;
                case 1:
                    this.mPreviewImage = Bitmap.createBitmap(this.mInitParam.preview_img_width, this.mInitParam.preview_img_height, Config.ARGB_8888);
                    this.mDispPreviewImage = Bitmap.createBitmap((((int) (((float) Util.sWindowHeight) * (((float) (this.mInitParam.preview_img_width - (this.mPreviewCroppingAdjustByAuto * 2))) / ((float) this.mInitParam.preview_img_height)))) * 80) / 100, Util.sWindowHeight, Config.ARGB_8888);
                    this.mAttachPosOffsetY = ((this.mDispPreviewImage.getWidth() * this.mPreviewWidth) / this.mPreviewHeight) / 2;
                    return;
                case 4:
                    this.mPreviewImage = Bitmap.createBitmap(this.mInitParam.preview_img_width, this.mInitParam.preview_img_height, Config.ARGB_8888);
                    f = ((float) (this.mInitParam.preview_img_height - (this.mPreviewCroppingAdjustByAuto * 2))) / ((float) this.mInitParam.preview_img_width);
                    this.mDispPreviewImage = Bitmap.createBitmap(Util.sWindowWidth, (((int) (((float) Util.sWindowWidth) * f)) * 80) / 100, Config.ARGB_8888);
                    this.mAttachPosOffsetX = ((((int) (((float) Util.sWindowWidth) * f)) * this.mPreviewHeight) / this.mPreviewWidth) / 2;
                    return;
                default:
                    this.mPreviewImage = Bitmap.createBitmap(this.mInitParam.preview_img_width, this.mInitParam.preview_img_height, Config.ARGB_8888);
                    this.mDispPreviewImage = Bitmap.createBitmap((((int) (((float) Util.sWindowHeight) * (((float) this.mInitParam.preview_img_width) / ((float) this.mInitParam.preview_img_height)))) * 80) / 100, Util.sWindowHeight, Config.ARGB_8888);
                    this.mAttachPosOffsetY = ((this.mDispPreviewImage.getWidth() * this.mPreviewWidth) / this.mPreviewHeight) / 2;
                    return;
            }
        }
    }

    private int calibrateRotation(int i) {
        if (!(i == 0 || i == 90 || i == 180 || i == 270)) {
            i = 0;
        }
        return (this.mDeviceOrientationAtCapture + i) % 360;
    }

    private void configureCamera(Parameters parameters) {
        this.mCameraDevice.setParameters(parameters);
    }

    private void createContentView() {
        getUIController().getPreviewPage().inflatePanoramaView();
        this.mPanoramaViewRoot = getUIController().getPanoramaViewRoot();
        this.mUseHintArea = this.mPanoramaViewRoot.findViewById(C0049R.id.pano_use_hint_area);
        this.mLeftIndicator = this.mPanoramaViewRoot.findViewById(C0049R.id.left_direction_indi);
        this.mRightIndicator = this.mPanoramaViewRoot.findViewById(C0049R.id.right_direction_indi);
        this.mUseHint = (TextView) this.mPanoramaViewRoot.findViewById(C0049R.id.pano_use_hint);
        this.mPanoramaPreview = (ImageView) this.mPanoramaViewRoot.findViewById(C0049R.id.panorama_image_preview);
        this.mStillPreview = (GLTextureView) this.mPanoramaViewRoot.findViewById(C0049R.id.panorama_still_preview);
        this.mMovingDirectionView = (PanoMovingIndicatorView) this.mPanoramaViewRoot.findViewById(C0049R.id.pano_move_direction_view);
        this.mMoveReferenceLine = this.mPanoramaViewRoot.findViewById(C0049R.id.pano_move_reference_line);
        this.mStillPreviewHintArea = this.mPanoramaViewRoot.findViewById(C0049R.id.pano_still_preview_hint_area);
        if (this.mStillPreview.getRenderer() == null) {
            this.mStillPreviewRender = new StillPreviewRender();
            this.mStillPreview.setEGLContextClientVersion(2);
            this.mStillPreview.setEGLShareContextGetter(new C01092());
            this.mStillPreview.setRenderer(this.mStillPreviewRender);
            this.mStillPreview.setRenderMode(0);
            this.mStillPreview.onPause();
        } else {
            this.mStillPreviewRender = (StillPreviewRender) this.mStillPreview.getRenderer();
        }
        V6ModeExitView modeExitView = getUIController().getModeExitView();
        int dimensionPixelSize = this.mActivity.getResources().getDimensionPixelSize(C0049R.dimen.pano_mode_exit_button_margin_bottom);
        int navigationBarHeight = (Util.checkDeviceHasNavigationBar(this.mActivity) && ((LayoutParams) getUIController().getBottomControlLowerGroup().getLayoutParams()).bottomMargin == 0) ? Util.getNavigationBarHeight(this.mActivity) / 2 : 0;
        modeExitView.setLayoutParameters(0, dimensionPixelSize - navigationBarHeight);
        getUIController().getModeExitView().updateExitButton(-1, true);
    }

    private String createNameString(long j, int i) {
        String charSequence = DateFormat.format(getString(C0049R.string.pano_file_name_format), j).toString();
        return (sSaveOutputType == 3 && i == 2) ? charSequence + "_bounding" : charSequence;
    }

    private void finishAttachStillImageTask() {
        while (!this.mPaused && this.mCntReqShoot > this.mCntProcessd) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.mStillProcTask = null;
    }

    private void initCommenConfig() {
        this.mCameraPreviewBuff = new byte[(((this.mPreviewWidth * this.mPreviewHeight) * 3) / 2)];
    }

    private void initPreviewLayout() {
        this.mActivity.getCameraScreenNail().setSize(this.mPreviewWidth, this.mPreviewHeight);
        ViewGroup.LayoutParams layoutParams = this.mStillPreview.getLayoutParams();
        CameraScreenNail cameraScreenNail = this.mActivity.getCameraScreenNail();
        int width = cameraScreenNail.getWidth();
        int height = ((cameraScreenNail.getHeight() - (this.mPreviewCroppingAdjustByAuto * 2)) * 80) / 100;
        layoutParams.height = this.mActivity.getResources().getDimensionPixelSize(C0049R.dimen.pano_preview_hint_frame_height);
        layoutParams.width = (layoutParams.height * width) / height;
        this.mStillPreviewTextureWidth = layoutParams.width;
        this.mStillPreviewTextureHeight = (layoutParams.width * this.mPreviewWidth) / this.mPreviewHeight;
        this.mStillPreviewTextureOffsetX = 0;
        this.mStillPreviewTextureOffsetY = (-(this.mStillPreviewTextureHeight - layoutParams.height)) / 2;
        this.mStillPreview.requestLayout();
    }

    private void initializeCapabilities() {
        this.mAeLockSupported = this.mParameters.isAutoExposureLockSupported();
        this.mAwbLockSupported = this.mParameters.isAutoWhiteBalanceLockSupported();
    }

    private void initializeMiscControls() {
        this.mStillProcList = new ArrayList();
    }

    private boolean isProcessingFinishTask() {
        return (this.mSaveOutputImageTask == null || this.mSaveOutputImageTask.getStatus() == Status.FINISHED) ? false : true;
    }

    private boolean isZslMode() {
        return sProxy.isZSLMode(this.mParameters);
    }

    private void keepScreenOn() {
        this.mMainHandler.removeMessages(1);
        getWindow().addFlags(128);
    }

    private void keepScreenOnAwhile() {
        this.mMainHandler.removeMessages(1);
        getWindow().addFlags(128);
        this.mMainHandler.sendEmptyMessageDelayed(1, (long) getScreenDelay());
    }

    private void onCaptureOrientationDecided() {
        this.mMoveReferenceLine.setVisibility(0);
        this.mStillPreviewHintArea.setVisibility(8);
        this.mStillPreview.onPause();
        this.mLeftIndicator.setVisibility(8);
        this.mRightIndicator.setVisibility(8);
        this.mUseHint.setText(C0049R.string.pano_how_to_use_prompt_go_on_moving);
        this.mMovingDirectionView.setVisibility(0);
        this.mMovingDirectionView.setMovingAttibute(this.mDirection[0], this.mAttachPosOffsetX, this.mAttachPosOffsetY);
        trackPictureTaken();
    }

    private void onFrameLayoutChange(View view, Rect rect) {
        this.mActivity.onLayoutChange(rect.width(), rect.height());
        if (this.mActivity.getCameraScreenNail() != null) {
            this.mActivity.getCameraScreenNail().setRenderArea(rect);
        }
    }

    private void onPreviewMoving() {
        if (this.mLeftIndicator.getVisibility() == 0) {
            return;
        }
        if (this.mMovingDirectionView.isTooFast()) {
            this.mUseHint.setText(C0049R.string.pano_how_to_use_prompt_slow_down);
        } else if (this.mMovingDirectionView.isFar()) {
            this.mUseHint.setText(C0049R.string.pano_how_to_use_prompt_align_reference_line);
        } else {
            this.mUseHint.setText(C0049R.string.pano_how_to_use_prompt_go_on_moving);
        }
    }

    private void releaseCamera() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setPreviewCallbackWithBuffer(null);
            this.mCameraDevice.removeAllAsyncMessage();
            CameraHolder.instance().release();
            this.mCameraDevice = null;
            this.mCameraState = 0;
        }
    }

    private void releaseResources() {
        releaseCamera();
        CameraDataAnalytics.instance().uploadToServer();
        this.mWaitForRelease = false;
    }

    private void requestStillPreviewRender() {
        if (this.mActivity.getCameraScreenNail() != null) {
            if (this.mWaitingFirstFrame) {
                this.mStillPreview.onResume();
                this.mWaitingFirstFrame = false;
            }
            this.mStillPreview.requestRender();
        }
    }

    private void resetScreenOn() {
        this.mMainHandler.removeMessages(1);
        getWindow().clearFlags(128);
    }

    private void resetToPreview() {
        if (this.mPanoramaViewRoot.getVisibility() == 0) {
            resetUI();
        }
        if (!this.mPaused) {
            if (!isZslMode() || !USE_PREVIEW_IMAGE) {
                startCameraPreview();
            }
        }
    }

    private void resetUI() {
        getUIController().getModeExitView().show();
        getUIController().getZoomButton().show();
        if (this.mPaused) {
            getUIController().getThumbnailButton().setVisibility(0);
            getUIController().getModulePicker().setVisibility(0);
            getUIController().getShutterButton().setImageResource(C0049R.drawable.camera_shutter_button_bg);
        } else {
            getUIController().getThumbnailButton().animateIn(null, 100, true);
            getUIController().getModulePicker().animateIn(null, 100, true);
            getUIController().getShutterButton().changeImageWithAnimation(C0049R.drawable.camera_shutter_button_bg, 200);
        }
        this.mMoveReferenceLine.setVisibility(8);
        this.mStillPreviewHintArea.setVisibility(8);
        this.mStillPreview.onPause();
        this.mMovingDirectionView.setVisibility(8);
        this.mUseHintArea.setVisibility(8);
        showSmallPreview();
    }

    private void saveOutputJpeg(String str, Rect rect) {
        int[] iArr = new int[1];
        String generateFilepath = Storage.generateFilepath(str);
        if (this.mMorphoPanoramaGP.saveOutputJpeg(generateFilepath, rect, 1, iArr) != 0) {
            Log.e(TAG, String.format("%s:saveOutputJpeg() -> 0x%x", new Object[]{TAG, Integer.valueOf(r3)}));
        }
        addImageAsApplication(generateFilepath, rect, calibrateRotation(1));
    }

    private void setProgressUI(boolean z) {
        getUIController().getCaptureProgressBar().setVisibility(z ? 0 : 8);
    }

    private void setShootUI() {
        this.mUseHintArea.setVisibility(0);
        this.mMovingDirectionView.setVisibility(8);
        this.mUseHint.setText(C0049R.string.pano_how_to_use_prompt_to_move);
        this.mMoveReferenceLine.setVisibility(8);
        this.mMovingDirectionView.setVisibility(8);
        this.mStillPreviewHintArea.setVisibility(0);
        getUIController().getThumbnailButton().animateOut(null, 100, true);
        getUIController().getModulePicker().animateOut(null, 100, true);
        getUIController().getModeExitView().hide();
        getUIController().getZoomButton().hide();
        keepScreenOn();
        getUIController().getShutterButton().changeImageWithAnimation(C0049R.drawable.pano_shutter_button_stop_bg, 200);
        enableCameraControls(false);
    }

    private void setZsl() {
        if (Device.isQcomPlatform()) {
            sProxy.setZSLMode(this.mParameters, "on");
        } else if (Device.isMTKPlatform()) {
            if (Device.IS_HM3Y || Device.IS_HM3Z) {
                sProxy.setZSLMode(this.mParameters, "on");
            }
        } else if (Device.isLCPlatform()) {
            sProxy.setZSLMode(this.mParameters, "true");
        }
    }

    private void setupCaptureParams() {
        PictureSizeManager.initialize(getActivity(), this.mParameters.getSupportedPictureSizes(), 0);
        PictureSize bestPanoPictureSize = PictureSizeManager.getBestPanoPictureSize();
        this.mPictureWidth = bestPanoPictureSize.width;
        this.mPictureHeight = bestPanoPictureSize.height;
        Log.v(TAG, "picture h = " + this.mPictureHeight + " , w = " + this.mPictureWidth);
        this.mParameters.setPictureSize(this.mPictureWidth, this.mPictureHeight);
        Size optimalPreviewSize = Util.getOptimalPreviewSize(this.mActivity, sProxy.getSupportedPreviewSizes(this.mParameters), (double) (((float) Util.sWindowHeight) / ((float) Util.sWindowWidth)));
        if (optimalPreviewSize == null) {
            throw new RuntimeException("Can not find suitable preview size for panorama");
        }
        this.mPreviewWidth = optimalPreviewSize.width;
        this.mPreviewHeight = optimalPreviewSize.height;
        Log.v(TAG, "preview h = " + this.mPreviewHeight + " , w = " + this.mPreviewWidth);
        Size previewSize = this.mParameters.getPreviewSize();
        this.mParameters.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
        if (!previewSize.equals(optimalPreviewSize)) {
            this.mCameraDevice.setParameters(this.mParameters);
            this.mParameters = this.mCameraDevice.getParameters();
        }
        List supportedPreviewFpsRange = this.mParameters.getSupportedPreviewFpsRange();
        int size = supportedPreviewFpsRange.size() - 1;
        int i = ((int[]) supportedPreviewFpsRange.get(size))[0];
        int i2 = ((int[]) supportedPreviewFpsRange.get(size))[1];
        this.mParameters.setPreviewFpsRange(i, i2);
        Log.v(TAG, "preview fps: " + i + ", " + i2);
        if (this.mParameters.getSupportedFocusModes().indexOf(this.mTargetFocusMode) >= 0) {
            this.mParameters.setFocusMode(this.mTargetFocusMode);
        } else {
            Log.w(TAG, "Cannot set the focus mode to " + this.mTargetFocusMode + " because the mode is not supported.");
        }
        this.mParameters.setZoom(0);
        this.mParameters.setRecordingHint(false);
        this.mParameters.setRotation(0);
        String string = this.mPreferences.getString("pref_camera_antibanding_key", getString(CameraSettings.getDefaultPreferenceId(C0049R.string.pref_camera_antibanding_default)));
        Log.v(TAG, "antiBanding value =" + string);
        if (BaseModule.isSupported(string, this.mParameters.getSupportedAntibanding())) {
            this.mParameters.setAntibanding(string);
        }
        setZsl();
        addMuteToParameters(this.mParameters);
        if (Device.isSupportedObjectTrack()) {
            this.mParameters.set("t2t", "off");
        }
        configOisParameters(this.mParameters, false);
        resetFaceBeautyParams(this.mParameters);
        sProxy.setTimeWatermark(this.mParameters, "off");
        sProxy.setFaceWatermark(this.mParameters, false);
        sProxy.clearExposureTime(this.mParameters);
    }

    private void showSmallPreview() {
        if (this.mMorphoPanoramaGP == null) {
            this.mPanoramaViewRoot.setVisibility(0);
            this.mMovingDirectionView.setVisibility(8);
            this.mLeftIndicator.setVisibility(0);
            this.mRightIndicator.setVisibility(0);
            if (this.mSwitchingCamera) {
                this.mWaitingFirstFrame = true;
            } else {
                this.mStillPreview.onResume();
                requestStillPreviewRender();
            }
            this.mStillPreviewHintArea.setVisibility(0);
        }
    }

    private void startCameraPreview() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setErrorCallback(this.mErrorCallback);
            if (this.mCameraState != 0) {
                stopCameraPreview();
            }
            setDisplayOrientation();
            this.mCameraDevice.setDisplayOrientation(this.mCameraDisplayOrientation);
            this.mCameraDevice.cancelAutoFocus();
            if (this.mAeLockSupported) {
                this.mParameters.setAutoExposureLock(false);
            }
            if (this.mAwbLockSupported) {
                this.mParameters.setAutoWhiteBalanceLock(false);
            }
            this.mParameters.setFocusMode(this.mTargetFocusMode);
            this.mCameraDevice.setParameters(this.mParameters);
            this.mCameraDevice.setPreviewTexture(this.mActivity.getCameraScreenNail().getSurfaceTexture());
            this.mCameraDevice.startPreviewAsync();
            this.mCameraState = 1;
        }
    }

    private boolean startPanoramaGP() {
        if (this.mMorphoPanoramaGP == null) {
            float f;
            InitParam initParam;
            int[] iArr = new int[1];
            this.mMorphoPanoramaGP = new MorphoPanoramaGP();
            this.mInitParam = new InitParam();
            this.mInitParam.format = "YVU420_SEMIPLANAR";
            this.mInitParam.use_threshold = this.mUseThres;
            this.mInitParam.preview_width = this.mPreviewWidth;
            this.mInitParam.preview_height = this.mPreviewHeight;
            if (USE_PREVIEW_IMAGE) {
                this.mInitParam.still_width = this.mPreviewWidth;
                this.mInitParam.still_height = this.mPreviewHeight;
            } else {
                this.mInitParam.still_width = this.mPictureWidth;
                this.mInitParam.still_height = this.mPictureHeight;
            }
            this.mInitParam.angle_of_view_degree = (double) this.mParameters.getVerticalViewAngle();
            this.mInitParam.draw_cur_image = 0;
            int displayOrientation = Util.getDisplayOrientation(Util.getDisplayRotation(this.mActivity), this.mCameraId);
            this.mPreviewCroppingAdjustByAuto = 0;
            this.mAppPanoramaDirection = this.mAppPanoramaDirectionSettings;
            this.mInitParam.direction = 1;
            this.mInitParam.dst_img_width = (int) (((float) this.mInitParam.still_height) * this.mFrameRatio);
            this.mInitParam.dst_img_height = this.mInitParam.still_width;
            this.mInitParam.preview_img_width = (int) (((float) this.mInitParam.preview_height) * this.mFrameRatio);
            this.mInitParam.preview_img_height = this.mInitParam.preview_width;
            switch (displayOrientation) {
                case 270:
                    this.mInitParam.output_rotation = 270;
                    break;
                default:
                    this.mInitParam.output_rotation = 90;
                    break;
            }
            this.mInitParam.preview_shrink_ratio = Math.max((this.mInitParam.preview_img_width / Util.sWindowWidth) - 1, 1);
            MorphoPanoramaGP.calcImageSize(this.mInitParam, 360.0d);
            if (this.MAX_DST_IMG_WIDTH < this.mInitParam.dst_img_width) {
                f = ((float) this.MAX_DST_IMG_WIDTH) / ((float) this.mInitParam.dst_img_width);
                this.mInitParam.dst_img_width = this.MAX_DST_IMG_WIDTH;
                initParam = this.mInitParam;
                initParam.preview_img_width = (int) (((float) initParam.preview_img_width) * f);
            }
            if (this.MAX_DST_IMG_WIDTH < this.mInitParam.dst_img_height) {
                f = ((float) this.MAX_DST_IMG_WIDTH) / ((float) this.mInitParam.dst_img_height);
                this.mInitParam.dst_img_height = this.MAX_DST_IMG_WIDTH;
                initParam = this.mInitParam;
                initParam.preview_img_height = (int) (((float) initParam.preview_img_height) * f);
            }
            initParam = this.mInitParam;
            initParam.preview_img_width &= -2;
            initParam = this.mInitParam;
            initParam.preview_img_height &= -2;
            if (this.mMorphoPanoramaGP.initialize(this.mInitParam, iArr) != 0) {
                Log.e(TAG, String.format("%s:initialize() -> 0x%x", new Object[]{TAG, Integer.valueOf(this.mMorphoPanoramaGP.initialize(this.mInitParam, iArr))}));
            }
        }
        this.mMorphoPanoramaGP.setMotionlessThreshold(this.mMotionlessThres);
        this.mMorphoPanoramaGP.setUseSensorThreshold(this.mUseSensorThres);
        allocateDisplayBuffers(this.mAppDeviceRotation + this.mAppPanoramaDirection);
        if (this.mMorphoPanoramaGP.setUseSensorAssist(0, this.mUseSensorAWF ? 1 : 0) != 0) {
            Log.e(TAG, String.format("%s:setUseSensorAssist() -> 0x%x", new Object[]{TAG, Integer.valueOf(this.mMorphoPanoramaGP.setUseSensorAssist(0, this.mUseSensorAWF ? 1 : 0))}));
        }
        if (this.mMorphoPanoramaGP.start() == 0) {
            return true;
        }
        Log.e(TAG, String.format("%s:start() -> 0x%x", new Object[]{TAG, Integer.valueOf(this.mMorphoPanoramaGP.start())}));
        return false;
    }

    private void stopCameraPreview() {
        if (!(this.mCameraDevice == null || this.mCameraState == 0)) {
            Log.v(TAG, "stopPreview");
            this.mCameraDevice.stopPreview();
        }
        this.mCameraState = 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void stopPanoramaShooting(boolean r5) {
        /*
        r4 = this;
        r3 = 0;
        r2 = 0;
        r0 = TAG;
        r1 = "stopPanoramaShooting";
        android.util.Log.v(r0, r1);
        r4.keepScreenOnAwhile();
        r0 = r4.mCameraDevice;
        if (r0 == 0) goto L_0x0015;
    L_0x0011:
        r0 = r4.mMorphoPanoramaGP;
        if (r0 != 0) goto L_0x0016;
    L_0x0015:
        return;
    L_0x0016:
        r0 = r4.mPanoramaPreview;
        if (r0 == 0) goto L_0x0015;
    L_0x001a:
        r0 = r4.isProcessingFinishTask();
        if (r0 == 0) goto L_0x0021;
    L_0x0020:
        return;
    L_0x0021:
        r0 = r4.mCameraDevice;
        r0.setPreviewCallbackWithBuffer(r2);
        if (r5 != 0) goto L_0x002a;
    L_0x0028:
        r4.mIsShooting = r3;
    L_0x002a:
        r0 = r4.mPrevDirection;
        if (r0 == 0) goto L_0x0033;
    L_0x002e:
        r0 = r4.mPrevDirection;
        r1 = 1;
        if (r0 != r1) goto L_0x0034;
    L_0x0033:
        r5 = 0;
    L_0x0034:
        r0 = new com.android.camera.module.MorphoPanoramaModule$SaveOutputImageTask;
        r1 = r4.mActivity;
        r0.<init>(r1, r5);
        r4.mSaveOutputImageTask = r0;
        r0 = r4.mSaveOutputImageTask;
        r1 = new java.lang.Void[r3];
        r0.execute(r1);
        r4.resetUI();
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.MorphoPanoramaModule.stopPanoramaShooting(boolean):void");
    }

    private void switchToCameraMode() {
        this.mActivity.getCameraScreenNail().switchModule();
        switchToOtherMode(0);
        CameraSettings.resetPreference("pref_camera_panoramamode_key");
    }

    private void switchToOtherMode(int i) {
        if (!this.mActivity.isFinishing()) {
            this.mActivity.switchToOtherModule(i);
            CameraSettings.resetPreference("pref_camera_panoramamode_key");
        }
    }

    private void trackPictureTaken() {
        switch (this.mDirection[0]) {
            case 2:
            case 4:
                CameraDataAnalytics.instance().trackEvent("panorama_capture_left_start");
                break;
            case 3:
            case 5:
                CameraDataAnalytics.instance().trackEvent("panorama_capture_right_start");
                break;
        }
        int i = this.mPreferences.getInt("panorama_last_start_direction_key", -1);
        if (i > 0) {
            if (i == this.mDirection[0]) {
                switch (this.mDirection[0]) {
                    case 2:
                    case 4:
                        CameraDataAnalytics.instance().trackEvent("panorama_capture_2_times_left_start");
                        break;
                    case 3:
                    case 5:
                        CameraDataAnalytics.instance().trackEvent("panorama_capture_2_times_right_start");
                        break;
                }
            }
            CameraDataAnalytics.instance().trackEvent("panorama_capture_2_times_random_start");
            this.mPreferences.edit().remove("panorama_last_start_direction_key").apply();
            return;
        }
        this.mPreferences.edit().putInt("panorama_last_start_direction_key", this.mDirection[0]).apply();
    }

    private void waitCameraStartUpThread() {
        try {
            if (this.mSetupCameraThread != null) {
                this.mSetupCameraThread.cancel();
                this.mSetupCameraThread.join();
                this.mSetupCameraThread = null;
                this.mCameraState = 0;
            }
        } catch (InterruptedException e) {
        }
    }

    public void computeFrameRatio() {
        this.mFrameRatio = ((float) Util.sWindowWidth) / ((this.mActivity.getResources().getDimension(C0049R.dimen.pano_preview_hint_frame_height) * 100.0f) / 80.0f);
        if (USE_PREVIEW_IMAGE) {
            this.mFrameRatio = (this.mFrameRatio * ((float) this.mPreviewWidth)) / ((float) this.mPreviewHeight);
        } else {
            this.mFrameRatio = (this.mFrameRatio * ((float) this.mPictureWidth)) / ((float) this.mPictureHeight);
        }
    }

    public List<String> getSupportedSettingKeys() {
        return Device.isQcomPlatform() ? CameraModule.getLayoutModeKeys(this.mActivity, isBackCamera(), false) : Device.isNvPlatform() ? CameraModule.getLayoutModeKeys(this.mActivity, isBackCamera(), false) : Device.isLCPlatform() ? CameraModule.getLayoutModeKeys(this.mActivity, isBackCamera(), false) : Device.isMTKPlatform() ? CameraModule.getLayoutModeKeys(this.mActivity, isBackCamera(), false) : Device.isPad() ? CameraPadOne.getLayoutModeKeys(this.mActivity, isBackCamera(), false) : new ArrayList();
    }

    public boolean handleMessage(int i, int i2, final Object obj, Object obj2) {
        if (super.handleMessage(i, i2, obj, obj2)) {
            return true;
        }
        switch (i2) {
            case C0049R.id.v6_thumbnail_button:
                onThumbnailClicked(null);
                return true;
            case C0049R.id.v6_shutter_button:
                if (i == 0) {
                    onShutterButtonClick();
                    if (!this.mIsShooting) {
                        CameraDataAnalytics.instance().trackEvent("capture_times_shutter");
                    }
                } else if (i == 2) {
                    if (isBackCamera()) {
                        Point point = (Point) obj;
                        Point point2 = (Point) obj2;
                        getUIController().getSmartShutterButton().flyin(point.x, point.y, point2.x, point2.y);
                    }
                } else if (i == 3) {
                    onShutterButtonFocus(((Boolean) obj).booleanValue());
                }
                return true;
            case C0049R.id.v6_module_picker:
                Runnable c01081 = new Runnable() {
                    public void run() {
                        MorphoPanoramaModule.this.switchToOtherMode(((Integer) obj).intValue());
                    }
                };
                getUIController().enableControls(false);
                getUIController().getShutterButton().onPause();
                getUIController().getBottomControlLowerPanel().animationSwitchToVideo(c01081);
                this.mActivity.getCameraScreenNail().switchModule();
                return true;
            case C0049R.id.v6_frame_layout:
                if (i == 1) {
                    onFrameLayoutChange((View) obj, (Rect) obj2);
                }
                return true;
            case C0049R.id.v6_setting_page:
                this.mPaused = true;
                switchToCameraMode();
                return true;
            case C0049R.id.setting_button:
                openSettingActivity();
                return true;
            default:
                return false;
        }
    }

    public boolean isCameraEnabled() {
        return !this.mPaused && this.mCameraState == 1;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onBackPressed() {
        /*
        r4 = this;
        r3 = 1;
        r1 = r4.mSyncObj;
        monitor-enter(r1);
        r0 = r4.mIsShooting;	 Catch:{ all -> 0x002a }
        if (r0 == 0) goto L_0x0018;
    L_0x0008:
        r0 = r4.isProcessingFinishTask();	 Catch:{ all -> 0x002a }
        if (r0 != 0) goto L_0x0018;
    L_0x000e:
        r0 = 3;
        r4.playCameraSound(r0);	 Catch:{ all -> 0x002a }
        r0 = 1;
        r4.stopPanoramaShooting(r0);	 Catch:{ all -> 0x002a }
        monitor-exit(r1);
        return r3;
    L_0x0018:
        monitor-exit(r1);
        r0 = r4.getUIController();
        r0 = r0.getModeExitView();
        r1 = -1;
        r2 = 0;
        r0.updateExitButton(r1, r2);
        r4.switchToCameraMode();
        return r3;
    L_0x002a:
        r0 = move-exception;
        monitor-exit(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.MorphoPanoramaModule.onBackPressed():boolean");
    }

    public boolean onCameraPickerClicked(int i) {
        this.mSwitchingCamera = true;
        this.mStillPreview.onPause();
        enableCameraControls(false);
        releaseCamera();
        this.mSetupCameraThread = new SetupCameraThread();
        this.mSetupCameraThread.start();
        return true;
    }

    public void onCreate(Camera camera) {
        super.onCreate(camera);
        this.mActivity.createContentView();
        this.mPreferences = CameraSettingPreferences.instance();
        CameraSettings.upgradeGlobalPreferences(this.mPreferences);
        this.mCameraId = CameraHolder.instance().getBackCameraId();
        this.mSetupCameraThread = new SetupCameraThread();
        this.mSetupCameraThread.start();
        getUIController().onCreate();
        getUIController().useProperView();
        EffectController.getInstance().setEffect(0);
        this.mActivity.createCameraScreenNail(true, false);
        this.mStartPreviewPrerequisiteReady.open();
        createContentView();
        initializeMiscControls();
    }

    public void onDestroy() {
        this.mPanoramaViewRoot.setVisibility(8);
        getUIController().getModeExitView().setLayoutParameters(C0049R.id.v6_setting_popup_parent_layout, 0);
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        boolean z = false;
        if (this.mPaused) {
            return true;
        }
        switch (i) {
            case 23:
                if (keyEvent.getRepeatCount() == 0 && getUIController().getPreviewPage().isPreviewPageVisible()) {
                    onShutterButtonClick();
                    return true;
                }
            case 24:
            case 25:
                if (getUIController().getPreviewPage().isPreviewPageVisible()) {
                    if (i == 24) {
                        z = true;
                    }
                    if (handleVolumeKeyEvent(z, true, keyEvent.getRepeatCount())) {
                        return true;
                    }
                }
                break;
            case 27:
            case 66:
                if (keyEvent.getRepeatCount() == 0 && getUIController().getPreviewPage().isPreviewPageVisible()) {
                    onShutterButtonClick();
                    if (Util.isFingerPrintKeyEvent(keyEvent)) {
                        CameraDataAnalytics.instance().trackEvent("capture_times_finger");
                    }
                    return true;
                }
        }
        return super.onKeyDown(i, keyEvent);
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (this.mPaused) {
            return true;
        }
        switch (i) {
            case 27:
            case 66:
                getUIController().getShutterButton().setPressed(false);
                return true;
            default:
                return super.onKeyUp(i, keyEvent);
        }
    }

    public void onPauseAfterSuper() {
        super.onPauseAfterSuper();
        synchronized (this.mSyncObj) {
            if (this.mIsShooting) {
                playCameraSound(3);
                stopPanoramaShooting(true);
            }
            if (this.mRequestTakePicture) {
                this.mCntReqShoot--;
            }
            if (this.mPreviewImage != null) {
                this.mPreviewImage.recycle();
                this.mPreviewImage = null;
            }
            if (this.mPanoramaPreview != null) {
                this.mPanoramaPreview.setImageDrawable(null);
            }
            if (this.mDispPreviewImage != null) {
                this.mDispPreviewImage.recycle();
                this.mDispPreviewImage = null;
            }
            this.mCameraPreviewBuff = null;
        }
        waitCameraStartUpThread();
        if (this.mActivity.isGotoGallery() ? Device.isReleaseLaterForGallery() : false) {
            this.mWaitForRelease = true;
        } else {
            releaseResources();
        }
        if (this.mWaitProcessorTask != null) {
            this.mWaitProcessorTask.cancel(true);
            this.mWaitProcessorTask = null;
        }
        resetScreenOn();
        getUIController().onPause();
        setProgressUI(false);
        this.mMainHandler.removeMessages(4);
        this.mMainHandler.removeMessages(5);
        this.mMainHandler.removeMessages(2);
        this.mMainHandler.removeMessages(3);
        this.mMainHandler.removeMessages(6);
        System.gc();
    }

    public void onPauseBeforeSuper() {
        super.onPauseBeforeSuper();
        this.mStillPreview.onPause();
        this.mSwitchingCamera = false;
        this.mWaitingFirstFrame = false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onPictureTaken(byte[] r10, android.hardware.Camera r11) {
        /*
        r9 = this;
        r1 = 0;
        r9.mRequestTakePicture = r1;
        r1 = TAG;
        r2 = "onPictureTaken";
        android.util.Log.v(r1, r2);
        r8 = r9.mSyncObj;
        monitor-enter(r8);
        r1 = r9.mCameraDevice;	 Catch:{ all -> 0x0058 }
        if (r1 == 0) goto L_0x0016;
    L_0x0012:
        r1 = r9.mMorphoPanoramaGP;	 Catch:{ all -> 0x0058 }
        if (r1 != 0) goto L_0x0018;
    L_0x0016:
        monitor-exit(r8);
        return;
    L_0x0018:
        r1 = r9.mCaptureInfoList;	 Catch:{ all -> 0x0058 }
        r1 = r1.size();	 Catch:{ all -> 0x0058 }
        if (r1 == 0) goto L_0x0016;
    L_0x0020:
        r1 = r9.mCaptureInfoList;	 Catch:{ all -> 0x0058 }
        r2 = 0;
        r6 = r1.remove(r2);	 Catch:{ all -> 0x0058 }
        r6 = (com.android.camera.module.MorphoPanoramaModule.CaptureInfo) r6;	 Catch:{ all -> 0x0058 }
        r0 = new com.android.camera.module.MorphoPanoramaModule$StillImageData;	 Catch:{ all -> 0x0058 }
        r2 = r6.mId;	 Catch:{ all -> 0x0058 }
        r3 = r9.mPreviewCount;	 Catch:{ all -> 0x0058 }
        r5 = r9.mMotionData;	 Catch:{ all -> 0x0058 }
        r1 = r9;
        r4 = r10;
        r0.<init>(r2, r3, r4, r5);	 Catch:{ all -> 0x0058 }
        r9.addStillImage(r0);	 Catch:{ all -> 0x0058 }
        r1 = r6.mStatus;	 Catch:{ all -> 0x0058 }
        switch(r1) {
            case 1: goto L_0x0053;
            case 3: goto L_0x0053;
            case 11: goto L_0x0053;
            default: goto L_0x003e;
        };	 Catch:{ all -> 0x0058 }
    L_0x003e:
        r1 = r9.isZslMode();	 Catch:{ all -> 0x0058 }
        if (r1 == 0) goto L_0x005b;
    L_0x0044:
        r1 = r9.mCameraDevice;	 Catch:{ all -> 0x0058 }
        r1.startPreview();	 Catch:{ all -> 0x0058 }
    L_0x0049:
        monitor-exit(r8);
        r1 = TAG;
        r2 = "onPictureTaken done";
        android.util.Log.v(r1, r2);
        return;
    L_0x0053:
        r1 = 1;
        r9.stopPanoramaShooting(r1);	 Catch:{ all -> 0x0058 }
        goto L_0x0049;
    L_0x0058:
        r1 = move-exception;
        monitor-exit(r8);
        throw r1;
    L_0x005b:
        r2 = 50;
        java.lang.Thread.sleep(r2);	 Catch:{ InterruptedException -> 0x0079 }
    L_0x0060:
        r1 = r9.mCameraDevice;	 Catch:{ all -> 0x0058 }
        r1.startPreview();	 Catch:{ all -> 0x0058 }
        r1 = r9.mIsShooting;	 Catch:{ all -> 0x0058 }
        if (r1 == 0) goto L_0x0049;
    L_0x0069:
        r1 = r9.mCameraDevice;	 Catch:{ all -> 0x0058 }
        r2 = r9.mCameraPreviewBuff;	 Catch:{ all -> 0x0058 }
        r1.addCallbackBuffer(r2);	 Catch:{ all -> 0x0058 }
        r1 = 1;
        r9.mPreviewSkipCount = r1;	 Catch:{ all -> 0x0058 }
        r1 = r9.mCameraDevice;	 Catch:{ all -> 0x0058 }
        r1.setPreviewCallbackWithBuffer(r9);	 Catch:{ all -> 0x0058 }
        goto L_0x0049;
    L_0x0079:
        r7 = move-exception;
        r7.printStackTrace();	 Catch:{ all -> 0x0058 }
        goto L_0x0060;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.MorphoPanoramaModule.onPictureTaken(byte[], android.hardware.Camera):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onPictureTakenPreview(byte[] r9) {
        /*
        r8 = this;
        r7 = r8.mSyncObj;
        monitor-enter(r7);
        r1 = r8.mMorphoPanoramaGP;	 Catch:{ all -> 0x0046 }
        if (r1 != 0) goto L_0x0009;
    L_0x0007:
        monitor-exit(r7);
        return;
    L_0x0009:
        r1 = r8.mCaptureInfoList;	 Catch:{ all -> 0x0046 }
        r2 = 0;
        r6 = r1.remove(r2);	 Catch:{ all -> 0x0046 }
        r6 = (com.android.camera.module.MorphoPanoramaModule.CaptureInfo) r6;	 Catch:{ all -> 0x0046 }
        r0 = new com.android.camera.module.MorphoPanoramaModule$StillImageData;	 Catch:{ all -> 0x0046 }
        r2 = r6.mId;	 Catch:{ all -> 0x0046 }
        r3 = r8.mPreviewCount;	 Catch:{ all -> 0x0046 }
        r5 = r8.mMotionData;	 Catch:{ all -> 0x0046 }
        r1 = r8;
        r4 = r9;
        r0.<init>(r2, r3, r4, r5);	 Catch:{ all -> 0x0046 }
        r8.addStillImage(r0);	 Catch:{ all -> 0x0046 }
        r1 = r6.mStatus;	 Catch:{ all -> 0x0046 }
        switch(r1) {
            case 1: goto L_0x0041;
            case 3: goto L_0x0041;
            case 11: goto L_0x0041;
            default: goto L_0x0027;
        };	 Catch:{ all -> 0x0046 }
    L_0x0027:
        r1 = r8.mCameraDevice;	 Catch:{ all -> 0x0046 }
        r1.startPreview();	 Catch:{ all -> 0x0046 }
        r1 = r8.mIsShooting;	 Catch:{ all -> 0x0046 }
        if (r1 == 0) goto L_0x003f;
    L_0x0030:
        r1 = r8.mCameraDevice;	 Catch:{ all -> 0x0046 }
        r2 = r8.mCameraPreviewBuff;	 Catch:{ all -> 0x0046 }
        r1.addCallbackBuffer(r2);	 Catch:{ all -> 0x0046 }
        r1 = 1;
        r8.mPreviewSkipCount = r1;	 Catch:{ all -> 0x0046 }
        r1 = r8.mCameraDevice;	 Catch:{ all -> 0x0046 }
        r1.setPreviewCallbackWithBuffer(r8);	 Catch:{ all -> 0x0046 }
    L_0x003f:
        monitor-exit(r7);
        return;
    L_0x0041:
        r1 = 1;
        r8.stopPanoramaShooting(r1);	 Catch:{ all -> 0x0046 }
        goto L_0x003f;
    L_0x0046:
        r1 = move-exception;
        monitor-exit(r7);
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.MorphoPanoramaModule.onPictureTakenPreview(byte[]):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onPreviewFrame(byte[] r23, android.hardware.Camera r24) {
        /*
        r22 = this;
        r2 = TAG;
        r3 = "onPreviewFrame";
        com.android.camera.Log.m5v(r2, r3);
        r0 = r22;
        r0 = r0.mSyncObj;
        r21 = r0;
        monitor-enter(r21);
        r0 = r22;
        r2 = r0.mCameraDevice;	 Catch:{ all -> 0x035c }
        if (r2 == 0) goto L_0x001b;
    L_0x0015:
        r0 = r22;
        r2 = r0.mMorphoPanoramaGP;	 Catch:{ all -> 0x035c }
        if (r2 != 0) goto L_0x001d;
    L_0x001b:
        monitor-exit(r21);
        return;
    L_0x001d:
        r2 = r22.isProcessingFinishTask();	 Catch:{ all -> 0x035c }
        if (r2 != 0) goto L_0x001b;
    L_0x0023:
        r0 = r22;
        r2 = r0.mPreviewSkipCount;	 Catch:{ all -> 0x035c }
        if (r2 <= 0) goto L_0x0040;
    L_0x0029:
        r0 = r22;
        r2 = r0.mPreviewSkipCount;	 Catch:{ all -> 0x035c }
        r2 = r2 + -1;
        r0 = r22;
        r0.mPreviewSkipCount = r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mCameraDevice;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mCameraPreviewBuff;	 Catch:{ all -> 0x035c }
        r2.addCallbackBuffer(r3);	 Catch:{ all -> 0x035c }
        monitor-exit(r21);
        return;
    L_0x0040:
        r0 = r22;
        r2 = r0.mPreviewCount;	 Catch:{ all -> 0x035c }
        r2 = r2 + 1;
        r0 = r22;
        r0.mPreviewCount = r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mMoveSpeed;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r4 = 0;
        r2[r4] = r3;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mMorphoPanoramaGP;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r4 = r0.mUseImage;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r5 = r0.mImageID;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r6 = r0.mMotionData;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r7 = r0.mStatus;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r8 = r0.mPreviewImage;	 Catch:{ all -> 0x035c }
        r3 = r23;
        r17 = r2.attachPreview(r3, r4, r5, r6, r7, r8);	 Catch:{ all -> 0x035c }
        if (r17 == 0) goto L_0x008d;
    L_0x0072:
        r2 = TAG;	 Catch:{ all -> 0x035c }
        r3 = "%s:attachPreview() -> 0x%x";
        r4 = 2;
        r4 = new java.lang.Object[r4];	 Catch:{ all -> 0x035c }
        r5 = TAG;	 Catch:{ all -> 0x035c }
        r6 = 0;
        r4[r6] = r5;	 Catch:{ all -> 0x035c }
        r5 = java.lang.Integer.valueOf(r17);	 Catch:{ all -> 0x035c }
        r6 = 1;
        r4[r6] = r5;	 Catch:{ all -> 0x035c }
        r3 = java.lang.String.format(r3, r4);	 Catch:{ all -> 0x035c }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x035c }
    L_0x008d:
        r2 = TAG;	 Catch:{ all -> 0x035c }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x035c }
        r3.<init>();	 Catch:{ all -> 0x035c }
        r4 = "onPreviewFrame attachPreview status=";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r4 = r0.mStatus;	 Catch:{ all -> 0x035c }
        r5 = 0;
        r4 = r4[r5];	 Catch:{ all -> 0x035c }
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r3 = r3.toString();	 Catch:{ all -> 0x035c }
        com.android.camera.Log.m5v(r2, r3);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mStatus;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = r2[r3];	 Catch:{ all -> 0x035c }
        if (r2 == 0) goto L_0x00e6;
    L_0x00b6:
        r2 = TAG;	 Catch:{ all -> 0x035c }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x035c }
        r3.<init>();	 Catch:{ all -> 0x035c }
        r4 = TAG;	 Catch:{ all -> 0x035c }
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r4 = ":attachPreview Status : ";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r4 = r0.mStatus;	 Catch:{ all -> 0x035c }
        r5 = 0;
        r4 = r4[r5];	 Catch:{ all -> 0x035c }
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r3 = r3.toString();	 Catch:{ all -> 0x035c }
        android.util.Log.w(r2, r3);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mStatus;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = r2[r3];	 Catch:{ all -> 0x035c }
        switch(r2) {
            case 4: goto L_0x033f;
            case 5: goto L_0x034f;
            case 6: goto L_0x0347;
            case 7: goto L_0x00e6;
            case 8: goto L_0x0347;
            case 9: goto L_0x0347;
            case 10: goto L_0x033f;
            default: goto L_0x00e6;
        };	 Catch:{ all -> 0x035c }
    L_0x00e6:
        r0 = r22;
        r2 = r0.mMorphoPanoramaGP;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mDirection;	 Catch:{ all -> 0x035c }
        r17 = r2.getCurrentDirection(r3);	 Catch:{ all -> 0x035c }
        if (r17 == 0) goto L_0x010f;
    L_0x00f4:
        r2 = TAG;	 Catch:{ all -> 0x035c }
        r3 = "%s:getCurrentDirection() -> 0x%x";
        r4 = 2;
        r4 = new java.lang.Object[r4];	 Catch:{ all -> 0x035c }
        r5 = TAG;	 Catch:{ all -> 0x035c }
        r6 = 0;
        r4[r6] = r5;	 Catch:{ all -> 0x035c }
        r5 = java.lang.Integer.valueOf(r17);	 Catch:{ all -> 0x035c }
        r6 = 1;
        r4[r6] = r5;	 Catch:{ all -> 0x035c }
        r3 = java.lang.String.format(r3, r4);	 Catch:{ all -> 0x035c }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x035c }
    L_0x010f:
        r0 = r22;
        r2 = r0.mPrevDirection;	 Catch:{ all -> 0x035c }
        if (r2 == 0) goto L_0x03a3;
    L_0x0115:
        r0 = r22;
        r2 = r0.mPrevDirection;	 Catch:{ all -> 0x035c }
        r3 = 1;
        if (r2 == r3) goto L_0x03a3;
    L_0x011c:
        r0 = r22;
        r2 = r0.mPrevDirection;	 Catch:{ all -> 0x035c }
        r3 = 8;
        if (r2 == r3) goto L_0x03a3;
    L_0x0124:
        r0 = r22;
        r2 = r0.mPreviewImage;	 Catch:{ all -> 0x035c }
        r20 = r2.getWidth();	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mPreviewImage;	 Catch:{ all -> 0x035c }
        r18 = r2.getHeight();	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mDispPreviewImage;	 Catch:{ all -> 0x035c }
        r14 = r2.getWidth();	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mDispPreviewImage;	 Catch:{ all -> 0x035c }
        r12 = r2.getHeight();	 Catch:{ all -> 0x035c }
        r10 = new android.graphics.Canvas;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mDispPreviewImage;	 Catch:{ all -> 0x035c }
        r10.<init>(r2);	 Catch:{ all -> 0x035c }
        r2 = android.graphics.PorterDuff.Mode.SRC;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r10.drawColor(r3, r2);	 Catch:{ all -> 0x035c }
        r13 = new android.graphics.Rect;	 Catch:{ all -> 0x035c }
        r2 = 0;
        r3 = 0;
        r13.<init>(r2, r3, r14, r12);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mPreviewCroppingAdjustByAuto;	 Catch:{ all -> 0x035c }
        r2 = r2 * 2;
        r18 = r18 - r2;
        r19 = new android.graphics.Rect;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mPreviewCroppingAdjustByAuto;	 Catch:{ all -> 0x035c }
        r3 = r18 * 10;
        r3 = r3 / 100;
        r2 = r2 + r3;
        r0 = r22;
        r3 = r0.mPreviewCroppingAdjustByAuto;	 Catch:{ all -> 0x035c }
        r4 = r18 * 90;
        r4 = r4 / 100;
        r3 = r3 + r4;
        r4 = 0;
        r0 = r19;
        r1 = r20;
        r0.<init>(r4, r2, r1, r3);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mPreviewImage;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r0 = r19;
        r10.drawBitmap(r2, r0, r13, r3);	 Catch:{ all -> 0x035c }
        r9 = new android.graphics.Point;	 Catch:{ all -> 0x035c }
        r9.<init>();	 Catch:{ all -> 0x035c }
        r15 = new android.graphics.Point;	 Catch:{ all -> 0x035c }
        r15.<init>();	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mMorphoPanoramaGP;	 Catch:{ all -> 0x035c }
        r2.getGuidancePos(r9, r15);	 Catch:{ all -> 0x035c }
        r2 = TAG;	 Catch:{ all -> 0x035c }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x035c }
        r3.<init>();	 Catch:{ all -> 0x035c }
        r4 = "onPreviewFrame getGuidancePos attachedPos=";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r3 = r3.append(r9);	 Catch:{ all -> 0x035c }
        r4 = "  guidePos ";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r3 = r3.append(r15);	 Catch:{ all -> 0x035c }
        r3 = r3.toString();	 Catch:{ all -> 0x035c }
        com.android.camera.Log.m5v(r2, r3);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mDirection;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = r2[r3];	 Catch:{ all -> 0x035c }
        switch(r2) {
            case 2: goto L_0x035f;
            case 3: goto L_0x035f;
            default: goto L_0x01c7;
        };	 Catch:{ all -> 0x035c }
    L_0x01c7:
        r0 = r22;
        r2 = r0.mPreviewDisplayRatio;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = (r2 > r3 ? 1 : (r2 == r3 ? 0 : -1));
        if (r2 != 0) goto L_0x01db;
    L_0x01d0:
        if (r14 >= r12) goto L_0x0399;
    L_0x01d2:
        r2 = (float) r12;	 Catch:{ all -> 0x035c }
        r0 = r18;
        r3 = (float) r0;	 Catch:{ all -> 0x035c }
        r2 = r2 / r3;
    L_0x01d7:
        r0 = r22;
        r0.mPreviewDisplayRatio = r2;	 Catch:{ all -> 0x035c }
    L_0x01db:
        r0 = r20;
        r1 = r18;
        r2 = java.lang.Math.min(r0, r1);	 Catch:{ all -> 0x035c }
        r2 = r2 / 2;
        r0 = r22;
        r0.mPreviewRefY = r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mPreviewRefY;	 Catch:{ all -> 0x035c }
        if (r2 == 0) goto L_0x0207;
    L_0x01ef:
        r0 = r22;
        r2 = r0.mPreviewDisplayRatio;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = (r2 > r3 ? 1 : (r2 == r3 ? 0 : -1));
        if (r2 == 0) goto L_0x0207;
    L_0x01f8:
        r0 = r22;
        r2 = r0.mPreviewRefY;	 Catch:{ all -> 0x035c }
        r2 = (float) r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewDisplayRatio;	 Catch:{ all -> 0x035c }
        r2 = r2 * r3;
        r2 = (int) r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r0.mPreviewRefY = r2;	 Catch:{ all -> 0x035c }
    L_0x0207:
        r2 = r9.x;	 Catch:{ all -> 0x035c }
        r2 = (float) r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewDisplayRatio;	 Catch:{ all -> 0x035c }
        r2 = r2 * r3;
        r2 = (int) r2;	 Catch:{ all -> 0x035c }
        r9.x = r2;	 Catch:{ all -> 0x035c }
        r2 = r9.y;	 Catch:{ all -> 0x035c }
        r2 = (float) r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewDisplayRatio;	 Catch:{ all -> 0x035c }
        r2 = r2 * r3;
        r2 = (int) r2;	 Catch:{ all -> 0x035c }
        r9.y = r2;	 Catch:{ all -> 0x035c }
        r2 = r15.x;	 Catch:{ all -> 0x035c }
        r2 = (float) r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewDisplayRatio;	 Catch:{ all -> 0x035c }
        r2 = r2 * r3;
        r2 = (int) r2;	 Catch:{ all -> 0x035c }
        r15.x = r2;	 Catch:{ all -> 0x035c }
        r2 = r15.y;	 Catch:{ all -> 0x035c }
        r2 = (float) r2;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewDisplayRatio;	 Catch:{ all -> 0x035c }
        r2 = r2 * r3;
        r2 = (int) r2;	 Catch:{ all -> 0x035c }
        r15.y = r2;	 Catch:{ all -> 0x035c }
        r2 = TAG;	 Catch:{ all -> 0x035c }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x035c }
        r3.<init>();	 Catch:{ all -> 0x035c }
        r4 = "onPreviewFrame change position with ratio Point=";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r3 = r3.append(r9);	 Catch:{ all -> 0x035c }
        r4 = " ";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r3 = r3.append(r15);	 Catch:{ all -> 0x035c }
        r4 = " output_rotation=";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r4 = r0.mInitParam;	 Catch:{ all -> 0x035c }
        r4 = r4.output_rotation;	 Catch:{ all -> 0x035c }
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r4 = " mDirection[0]=";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r4 = r0.mDirection;	 Catch:{ all -> 0x035c }
        r5 = 0;
        r4 = r4[r5];	 Catch:{ all -> 0x035c }
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r3 = r3.toString();	 Catch:{ all -> 0x035c }
        com.android.camera.Log.m5v(r2, r3);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mMorphoPanoramaGP;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mMoveSpeed;	 Catch:{ all -> 0x035c }
        r2.getMoveSpeed(r3);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mMovingDirectionView;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mStatus;	 Catch:{ all -> 0x035c }
        r4 = 0;
        r2 = r2[r4];	 Catch:{ all -> 0x035c }
        r4 = 5;
        if (r2 != r4) goto L_0x03a0;
    L_0x0293:
        r2 = 1;
    L_0x0294:
        r0 = r22;
        r4 = r0.mMoveSpeed;	 Catch:{ all -> 0x035c }
        r5 = 0;
        r4 = r4[r5];	 Catch:{ all -> 0x035c }
        r3.setToofast(r2, r4);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mMovingDirectionView;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewRefY;	 Catch:{ all -> 0x035c }
        r2.setPosition(r9, r3);	 Catch:{ all -> 0x035c }
        r22.onPreviewMoving();	 Catch:{ all -> 0x035c }
    L_0x02ac:
        r0 = r22;
        r2 = r0.mPanoramaPreview;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mDispPreviewImage;	 Catch:{ all -> 0x035c }
        r2.setImageBitmap(r3);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mImageID;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = r2[r3];	 Catch:{ all -> 0x035c }
        if (r2 < 0) goto L_0x03c6;
    L_0x02c0:
        r11 = r23;
        r0 = r22;
        r2 = r0.mCaptureInfoList;	 Catch:{ all -> 0x035c }
        r3 = new com.android.camera.module.MorphoPanoramaModule$CaptureInfo;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r4 = r0.mImageID;	 Catch:{ all -> 0x035c }
        r5 = 0;
        r4 = r4[r5];	 Catch:{ all -> 0x035c }
        r0 = r22;
        r5 = r0.mStatus;	 Catch:{ all -> 0x035c }
        r6 = 0;
        r5 = r5[r6];	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3.<init>(r4, r5);	 Catch:{ all -> 0x035c }
        r2.add(r3);	 Catch:{ all -> 0x035c }
        r16 = new android.os.Handler;	 Catch:{ all -> 0x035c }
        r16.<init>();	 Catch:{ all -> 0x035c }
        r2 = new com.android.camera.module.MorphoPanoramaModule$3;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r1 = r23;
        r2.<init>(r1);	 Catch:{ all -> 0x035c }
        r0 = r16;
        r0.post(r2);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r2 = r0.mCntReqShoot;	 Catch:{ all -> 0x035c }
        r2 = r2 + 1;
        r0 = r22;
        r0.mCntReqShoot = r2;	 Catch:{ all -> 0x035c }
    L_0x02fb:
        r2 = r22.getUIController();	 Catch:{ all -> 0x035c }
        r2 = r2.getShutterButton();	 Catch:{ all -> 0x035c }
        r2 = r2.isEnabled();	 Catch:{ all -> 0x035c }
        if (r2 != 0) goto L_0x0315;
    L_0x0309:
        r2 = r22.getUIController();	 Catch:{ all -> 0x035c }
        r2 = r2.getShutterButton();	 Catch:{ all -> 0x035c }
        r3 = 1;
        r2.setEnabled(r3);	 Catch:{ all -> 0x035c }
    L_0x0315:
        r0 = r22;
        r2 = r0.mDirection;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = r2[r3];	 Catch:{ all -> 0x035c }
        r0 = r22;
        r0.mPrevDirection = r2;	 Catch:{ all -> 0x035c }
        r2 = TAG;	 Catch:{ all -> 0x035c }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x035c }
        r3.<init>();	 Catch:{ all -> 0x035c }
        r4 = "onPreviewFrame mPrevDirection=";
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r0 = r22;
        r4 = r0.mPrevDirection;	 Catch:{ all -> 0x035c }
        r3 = r3.append(r4);	 Catch:{ all -> 0x035c }
        r3 = r3.toString();	 Catch:{ all -> 0x035c }
        com.android.camera.Log.m5v(r2, r3);	 Catch:{ all -> 0x035c }
        monitor-exit(r21);
        return;
    L_0x033f:
        r2 = 1;
        r0 = r22;
        r0.stopPanoramaShooting(r2);	 Catch:{ all -> 0x035c }
        monitor-exit(r21);
        return;
    L_0x0347:
        r2 = 1;
        r0 = r22;
        r0.stopPanoramaShooting(r2);	 Catch:{ all -> 0x035c }
        monitor-exit(r21);
        return;
    L_0x034f:
        r0 = r22;
        r2 = r0.mMorphoPanoramaGP;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mMoveSpeed;	 Catch:{ all -> 0x035c }
        r2.getMoveSpeed(r3);	 Catch:{ all -> 0x035c }
        goto L_0x00e6;
    L_0x035c:
        r2 = move-exception;
        monitor-exit(r21);
        throw r2;
    L_0x035f:
        r0 = r22;
        r2 = r0.mInitParam;	 Catch:{ all -> 0x035c }
        r2 = r2.output_rotation;	 Catch:{ all -> 0x035c }
        if (r2 == 0) goto L_0x0371;
    L_0x0367:
        r0 = r22;
        r2 = r0.mInitParam;	 Catch:{ all -> 0x035c }
        r2 = r2.output_rotation;	 Catch:{ all -> 0x035c }
        r3 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        if (r2 != r3) goto L_0x0385;
    L_0x0371:
        r2 = r9.y;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewCroppingAdjustByAuto;	 Catch:{ all -> 0x035c }
        r2 = r2 - r3;
        r9.y = r2;	 Catch:{ all -> 0x035c }
        r2 = r15.y;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewCroppingAdjustByAuto;	 Catch:{ all -> 0x035c }
        r2 = r2 - r3;
        r15.y = r2;	 Catch:{ all -> 0x035c }
        goto L_0x01c7;
    L_0x0385:
        r2 = r9.x;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewCroppingAdjustByAuto;	 Catch:{ all -> 0x035c }
        r2 = r2 - r3;
        r9.x = r2;	 Catch:{ all -> 0x035c }
        r2 = r15.x;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mPreviewCroppingAdjustByAuto;	 Catch:{ all -> 0x035c }
        r2 = r2 - r3;
        r15.x = r2;	 Catch:{ all -> 0x035c }
        goto L_0x01c7;
    L_0x0399:
        r2 = (float) r14;	 Catch:{ all -> 0x035c }
        r0 = r20;
        r3 = (float) r0;	 Catch:{ all -> 0x035c }
        r2 = r2 / r3;
        goto L_0x01d7;
    L_0x03a0:
        r2 = 0;
        goto L_0x0294;
    L_0x03a3:
        r0 = r22;
        r2 = r0.mDirection;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = r2[r3];	 Catch:{ all -> 0x035c }
        if (r2 == 0) goto L_0x02ac;
    L_0x03ac:
        r0 = r22;
        r2 = r0.mDirection;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = r2[r3];	 Catch:{ all -> 0x035c }
        r3 = 1;
        if (r2 == r3) goto L_0x02ac;
    L_0x03b6:
        r0 = r22;
        r2 = r0.mDirection;	 Catch:{ all -> 0x035c }
        r3 = 0;
        r2 = r2[r3];	 Catch:{ all -> 0x035c }
        r3 = 8;
        if (r2 == r3) goto L_0x02ac;
    L_0x03c1:
        r22.onCaptureOrientationDecided();	 Catch:{ all -> 0x035c }
        goto L_0x02ac;
    L_0x03c6:
        r0 = r22;
        r2 = r0.mCameraDevice;	 Catch:{ all -> 0x035c }
        r0 = r22;
        r3 = r0.mCameraPreviewBuff;	 Catch:{ all -> 0x035c }
        r2.addCallbackBuffer(r3);	 Catch:{ all -> 0x035c }
        goto L_0x02fb;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.MorphoPanoramaModule.onPreviewFrame(byte[], android.hardware.Camera):void");
    }

    public void onResumeAfterSuper() {
        super.onResumeAfterSuper();
        com.android.camera.Log.m5v(TAG, "mSetupCameraThread=" + this.mSetupCameraThread + " mCameraState=" + this.mCameraState + " mCameraDevice=" + this.mCameraDevice + " mWaitForRelease=" + this.mWaitForRelease);
        if (this.mSetupCameraThread == null && (this.mCameraState == 0 || this.mCameraDevice == null)) {
            this.mSetupCameraThread = new SetupCameraThread();
            this.mSetupCameraThread.start();
        } else if (this.mWaitForRelease) {
            startCameraPreview();
            this.mMainHandler.sendEmptyMessage(5);
        }
        this.mWaitForRelease = false;
        getUIController().onResume();
        this.mActivity.loadCameraSound(2);
        this.mActivity.loadCameraSound(3);
        this.mActivity.getThumbnailUpdater().getLastThumbnail();
        keepScreenOnAwhile();
    }

    public void onResumeBeforeSuper() {
        super.onResumeBeforeSuper();
        if (CameraSettings.sCameraChangeManager.check(3)) {
            this.mPaused = true;
            switchToCameraMode();
        }
    }

    public void onShutterButtonClick() {
        if (!this.mPaused && this.mCameraState != 0) {
            this.mActivity.getScreenHint().updateHint();
            if (!Storage.isLowStorageAtLastPoint()) {
                synchronized (this.mSyncObj) {
                    if (this.mIsShooting) {
                        playCameraSound(3);
                        stopPanoramaShooting(true);
                    } else {
                        playCameraSound(2);
                        startPanoramaShooting();
                    }
                }
            }
        }
    }

    public void onShutterButtonFocus(boolean z) {
    }

    public void onStop() {
        super.onStop();
        if (this.mActivity.isNeedResetGotoGallery() && Device.isReleaseLaterForGallery()) {
            releaseResources();
        }
    }

    @OnClickAttr
    public void onThumbnailClicked(View view) {
        if (!this.mPaused && !isProcessingFinishTask() && this.mActivity.getThumbnailUpdater().getThumbnail() != null) {
            this.mActivity.gotoGallery();
        }
    }

    public void onUserInteraction() {
        super.onUserInteraction();
        if (!this.mIsShooting) {
            keepScreenOnAwhile();
        }
    }

    protected void openSettingActivity() {
        Intent intent = new Intent();
        intent.setClass(this.mActivity, CameraPreferenceActivity.class);
        intent.putExtra("from_where", 1);
        intent.putExtra(":miui:starting_window_label", getResources().getString(C0049R.string.pref_camera_settings_category));
        if (this.mActivity.startFromKeyguard()) {
            intent.putExtra("StartActivityWhenLocked", true);
        }
        this.mActivity.startActivity(intent);
        this.mActivity.setJumpFlag(2);
        CameraDataAnalytics.instance().trackEvent("pref_settings");
    }

    protected void performVolumeKeyClicked(int i, boolean z) {
        if (i == 0 && z) {
            onShutterButtonClick();
            if (this.mCameraState == 1) {
                CameraDataAnalytics.instance().trackEvent("capture_times_volume");
            }
        }
    }

    public void requestRender() {
        if (this.mStillPreviewHintArea.getVisibility() == 0) {
            requestStillPreviewRender();
        }
    }

    public void startPanoramaShooting() {
        Log.v(TAG, "startPanoramaShooting");
        if (!isProcessingFinishTask()) {
            startPanoramaGP();
            this.mPrevDirection = this.mInitParam.direction;
            this.mPreviewCount = -1;
            this.mCntReqShoot = 0;
            this.mCntProcessd = 0;
            this.mCaptureInfoList.clear();
            this.mTimeTaken = System.currentTimeMillis();
            this.mPreviewDisplayRatio = 0.0f;
            this.mPreviewSkipCount = 1;
            this.mDeviceOrientationAtCapture = this.mOrientationCompensation;
            this.mIsShooting = true;
            if (this.mAeLockSupported) {
                this.mParameters.setAutoExposureLock(true);
            }
            if (this.mAwbLockSupported) {
                this.mParameters.setAutoWhiteBalanceLock(true);
            }
            this.mLocation = LocationManager.instance().getCurrentLocation();
            Util.setGpsParameters(this.mParameters, this.mLocation);
            this.mParameters.setFocusMode(this.mSnapshotFocusMode);
            this.mCameraDevice.setParameters(this.mParameters);
            this.mCameraDevice.addCallbackBuffer(this.mCameraPreviewBuff);
            this.mCameraDevice.setPreviewCallbackWithBuffer(this);
            setShootUI();
        }
    }
}
