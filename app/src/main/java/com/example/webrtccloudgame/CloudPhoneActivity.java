package com.example.webrtccloudgame;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;

public class CloudPhoneActivity extends Activity implements WebRtcClient.RtcListener {

    private static final String TAG = CloudPhoneActivity.class.getCanonicalName();

    private WebRtcClient mWebRtcClient;

    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1116;
    private static Intent mMediaProjectionPermissionResultData;
    private static int mMediaProjectionPermissionResultCode;

    private static final String STREAM_NAME_PREFIX = "cloud_phone_stream";
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.INTERNET"};
    protected PermissionChecker permissionChecker = new PermissionChecker();

    public static int sDeviceWidth;
    public static int sDeviceHeight;
    public static final int SCREEN_RESOLUTION_SCALE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_cloud_phone);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        sDeviceWidth = metrics.widthPixels;
        sDeviceHeight = metrics.heightPixels;

        checkPermissions();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startScreenCapture();
        } else {
            init();
        }
    }

    private void checkPermissions() {
        permissionChecker.verifyPermissions(this, MANDATORY_PERMISSIONS, new PermissionChecker.VerifyPermissionsCallback() {

            @Override
            public void onPermissionAllGranted() {

            }

            @Override
            public void onPermissionDeny(String[] permissions) {
                Toast.makeText(CloudPhoneActivity.this, "Please grant required permissions.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @TargetApi(21)
    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @TargetApi(21)
    private VideoCapturer createScreenCapturer() {
        if (mMediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            Log.e(TAG, "User didn't give permission to capture the screen.");
            return null;
        }
        return new ScreenCapturerAndroid(
                mMediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.e(TAG, "User revoked permission to capture the screen.");
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        mMediaProjectionPermissionResultCode = resultCode;
        mMediaProjectionPermissionResultData = data;
        init();
    }

    private void init() {
        PeerConnectionClient.PeerConnectionParameters peerConnectionParameters =
                new PeerConnectionClient.PeerConnectionParameters(
                        true,
                        false,
                        true,
                        sDeviceWidth / SCREEN_RESOLUTION_SCALE,
                        sDeviceHeight / SCREEN_RESOLUTION_SCALE,
                        30,
                        0,
                        "VP9",
                        true,
                        true,
                        0,
                        "OPUS",
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        null);
        mWebRtcClient = new WebRtcClient(getApplicationContext(), EglBase.create(), peerConnectionParameters, this, createScreenCapturer());
    }

    @Override
    public void onDestroy() {
        if (mWebRtcClient != null) {
            mWebRtcClient.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onReady(String socketId) {
        Log.d(TAG, "On ready, callId = " + socketId);
        mWebRtcClient.start(STREAM_NAME_PREFIX);
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAddLocalStream(MediaStream localStream) {

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {

    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {

    }

}
