package com.example.webrtccloudgame;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

public class ClientActivity extends Activity implements WebRtcClient.RtcListener {

    private static final String TAG = ClientActivity.class.getCanonicalName();

    private WebRtcClient mWebRtcClient;
    private String cloudPhoneSocketId = "EUZWeNA1G4yCPrkDAABl";

    private EglBase eglBase = EglBase.create();
    private SurfaceViewRenderer fullscreenRenderer;
    private ProxyRenderer remoteProxyRenderer = new ProxyRenderer();

    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.INTERNET"};
    protected PermissionChecker permissionChecker = new PermissionChecker();

    public static int sDeviceWidth;
    public static int sDeviceHeight;
    public static final int SCREEN_RESOLUTION_SCALE = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_client);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        sDeviceWidth = metrics.widthPixels;
        sDeviceHeight = metrics.heightPixels;

        fullscreenRenderer = findViewById(R.id.fullscreen_video_view);
        fullscreenRenderer.setKeepScreenOn(true);
        fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        fullscreenRenderer.setEnableHardwareScaler(true);
        remoteProxyRenderer.setTarget(fullscreenRenderer);

        checkPermissions();
        init();
    }

    private void checkPermissions() {
        permissionChecker.verifyPermissions(this, MANDATORY_PERMISSIONS, new PermissionChecker.VerifyPermissionsCallback() {

            @Override
            public void onPermissionAllGranted() {

            }

            @Override
            public void onPermissionDeny(String[] permissions) {
                Toast.makeText(ClientActivity.this, "Please grant required permissions.", Toast.LENGTH_LONG).show();
            }
        });
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
        mWebRtcClient = new WebRtcClient(getApplicationContext(), eglBase, peerConnectionParameters, this, null);
    }

    public void answer(String callerId) throws JSONException {
        mWebRtcClient.sendMessage(callerId, "init", null);
    }

    @Override
    public void onReady(String socketId) {
        try {
            answer(cloudPhoneSocketId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String newStatus) {
    }

    @Override
    public void onAddLocalStream(MediaStream localStream) {
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        if (remoteStream.videoTracks.size() == 1) {
            Log.d(TAG, "onAddRemoteStream");
            VideoTrack remoteVideoTrack = remoteStream.videoTracks.get(0);
            remoteVideoTrack.setEnabled(true);
            remoteVideoTrack.addRenderer(new VideoRenderer(remoteProxyRenderer));
        }
    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
    }

    private static class ProxyRenderer implements VideoRenderer.Callbacks {
        private VideoRenderer.Callbacks target;

        @Override
        synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                VideoRenderer.renderFrameDone(frame);
                return;
            }

            target.renderFrame(frame);
        }

        synchronized public void setTarget(VideoRenderer.Callbacks target) {
            this.target = target;
        }
    }
}
