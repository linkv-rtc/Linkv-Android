package com.linkv.live;


import com.linkv.live.view.DisplayContainer;
import com.linkv.rtc.entity.LVVideoStatistic;

public interface LivePresenterCallback {

    void updateUid(String uid);

    void updateRoomId(String roomId);

    void updateMic(boolean enable);

    void showToast(String msg);

    DisplayContainer getRemoteDisplayLayout(int size, boolean isHost);

    DisplayContainer getLocalDisplayLayout(int size);

    void removeDisplayLayout(DisplayContainer index);

    int getLayoutLimit();

    void updateLiveButton(int visible);

    void finishAct();

    void setBeamButtonClickable(boolean clickable);

    void onAddMemberList(String uid);

    void onRemoveMemberList(String uid);

    void updateDisplayQuality(LVVideoStatistic stats, String uid);
}
