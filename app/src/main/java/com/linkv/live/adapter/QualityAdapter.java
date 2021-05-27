package com.linkv.live.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.linkv.live.R;
import com.linkv.rtc.entity.LVVideoStatistic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QualityAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private List<Quality> mList;
    private Map<String, Quality> mQualityMap;

    public QualityAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mList = new ArrayList<>();
        mQualityMap = new HashMap<>();
        mList.add(new Quality(null, ""));
    }

    public void updateQuality(LVVideoStatistic statistic, String uid) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        if (statistic == null) {
            Quality quality = mQualityMap.get(uid);
            mList.remove(quality);
            mQualityMap.remove(uid);
            notifyDataSetChanged();
            return;
        }

        if (!mQualityMap.containsKey(uid)) {
            Quality quality = new Quality(statistic, uid);
            mQualityMap.put(uid, quality);
            mList.add(quality);
        } else {
            Quality quality = mQualityMap.get(uid);
            assert quality != null;
            quality.statistic = statistic;
        }
        notifyDataSetChanged();
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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mList.size()) {
            return null;
        }
        ViewHolder holder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_quality, parent, false);
            holder = new ViewHolder();
            holder.tv_qua_uid = convertView.findViewById(R.id.tv_qua_uid);
            holder.tv_qua_video_lost = convertView.findViewById(R.id.tv_qua_video_lost);
            holder.tv_qua_audio_lost = convertView.findViewById(R.id.tv_qua_audio_lost);
            holder.tv_qua_video_bitrate = convertView.findViewById(R.id.tv_qua_video_bitrate);
            holder.tv_qua_audio_bitrate = convertView.findViewById(R.id.tv_qua_audio_bitrate);
            holder.tv_qua_rtt = convertView.findViewById(R.id.tv_qua_rtt);
            holder.tv_qua_fps = convertView.findViewById(R.id.tv_qua_fps);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (position != 0) {
            Quality quality = mList.get(position);
            String uid = quality.uid;
            if (!TextUtils.isEmpty(uid) && uid.length() > 6) {
                uid = uid.substring(uid.length() - 6);
            }
            holder.tv_qua_uid.setText(uid);
            holder.tv_qua_video_lost.setText(quality.statistic.videoLostPercent + "%");
            holder.tv_qua_audio_lost.setText(quality.statistic.audioLostPercent + "%");
            holder.tv_qua_video_bitrate.setText(Integer.toString(quality.statistic.videoBitratebps));
            holder.tv_qua_audio_bitrate.setText(Integer.toString(quality.statistic.audioBitratebps));
            holder.tv_qua_rtt.setText(quality.statistic.videoRtt + "/" + quality.statistic.audioRtt);
            holder.tv_qua_fps.setText(Integer.toString(quality.statistic.videoFps));
        } else {
            holder.tv_qua_uid.setText(mContext.getString(R.string.uid));
            holder.tv_qua_video_lost.setText(mContext.getString(R.string.video_lost));
            holder.tv_qua_audio_lost.setText(mContext.getString(R.string.audio_lost));
            holder.tv_qua_video_bitrate.setText(mContext.getString(R.string.video_bitrate));
            holder.tv_qua_audio_bitrate.setText(mContext.getString(R.string.audio_bitrate));
            holder.tv_qua_rtt.setText("RTT(V/A)");
            holder.tv_qua_fps.setText(mContext.getString(R.string.fps));
        }

        return convertView;
    }

    public static class Quality {
        LVVideoStatistic statistic;
        String uid;

        Quality(LVVideoStatistic statistic, String uid) {
            this.statistic = statistic;
            this.uid = uid;
        }
    }

    class ViewHolder {
        TextView tv_qua_uid;
        TextView tv_qua_video_lost;
        TextView tv_qua_audio_lost;
        TextView tv_qua_video_bitrate;
        TextView tv_qua_audio_bitrate;
        TextView tv_qua_rtt;
        TextView tv_qua_fps;
    }
}
