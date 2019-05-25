package com.github.esabook.swaplock.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.github.esabook.swaplock.R;
import com.github.esabook.swaplock.databinding.ActivityScanBinding;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.github.esabook.swaplock.zxing.AmbientLightManager;
import com.github.esabook.swaplock.zxing.CaptureActivityHandler;
import com.github.esabook.swaplock.zxing.DecodeFormatManager;
import com.github.esabook.swaplock.zxing.InactivityTimer;
import com.github.esabook.swaplock.zxing.IntentSource;
import com.github.esabook.swaplock.zxing.Intents;
import com.github.esabook.swaplock.zxing.ViewfinderView;
import com.github.esabook.swaplock.zxing.camera.CameraManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

//import com.github.esabook.swaplock.zxing.BeepManager;
//import com.github.esabook.swaplock.zxing.result.ResultButtonListener;
//import com.github.esabook.swaplock.zxing.result.ResultHandlerFactory;

public class BatteryScanActivity extends BatterySwapActivity implements SurfaceHolder.Callback {

    private static final String TAG = BatteryScanActivity.class.getSimpleName();
    ActivityScanBinding mBinding;
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private Result lastResult;
    private boolean hasSurface;
    private IntentSource source;
    private Collection<BarcodeFormat> decodeFormats = DecodeFormatManager.QR_CODE_FORMATS;
    private Map<DecodeHintType, ?> decodeHints;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private AmbientLightManager ambientLightManager;

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_scan);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        ambientLightManager = new AmbientLightManager(this);

        // button scan (next page)
        mBinding.btnForScan.setOnClickListener(v -> {
            Intent intent = new Intent(BatteryScanActivity.this, BatteryControlActivity.class);
            intent.putExtra(BatteryControlActivity.BT_DEVICE_ADDRES, mBinding.mac.getText().toString());
            startActivity(intent);
            finish();
        });

        // back to list
        mBinding.btnOpenList.setOnClickListener(v -> {
            finish();
        });

        // text watcher
        mBinding.mac.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().matches("(^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$)")) {

                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(200);

                    inactivityTimer.onActivity();
                    stopScanner();
                    if (source == IntentSource.NATIVE_APP_INTENT) {
                        setResult(RESULT_CANCELED);
                    }
                    mBinding.btnForScan.setEnabled(true);
                } else {
                    mBinding.btnForScan.setEnabled(false);
                    if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK) && lastResult != null) {
                        restartPreviewAfterDelay(0L);
                    }
                }
            }
        });

        mBinding.previewView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (cameraManager == null) return;
            cameraManager.getConfigManager().getScreenResolution().set(v.getWidth(), v.getHeight());
        });


        getSupportActionBar().hide();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isCameraPermissionGranted()) {
            startScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (isCameraPermissionGranted()) {
            finish();
            startActivity(new Intent(this, this.getClass()));
        } else {
            stopScanner();
        }
    }


    @Override
    protected void onPause() {
        stopScanner();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intents.FLAG_NEW_DOC);
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // do nothing
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    boolean isCameraPermissionGranted() {
        boolean shouldShowReqPermission = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA);
        boolean isNeedPermission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;

        // dialog request permission
        if (isNeedPermission) {
            if (shouldShowReqPermission) {
                // manual storage permission
                AlertDialog.Builder alert = new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("Enabled camera to able start QR scanner.");

                alert.setPositiveButton("Open Setting", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + this.getPackageName()));
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
                alert.setNeutralButton("Turn On", (d, w) -> {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{
                                    Manifest.permission.CAMERA},
                            0);
                });
                alert.setNegativeButton("Later", (dialog, which) -> dialog.dismiss());
                alert.create().show();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.CAMERA},
                        0);
            }
        }
        return !isNeedPermission;
    }

    void startScanner() {

        if (handler != null) return;

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());


        viewfinderView = findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        handler = null;
        lastResult = null;

        setRequestedOrientation(getCurrentOrientation());

        resetStatusView();

        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();
        source = IntentSource.NONE;
        decodeFormats = null;
        characterSet = null;


        SurfaceView surfaceView = findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();

        if (hasSurface) {
//            // The activity was paused but not stopped, so the surface still exists. Therefore
//            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
//            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    private int getCurrentOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
        } else {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
        }
    }

    void stopScanner() {
        try {

            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }
            inactivityTimer.onPause();
            ambientLightManager.stop();
//            beepManager.close();
            if (cameraManager != null)
                cameraManager.closeDriver();

            if (!hasSurface) {
                SurfaceView surfaceView = findViewById(R.id.preview_view);
                SurfaceHolder surfaceHolder = surfaceView.getHolder();
                surfaceHolder.removeCallback(this);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }


    }


    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        lastResult = rawResult;

        mBinding.mac.setText(lastResult.getText());
    }


    @SuppressLint("MissingPermission")
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);

        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            e.printStackTrace();
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }


    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

}
