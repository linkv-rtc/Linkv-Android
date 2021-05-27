package com.linkv.live.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.linkv.live.LivePresenter;
import com.linkv.live.R;
import com.linkv.live.network.NetManager;
import com.linkv.live.utils.Constants;

import java.util.ArrayList;
import java.util.List;


public class RoomListActivity extends BaseActivity {

    private RoomListAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);
        ListView lv_room_list = findViewById(R.id.lv_room_list);
        mAdapter = new RoomListAdapter(this);
        lv_room_list.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getRoomList();
    }

    private void getRoomList() {
        NetManager.getRoomList(Constants.AppId, (resultCode, roomList, message) -> {
            if (resultCode == 200) {
                runOnUiThread(() -> updateRoomList(roomList));
            }
        });
    }

    private void updateRoomList(List<String> roomList) {
        if (mAdapter != null) {
            mAdapter.setData(roomList);
        }
    }

    public void goBack(View view) {
        finish();
    }

    public void updateList(View view) {
        getRoomList();
    }

    public class RoomListAdapter extends BaseAdapter {

        private List<String> roomList = new ArrayList<>();
        private LayoutInflater layoutInflater;

        RoomListAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        void setData(List<String> roomList) {
            this.roomList.clear();
            if (roomList != null && roomList.size() > 0) {
//                this.roomList.addAll(roomList);
                for (String uid : roomList) {
                    if (!TextUtils.isEmpty(uid) &&
                            (uid.startsWith(LivePresenter.HEAD_ROOM_AUDIO) ||
                                    uid.startsWith(LivePresenter.HEAD_ROOM_LIVE) ||
                                    uid.startsWith(LivePresenter.HEAD_ROOM_MEETING))) {
                        this.roomList.add(uid);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return roomList.size();
        }

        @Override
        public Object getItem(int position) {
            return roomList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position >= roomList.size()) {
                return null;
            }
            ViewHolder holder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.item_room_list, parent, false);
                holder = new ViewHolder();
                holder.text = convertView.findViewById(R.id.tv_room_id);
                holder.btn_enter_live = convertView.findViewById(R.id.btn_enter_live);
                holder.v_line = convertView.findViewById(R.id.v_line);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.text.setText(roomList.get(position));
            if (position == roomList.size() - 1) {
                holder.v_line.setVisibility(View.GONE);
            } else {
                holder.v_line.setVisibility(View.VISIBLE);
            }

            holder.btn_enter_live.setOnClickListener(v -> {
                Intent intent = new Intent(RoomListActivity.this, LiveActivity.class);
                intent.putExtra(LiveActivity.KEY_ENTER_FROM, LiveActivity.FROM_WATCH);
                intent.putExtra(LiveActivity.KEY_ENTER_ROOM_ID, mAdapter.getRoomId(position));
                startActivity(intent);
            });
            return convertView;
        }

        String getRoomId(int position) {
            if (position < roomList.size()) {
                return roomList.get(position);
            }
            return null;
        }

        class ViewHolder {
            TextView text;
            Button btn_enter_live;
            View v_line;
        }
    }

}
