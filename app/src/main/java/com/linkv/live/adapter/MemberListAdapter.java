package com.linkv.live.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.linkv.live.LivePresenter;
import com.linkv.live.R;

import java.util.ArrayList;
import java.util.List;

public class MemberListAdapter extends BaseAdapter {

    private String mUid;
    private LayoutInflater layoutInflater;
    private List<String> mList;
    private Context mContext;

    public MemberListAdapter(Context context, String uid) {
        mUid = uid;
        mContext = context;
        layoutInflater = LayoutInflater.from(context);
        mList = new ArrayList<>();
    }

    public void addMember(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        if (!findMember(uid)) {
            mList.add(uid);
            notifyDataSetChanged();
        }
    }

    public void removeMember(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        if (findMember(uid)) {
            mList.remove(uid);
            notifyDataSetChanged();
        }
    }

    private boolean findMember(String uid) {
        if (TextUtils.isEmpty(uid)) {
            return false;
        }
        boolean find = false;
        for (int i = 0; i < mList.size(); i++) {
            if (uid.equalsIgnoreCase(mList.get(i))) {
                find = true;
                break;
            }
        }
        return find;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public String getItem(int position) {
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
            convertView = layoutInflater.inflate(R.layout.item_member, parent, false);
            holder = new ViewHolder();
            holder.tv_member_item = convertView.findViewById(R.id.tv_member_item);
            holder.v_line = convertView.findViewById(R.id.view_line_member);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (position == mList.size() - 1) {
            holder.v_line.setVisibility(View.GONE);
        } else {
            holder.v_line.setVisibility(View.VISIBLE);
        }

        String uid = mList.get(position);
        if (!TextUtils.isEmpty(uid) && uid.equalsIgnoreCase(mUid)) {
            holder.tv_member_item.setText(uid + "---" + mContext.getString(R.string.me));
        } else if (!TextUtils.isEmpty(uid) && uid.startsWith(LivePresenter.HEAD_UID_HOST)) {
            holder.tv_member_item.setText(uid + "---" + mContext.getString(R.string.host));
        } else {
            holder.tv_member_item.setText(mList.get(position));
        }
        return convertView;
    }

    class ViewHolder {
        TextView tv_member_item;
        View v_line;
    }
}
