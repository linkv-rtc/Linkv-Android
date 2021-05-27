package com.linkv.live.network;

import android.text.TextUtils;
import android.util.Log;

import com.linkv.rtc.LVErrorCode;
import com.linkv.rtc.internal.network.LVHttpException;
import com.linkv.rtc.internal.network.LVHttpManager;
import com.linkv.rtc.internal.network.LVHttpMsg;
import com.linkv.rtc.internal.utils.LMEngineLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NetManager {

    private static final String TAG = "NetManager";
    private static final String URL_INTERNATIONAL_ONLINE = "http://rtc-backend-orion.linkv.fun";
    private static final String URL_INTERNATIONAL_TEST = "http://qa-rtc-backend-orion.ksmobile.net";
    private static boolean IS_TEST_ENV = false;
    private static String URL_UPDATE_ROOM;
    private static String URL_GET_ROOM_STATUS;
    private static String URL_GEN_ROOM;
    private static String URL_GET_ROOM_LIST;

    static {
        setUseTestEnv(IS_TEST_ENV);
    }

    public static void setUseTestEnv(boolean useTestEnv) {
        IS_TEST_ENV = useTestEnv;
        log("setUseTestEnv  isTestEnv: " + useTestEnv);
        updateUrls();
    }

    private static void updateUrls() {
        String URL_BASE;
        if (IS_TEST_ENV) {
            URL_BASE = URL_INTERNATIONAL_TEST;
        } else {
            URL_BASE = URL_INTERNATIONAL_ONLINE;
        }
        URL_UPDATE_ROOM = URL_BASE + "/api/v1/update_room";
        URL_GET_ROOM_STATUS = URL_BASE + "/api/v1/room_status";
        URL_GEN_ROOM = URL_BASE + "/api/v1/gen_room";
        URL_GET_ROOM_LIST = URL_BASE + "/api/v1/live_room_list";
    }

    public static void genRoom(String appId, final GenRoomCallback callback) {
        if (callback == null) {
            return;
        }
        JSONObject obj = new JSONObject();
        try {
            if (!TextUtils.isEmpty(appId)) {
                obj.put("app_id", appId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        log("genRoom  params:" + obj.toString());
        LVHttpMsg msg = new LVHttpMsg(URL_GEN_ROOM);
        msg.setMethod(LVHttpMsg.Method.POST);
        msg.setReqTextData(obj.toString());
        msg.setListener(new LVHttpMsg.AbstractHttpMsgListener() {

            @Override
            public void onResponse(int responseCode, HashMap<String, String> headers, int responseLength, String respData) {
                log("genRoom  onResponse:" + respData);
                if (!TextUtils.isEmpty(respData)) {
                    try {
                        JSONObject response = new JSONObject(respData);
                        int code = response.getInt("code");
                        String message = response.getString("message");
                        if (code == 200) {
                            GenRoomBean roomBean = null;
                            JSONObject data = response.getJSONObject("data");
                            if (data != null) {
                                roomBean = new GenRoomBean();
                                roomBean.roomId = data.getString("room_id");
                                roomBean.userId = data.getString("user_id");
                            }
                            callback.onGenRoomResult(LVErrorCode.SUCCESS, roomBean, message);
                        } else {
                            callback.onGenRoomResult(code, null, message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onGenRoomResult(-1, null, e.getMessage());
                    }
                } else {
                    callback.onGenRoomResult(-1, null, "no response");
                }
            }

            @Override
            public void onError(LVHttpException e) {
                log("genRoom  onError  e:" + e.getMessage());
                callback.onGenRoomResult(-1, null, e.getMessage());
            }

            @Override
            public void onSocketTimeOut(Exception e) {
                log("genRoom  onError  e:" + e.getMessage());
                callback.onGenRoomResult(-1, null, "socket connect time out.");
            }
        });

        LVHttpManager.getInstance().send(msg);
    }

    public static void getRoomList(String appId, ResponseStringArrayCallback callback) {
        if (TextUtils.isEmpty(appId) || callback == null) {
            return;
        }
        log("getRoomList  appId: " + appId);
        LVHttpMsg msg = new LVHttpMsg(URL_GET_ROOM_LIST + "?app_id=" + appId);
        msg.setMethod(LVHttpMsg.Method.GET);
        msg.setListener(new LVHttpMsg.AbstractHttpMsgListener() {

            @Override
            public void onResponse(int responseCode, HashMap<String, String> headers, int responseLength, String respData) {
                log("getRoomList  onResponse:" + respData);
                if (!TextUtils.isEmpty(respData)) {
                    try {
                        JSONObject response = new JSONObject(respData);
                        int code = response.getInt("code");
                        String message = response.getString("message");
                        if (code == 200) {
                            JSONArray array = response.getJSONArray("data");
                            if (array != null) {
                                List<String> list = new ArrayList<>();
                                for (int i = 0; i < array.length(); i++) {
                                    list.add(array.getString(i));
                                }
                                callback.onResponseStringArrayResult(code, list, message);
                            } else {
                                callback.onResponseStringArrayResult(code, null, message);
                            }
                        } else {
                            callback.onResponseStringArrayResult(code, null, message);
                            log("getRoomList  appId: " + appId);
                            log("getRoomList  onResponse  code != 200. respData: " + respData);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    callback.onResponseStringArrayResult(-1, null, "onResponse  respData is empty");
                    log("getRoomList  onResponse  respData is empty.");
                }
            }

            @Override
            public void onError(LVHttpException e) {
                callback.onResponseStringArrayResult(-1, null, e.getMessage());
                log("getRoomList  onResponse  e: " + e.getMessage());
            }

            @Override
            public void onSocketTimeOut(Exception e) {
                callback.onResponseStringArrayResult(-1, null, "socket time out");
                log("getRoomList  onResponse  socket time out. ");
            }
        });

        LVHttpManager.getInstance().send(msg);
    }

    /**
     * state: ０:初始化, １:创建, ２:开始直播, 3:直播结束
     */
    public static void updateRoomState(String appId, String roomId, String state) {
        JSONObject obj = new JSONObject();
        try {
            if (!TextUtils.isEmpty(appId)) {
                obj.put("app_id", appId);
            }
            if (!TextUtils.isEmpty(roomId)) {
                obj.put("room_id", roomId);
            }
            if (!TextUtils.isEmpty(state)) {
                obj.put("status", state);
            }
            if (!TextUtils.isEmpty(state)) {
                obj.put("vendor", "octopus");
            }
            if (!TextUtils.isEmpty(state)) {
                obj.put("os", "android");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if ("1".equalsIgnoreCase(state) || "3".equalsIgnoreCase(state)) {
            log("updateRoomState  request start state: " + state);
        }

        LVHttpMsg msg = new LVHttpMsg(URL_UPDATE_ROOM);
        msg.setMethod(LVHttpMsg.Method.POST);
        msg.setReqTextData(obj.toString());
        msg.setListener(new LVHttpMsg.AbstractHttpMsgListener() {

            @Override
            public void onResponse(int responseCode, HashMap<String, String> headers, int responseLength, String respData) {
                if (!TextUtils.isEmpty(respData)) {
                    try {
                        JSONObject response = new JSONObject(respData);
                        int code = response.getInt("code");
                        if (code != 200) {
                            log("updateRoomState  params: " + obj.toString());
                            log("updateRoomState  onResponse  data: " + respData);
                        }

                        if ("1".equalsIgnoreCase(state) || "3".equalsIgnoreCase(state)) {
                            log("updateRoomState  response code: " + code);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        log("updateRoomState  onResponse  appId: " + appId + ", roomId: " + roomId + ", state: " + state +
                                ", error:" + e.getMessage());
                    }
                } else {
                    log("updateRoomState  onResponse  respData is empty.  appId: " + appId + ", roomId: " + roomId + ", state: " + state);
                }
            }

            @Override
            public void onError(LVHttpException e) {
                log("updateRoomState  onError  appId:" + appId + ", roomId: " + roomId + ",state: " + state + ", e: " + e.getMessage());
            }

            @Override
            public void onSocketTimeOut(Exception e) {
                log("onSocketTimeOut  onError  appId:" + appId + ", roomId: " + roomId + ",state: " + state + ", e: " + e.getMessage());
            }
        });

        LVHttpManager.getInstance().send(msg);
    }

    /**
     * state: ０:初始化, １:创建, ２:开始直播, 3:直播结束
     */
    public static void getRoomState(String roomId, ResponseStringCallback callback) {
        if (TextUtils.isEmpty(roomId) || callback == null) {
            return;
        }
        LVHttpMsg msg = new LVHttpMsg(URL_GET_ROOM_STATUS + "?room_id=" + roomId);
        msg.setMethod(LVHttpMsg.Method.GET);
        msg.setListener(new LVHttpMsg.AbstractHttpMsgListener() {

            @Override
            public void onResponse(int responseCode, HashMap<String, String> headers, int responseLength, String respData) {
                if (!TextUtils.isEmpty(respData)) {
                    try {
                        JSONObject response = new JSONObject(respData);
                        int code = response.getInt("code");
                        String message = response.getString("message");
                        if (code == 200) {
                            String state = response.getString("data");
                            callback.onResponseStringResult(code, state, message);
                        } else {
                            Log.d(TAG, "getRoomState  roomId: " + roomId);
                            log("getRoomState  onResponse  code != 200. respData: " + respData);
                            callback.onResponseStringResult(code, null, message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    callback.onResponseStringResult(-1, null, "onResponse.  respData is empty");
                    log("getRoomState  onResponse  respData is empty.");
                }
            }

            @Override
            public void onError(LVHttpException e) {
                callback.onResponseStringResult(-1, null, e.getMessage());
                log("getRoomState  onResponse  e: " + e.getMessage());
            }

            @Override
            public void onSocketTimeOut(Exception e) {
                callback.onResponseStringResult(-1, null, "socket time out");
                log("getRoomState  onResponse  socket time out. ");
            }
        });

        LVHttpManager.getInstance().send(msg);
    }

    private static void log(String msg) {
        LMEngineLogger.log(TAG, msg);
    }

    public interface ResponseStringCallback {
        void onResponseStringResult(int resultCode, String data, String message);
    }

    public interface GenRoomCallback {
        void onGenRoomResult(int resultCode, GenRoomBean roomBean, String message);
    }

    public interface ResponseStringArrayCallback {
        void onResponseStringArrayResult(int resultCode, List<String> roomList, String message);
    }

    public static class GenRoomBean {
        public String roomId;
        public String userId;
    }
}
