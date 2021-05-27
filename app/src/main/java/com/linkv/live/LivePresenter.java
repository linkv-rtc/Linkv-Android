package com.linkv.live;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.MainThread;

import com.linkv.live.activity.LiveActivity;
import com.linkv.live.network.NetManager;
import com.linkv.live.rtc.AudioRecorder;
import com.linkv.live.rtc.ScreenRecorder;
import com.linkv.live.utils.Constants;
import com.linkv.live.utils.DimenUtils;
import com.linkv.live.utils.SPUtils;
import com.linkv.live.view.DisplayContainer;
import com.linkv.live.view.RemoteDisplayView;
import com.linkv.rtc.LVConstants;
import com.linkv.rtc.LVErrorCode;
import com.linkv.rtc.LVRTCEngine;
import com.linkv.rtc.callback.LVRTCCallback;
import com.linkv.rtc.entity.LVAVConfig;
import com.linkv.rtc.entity.LVAudioVolume;
import com.linkv.rtc.entity.LVExternalAudioConfig;
import com.linkv.rtc.entity.LVUser;
import com.linkv.rtc.entity.LVVideoStatistic;
import com.linkv.rtc.internal.base.Frame;
import com.linkv.rtc.internal.base.I420Frame;
import com.linkv.rtc.render.LVDisplayView;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class LivePresenter {

    public static final String HEAD_UID_HOST = "H";
    public static final String HEAD_ROOM_LIVE = "L";
    public static final String HEAD_ROOM_MEETING = "M";
    public static final String HEAD_ROOM_AUDIO = "A";
    private static final String TAG = "LivePresenter";
    private static final String HEAD_UID_GUEST = "G";
    private static volatile boolean initResult = false;
    private static LivePresenter instance;
    private int mLiveType;
    private String mUid;
    private String mRoomId;
    private int mVideoResolutionLevel = LVAVConfig.PROFILE_720P;
    private boolean mIsHost;
    private boolean mMicEnable = true;
    private boolean mSeiEnable = false;
    private boolean mIsFrontCamera = true;
    private boolean mIsCaptureStated = false;
    private volatile boolean mIsPublished = false;
    private Activity mContext;
    private LVAVConfig mAVConfig;
    private LVRTCEngine mRtcEngine;
    private LVDisplayView mLocalDisplayView;
    private LVConstants.LVVideoRotation mOutputRotation;
    private HashMap<String, RemoteDisplayView> mRemoteDisplayMap;
    private HashMap<String, DisplayContainer> mDisplayLayoutMap;
    private LivePresenterCallback mPresenterCallback;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private AudioRecorder mAudioRecorder;
    private ScreenRecorder mScreenRecorder;
    private List<LVAVConfig> mVideoConfigLevels;

    private LivePresenter(Activity context) {
        mContext = context;
        mRemoteDisplayMap = new HashMap<>();
        mDisplayLayoutMap = new HashMap<>();
    }

    public static LivePresenter getInstance(Activity context) {
        if (instance == null) {
            synchronized (LivePresenter.class) {
                if (instance == null) {
                    instance = new LivePresenter(context);
                }
            }
        }
        return instance;
    }

    public void resetContext(Activity context) {
        this.mContext = context;
    }

    public void setPresenterCallback(LivePresenterCallback callback) {
        this.mPresenterCallback = callback;
    }

    public void initData(boolean isHost, String roomId, int type) {
        mIsHost = isHost;
        mRoomId = roomId;
        mLiveType = type;
        mVideoResolutionLevel = isMeetingLive() ? LVAVConfig.PROFILE_180P
                : isScreenSharingLive() ? LVAVConfig.PROFILE_540P
                : LVAVConfig.PROFILE_720P;
        changeVideoConfig(mVideoResolutionLevel, 0, 0);
    }

    public void initEngine(boolean startEngine) {
        mRtcEngine = LVRTCEngine.getInstance(mContext.getApplication());
        if (Constants.IsTestEnv) {
            LVRTCEngine.setDebugServerIp(SPUtils.getDebugServerIp());
        }
        mRtcEngine.initSDK();
        mRtcEngine.setNsMode(GlobalConfig.sw3AMode ? LVConstants.Audio3AMode.SOFTWARE : LVConstants.Audio3AMode.HARDWARE);
        mRtcEngine.setAecMode(GlobalConfig.sw3AMode ? LVConstants.Audio3AMode.SOFTWARE : LVConstants.Audio3AMode.HARDWARE);
        if (!initResult) {
            mRtcEngine.auth(Constants.AppIdSec, Constants.AppSignSec, "", integer ->
            {
                log("auth result: " + integer);
                initResult = (integer == LVErrorCode.SUCCESS);
                if (startEngine) {
                    if (!initResult) {
                        showToast(mContext.getString(R.string.network_error) + mContext.getString(R.string.error_code) + integer);
                    } else {
                        startCaptureOrLogin();
                    }
                }
            });
        } else if (startEngine) {
            startCaptureOrLogin();
        }
    }

    public void resetEngine() {
        if (initResult) {
            initResult = false;
            if (mRtcEngine != null) {
                mRtcEngine.unInitSDK();
            }
            mRtcEngine = null;
            initEngine(false);
        }
    }

    private boolean isCommonLive() {
        return mLiveType == LiveActivity.COMMON_LIVE;
    }

    private boolean isAudioLive() {
        return mLiveType == LiveActivity.AUDIO_LIVE;
    }

    private boolean isMeetingLive() {
        return mLiveType == LiveActivity.MEETING_LIVE;
    }

    private boolean isScreenSharingLive() {
        return mLiveType == LiveActivity.SCREEN_SHARING_LIVE;
    }

    private void startCaptureOrLogin() {
        String head = mIsHost ? HEAD_UID_HOST : HEAD_UID_GUEST;
        mUid = head + System.nanoTime() + new Random().nextInt(100000);
        if (mIsHost) {
            String roomHead = isCommonLive() || isScreenSharingLive() ? HEAD_ROOM_LIVE
                    : isMeetingLive() ? HEAD_ROOM_MEETING
                    : isAudioLive() ? HEAD_ROOM_AUDIO : "";
            mRoomId = roomHead + System.nanoTime() + new Random().nextInt(100000);
        }

        checkLocalRoomIdAndUid();

        runOnUiThread(() -> {
            if (mPresenterCallback == null) {
                return;
            }
            mPresenterCallback.updateUid(mUid);
            if (mIsHost) {
                mPresenterCallback.updateRoomId(mRoomId);
            }
            if (mIsHost && !isAudioLive()) {
                if (addLocalDisplayView() && !isScreenSharingLive()) {
                    startCapture();
                } else if (isScreenSharingLive()) {
                    checkScreenSharingPermission(LiveActivity.SCREEN_PERMISSION_CODE);
                }
            } else if (mIsHost) {
                // audio 直播模式下，只展示布局
                DisplayContainer container = getLocalDisplayLayout();
                if (container == null || container.getLayout() == null) {
                    return;
                }
                setupDisplayContainer(container, mUid);
            }
        });

        if (!mIsHost) {
            startLogin();
        }

    }

    private void checkLocalRoomIdAndUid() {
        if (!TextUtils.isEmpty(SPUtils.getRoomId())) {
            mRoomId = SPUtils.getRoomId();
        }

        if (!TextUtils.isEmpty(SPUtils.getUid())) {
            mUid = SPUtils.getUid();
        }
    }

    private void checkScreenSharingPermission(int requestCode) {
        if (ScreenRecorder.isSupportRecord()) {
            ScreenRecorder.checkPermission(mContext, requestCode);
        } else {
            showToast("版本过低，不支持屏幕共享功能");
        }
    }

    private void startLogin() {
        if (TextUtils.isEmpty(mRoomId) || TextUtils.isEmpty(mUid)) {
            log("参数异常, roomId:" + mRoomId + " uid:" + mUid);
            return;
        }

        if (!initResult) {
            showToast(mContext.getString(R.string.try_later));
            return;
        }
        if (mAVConfig == null) {
            changeOutputResolution(mVideoResolutionLevel);
        }
        mRtcEngine.switchCamera(mIsFrontCamera ? LVConstants.LVRTCCameraPosition.FRONT : LVConstants.LVRTCCameraPosition.BACK);
        setLiveRoomCallback();
        mRtcEngine.loginRoom(mRoomId, mUid, mIsHost, false, (code, cmUsers) -> {
            log("onLoginResult  , code:" + code + ", infoList.size:" + (cmUsers == null ? "0" : cmUsers.size()));
            if (code == LVErrorCode.SUCCESS) {
                showToast(mContext.getString(R.string.login_success));
                setRtcEngineConfig();
                if (mIsHost) {
                    mRtcEngine.setPublishQualityMonitorCycle(1);
                    startPublishStream();
                } else {
                    mRtcEngine.setPlayQualityMonitorCycle(1);
                }

                runOnUiThread(() -> {
                    if (mPresenterCallback != null) {
                        mPresenterCallback.updateLiveButton(View.GONE);
                    }
                });
            } else {
                showToast(mContext.getString(R.string.login_failed) + " " + mContext.getString(R.string.error_code) + code);
            }
        });
    }

    private void setRtcEngineConfig() {
        if (mRtcEngine != null) {
            enableSpeakerPhone(true);
            mRtcEngine.setPublishQualityMonitorCycle(2);
            mRtcEngine.setPlayQualityMonitorCycle(2);
        }
    }

    public void changeOutputResolution(int profileLevel) {
        changeVideoConfig(profileLevel, 0, 0);
    }

    public void changeOutputRotation(LVConstants.LVVideoRotation rotation) {
        if (rotation == null) {
            return;
        }
        mRtcEngine.enableVideoAutoRotation(false);
        mOutputRotation = rotation;
        mRtcEngine.setOutputVideoRotation(rotation);
    }

    private void setLiveRoomCallback() {
        mRtcEngine.setLiveRoomCallback(new LVRTCCallback() {

            private long receiveSeiCount = 0;

            @Override
            public long onDrawFrame(ByteBuffer i420Buffer, final int width, final int height, int strideY, final String userId, String ext) {
                if (TextUtils.isEmpty(userId)) {
                    return 0;
                }

                if (mPresenterCallback == null) {
                    return 0;
                }

                if (isAudioLive()) {
                    return 0;
                }

                if (mRemoteDisplayMap.get(userId) == null) {
                    //添加视频布局
                    runOnUiThread(() -> {
                        // 两次判断，避免异步问题
                        if (mRemoteDisplayMap.get(userId) == null) {
                            setupRemoteVideoDisplayView(userId, width, height);
                        }
                    });
                } else {
                    //更新视频分辨率
                    RemoteDisplayView remoteDisplayView = mRemoteDisplayMap.get(userId);
                    if (remoteDisplayView != null && (remoteDisplayView.getWidth() != width || remoteDisplayView.getHeight() != height)) {
                        runOnUiThread(() -> {
                            DisplayContainer container = mDisplayLayoutMap.get(userId);
                            if (container != null) {
                                remoteDisplayView.setWidth(width);
                                remoteDisplayView.setHeight(height);
                            }
                        });
                    }
                }

                if (!TextUtils.isEmpty(ext)) {
                    if (receiveSeiCount % 1000 == 0) {
                        showToast(mContext.getString(R.string.receive_sei) + ext);
                    }
                    receiveSeiCount++;
                }
                return 0;
            }

            @Override
            public void onReceivedFirstFrame(String userId, String streamId) {
                Log.i(TAG, "onReceivedFirstFrame: " + userId);
            }

            @Override
            public void onAddRemoter(LVUser user) {
                if (user == null) {
                    return;
                }
                String uid = user.userId;
                log("onAddMember userId =" + uid);
                if (TextUtils.isEmpty(uid) || uid.equalsIgnoreCase(mUid)) {
                    return;
                }
                mRtcEngine.startPlayingStream(uid);
                runOnUiThread(() -> {
                    if (!mRemoteDisplayMap.containsKey(uid)) {
                        addRemoteDisplayView(uid);
                    }

                });

            }

            @Override
            public void onDeleteRemoter(String userId) {
                log("onDeleteRemoter userId :" + userId);
                if (TextUtils.isEmpty(userId)) {
                    return;
                }
                runOnUiThread(() -> {
                    if (userId.startsWith(HEAD_UID_HOST)) {
                        if (mPresenterCallback != null) {
                            mPresenterCallback.finishAct();
                        }
                    } else {
                        stopPlayStream(userId);
                        if (mRemoteDisplayMap.containsKey(userId)) {
                            removeRemoteDisplayView(userId);
                        }
                    }
                });
            }

            @Override
            public void onKickOff(int reason, String roomId) {
                log("onKickOff reason: " + reason + ", mRoomId:" + mRoomId);
                runOnUiThread(() -> {
                    showToast("Be kicked off, reason: " + reason);
                    if (mPresenterCallback != null) {
                        mPresenterCallback.finishAct();
                    }
                });
            }

            @Override
            public void onRoomDisconnected(final int errorCode) {
                log("onRoomDisconnect errorCode:" + errorCode + "  mRoomId:" + mRoomId);
                runOnUiThread(() -> {
                    if (mPresenterCallback != null) {
                        mPresenterCallback.finishAct();
                    }
                });
            }

            @Override
            public void onRoomReconnected() {
                log("onRoomReconnected.");
            }

            @Override
            public void onMixComplete(boolean success) {
                log("onMixStreamResult success:" + success);
            }

            @Override
            public void onPublishStateUpdate(final int state) {
                log("onPublishStateUpdate " + " stateCode:" + state);
                if (state == LVErrorCode.SUCCESS && mIsHost) {
                    NetManager.updateRoomState(Constants.AppId, mRoomId, "1");
                    mIsPublished = true;
                    updatePublishState();
                }
                if (state == LVErrorCode.SUCCESS) {
                    mRtcEngine.enableMic(mMicEnable);
                }
//                showToast("推流成功");
            }

            @Override
            public void onPublishQualityUpdate(LVVideoStatistic stats) {
                if (stats == null) {
                    return;
                }
                runOnUiThread(() -> {
                    DisplayContainer container = mDisplayLayoutMap.get(mUid);
                    if (container != null) {
                        updateDisplayFps(container, stats.videoFps);
                        updateDisplayQuality(stats, mUid);
                        updateDisplayResolution(container, stats.frameWidth, stats.frameHeight);
                    }
                });
            }

            @Override
            public void onPlayStateUpdate(int state, String userId) {
                log("onPlayStateUpdate  uid:" + userId + ", state: " + state);
            }

            @Override
            public void onAudioMixStream(ByteBuffer audioBuffer, int samples, int nChannel, int sampleSperSec,
                                         int sampleType, LVConstants.AudioRecordType type) {
            }

            @Override
            public void onAudioVolumeUpdate(ArrayList<LVAudioVolume> volumes) {
            }

            @Override
            public void onExitRoomComplete() {
                log("onExitRoomComplete ");
            }

            @Override
            public String onMediaSideInfoInPublishVideoFrame() {
                return mSeiEnable ? Long.toString(System.currentTimeMillis()) : null;
            }

            @Override
            public void onPlayQualityUpdate(final LVVideoStatistic stats, final String userId) {
                if (stats == null) {
                    return;
                }
//                Log.i(TAG, "onPlayQualityUpdate, userId: " + userId + ", stats: " + stats.toString());
                runOnUiThread(() -> {
                    DisplayContainer container = mDisplayLayoutMap.get(userId);
                    if (container != null) {
                        updateDisplayFps(container, stats.videoFps);
                        updateDisplayQuality(stats, userId);
                        updateDisplayResolution(container, stats.frameWidth, stats.frameHeight);
                    }
                });
            }

            @Override
            public void onError(int code) {

            }
        });
    }

    private void removeQuality(String userId) {
        if (mPresenterCallback != null) {
            mPresenterCallback.updateDisplayQuality(null, userId);
        }
    }

    private void updateDisplayQuality(LVVideoStatistic stats, String uid) {
        if (mPresenterCallback != null) {
            mPresenterCallback.updateDisplayQuality(stats, uid);
        }
    }

    private void updateDisplayResolution(DisplayContainer container, int width, int height) {
//        log("updateDisplayResolution width: " + width + ", height: " + width);
        TextView resolutionView = container.getResolutionView();
        if (resolutionView != null) {
            resolutionView.setText(width + "x" + height);
        }
        TextView fpsView = container.getFpsView();
        View backslash = container.getBackslash();
        if (backslash != null && fpsView != null && fpsView.getVisibility() == View.VISIBLE
                && !TextUtils.isEmpty(fpsView.getText().toString())) {
            backslash.setVisibility(View.VISIBLE);
        } else if (backslash != null) {
            backslash.setVisibility(View.GONE);
        }
    }

    private void updateDisplayFps(DisplayContainer container, int fps) {
//        log("updateDisplayFps: " + fps);
        TextView fpsView = container.getFpsView();
        if (fpsView != null) {
            fpsView.setText(fps + "");
        }
        TextView resolutionView = container.getResolutionView();
        View backslash = container.getBackslash();
        if (backslash != null && resolutionView != null && resolutionView.getVisibility() == View.VISIBLE
                && !TextUtils.isEmpty(resolutionView.getText().toString())) {
            backslash.setVisibility(View.VISIBLE);
        } else if (backslash != null) {
            backslash.setVisibility(View.GONE);
        }
    }

    private void setupRemoteVideoDisplayView(String userId, int width, int height) {
        DisplayContainer container = getRemoteDisplayLayout(userId.startsWith(HEAD_UID_HOST));
        if (container == null || container.getLayout() == null) {
            return;
        }
        setupDisplayContainer(container, userId);

        if (!isAudioLive()) {
            LVDisplayView remoteDisplayView = new LVDisplayView();
            remoteDisplayView.setUid(userId).setLayoutContainer(container.getLayout());
            //控制层级，全屏布局放下面，其他布局都放上面
            if (mIsHost) {
                remoteDisplayView.isZOrderMediaOverlay(true);
            } else {
                // 如果是主播，放底层大屏渲染
                if (userId.startsWith(HEAD_UID_HOST)) {
                    remoteDisplayView.isZOrderMediaOverlay(false);
                } else {
                    remoteDisplayView.isZOrderMediaOverlay(true);
                }
            }

            mRtcEngine.addDisplayView(mContext, remoteDisplayView);
            mRemoteDisplayMap.put(userId, new RemoteDisplayView()
                    .setCMDisplayView(remoteDisplayView)
                    .setWidth(width)
                    .setHeight(height));
        }
    }

    private void setupAudioDisplayView(String uid) {
        DisplayContainer container = getRemoteDisplayLayout(false);
        if (container == null || container.getLayout() == null) {
            return;
        }
        setupDisplayContainer(container, uid);
    }

    private void setupDisplayContainer(DisplayContainer container, String uid) {
        container.setUid(uid);
        View headImage = container.getHeadImage();
        if (isAudioLive() && headImage != null) {
            headImage.setVisibility(View.VISIBLE);
        }
        View talkImage = container.getTalkIcon();
        if (container.getCloseView() != null) {
            container.getCloseView().setVisibility(View.VISIBLE);
            container.getCloseView().setOnClickListener(v -> {
                stopPlayStream(container);
                if (mPresenterCallback != null) {
                    mPresenterCallback.removeDisplayLayout(container);
                }
                mDisplayLayoutMap.remove(uid);

                if (container.getResolutionView() != null) {
                    container.getResolutionView().setText("");
                }
                container.getCloseView().setVisibility(View.GONE);
                if (headImage != null) {
                    headImage.setVisibility(View.GONE);
                }
                if (talkImage != null) {
                    talkImage.setVisibility(View.GONE);
                }
                View backslash = container.getBackslash();
                if (backslash != null) {
                    backslash.setVisibility(View.GONE);
                }
            });
        }

        mDisplayLayoutMap.put(uid, container);
    }

    //添加远端布局
    private void addRemoteDisplayView(String uid) {
        log("addRemoteDisplayView  userId: " + uid);
        if (!mRemoteDisplayMap.containsKey(uid)) {
            mRemoteDisplayMap.put(uid, null);
            // 音频布局在这里添加，视频的布局放在视频帧到来之后添加，避免黑屏
            if (isAudioLive()) {
                setupAudioDisplayView(uid);
            }
            if (mPresenterCallback != null) {
                mPresenterCallback.onAddMemberList(uid);
            }
        }
    }

    // 移除远端布局
    private void removeRemoteDisplayView(String uid) {
        mRtcEngine.removeDisplayView(uid);
        DisplayContainer container = mDisplayLayoutMap.get(uid);
        if (container != null && mPresenterCallback != null) {
            mPresenterCallback.removeDisplayLayout(container);
            mDisplayLayoutMap.remove(uid);
        }
        mRemoteDisplayMap.remove(uid);
        if (mPresenterCallback != null) {
            mPresenterCallback.onRemoveMemberList(uid);
        }
    }

    private void showToast(String msg) {
        if (mPresenterCallback != null) {
            if (Thread.currentThread() instanceof MainThread) {
                mPresenterCallback.showToast(msg);
            } else {
                runOnUiThread(() -> {
                    if (mPresenterCallback != null) {
                        mPresenterCallback.showToast(msg);
                    }
                });
            }
        }
    }

    private void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }

    public void onResume() {
        if (mIsCaptureStated) {
            // 避免长时间切后台后，surface被释放，重新打开后画面黑屏
            if (addLocalDisplayView()) {
                startCapture();
            }
        }
    }

    public void onStop() {
        if (mIsCaptureStated) {
            // 避免长时间切后台后，surface被释放，重新打开后画面黑屏
            stopCapture(true);
        }
    }

    public void startBeam() {
        if (mPresenterCallback == null) {
            return;
        }
        if (!initResult) {
            mPresenterCallback.setBeamButtonClickable(true);
            return;
        }
        if ((mDisplayLayoutMap.size() >= mPresenterCallback.getLayoutLimit())) {
            mPresenterCallback.showToast(mContext.getString(R.string.member_limited));
            mPresenterCallback.setBeamButtonClickable(true);
            return;
        }
        if (!isAudioLive()) {
            if (addLocalDisplayView() && !isScreenSharingLive()) {
                startCapture();
            }
        } else {
            setupAudioDisplayView(mUid);
        }
        changeOutputResolution(mVideoResolutionLevel);
        startPublishStream();
    }

    private void startPublishStream() {
        if (mPresenterCallback != null) {
            mRtcEngine.startPublishing();
            mPresenterCallback.onAddMemberList(mUid);
        }
    }

    private void stopPublishStream() {
        if (mPresenterCallback != null) {
            mRtcEngine.stopPublishing();
            mPresenterCallback.onRemoveMemberList(mUid);
            removeQuality(mUid);
        }
    }

    private void stopPlayStream(String uid) {
        mRtcEngine.stopPlayingStream(uid);
        removeQuality(uid);
    }

    // 停止推流或拉流，移除布局
    private void stopPlayStream(DisplayContainer container) {
        String uid = container.getUid();
        container.setUid("");
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        if (!TextUtils.isEmpty(mUid) && mUid.equalsIgnoreCase(uid)) {
            if (mIsHost && mPresenterCallback != null) {
                mPresenterCallback.finishAct();
            } else {
                if (!isAudioLive()) {
                    mRtcEngine.stopCapture();
                    mIsCaptureStated = false;
                }
                stopPublishStream();
                if (mPresenterCallback != null) {
                    mPresenterCallback.setBeamButtonClickable(true);
                }
            }

        } else {
            stopPlayStream(uid);
        }
        mRtcEngine.removeDisplayView(uid);
        mRemoteDisplayMap.remove(uid);
    }

    private boolean addLocalDisplayView() {
        if (TextUtils.isEmpty(mUid)) {
            return false;
        }

        DisplayContainer container = getLocalDisplayLayout();
        if (container == null || container.getLayout() == null) {
            return false;
        }
        setupDisplayContainer(container, mUid);

        if (mAVConfig == null) {
            changeOutputResolution(mVideoResolutionLevel);
        }

        mLocalDisplayView = new LVDisplayView()
                .setUid(mUid)
                .isLocalPreviewView(true)
                .setLayoutContainer(container.getLayout())
                .isZOrderMediaOverlay(!mIsHost); //控制层级，全屏布局放下面，其他布局都放上面

        mRtcEngine.addDisplayView(mContext, mLocalDisplayView);

        if (mOutputRotation != null) {
            mRtcEngine.setOutputVideoRotation(mOutputRotation);
        }
        return true;
    }

    private void startCapture() {
        mRtcEngine.startCapture();
        mIsCaptureStated = true;
    }

    // 根据远端人数多少来确定使用什么样的布局
    private DisplayContainer getLocalDisplayLayout() {
        if (mPresenterCallback == null) {
            return null;
        }
        return mPresenterCallback.getLocalDisplayLayout(mDisplayLayoutMap.size());
    }

    private DisplayContainer getRemoteDisplayLayout(boolean isHost) {
        if (mPresenterCallback == null) {
            return null;
        }
        return mPresenterCallback.getRemoteDisplayLayout(mDisplayLayoutMap.size(), isHost);
    }

    private void stopCapture(boolean autoStop) {
        mRtcEngine.stopCapture();
        if (mLocalDisplayView != null) {
            mRtcEngine.removeDisplayView(mLocalDisplayView);
            if (mPresenterCallback != null) {
                mPresenterCallback.removeDisplayLayout(mDisplayLayoutMap.get(mUid));
            }
            ViewGroup layout = mLocalDisplayView.getLayoutContainer();
            if (layout != null) {
                mDisplayLayoutMap.remove(mUid);
            }
        }
        if (!autoStop) {
            mIsCaptureStated = false;
        }
    }

    private void logout() {
        mRtcEngine.logoutRoom(code -> log("logoutRoom  code: " + code));
        if (mIsHost) {
            mIsPublished = false;
            NetManager.updateRoomState(Constants.AppId, mRoomId, "3");
        }
    }

    private void log(String text) {
        Log.d(TAG, text);
    }

    private void updatePublishState() {
        if (!mIsPublished) {
            return;
        }
        NetManager.updateRoomState(Constants.AppId, mRoomId, "2");
        mHandler.postDelayed(() -> updatePublishState(), 5000);
    }

    public void finish() {
        mRtcEngine.getLVBeautyManager().setBeautyLevel(-1);
        mRtcEngine.getLVBeautyManager().setBrightLevel(-1);
        mRtcEngine.getLVBeautyManager().setToneLevel(-1);

        stopCapture(false);
        stopScreenRecord();
        if (mAudioRecorder != null) {
            mAudioRecorder.stopRecording();
            mAudioRecorder = null;
        }
        removeDisplayViews();
        mRtcEngine.enableVideoAutoRotation(true);
        stopPublishStream();
        mOutputRotation = LVConstants.LVVideoRotation.ROTATION_0;
        logout();
        resetData();
        mRemoteDisplayMap.clear();
        mDisplayLayoutMap.clear();
        mPresenterCallback = null;
    }

    private void removeDisplayViews() {
        if (mRemoteDisplayMap != null && !mRemoteDisplayMap.isEmpty() && mRtcEngine != null) {
            for (String uid : mRemoteDisplayMap.keySet()) {
                mRtcEngine.removeDisplayView(uid);
            }
        }
    }

    private void resetData() {
        mMicEnable = true;
        mSeiEnable = false;
        mIsFrontCamera = true;
        mIsCaptureStated = false;
        mLiveType = -1;
        mUid = "";
        mRoomId = "";
        mVideoResolutionLevel = LVAVConfig.PROFILE_720P;
        mOutputRotation = LVConstants.LVVideoRotation.ROTATION_0;
        mRtcEngine.enableVideoAutoRotation(true);
        mRtcEngine.setOutputVideoRotation(mOutputRotation);
    }

    public void startLive() {
        startLogin();
    }

    public void switchCamera() {
        mIsFrontCamera = !mIsFrontCamera;
        if (mRtcEngine != null) {
            mRtcEngine.switchCamera(mIsFrontCamera ? LVConstants.LVRTCCameraPosition.FRONT : LVConstants.LVRTCCameraPosition.BACK);
        }
    }

    public void enableMic() {
        mMicEnable = !mMicEnable;
        if (mRtcEngine != null) {
            mRtcEngine.enableMic(mMicEnable);
        }
        if (mPresenterCallback != null) {
            mPresenterCallback.updateMic(mMicEnable);
        }
    }

    // 分辨率自适应开关
    public void enableAutoResolution(boolean enable) {
        if (mAVConfig == null || mRtcEngine == null) {
            return;
        }
        if (enable) {
            mAVConfig.setVideoDegradationPreference(LVAVConfig.MAINTAIN_FRAMERATE);
        } else {
            mAVConfig.setVideoDegradationPreference(LVAVConfig.MAINTAIN_RESOLUTION);
        }
        mRtcEngine.setAVConfig(mAVConfig);
    }

    public void setDegradationPreference(int degradationPreference) {
        Log.i(TAG, "setDegradationPreference: " + degradationPreference);
        if (mAVConfig == null || mRtcEngine == null) {
            return;
        }
        mAVConfig.setVideoDegradationPreference(degradationPreference);
        mRtcEngine.setAVConfig(mAVConfig);
    }

    // 伴奏开关
    public int enableAudioMixing(boolean enable) {
        if (enable) {
            return mRtcEngine.startAudioMixing(Uri.fromFile(new File("sdcard/audio/audio1.mp3")),
                    LVConstants.LVAudioMixingMode.SEND_AND_PLAYOUT, 1);
        } else {
            return mRtcEngine.stopAudioMixing();
        }
    }

    public void setAudioMixingMode(LVConstants.LVAudioMixingMode mode) {
        mRtcEngine.setAudioMixingMode(mode);
    }

    public void adjustAudioMixingVolume(int volume) {
        mRtcEngine.adjustAudioMixingVolume(volume);
    }

    public void pauseAudioMixing(boolean pause) {
        if (pause) {
            mRtcEngine.pauseAudioMixing();
        } else {
            mRtcEngine.resumeAudioMixing();
        }
    }

    // 外部输入音频数据开关
    public void enableExternalAudioInput(boolean enable) {
        if (enable) {
            mRtcEngine.enableExternalAudioInput(true);
            mRtcEngine.setExternalAudioConfig(new LVExternalAudioConfig(48000, 1));

            mAudioRecorder = new AudioRecorder();
            mAudioRecorder.setOnAudioSamplesReady(samples -> mRtcEngine.sendAudioFrame(samples.getData()));
            mAudioRecorder.initRecording(48000, 1);
            mAudioRecorder.startRecording();
        } else {
            mRtcEngine.enableExternalAudioInput(false);
            if (mAudioRecorder != null) {
                mAudioRecorder.stopRecording();
                mAudioRecorder = null;
            }
        }
    }

    public void enableVideoSei(boolean enable) {
        mSeiEnable = enable;
    }

    public LVAVConfig getAVConfig() {
        if (mAVConfig == null) {
            changeOutputResolution(mVideoResolutionLevel);
        }
        return mAVConfig;
    }

    public List<LVAVConfig> getVideoConfigLevels() {
        if (mVideoConfigLevels == null) {
            mVideoConfigLevels = new ArrayList<>();
            mVideoConfigLevels.add(new LVAVConfig(LVAVConfig.PROFILE_180P));
            mVideoConfigLevels.add(new LVAVConfig(LVAVConfig.PROFILE_270P));
            mVideoConfigLevels.add(new LVAVConfig(LVAVConfig.PROFILE_360P));
            mVideoConfigLevels.add(new LVAVConfig(LVAVConfig.PROFILE_480P));
            mVideoConfigLevels.add(new LVAVConfig(LVAVConfig.PROFILE_540P));
            mVideoConfigLevels.add(new LVAVConfig(LVAVConfig.PROFILE_720P));
        }
        return mVideoConfigLevels;
    }

    public void changeVideoConfig(int configProfile, int fps, int bitrate) {
        mVideoResolutionLevel = configProfile;
        mAVConfig = new LVAVConfig(mVideoResolutionLevel);
        mAVConfig.setVideoDegradationPreference(LVAVConfig.DISABLED);
        if (fps > 0) {
            mAVConfig.setVideoFrameRate(fps);
        }
        if (bitrate > 0) {
            mAVConfig.setVideoTargetBitrate(bitrate);
        }
        if (mRtcEngine != null) {
            mRtcEngine.setAVConfig(mAVConfig);
        }
    }

    public int getAVConfigLevel() {
        return mVideoResolutionLevel;
    }

    public void enableSpeakerPhone(boolean enable) {
        AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if (enable && !mAudioManager.isWiredHeadsetOn()) {
            mRtcEngine.enableSpeakerphone(true);
        } else {
            mRtcEngine.enableSpeakerphone(false);
        }
    }

    public String getMUid() {
        return mUid;
    }

    //重新拉流
    public void rePlayStream(String uid) {
        if (!mRemoteDisplayMap.containsKey(uid) && !uid.equalsIgnoreCase(mUid)) {
            mRtcEngine.startPlayingStream(uid);
            runOnUiThread(() -> {
                if (!mRemoteDisplayMap.containsKey(uid)) {
                    addRemoteDisplayView(uid);
                }
            });
        }
    }

    // 设置滤镜美颜级别
    public void changeBeautyLevel(float beautyLevel) {
        mRtcEngine.getLVBeautyManager().setBeautyLevel(beautyLevel);
    }

    // 设置滤镜明亮度
    public void changeBeautyBrightLevel(float brightLevel) {
        mRtcEngine.getLVBeautyManager().setBrightLevel(brightLevel);
    }

    // 设置滤镜饱和度
    public void changeBeautyToneLevel(float toneLevel) {
        mRtcEngine.getLVBeautyManager().setToneLevel(toneLevel);
    }

    public void startScreenSharing(Activity activity, int resultCode, Intent resultData) {
        initScreenRecorder(activity);
        mScreenRecorder.startRecord(activity, resultCode, resultData);
    }

    private void initScreenRecorder(Activity activity) {
        stopScreenRecord();
        mScreenRecorder = new ScreenRecorder(DimenUtils.getScreenWidth(activity), DimenUtils.getScreenHeight(activity));
        mScreenRecorder.setScreenRecorderCallback(new ScreenRecorder.ScreenRecorderCallback() {
            @Override
            public void onDataUpdate(Frame frame) {
                if (mRtcEngine != null) {
                    I420Frame i420Frame = (I420Frame) frame;
                    if (i420Frame.data() == null) {
                        return;
                    }
                    mRtcEngine.sendVideoFrame(i420Frame.data(), i420Frame.width(), i420Frame.height(),
                            i420Frame.rotationDegrees(), i420Frame.format(), i420Frame.timeStamp(),
                            mSeiEnable ? Long.toString(System.currentTimeMillis()) : "");
                }
            }

            @Override
            public void onStopped() {
                stopScreenRecord();
            }

            @Override
            public void onRecordFailed() {

            }

            @Override
            public void updateVideoSize(int width, int height) {
                if (mRtcEngine != null && mAVConfig != null) {
                    mAVConfig.setVideoEncodeWidth(width);
                    mAVConfig.setVideoEncodeHeight(height);
                    mRtcEngine.setAVConfig(mAVConfig);
                }
            }
        });
    }

    private void stopScreenRecord() {
        if (mScreenRecorder != null) {
            mScreenRecorder.stopRecord();
            mScreenRecorder = null;
        }
    }
}
