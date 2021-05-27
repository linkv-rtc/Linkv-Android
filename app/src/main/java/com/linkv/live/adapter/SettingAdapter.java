package com.linkv.live.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.linkv.live.R;

import java.util.ArrayList;
import java.util.List;

public class SettingAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private List<SettingData> mList;
    private SettingAdapterCallback mCallback;

    public SettingAdapter(Context context, SettingAdapterCallback callback) {
        this.mCallback = callback;
        layoutInflater = LayoutInflater.from(context);
        mList = new ArrayList<>();
        mList.add(new SettingData().setText(context.getString(R.string.send_sei)).isShowSwitch(true).isSwitched(false));
        mList.add(new SettingData().setText(context.getString(R.string.resolution_adaptation)).isShowSwitch(false).isSwitched(false));
        mList.add(new SettingData().setText(context.getString(R.string.video_parameter)).isShowSwitch(false).isSwitched(false));
        mList.add(new SettingData().setText(context.getString(R.string.video_angle)).isShowSwitch(false).isSwitched(false));
        mList.add(new SettingData().setText(context.getString(R.string.accompaniment_demo)).isShowSwitch(true).isSwitched(false));
        mList.add(new SettingData().setText(context.getString(R.string.external_audio_input)).isShowSwitch(true).isSwitched(false));
        mList.add(new SettingData().setText(context.getString(R.string.quality)).isShowSwitch(true).isSwitched(false));
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mList.size()) {
            return null;
        }
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_setting, parent, false);
            holder = new ViewHolder();
            holder.fl_item_setting = convertView.findViewById(R.id.fl_item_setting);
            holder.tv_setting_item = convertView.findViewById(R.id.tv_setting_item);
            holder.tool_switch = convertView.findViewById(R.id.tool_switch);
            holder.v_line = convertView.findViewById(R.id.view_line_setting);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (position == mList.size() - 1) {
            holder.v_line.setVisibility(View.GONE);
        } else {
            holder.v_line.setVisibility(View.VISIBLE);
        }

        SettingData settingData = mList.get(position);
        holder.tv_setting_item.setText(settingData.text);
        holder.tool_switch.setVisibility(settingData.isShowSwitch ? View.VISIBLE : View.GONE);
        holder.tool_switch.setChecked(settingData.isSwitched);

        holder.tool_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingData.isSwitched(isChecked);
            if (mCallback == null) {
                return;
            }

            if (position == 0) {
                // 添加SEI数据
                mCallback.toggleSendVideoSei(buttonView);
            } else if (position == 4) {
                // 伴奏
                mCallback.toggleAudioMixing(buttonView);
            } else if (position == 5) {
                // 外部输入音频数据
                mCallback.toggleExternalAudioInput(buttonView);
            } else if (position == 6) {
                // 统计视图展示
                mCallback.toggleQualityView(buttonView);
            }
        });

        holder.fl_item_setting.setOnClickListener(v -> {
            if (mCallback == null) {
                return;
            }

            if (position == 1) {
                // 视频降级策略
                mCallback.changeVideoDegradation();
            } else if (position == 2) {
                // 设置视频参数
                mCallback.changeVideoConfig();
            } else if (position == 3) {
                // 视频输出角度设置
                mCallback.showOutputRotation();
            } else if (position == 4) {
                // 设置伴奏模式
                mCallback.changeAudioMixingMode();
            }
        });


        return convertView;
    }

    public interface SettingAdapterCallback {
        void changeVideoDegradation();

        void changeVideoConfig();

        void showOutputRotation();

        void changeAudioMixingMode();

        void toggleSendVideoSei(CompoundButton button);

        void toggleAudioMixing(CompoundButton button);

        void toggleExternalAudioInput(CompoundButton button);

        void toggleQualityView(CompoundButton buttonView);
    }

    static class SettingData {
        String text;
        boolean isShowSwitch;
        boolean isSwitched;

        SettingData setText(String text) {
            this.text = text;
            return this;
        }

        SettingData isShowSwitch(boolean isShow) {
            this.isShowSwitch = isShow;
            return this;
        }

        SettingData isSwitched(boolean isSwitch) {
            this.isSwitched = isSwitch;
            return this;
        }
    }

    class ViewHolder {
        FrameLayout fl_item_setting;
        TextView tv_setting_item;
        Switch tool_switch;
        View v_line;
    }
}
