package com.pokemonshowdown.data;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.util.Log;

import com.pokemonshowdown.app.BattleFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class BattleFieldData {
    private final static String BTAG = BattleFieldData.class.getName();
    private ArrayList<FormatType> mFormatTypes;
    private int mCurrentFormat;
    private JSONObject mAvailableBattle;
    private ArrayList<String> mRoomList;
    private HashMap<String, BattleLog> mRoomDataHashMap;
    private HashMap<String, RoomData> mAnimationDataHashMap;
    private HashMap<String, ViewData> mViewDataHashMap;

    private static BattleFieldData sBattleFieldData;
    private Context mAppContext;

    private BattleFieldData(Context appContext) {
        mAppContext = appContext;
        mFormatTypes = new ArrayList<>();
        mRoomList = new ArrayList<>();
        mRoomList.add("global");
        mRoomDataHashMap = new HashMap<>();
        mAnimationDataHashMap = new HashMap<>();
        mViewDataHashMap = new HashMap<>();
    }

    public static BattleFieldData get(Context c) {
        if (sBattleFieldData == null) {
            sBattleFieldData = new BattleFieldData(c.getApplicationContext());
        }
        return sBattleFieldData;
    }

    public ArrayList<FormatType> getFormatTypes() {
        return mFormatTypes;
    }

    public void generateAvailableRoomList(String message) {
        while (message.length() != 0) {
            if (message.charAt(0) == ',') {
                message = message.substring(message.indexOf('|') + 1);
                String formatType = message.substring(0, message.indexOf('|'));
                mFormatTypes.add(new FormatType(formatType));
                message = message.substring(message.indexOf('|') + 1);
            } else {
                int separator = message.indexOf('|');
                String formatName = (separator == -1) ? message : message.substring(0, separator);
                mFormatTypes.get(mFormatTypes.size() - 1).getFormatList().add(processSpecialRoomTrait(formatName));
                message = (separator == -1) ? "" : message.substring(separator + 1);
            }
        }
        LocalBroadcastManager.getInstance(mAppContext).sendBroadcast(new Intent(MyApplication.ACTION_FROM_MY_APPLICATION).putExtra(MyApplication.EXTRA_DETAILS, MyApplication.EXTRA_AVAILABLE_FORMATS));

    }
    public Format getFormat(String formatName) {
        for (FormatType formatType : mFormatTypes) {
            for (Format format : formatType.getFormatList()) {
                if(format.getName().equals(formatName)) {
                    return format;
                }
            }
        }
        return null;
    }
    public String getCurrentFormatName() {
        int currentFormat = getCurrentFormat();
        int count = 0;
        do {
            int mask = mFormatTypes.get(count).getSearchableFormatList().size();
            if (mask > currentFormat) {
                return mFormatTypes.get(count).getSearchableFormatList().get(currentFormat);
            }
            count++;
            currentFormat -= mask;
        } while (currentFormat >= 0);
        return null;
    }

    public int getCurrentFormat() {
        return mCurrentFormat;
    }

    public void setCurrentFormat(int currentFormat) {
        mCurrentFormat = currentFormat;
    }

    public Format processSpecialRoomTrait(String query) {
        int separator = query.indexOf(',');
        Format format;
        if (separator == -1) {
            format = new Format(query);
        } else {
            format = new Format(query.substring(0, separator));
            String special = query.substring(separator);
            int specs = special.indexOf(",#");
            if (specs != -1) {
                format.getSpecialTrait().add(",#");
                special = special.substring(0, specs);
            }
            specs = special.indexOf(",,");
            if (specs != -1) {
                format.getSpecialTrait().add(",,");
                special = special.substring(0, specs);
            }
            specs = special.indexOf(",");
            if (specs != -1) {
                format.getSpecialTrait().add(",");
            }
        }
        return format;
    }

    public void parseAvailableWatchBattleList(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            mAvailableBattle = jsonObject.getJSONObject("rooms");
            LocalBroadcastManager.getInstance(mAppContext).sendBroadcast(new Intent(MyApplication.ACTION_FROM_MY_APPLICATION).putExtra(MyApplication.EXTRA_DETAILS, MyApplication.EXTRA_WATCH_BATTLE_LIST_READY));
        } catch (JSONException e) {
            Log.d(BTAG, e.toString());
        }
    }

    public HashMap<String, String> getAvailableWatchBattleList() {
        if (mAvailableBattle == null) {
            return null;
        }
        HashMap<String, String> toReturn = new HashMap<>();
        Iterator<String> rooms = mAvailableBattle.keys();
        String currentFormat = getCurrentFormatName();
        currentFormat = "-" + MyApplication.toId(currentFormat) + "-";
        while (rooms.hasNext()) {
            String roomId = rooms.next();
            if (roomId.contains(currentFormat)) {
                try {
                    JSONObject players = mAvailableBattle.getJSONObject(roomId);
                    String sb = players.getString("p1") + " vs. " + players.getString("p2");
                    toReturn.put(roomId, sb);
                } catch (JSONException e) {
                    Log.d(BTAG, e.toString());
                }
            }
        }
        return toReturn;
    }

    public static String getRoomFormat(String roomId) {
        return roomId.substring(roomId.indexOf("-") + 1, roomId.lastIndexOf("-"));
    }

    public ArrayList<String> getRoomList() {
        return mRoomList;
    }

    public HashMap<String, BattleLog> getRoomDataHashMap() {
        return mRoomDataHashMap;
    }

    public void saveRoomInstance(String roomId, CharSequence chatBox, boolean messageListener) {
        mRoomDataHashMap.put(roomId, new BattleLog(roomId, chatBox, messageListener));
    }

    public BattleLog getRoomInstance(String roomId) {
        return mRoomDataHashMap.get(roomId);
    }

    public HashMap<String, RoomData> getAnimationDataHashMap() {
        return mAnimationDataHashMap;
    }

    public RoomData getAnimationInstance(String roomId) {
        return mAnimationDataHashMap.get(roomId);
    }

    public HashMap<String, ViewData> getViewDataHashMap() {
        return mViewDataHashMap;
    }

    public ViewData getViewData(String roomId) {
        return mViewDataHashMap.get(roomId);
    }

    public void joinRoom(String roomId, boolean watch) {
        HashMap<String, BattleLog> roomDataHashMap = getRoomDataHashMap();
        if (!roomDataHashMap.containsKey(roomId)) {
            roomDataHashMap.put(roomId, new BattleLog(roomId, "", true));
            getAnimationDataHashMap().put(roomId, new RoomData(roomId, true));
            getViewDataHashMap().put(roomId, new ViewData(roomId));
        }
        if (watch) {
            MyApplication.getMyApplication().sendClientMessage("|/join " + roomId);
        }
    }

    public void leaveRoom(String roomId) {
        mRoomList.remove(roomId);
        getRoomDataHashMap().remove(roomId);
        getAnimationDataHashMap().remove(roomId);
        getViewDataHashMap().remove(roomId);
        MyApplication.getMyApplication().sendClientMessage("|/leave " + roomId);
    }

    public void leaveAllRooms() {
        for (String roomId : mRoomList) {
            leaveRoom(roomId);
        }
    }

    public static class BattleLog {
        private String mRoomId;
        private CharSequence mChatBox;
        private boolean mMessageListener;
        private ArrayList<Spannable> mServerMessageOnHold;

        public BattleLog(String roomId, CharSequence chatBox, boolean messageListener) {
            mRoomId = roomId;
            mChatBox = chatBox;
            mServerMessageOnHold = new ArrayList<>();
            mMessageListener = messageListener;
        }

        public String getRoomId() {
            return mRoomId;
        }

        public CharSequence getChatBox() {
            return mChatBox;
        }

        public void setChatBox(CharSequence chatBox) {
            mChatBox = chatBox;
        }

        public ArrayList<Spannable> getServerMessageOnHold() {
            return mServerMessageOnHold;
        }

        public void setServerMessageOnHold(ArrayList<Spannable> serverMessageOnHold) {
            mServerMessageOnHold = serverMessageOnHold;
        }

        public void addServerMessageOnHold(Spannable serverMessageOnHold) {
            mServerMessageOnHold.add(serverMessageOnHold);
        }

        public boolean isMessageListener() {
            return mMessageListener;
        }

        public void setMessageListener(boolean messageListener) {
            mMessageListener = messageListener;
        }
    }

    public static class RoomData {
        private String mRoomId;
        private boolean mMessageListener;
        private ArrayList<String> mServerMessageOnHold;

        private String mPlayer1;
        private String mPlayer2;

        private HashMap<BattleFragment.ViewBundle, Object> mViewBundle;


        public RoomData(String roomId, boolean messageListener) {
            mRoomId = roomId;
            mServerMessageOnHold = new ArrayList<>();
            mMessageListener = messageListener;
        }

        public String getRoomId() {
            return mRoomId;
        }

        public ArrayList<String> getServerMessageOnHold() {
            return mServerMessageOnHold;
        }

        public void clearServerMessageOnHold() {
            mServerMessageOnHold = new ArrayList<>();
        }

        public void addServerMessageOnHold(String serverMessageOnHold) {
            mServerMessageOnHold.add(serverMessageOnHold);
        }

        public boolean isMessageListener() {
            return mMessageListener;
        }

        public void setMessageListener(boolean messageListener) {
            mMessageListener = messageListener;
        }

        public HashMap<BattleFragment.ViewBundle, Object> getViewBundle() {
            return mViewBundle;
        }

        public void setViewBundle(HashMap<BattleFragment.ViewBundle, Object> viewBundle) {
            mViewBundle = viewBundle;
        }

        public String getPlayer1() {
            return mPlayer1;
        }

        public void setPlayer1(String player1) {
            mPlayer1 = player1;
        }

        public String getPlayer2() {
            return mPlayer2;
        }

        public void setPlayer2(String player2) {
            mPlayer2 = player2;
        }
    }

    public static class ViewData {
        private String mRoomId;
        private LinkedList<ViewSetter> mViewSetterOnHold;

        public static enum SetterType {
            BATTLE_START,
            TEXTVIEW_SETTEXT, IMAGEVIEW_SETIMAGERESOURCE,
            VIEW_VISIBLE, VIEW_INVISIBLE, VIEW_GONE
        }

        public ViewData(String roomId) {
            mRoomId = roomId;
            mViewSetterOnHold = new LinkedList<>();
        }

        public String getRoomId() {
            return mRoomId;
        }

        public void addViewSetterOnHold(int viewId, Object value, SetterType type) {
            mViewSetterOnHold.addLast(new ViewSetter(viewId, value, type));
        }

        public LinkedList<ViewSetter> getViewSetterOnHold() {
            return mViewSetterOnHold;
        }

    }

    public static class ViewSetter {
        private int viewId;
        private Object value;
        private ViewData.SetterType type;

        public ViewSetter(int viewId, Object value, ViewData.SetterType type) {
            this.viewId = viewId;
            this.value = value;
            this.type = type;
        }

        public int getViewId() {
            return viewId;
        }

        public Object getValue() {
            return value;
        }

        public ViewData.SetterType getType() {
            return type;
        }
    }

    public static class FormatType {
        private String mName;
        private ArrayList<Format> mFormatList;

        public FormatType(String name) {
            mName = name;
            mFormatList = new ArrayList<>();
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public ArrayList<String> getSearchableFormatList() {
            ArrayList<String> formatList = new ArrayList<>();
            for (Format format : mFormatList) {
                if (!format.getSpecialTrait().contains(",")) {
                    formatList.add(format.getName());
                }
            }
            return formatList;
        }

        public ArrayList<Format> getFormatList() {
            return mFormatList;
        }

        public void setFormatList(ArrayList<Format> formatList) {
            mFormatList = formatList;
        }
    }

    public static class Format {
        private String mName;
        private ArrayList<String> mSpecialTrait;
        private static final String RANDOM_FORMAT_TRAIT = ",#";

        public Format(String name) {
            mName = name;
            mSpecialTrait = new ArrayList<>();
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public ArrayList<String> getSpecialTrait() {
            return mSpecialTrait;
        }

        public void setSpecialTrait(ArrayList<String> specialTrait) {
            mSpecialTrait = specialTrait;
        }

        public boolean isRandomFormat() {
            for (String s : mSpecialTrait) {
                if (s.equals(RANDOM_FORMAT_TRAIT)) {
                    return true;
                }
            }
            return false;
        }
    }
}
