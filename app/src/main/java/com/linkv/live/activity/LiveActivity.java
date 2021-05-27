package com.linkv.live.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.linkv.live.LivePresenter;
import com.linkv.live.LivePresenterCallback;
import com.linkv.live.R;
import com.linkv.live.adapter.MemberListAdapter;
import com.linkv.live.adapter.QualityAdapter;
import com.linkv.live.adapter.SettingAdapter;
import com.linkv.live.utils.DimenUtils;
import com.linkv.live.utils.SPUtils;
import com.linkv.live.view.DisplayContainer;
import com.linkv.live.view.NineItemLayout;
import com.linkv.rtc.LVConstants;
import com.linkv.rtc.LVErrorCode;
import com.linkv.rtc.entity.LVAVConfig;
import com.linkv.rtc.entity.LVVideoStatistic;

import java.util.ArrayList;
import java.util.List;

public class LiveActivity extends Activity implements View.OnClickListener, LivePresenterCallback, SettingAdapter.SettingAdapterCallback, SeekBar.OnSeekBarChangeListener {

    public static final String KEY_ENTER_FROM = "enter_from";
    public static final String KEY_ENTER_ROOM_ID = "enter_room_id";
    public static final String KEY_LIVE_TYPE = "liveType";
    public static final int FROM_LIVE = 1;
    public static final int FROM_WATCH = 2;
    public static final int COMMON_LIVE = 1;
    public static final int MEETING_LIVE = 2;
    public static final int AUDIO_LIVE = 3;
    public static final int SCREEN_SHARING_LIVE = 4;
    public static final int SCREEN_PERMISSION_CODE = 101;
    private static final String TAG = "LiveActivityLog";
    private static final int BITRATE_SCALER = 50;
    private final Object mLayoutLock = new Object();
    private int mLiveType;
    private boolean mIsHost;
    private String mRoomId;
    private TextView tv_room_id;
    private TextView tv_user_id;
    private FrameLayout fl_setting;
    private ImageView iv_mic;
    private View vs_change_video_config;
    private View vs_output_rotation;
    private View vs_beauty;
    private View vs_video_degradation;
    private View vs_audio_mixing_mode;
    private View ll_audio_mixing;
    private TextView tv_pause;
    private TextView tv_video_resolution;
    private TextView tv_video_bitrate;
    private TextView tv_video_fps;
    private TextView tv_start_live;
    private View fl_member_list;
    private ListView lv_quality;
    private Toast mToast;
    private int mLastConfigProfile;
    private LivePresenter mLivePresenter;
    private List<DisplayContainer> mLayoutList;
    private SeekBar sk_video_resolution;
    private SeekBar sk_video_bitrate;
    private SeekBar sk_video_fps;
    private ImageView iv_beam;
    private MemberListAdapter mMemberListAdapter;
    private QualityAdapter mQualityAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getIntentData();
        initPresenter();
        initView();
    }

    private void initPresenter() {
        mLivePresenter = LivePresenter.getInstance(this);
        mLivePresenter.resetContext(this);
        mLivePresenter.setPresenterCallback(this);
        mLivePresenter.initData(mIsHost, mRoomId, mLiveType);
        mLivePresenter.initEngine(true);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        int enterFrom = intent.getIntExtra(KEY_ENTER_FROM, 0);

        if (enterFrom == FROM_LIVE) {
            mIsHost = true;
            mLiveType = intent.getIntExtra(KEY_LIVE_TYPE, 0);
        } else if (enterFrom == FROM_WATCH) {
            mRoomId = intent.getStringExtra(KEY_ENTER_ROOM_ID);
            if (TextUtils.isEmpty(mRoomId)) {
                finish(); // 还没有开始其他初始化，直接调finish()退出即可.
                return;
            }
            if (mRoomId.startsWith(LivePresenter.HEAD_ROOM_LIVE)) {
                mLiveType = COMMON_LIVE;
            } else if (mRoomId.startsWith(LivePresenter.HEAD_ROOM_MEETING)) {
                mLiveType = MEETING_LIVE;
            } else if (mRoomId.startsWith(LivePresenter.HEAD_ROOM_AUDIO)) {
                mLiveType = AUDIO_LIVE;
            }
        }

        if (!TextUtils.isEmpty(SPUtils.getRoomId())) {
            mRoomId = SPUtils.getRoomId();
        }

    }

    private void initView() {
        ImageView iv_back = findViewById(R.id.iv_back);
        ImageView iv_member = findViewById(R.id.iv_member);
        iv_beam = findViewById(R.id.iv_beam);
        ImageView iv_beauty = findViewById(R.id.iv_beauty);
        ImageView iv_change_camera = findViewById(R.id.iv_change_camera);
        iv_mic = findViewById(R.id.iv_mic);
        ImageView iv_setting = findViewById(R.id.iv_setting);
        tv_room_id = findViewById(R.id.tv_room_id);
        tv_user_id = findViewById(R.id.tv_user_id);
        tv_start_live = findViewById(R.id.tv_start_live);

        View view_beauty = findViewById(R.id.view_beauty);
        fl_setting = findViewById(R.id.fl_setting);

        ll_audio_mixing = findViewById(R.id.ll_audio_mixing);
        SeekBar seek_audio_mixing = findViewById(R.id.seek_audio_mixing);
        tv_pause = findViewById(R.id.tv_pause);

        ListView lv_setting = findViewById(R.id.lv_setting);
        lv_setting.setAdapter(new SettingAdapter(this, this));

        lv_quality = findViewById(R.id.lv_quality);
        mQualityAdapter = new QualityAdapter(this);
        lv_quality.setAdapter(mQualityAdapter);

        ListView lv_member_list = findViewById(R.id.lv_member_list);
        fl_member_list = findViewById(R.id.fl_member_list);
        fl_member_list.setVisibility(View.GONE);
        mMemberListAdapter = new MemberListAdapter(this, mLivePresenter.getMUid());
        lv_member_list.setAdapter(mMemberListAdapter);

        fl_member_list.setOnTouchListener((v, event) -> {
            fl_member_list.setVisibility(View.GONE);
            return true;
        });
        lv_member_list.setOnItemClickListener((parent, view, position, id) -> {
            String uid = mMemberListAdapter.getItem(position);
            if (!TextUtils.isEmpty(uid) && mLivePresenter != null) {
                mLivePresenter.rePlayStream(uid);
            }
            fl_member_list.setVisibility(View.GONE);
        });

        iv_back.setOnClickListener(this);
        iv_member.setOnClickListener(this);
        iv_beam.setOnClickListener(this);
        iv_beauty.setOnClickListener(this);
        iv_change_camera.setOnClickListener(this);
        iv_mic.setOnClickListener(this);
        iv_setting.setOnClickListener(this);
        tv_start_live.setOnClickListener(this);
        tv_pause.setOnClickListener(this);
        seek_audio_mixing.setOnSeekBarChangeListener(this);

        fl_setting.setOnTouchListener((v, event) -> {
            fl_setting.setVisibility(View.GONE);
            return true;
        });

        if (!mIsHost && !TextUtils.isEmpty(mRoomId)) {
            updateRoomId(mRoomId);
        }
        if (!mIsHost) {
            tv_start_live.setVisibility(View.GONE);
        } else {
            iv_beam.setVisibility(View.GONE);
            view_beauty.setVisibility(View.GONE);
        }

        setUpDisplayLayout();
    }

    private void setUpDisplayLayout() {
        mLayoutList = new ArrayList<>();
        if (isCommonLive() || isScreenSharingLive()) {
            setCommonLiveLayout();
        } else {
            setNineLiveLayout();
        }
    }

    public boolean isCommonLive() {
        return mLiveType == COMMON_LIVE;
    }

    public boolean isScreenSharingLive() {
        return mLiveType == SCREEN_SHARING_LIVE;
    }

    private void setCommonLiveLayout() {
        ImageView iv_three_display_right_close = findViewById(R.id.iv_three_display_right_close);
        ImageView iv_three_display_middle_close = findViewById(R.id.iv_three_display_middle_close);
        ImageView iv_three_display_left_close = findViewById(R.id.iv_three_display_left_close);
        TextView tv_full_resolution = findViewById(R.id.tv_full_resolution);
        TextView tv_fps = findViewById(R.id.tv_fps);
        TextView tv_three_display_right_resolution = findViewById(R.id.tv_three_display_right_resolution);
        TextView tv_three_display_right_fps = findViewById(R.id.tv_three_display_right_fps);
        TextView tv_three_display_middle_resolution = findViewById(R.id.tv_three_display_middle_resolution);
        TextView tv_three_display_middle_fps = findViewById(R.id.tv_three_display_middle_fps);
        TextView tv_three_display_left_resolution = findViewById(R.id.tv_three_display_left_resolution);
        TextView tv_three_display_left_fps = findViewById(R.id.tv_three_display_left_fps);
        TextView tv_full_backslash = findViewById(R.id.tv_full_backslash);
        TextView tv_three_display_right_backslash = findViewById(R.id.tv_three_display_right_backslash);
        TextView tv_three_display_middle_backslash = findViewById(R.id.tv_three_display_middle_backslash);
        TextView tv_three_display_left_backslash = findViewById(R.id.tv_three_display_left_backslash);
        FrameLayout fl_three_right = findViewById(R.id.fl_three_right);
        FrameLayout fl_three_middle = findViewById(R.id.fl_three_middle);
        FrameLayout fl_three_left = findViewById(R.id.fl_three_left);
        FrameLayout fl_three_display_right = findViewById(R.id.fl_three_display_right);
        FrameLayout fl_three_display_middle = findViewById(R.id.fl_three_display_middle);
        FrameLayout fl_three_display_left = findViewById(R.id.fl_three_display_left);
        FrameLayout fl_full_screen = findViewById(R.id.fl_full_screen);
        fl_full_screen.setVisibility(View.VISIBLE);

        int itemWidth = DimenUtils.getScreenWidth(this) / 3;
        int height = (int) (itemWidth * 16.0f / 9);
        ViewGroup.LayoutParams fl_three_right_params = fl_three_right.getLayoutParams();
        ViewGroup.LayoutParams fl_three_middle_params = fl_three_middle.getLayoutParams();
        ViewGroup.LayoutParams fl_three_left_params = fl_three_left.getLayoutParams();
        fl_three_right_params.width = itemWidth;
        fl_three_right_params.height = height;
        fl_three_middle_params.width = itemWidth;
        fl_three_middle_params.height = height;
        fl_three_left_params.width = itemWidth;
        fl_three_left_params.height = height;
        fl_three_right.setLayoutParams(fl_three_right_params);
        fl_three_middle.setLayoutParams(fl_three_middle_params);
        fl_three_left.setLayoutParams(fl_three_left_params);

        mLayoutList.add(new DisplayContainer().setLayout(fl_full_screen).
                setResolutionView(tv_full_resolution).setFpsView(tv_fps).setBackslash(tv_full_backslash));
        mLayoutList.add(new DisplayContainer().setLayout(fl_three_display_right)
                .setResolutionView(tv_three_display_right_resolution)
                .setFpsView(tv_three_display_right_fps)
                .setCloseView(iv_three_display_right_close)
                .setBackslash(tv_three_display_right_backslash));
        mLayoutList.add(new DisplayContainer().setLayout(fl_three_display_middle)
                .setResolutionView(tv_three_display_middle_resolution)
                .setFpsView(tv_three_display_middle_fps)
                .setCloseView(iv_three_display_middle_close)
                .setBackslash(tv_three_display_middle_backslash));
        mLayoutList.add(new DisplayContainer().setLayout(fl_three_display_left)
                .setResolutionView(tv_three_display_left_resolution)
                .setFpsView(tv_three_display_left_fps)
                .setCloseView(iv_three_display_left_close)
                .setBackslash(tv_three_display_left_backslash));
    }

    private void setNineLiveLayout() {
        LinearLayout ll_nine_layout = findViewById(R.id.ll_nine_layout);
        ll_nine_layout.setVisibility(View.VISIBLE);
        LinearLayout ll_nine_layout_first = findViewById(R.id.ll_nine_layout_first);
        LinearLayout ll_nine_layout_second = findViewById(R.id.ll_nine_layout_second);
        LinearLayout ll_nine_layout_third = findViewById(R.id.ll_nine_layout_third);
        int itemWidth = DimenUtils.getScreenWidth(this) / 3;
        for (int i = 0; i < 9; i++) {
            NineItemLayout itemLayout = new NineItemLayout(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(itemWidth, itemWidth);
            itemLayout.setLayoutParams(layoutParams);
            if (i < 3) {
                ll_nine_layout_first.addView(itemLayout);
            } else if (i < 6) {
                ll_nine_layout_second.addView(itemLayout);
            } else {
                ll_nine_layout_third.addView(itemLayout);
            }

            mLayoutList.add(new DisplayContainer().setLayout(itemLayout.getSurfaceContainer())
                    .setCloseView(itemLayout.getCloseIcon())
                    .setHeadImage(itemLayout.getHeadIcon())
                    .setTalkIcon(itemLayout.getTalkIcon())
                    .setResolutionView(itemLayout.getResolutionView())
                    .setFpsView(itemLayout.getFpsView())
                    .setBackslash(itemLayout.getBackslash()));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLivePresenter != null) {
            mLivePresenter.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLivePresenter != null) {
            mLivePresenter.onStop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == SCREEN_PERMISSION_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showToast("录屏权限被拒绝");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCREEN_PERMISSION_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    mLivePresenter.startScreenSharing(this, resultCode, data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                showToast("录屏权限被拒绝");
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                finishAct();
                break;
            case R.id.iv_member:
                showMemberList();
                break;
            case R.id.tv_start_live:
                if (mLivePresenter != null) {
                    mLivePresenter.startLive();
                }
                break;
            case R.id.iv_beam:
                if (mLivePresenter != null) {
                    setBeamButtonClickable(false);
                    mLivePresenter.startBeam();
                }
                break;
            case R.id.iv_beauty:
                showBeautyLayout();
                break;
            case R.id.iv_change_camera:
                if (mLivePresenter != null) {
                    mLivePresenter.switchCamera();
                }
                break;
            case R.id.iv_mic:
                if (mLivePresenter != null) {
                    mLivePresenter.enableMic();
                }
                break;
            case R.id.iv_setting:
                fl_setting.setVisibility(View.VISIBLE);
                break;
            case R.id.tv_720p:
                changeOutputResolution(LVAVConfig.PROFILE_720P);
                break;
            case R.id.tv_540p:
                changeOutputResolution(LVAVConfig.PROFILE_540P);
                break;
            case R.id.tv_360p:
                changeOutputResolution(LVAVConfig.PROFILE_360P);
                break;
            case R.id.tv_degree0:
                changeOutputRotation(LVConstants.LVVideoRotation.ROTATION_0);
                break;
            case R.id.tv_degree90:
                changeOutputRotation(LVConstants.LVVideoRotation.ROTATION_90);
                break;
            case R.id.tv_degree180:
                changeOutputRotation(LVConstants.LVVideoRotation.ROTATION_180);
                break;
            case R.id.tv_degree270:
                changeOutputRotation(LVConstants.LVVideoRotation.ROTATION_270);
                break;
            case R.id.tv_pause:
                boolean pause = getString(R.string.volume_control_pause).contentEquals(tv_pause.getText());
                if (mLivePresenter != null) {
                    mLivePresenter.pauseAudioMixing(pause);
                }
                if (pause) {
                    tv_pause.setText(R.string.volume_control_play);
                } else {
                    tv_pause.setText(R.string.volume_control_pause);
                }
                break;
            case R.id.tv_send_only:
                vs_audio_mixing_mode.setVisibility(View.GONE);
                if (mLivePresenter != null) {
                    mLivePresenter.setAudioMixingMode(LVConstants.LVAudioMixingMode.SEND_ONLY);
                }
                break;
            case R.id.tv_playout_only:
                vs_audio_mixing_mode.setVisibility(View.GONE);
                if (mLivePresenter != null) {
                    mLivePresenter.setAudioMixingMode(LVConstants.LVAudioMixingMode.PLAYOUT_ONLY);
                }
                break;
            case R.id.tv_send_and_playout:
                vs_audio_mixing_mode.setVisibility(View.GONE);
                if (mLivePresenter != null) {
                    mLivePresenter.setAudioMixingMode(LVConstants.LVAudioMixingMode.SEND_AND_PLAYOUT);
                }
                break;
            case R.id.tv_replace_mic:
                vs_audio_mixing_mode.setVisibility(View.GONE);
                if (mLivePresenter != null) {
                    mLivePresenter.setAudioMixingMode(LVConstants.LVAudioMixingMode.REPLACE_MIC);
                }
                break;
            case R.id.tv_replace_mic_and_send_only:
                vs_audio_mixing_mode.setVisibility(View.GONE);
                if (mLivePresenter != null) {
                    mLivePresenter.setAudioMixingMode(LVConstants.LVAudioMixingMode.REPLACE_MIC_AND_SEND_ONLY);
                }
                break;
        }
    }

    private void showBeautyLayout() {
        fl_setting.setVisibility(View.GONE);
        if (vs_beauty == null) {
            vs_beauty = ((ViewStub) findViewById(R.id.vs_beauty)).inflate();
            View view_touch = vs_beauty.findViewById(R.id.view_touch);
            SeekBar sk_beauty_level = vs_beauty.findViewById(R.id.sk_beauty_level);
            SeekBar sk_bright_level = vs_beauty.findViewById(R.id.sk_bright_level);
            SeekBar sk_tone_level = vs_beauty.findViewById(R.id.sk_tone_level);
            sk_beauty_level.setOnSeekBarChangeListener(this);
            sk_bright_level.setOnSeekBarChangeListener(this);
            sk_tone_level.setOnSeekBarChangeListener(this);
            sk_beauty_level.setProgress(50);
            sk_bright_level.setProgress(50);
            sk_tone_level.setProgress(50);
            view_touch.setOnTouchListener((v, event) -> {
                vs_beauty.setVisibility(View.GONE);
                return true;
            });
        } else {
            vs_beauty.setVisibility(View.VISIBLE);
        }
    }

    private void showMemberList() {
        if (mMemberListAdapter.getCount() > 0) {
            fl_member_list.setVisibility(View.VISIBLE);
        }
    }

    private void changeOutputResolution(int profile) {
        if (mLivePresenter == null) {
            return;
        }
        vs_change_video_config.setVisibility(View.GONE);
        mLivePresenter.changeOutputResolution(profile);
    }

    private void changeOutputRotation(LVConstants.LVVideoRotation rotation) {
        if (mLivePresenter == null) {
            return;
        }
        vs_output_rotation.setVisibility(View.GONE);
        mLivePresenter.changeOutputRotation(rotation);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fl_setting.getVisibility() == View.VISIBLE) {
                fl_setting.setVisibility(View.GONE);
            } else if (vs_output_rotation != null && vs_output_rotation.getVisibility() == View.VISIBLE) {
                vs_output_rotation.setVisibility(View.GONE);
                fl_setting.setVisibility(View.VISIBLE);
            } else if (vs_change_video_config != null && vs_change_video_config.getVisibility() == View.VISIBLE) {
                vs_change_video_config.setVisibility(View.GONE);
                fl_setting.setVisibility(View.VISIBLE);
            } else if (fl_member_list.getVisibility() == View.VISIBLE) {
                fl_member_list.setVisibility(View.GONE);
            } else if (vs_beauty != null && vs_beauty.getVisibility() == View.VISIBLE) {
                vs_beauty.setVisibility(View.GONE);
            } else if (vs_video_degradation != null && vs_video_degradation.getVisibility() == View.VISIBLE) {
                vs_video_degradation.setVisibility(View.GONE);
                fl_setting.setVisibility(View.VISIBLE);
            } else if (vs_audio_mixing_mode != null && vs_audio_mixing_mode.getVisibility() == View.VISIBLE) {
                vs_audio_mixing_mode.setVisibility(View.GONE);
                fl_setting.setVisibility(View.VISIBLE);
            } else {
                return super.onKeyDown(keyCode, event);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void updateUid(String uid) {
        tv_user_id.setText(getString(R.string.user_id) + uid);
    }

    @Override
    public void updateRoomId(String roomId) {
        tv_room_id.setText(roomId);
    }

    @Override
    public void updateMic(boolean enable) {
        iv_mic.setImageResource(enable ? R.mipmap.mute_close_ico : R.mipmap.mute_open_ico);
    }

    @Override
    public void showToast(String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg);
        }
        mToast.show();
    }

    @Override
    public DisplayContainer getRemoteDisplayLayout(int size, boolean isHost) {
        synchronized (mLayoutLock) {
            if (isCommonLive()) {
                if (size < getLayoutLimit()) {
                    DisplayContainer container = null;
                    int position = -1;
                    for (int i = 0; i < mLayoutList.size(); i++) {
                        if (mIsHost && i == 0) { // 如果自己是主播，把全屏布局留给本地预览
                            continue;
                        } else if (isHost && !mIsHost) { // 如果远端是主播，把全屏设置给远端主播.
                            position = 0;
                        } else if (!mIsHost) { // 如果自己不是主播，远端也不是主播，仍然把全屏留给远端主播.
                            position = i + 1;
                        } else {
                            position = i;
                        }

                        if (position >= getLayoutLimit()) {
                            return null;
                        }

                        DisplayContainer temp = mLayoutList.get(position);
                        if (temp.getIndex() == -1) {
                            container = temp;
                            break;
                        }

                    }
                    if (container != null && container.getLayout() != null) {
                        container.setIndex(position);
                        ViewParent parent = container.getLayout().getParent();
                        if (parent instanceof ViewGroup) {
                            ((ViewGroup) parent).setVisibility(View.VISIBLE);
                        }
                        return container;
                    }
                }
            } else {
                return getNineLiveLayout(size);
            }
        }

        return null;
    }

    @Override
    public DisplayContainer getLocalDisplayLayout(int size) {
        synchronized (mLayoutLock) {
            if (isCommonLive()) {
                if (size < getLayoutLimit()) {
                    DisplayContainer container = null;
                    // 如果是主播，永远使用全屏布局
                    int position = -1;
                    if (mIsHost) {
                        position = 0;
                        container = mLayoutList.get(0);
                    } else {
                        for (int i = 0; i < mLayoutList.size(); i++) {
                            DisplayContainer temp = mLayoutList.get(i);
                            int index = mLayoutList.get(i).getIndex();
                            if (index == -1) {
                                position = i;
                                container = temp;
                                break;
                            }
                        }
                    }
                    if (container != null && container.getLayout() != null) {
                        ViewParent parent = container.getLayout().getParent();
                        container.setIndex(position);
                        if (parent instanceof ViewGroup) {
                            ((ViewGroup) parent).setVisibility(View.VISIBLE);
                        }
                        return container;
                    }
                }
            } else {
                return getNineLiveLayout(size);
            }
        }
        return null;
    }

    @Override
    public int getLayoutLimit() {
        return mLiveType == 0 ? 0 : isCommonLive() || isScreenSharingLive() ? 4 : 9;
    }

    DisplayContainer getNineLiveLayout(int size) {
        synchronized (mLayoutLock) {
            if (size >= getLayoutLimit()) {
                return null;
            }
            // 如果是主播，永远使用全屏布局
            DisplayContainer container = null;
            int position = -1;
            for (int i = 0; i < mLayoutList.size(); i++) {
                DisplayContainer temp = mLayoutList.get(i);
                if (temp.getIndex() == -1) {
                    container = temp;
                    position = i;
                    break;
                }
            }
            if (container != null && container.getLayout() != null) {
                ViewParent parent = container.getLayout().getParent();
                container.setIndex(position);
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).setVisibility(View.VISIBLE);
                }
                return container;
            }

            return null;
        }
    }

    @Override
    public void removeDisplayLayout(DisplayContainer container) {
        if (container == null) {
            return;
        }
        synchronized (mLayoutLock) {
            ViewParent layout = container.getLayout();
            if (layout != null && layout.getParent() instanceof ViewGroup) {
                ((ViewGroup) layout.getParent()).setVisibility(View.GONE);
            }
            container.setIndex(-1);
        }
    }

    @Override
    public void updateLiveButton(int visible) {
        tv_start_live.setVisibility(visible);
    }

    @Override
    public void finishAct() {
        if (mLivePresenter != null) {
            mLivePresenter.finish();
            mLivePresenter = null;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLivePresenter != null) {
            mLivePresenter.finish();
        }
    }

    @Override
    public void setBeamButtonClickable(boolean clickable) {
        if (iv_beam != null) {
            iv_beam.setClickable(clickable);
        }
    }

    @Override
    public void onAddMemberList(String uid) {
        mMemberListAdapter.addMember(uid);
    }

    @Override
    public void onRemoveMemberList(String uid) {
        mMemberListAdapter.removeMember(uid);
    }

    @Override
    public void updateDisplayQuality(LVVideoStatistic stats, String uid) {
        if (mQualityAdapter != null) {
            mQualityAdapter.updateQuality(stats, uid);
        }
    }

    // 分辨率自适应开关
    @Override
    public void changeVideoDegradation() {
        fl_setting.setVisibility(View.GONE);
        if (vs_video_degradation == null) {
            vs_video_degradation = ((ViewStub) findViewById(R.id.vs_video_degradation)).inflate();
            vs_video_degradation.findViewById(R.id.tv_disabled).setOnClickListener(v -> {
                if (mLivePresenter == null) {
                    return;
                }
                vs_video_degradation.setVisibility(View.GONE);
                mLivePresenter.setDegradationPreference(LVAVConfig.DISABLED);
            });
            vs_video_degradation.findViewById(R.id.tv_maintain_framerate).setOnClickListener(v -> {
                if (mLivePresenter == null) {
                    return;
                }
                vs_video_degradation.setVisibility(View.GONE);
                mLivePresenter.setDegradationPreference(LVAVConfig.MAINTAIN_FRAMERATE);
            });
            vs_video_degradation.findViewById(R.id.tv_maintain_resolution).setOnClickListener(v -> {
                if (mLivePresenter == null) {
                    return;
                }
                vs_video_degradation.setVisibility(View.GONE);
                mLivePresenter.setDegradationPreference(LVAVConfig.MAINTAIN_RESOLUTION);
            });
            vs_video_degradation.findViewById(R.id.view_touch).setOnTouchListener((v, event) -> {
                vs_video_degradation.setVisibility(View.GONE);
                fl_setting.setVisibility(View.VISIBLE);
                return true;
            });
        } else {
            vs_video_degradation.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void changeVideoConfig() {
        if (mLivePresenter == null) {
            return;
        }
        fl_setting.setVisibility(View.GONE);
        LVAVConfig avConfig = mLivePresenter.getAVConfig();
        mLastConfigProfile = mLivePresenter.getAVConfigLevel();

        if (vs_change_video_config == null) {
            vs_change_video_config = ((ViewStub) findViewById(R.id.vs_change_video_config)).inflate();
            tv_video_resolution = vs_change_video_config.findViewById(R.id.tv_video_resolution);
            tv_video_bitrate = vs_change_video_config.findViewById(R.id.tv_video_bitrate);
            tv_video_fps = vs_change_video_config.findViewById(R.id.tv_video_fps);
            View view_touch = vs_change_video_config.findViewById(R.id.view_touch);
            View btn_config_finish = vs_change_video_config.findViewById(R.id.btn_config_finish);
            sk_video_resolution = vs_change_video_config.findViewById(R.id.sk_video_resolution);
            sk_video_bitrate = vs_change_video_config.findViewById(R.id.sk_video_bitrate);
            sk_video_fps = vs_change_video_config.findViewById(R.id.sk_video_fps);

            sk_video_bitrate.setMax(3000 / BITRATE_SCALER);
            sk_video_fps.setMax(30);

            sk_video_resolution.setOnSeekBarChangeListener(this);
            sk_video_bitrate.setOnSeekBarChangeListener(this);
            sk_video_fps.setOnSeekBarChangeListener(this);
            view_touch.setOnTouchListener((v, event) -> {
                // 如果没有点确定，把数据恢复
                tv_video_resolution.setText(avConfig.getVideoEncodeWidth() + "x" + avConfig.getVideoEncodeHeight());
                tv_video_bitrate.setText(Integer.toString(avConfig.getVideoTargetBitrate() / 1000));
                tv_video_fps.setText(Integer.toString(avConfig.getVideoFrameRate()));
                vs_change_video_config.setVisibility(View.GONE);
                fl_setting.setVisibility(View.VISIBLE);
                return true;
            });

            btn_config_finish.setOnClickListener(v -> {
                vs_change_video_config.setVisibility(View.GONE);
                if (mLivePresenter != null) {
                    mLivePresenter.changeVideoConfig(mLastConfigProfile, sk_video_fps.getProgress(), sk_video_bitrate.getProgress() * BITRATE_SCALER * 1000);
                }
            });

        } else {
            vs_change_video_config.setVisibility(View.VISIBLE);
        }
        tv_video_resolution.setText(avConfig.getVideoEncodeWidth() + "x" + avConfig.getVideoEncodeHeight());
        tv_video_bitrate.setText(Integer.toString(avConfig.getVideoTargetBitrate() / 1000));
        tv_video_fps.setText(Integer.toString(avConfig.getVideoFrameRate()));

        List<LVAVConfig> levels = mLivePresenter.getVideoConfigLevels();
        int slice = sk_video_resolution.getMax() / (levels.size() - 1);
        sk_video_resolution.setProgress(mLastConfigProfile * slice);
        sk_video_bitrate.setProgress(avConfig.getVideoTargetBitrate() / BITRATE_SCALER / 1000);
        sk_video_fps.setProgress(avConfig.getVideoFrameRate());
    }

    @Override
    public void showOutputRotation() {
        fl_setting.setVisibility(View.GONE);
        if (vs_output_rotation == null) {
            vs_output_rotation = ((ViewStub) findViewById(R.id.vs_output_rotation)).inflate();
            View view_output_rotation = vs_output_rotation.findViewById(R.id.view_output_rotation);
            View tv_degree0 = vs_output_rotation.findViewById(R.id.tv_degree0);
            View tv_degree90 = vs_output_rotation.findViewById(R.id.tv_degree90);
            View tv_degree180 = vs_output_rotation.findViewById(R.id.tv_degree180);
            View tv_degree270 = vs_output_rotation.findViewById(R.id.tv_degree270);
            tv_degree0.setOnClickListener(this);
            tv_degree90.setOnClickListener(this);
            tv_degree180.setOnClickListener(this);
            tv_degree270.setOnClickListener(this);
            view_output_rotation.setOnTouchListener((v, event) -> {
                vs_output_rotation.setVisibility(View.GONE);
                fl_setting.setVisibility(View.VISIBLE);
                return true;
            });
        } else {
            vs_output_rotation.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void changeAudioMixingMode() {
        fl_setting.setVisibility(View.GONE);
        if (vs_audio_mixing_mode == null) {
            vs_audio_mixing_mode = ((ViewStub) findViewById(R.id.vs_audio_mixing_mode)).inflate();
            vs_audio_mixing_mode.findViewById(R.id.view_audio_mixing_mode).setOnTouchListener((v, event) -> {
                vs_audio_mixing_mode.setVisibility(View.GONE);
                fl_setting.setVisibility(View.VISIBLE);
                return true;
            });
            int[] ids = {R.id.tv_send_only, R.id.tv_playout_only, R.id.tv_send_and_playout,
                    R.id.tv_replace_mic, R.id.tv_replace_mic_and_send_only,};
            for (int id : ids) {
                findViewById(id).setOnClickListener(this);
            }
        } else {
            vs_audio_mixing_mode.setVisibility(View.VISIBLE);
        }
    }

    // sei数据发送开关
    @Override
    public void toggleSendVideoSei(CompoundButton button) {
        if (mLivePresenter != null) {
            mLivePresenter.enableVideoSei(button.isChecked());
        }
    }

    // 伴奏开关
    @Override
    public void toggleAudioMixing(CompoundButton button) {
        if (mLivePresenter == null) {
            return;
        }
        int result = mLivePresenter.enableAudioMixing(button.isChecked());
        if (result != LVErrorCode.SUCCESS) {
            log("伴奏开关失败 - " + result);
            button.setChecked(!button.isChecked());
        }

        if (button.isChecked()) {
            ll_audio_mixing.setVisibility(View.VISIBLE);
        } else {
            ll_audio_mixing.setVisibility(View.GONE);
        }
    }

    // 外部输入音频数据开关
    @Override
    public void toggleExternalAudioInput(CompoundButton button) {
        if (tv_start_live.getVisibility() != View.VISIBLE) {
            showToast(getString(R.string.join_room_setting));
            button.setChecked(!button.isChecked());
            return;
        }
        if (mLivePresenter != null) {
            mLivePresenter.enableExternalAudioInput(button.isChecked());
        }
    }

    // 统计视图开关
    @Override
    public void toggleQualityView(CompoundButton buttonView) {
        lv_quality.setVisibility(buttonView.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void log(String text) {
        Log.d(TAG, text);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.sk_video_resolution:
                if (mLivePresenter == null) {
                    return;
                }
                List<LVAVConfig> levels = mLivePresenter.getVideoConfigLevels();
                int slice = seekBar.getMax() / (levels.size() - 1);
                int p = progress / slice;
                float fp = progress * 1.0f / slice;
                if (fp - p >= 0.5) {
                    p = p + 1;
                }
                if (mLastConfigProfile != p) {
                    mLastConfigProfile = p;
                    LVAVConfig config = levels.get(p);
                    tv_video_resolution.setText(config.getVideoEncodeWidth() + "x" + config.getVideoEncodeHeight());
                    tv_video_bitrate.setText(Integer.toString(config.getVideoTargetBitrate() / 1000));
                    sk_video_bitrate.setProgress(config.getVideoTargetBitrate() / BITRATE_SCALER / 1000);
                }
                break;
            case R.id.sk_video_bitrate:
                tv_video_bitrate.setText(Integer.toString(progress * BITRATE_SCALER));
                break;
            case R.id.sk_video_fps:
                tv_video_fps.setText(Integer.toString(progress));
                break;
            case R.id.seek_audio_mixing:
                if (fromUser && mLivePresenter != null) {
                    mLivePresenter.adjustAudioMixingVolume(progress);
                }
                break;
            case R.id.sk_beauty_level:
                if (mLivePresenter != null) {
                    mLivePresenter.changeBeautyLevel(progress * 1.0f / seekBar.getMax());
                }
                break;
            case R.id.sk_bright_level:
                if (mLivePresenter != null) {
                    mLivePresenter.changeBeautyBrightLevel(progress * 1.0f / seekBar.getMax());
                }
                break;
            case R.id.sk_tone_level:
                if (mLivePresenter != null) {
                    mLivePresenter.changeBeautyToneLevel(progress * 1.0f / seekBar.getMax());
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (seekBar.getId() == R.id.sk_video_resolution) {
            int progress = seekBar.getProgress();
            if (mLivePresenter != null) {
                List<LVAVConfig> levels = mLivePresenter.getVideoConfigLevels();
                int slice = seekBar.getMax() / (levels.size() - 1);
                int p = progress / slice;
                float fp = progress * 1.0f / slice;
                if (fp - p >= 0.5) {
                    p = p + 1;
                }
                seekBar.setProgress(slice * p);
            }
        }
    }
}
