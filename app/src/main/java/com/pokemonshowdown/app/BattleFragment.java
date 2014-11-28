package com.pokemonshowdown.app;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pokemonshowdown.data.BattleFieldData;
import com.pokemonshowdown.data.BattleMessage;
import com.pokemonshowdown.data.MoveDex;
import com.pokemonshowdown.data.PokemonInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class BattleFragment extends Fragment {
    public final static String BTAG = BattleFragment.class.getName();
    public final static String ROOM_ID = "Room Id";
    public final static int ANIMATION_SHORT = 500;
    public final static int ANIMATION_LONG = 1000;
    public final static int[] BACKGROUND_LIBRARY = {R.drawable.bg, R.drawable.bg_beach, R.drawable.bg_beachshore, R.drawable.bg_city, R.drawable.bg_desert, R.drawable.bg_earthycave, R.drawable.bg_forest, R.drawable.bg_icecave, R.drawable.bg_meadow, R.drawable.bg_river, R.drawable.bg_route};
    public final static String[] stats = {"atk", "def", "spa", "spd", "spe", "accuracy", "evasion"};
    public final static String[] sttus = {"psn", "tox", "frz", "par", "slp", "brn"};
    public final static String[][] teammates = {{"p1a", "p1b", "p1c"}, {"p2a", "p2b", "p2c"}};

    public ArrayDeque<AnimatorSet> mAnimatorSetQueue;
    public int[] progressBarHolder = new int[6];

    private String mRoomId;
    /**
     * 0 if it's a simple watch battle
     * 1 if player is p1
     * -1 if player is p2
     */
    public int mBattling;
    public String mPlayer1;
    public String mPlayer2;
    public HashMap<Integer, PokemonInfo> mPlayer1Team;
    public HashMap<Integer, PokemonInfo> mPlayer2Team;

    public String currentWeather;
    public boolean weatherExist;
    public int turnNumber;
    public boolean myTurn;

    public static BattleFragment newInstance(String roomId) {
        BattleFragment fragment = new BattleFragment();
        Bundle args = new Bundle();
        args.putString(ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    public BattleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_battle, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            mRoomId = getArguments().getString(ROOM_ID);
        }

        mBattling = 0;

        int id = new Random().nextInt(BACKGROUND_LIBRARY.length);
        ((ImageView) view.findViewById(R.id.battle_background)).setImageResource(BACKGROUND_LIBRARY[id]);

        view.findViewById(R.id.battlelog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialogFragment = BattleLogDialog.newInstance(mRoomId);
                dialogFragment.show(getActivity().getSupportFragmentManager(), mRoomId);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        BattleFieldData.AnimationData animationData = BattleFieldData.get(getActivity()).getAnimationInstance(mRoomId);
        if (animationData != null) {
            animationData.setMessageListener(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        BattleFieldData.AnimationData animationData = BattleFieldData.get(getActivity()).getAnimationInstance(mRoomId);
        if (animationData != null) {
            animationData.setMessageListener(true);
        }
    }

    public String getRoomId() {
        return mRoomId;
    }

    public void setBattling(JSONObject object) throws JSONException {
        String side = object.getJSONObject("side").getString("id");
        if (side.equals("p1")) {
            mBattling = 1;
        } else {
            mBattling = -1;
            switchUpPlayer();
        }
    }

    public void setDisplayTeam(JSONObject object) throws JSONException {
        object = object.getJSONObject("side");
        JSONArray team = object.getJSONArray("pokemon");
        for (int i = 0; i < team.length(); i++) {
            JSONObject info = team.getJSONObject(i);
            PokemonInfo pkm = parsePokemonInfo(info);
            mPlayer1Team.put(i, pkm);
        }
    }

    private PokemonInfo parsePokemonInfo(JSONObject info) throws JSONException {
        String details = info.getString("details");
        String name = !details.contains(",") ? details : details.substring(0, details.indexOf(","));
        PokemonInfo pkm = new PokemonInfo(getActivity(), name);
        String nickname = info.getString("ident").substring(4);
        pkm.setNickname(nickname);
        if (details.contains(", L")) {
            String level = details.substring(details.indexOf(", L") + 3);
            level = !level.contains(",") ? level : level.substring(0, level.indexOf(","));
            pkm.setLevel(Integer.parseInt(level));
        }
        if (details.contains(", M")) {
            pkm.setGender("M");
        } else {
            if (details.contains(", F")) {
                pkm.setGender("F");
            }
        }
        if (details.contains("shiny")) {
            pkm.setShiny(true);
        }
        String hp = info.getString("condition");
        pkm.setHp(processHpFraction(hp));
        pkm.setActive(info.getBoolean("active"));
        JSONObject statsArray = info.getJSONObject("stats");
        int[] stats = new int[5];
        stats[0] = statsArray.getInt("atk");
        stats[1] = statsArray.getInt("def");
        stats[2] = statsArray.getInt("spa");
        stats[3] = statsArray.getInt("spd");
        stats[4] = statsArray.getInt("spe");
        pkm.setStats(stats);
        JSONArray movesArray = info.getJSONArray("moves");
        HashMap<String, Integer> moves = new HashMap<>();
        for (int i = 0; i < movesArray.length(); i++) {
            String move = movesArray.getString(i);
            JSONObject ppObject = MoveDex.get(getActivity()).getMoveJsonObject(move);
            if (ppObject == null) {
                moves.put(move, 0);
            } else {
                moves.put(move, ppObject.getInt("pp"));
            }
        }
        pkm.setMoves(moves);
        pkm.setAbility("baseAbility");
        pkm.setItem("item");
        try {
            pkm.setCanMegaEvo(info.getBoolean("canMegaEvo"));
        } catch (JSONException e) {
            pkm.setCanMegaEvo(false);
        }
        return pkm;
    }

    private void switchUpPlayer() {
        // Switch player name
        if (getView() == null) {
            return;
        }

        String holderString = mPlayer1;
        mPlayer1 = mPlayer2;
        mPlayer2 = holderString;

        HashMap<Integer, PokemonInfo> holderTeam = mPlayer1Team;
        mPlayer1Team = mPlayer2Team;
        mPlayer2Team = holderTeam;

        // Switch player avatar
        Drawable holderDrawable = ((ImageView) getView().findViewById(R.id.avatar)).getDrawable();
        ((ImageView) getView().findViewById(R.id.avatar)).setImageDrawable(((ImageView) getView().findViewById(R.id.avatar_o)).getDrawable());
        ((ImageView) getView().findViewById(R.id.avatar_o)).setImageDrawable(holderDrawable);
    }

    public void processServerMessage(String message) {
        if (mBattling == -1) {
            message = message.replace("p1", "p3").replace("p2", "p1").replace("p3", "p2");
        }
        BattleMessage.processMajorAction(this, message);
    }

    public AnimatorSet makeMinorToast(final Spannable message) {
        if (getView() == null) {
            return null;
        }
        TextView textView = (TextView) getView().findViewById(R.id.toast);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
        fadeIn.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartDelay(ANIMATION_SHORT);

        AnimatorSet animation = new AnimatorSet();
        animation.play(fadeIn);
        animation.play(fadeOut).after(fadeIn);
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (getView() == null) {
                    return;
                }
                TextView toast = (TextView) getView().findViewById(R.id.toast);
                if (toast != null) {
                    toast.setText(message);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return animation;
    }

    public AnimatorSet makeToast(final Spannable message, final int duration) {
        if (getView() == null) {
            return null;
        }
        TextView textView = (TextView) getView().findViewById(R.id.toast);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
        fadeIn.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartDelay(duration);

        AnimatorSet animation = new AnimatorSet();
        animation.play(fadeIn);
        animation.play(fadeOut).after(fadeIn);
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (getView() == null) {
                    return;
                }
                TextView toast = (TextView) getView().findViewById(R.id.toast);
                if (toast != null) {
                    toast.setText(message);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return animation;
    }

    public AnimatorSet makeToast(final String message) {
        return makeToast(message, ANIMATION_LONG);
    }

    public AnimatorSet makeToast(final String message, final int duration) {
        return makeToast(new SpannableString(message), duration);
    }

    public AnimatorSet makeToast(final Spannable message) {
        return makeToast(message, ANIMATION_LONG);
    }

    public void startAnimation(final AnimatorSet animator) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimatorSetQueue.pollFirst();
                        Animator nextOnQueue = mAnimatorSetQueue.peekFirst();
                        if (nextOnQueue != null) {
                            nextOnQueue.start();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });

                if (mAnimatorSetQueue == null) {
                    mAnimatorSetQueue = new ArrayDeque<>();
                }

                mAnimatorSetQueue.addLast(animator);

                if (mAnimatorSetQueue.size() == 1) {
                    animator.start();
                }
            }
        });
    }

    public void addToLog(Spannable logMessage) {
        BattleFieldData.RoomData roomData = BattleFieldData.get(getActivity()).getRoomInstance(mRoomId);
        if (roomData != null && roomData.isMessageListener()) {
            if (logMessage.length() > 0) {
                roomData.addServerMessageOnHold(logMessage);
            }
        } else {
            BattleLogDialog battleLogDialog =
                    (BattleLogDialog) getActivity().getSupportFragmentManager().findFragmentByTag(mRoomId);
            if (battleLogDialog != null) {
                if (logMessage.length() > 0) {
                    battleLogDialog.processServerMessage(logMessage);
                }
            }
        }
    }

    /**
     * @param player can be p1 or p2
     */
    public int getTeamPreviewSpriteId(String player, int id) {
        String p = player.substring(0, 2);
        switch (p) {
            case "p1":
                switch (id) {
                    case 0:
                        return R.id.p1a_prev;
                    case 1:
                        return R.id.p1b_prev;
                    case 2:
                        return R.id.p1c_prev;
                    case 3:
                        return R.id.p1d_prev;
                    case 4:
                        return R.id.p1e_prev;
                    case 5:
                        return R.id.p1f_prev;
                    default:
                        return 0;
                }
            case "p2":
                switch (id) {
                    case 0:
                        return R.id.p2a_prev;
                    case 1:
                        return R.id.p2b_prev;
                    case 2:
                        return R.id.p2c_prev;
                    case 3:
                        return R.id.p2d_prev;
                    case 4:
                        return R.id.p2e_prev;
                    case 5:
                        return R.id.p2f_prev;
                    default:
                        return 0;
                }
            default:
                return 0;
        }
    }

    public int getPkmLayoutId(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return R.id.p1a;
            case "p1b":
                return R.id.p1b;
            case "p1c":
                return R.id.p1c;
            case "p2a":
                return R.id.p2a;
            case "p2b":
                return R.id.p2b;
            case "p2c":
                return R.id.p2c;
            default:
                return 0;
        }
    }

    public int getSpriteId(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return R.id.p1a_icon;
            case "p1b":
                return R.id.p1b_icon;
            case "p1c":
                return R.id.p1c_icon;
            case "p2a":
                return R.id.p2a_icon;
            case "p2b":
                return R.id.p2b_icon;
            case "p2c":
                return R.id.p2c_icon;
            default:
                return 0;
        }
    }

    public int getSpriteNameid(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return R.id.p1a_pkm;
            case "p1b":
                return R.id.p1b_pkm;
            case "p1c":
                return R.id.p1c_pkm;
            case "p2a":
                return R.id.p2a_pkm;
            case "p2b":
                return R.id.p2b_pkm;
            case "p2c":
                return R.id.p2c_pkm;
            default:
                return 0;
        }
    }

    public int getIconId(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return R.id.icon1;
            case "p1b":
                return R.id.icon2;
            case "p1c":
                return R.id.icon3;
            case "p2a":
                return R.id.icon1_o;
            case "p2b":
                return R.id.icon2_o;
            case "p2c":
                return R.id.icon3_o;
            default:
                return 0;
        }
    }

    public int getIconId(String player, int id) {
        String p = player.substring(0, 2);
        switch (p) {
            case "p1":
                switch (id) {
                    case 0:
                        return R.id.icon1;
                    case 1:
                        return R.id.icon2;
                    case 2:
                        return R.id.icon3;
                    case 3:
                        return R.id.icon4;
                    case 4:
                        return R.id.icon5;
                    case 5:
                        return R.id.icon6;
                    default:
                        Log.d(BTAG, mPlayer1Team.toString());
                        Log.d(BTAG, mPlayer2Team.toString());
                        return R.id.icon1;
                }
            case "p2":
                switch (id) {
                    case 0:
                        return R.id.icon1_o;
                    case 1:
                        return R.id.icon2_o;
                    case 2:
                        return R.id.icon3_o;
                    case 3:
                        return R.id.icon4_o;
                    case 4:
                        return R.id.icon5_o;
                    case 5:
                        return R.id.icon6_o;
                    default:
                        Log.d(BTAG, mPlayer1Team.toString());
                        Log.d(BTAG, mPlayer2Team.toString());
                        return R.id.icon1_o;
                }
            default:
                Log.d(BTAG, mPlayer1Team.toString());
                Log.d(BTAG, mPlayer2Team.toString());
                return R.id.icon1;
        }
    }

    public int getGenderId(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return R.id.p1a_gender;
            case "p1b":
                return R.id.p1b_gender;
            case "p1c":
                return R.id.p1c_gender;
            case "p2a":
                return R.id.p2a_gender;
            case "p2b":
                return R.id.p2b_gender;
            case "p2c":
                return R.id.p2c_gender;
            default:
                return 0;
        }
    }

    public int getHpId(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return R.id.p1a_hp;
            case "p1b":
                return R.id.p1b_hp;
            case "p1c":
                return R.id.p1c_hp;
            case "p2a":
                return R.id.p2a_hp;
            case "p2b":
                return R.id.p2b_hp;
            case "p2c":
                return R.id.p2c_hp;
            default:
                return 0;
        }
    }

    public int getHpBarId(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return R.id.p1a_bar_hp;
            case "p1b":
                return R.id.p1b_bar_hp;
            case "p1c":
                return R.id.p1c_bar_hp;
            case "p2a":
                return R.id.p2a_bar_hp;
            case "p2b":
                return R.id.p2b_bar_hp;
            case "p2c":
                return R.id.p2c_bar_hp;
            default:
                return 0;
        }
    }

    public int getTempStatusId(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return R.id.p1a_temp_status;
            case "p1b":
                return R.id.p1b_temp_status;
            case "p1c":
                return R.id.p1c_temp_status;
            case "p2a":
                return R.id.p2a_temp_status;
            case "p2b":
                return R.id.p2b_temp_status;
            case "p2c":
                return R.id.p2c_temp_status;
            default:
                return 0;
        }
    }

    public int getOldHp(String tag) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                return progressBarHolder[0];
            case "p1b":
                return progressBarHolder[1];
            case "p1c":
                return progressBarHolder[2];
            case "p2a":
                return progressBarHolder[3];
            case "p2b":
                return progressBarHolder[4];
            case "p2c":
                return progressBarHolder[5];
            default:
                return 0;
        }
    }

    public void setOldHp(String tag, int hp) {
        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                progressBarHolder[0] = hp;
                break;
            case "p1b":
                progressBarHolder[1] = hp;
                break;
            case "p1c":
                progressBarHolder[2] = hp;
                break;
            case "p2a":
                progressBarHolder[3] = hp;
                break;
            case "p2b":
                progressBarHolder[4] = hp;
                break;
            case "p2c":
                progressBarHolder[5] = hp;
                break;
        }
    }

    public int getTeamSlot(String tag) {
        tag = Character.toString(tag.charAt(2));
        switch (tag) {
            case "a":
                return 0;
            case "b":
                return 1;
            case "c":
                return 2;
            default:
                return 0;
        }
    }

    public void setAddonStatus(String tag, String status) {
        if (getView() == null) {
            return;
        }
        LinearLayout statusBar = (LinearLayout) getView().findViewById(getTempStatusId(tag));

        TextView stt = new TextView(getActivity());
        stt.setTag(status);
        stt.setText(status.toUpperCase());
        stt.setTextSize(10);
        switch (status) {
            case "slp":
                stt.setBackgroundResource(R.drawable.editable_frame_blackwhite);
                break;
            case "psn":
            case "tox":
                stt.setBackgroundResource(R.drawable.editable_frame_light_purple);
                break;
            case "brn":
                stt.setBackgroundResource(R.drawable.editable_frame_light_red);
                break;
            case "par":
                stt.setBackgroundResource(R.drawable.editable_frame_light_orange);
                break;
            case "frz":
                stt.setBackgroundResource(R.drawable.editable_frame);
                break;
            default:
                stt.setBackgroundResource(R.drawable.editable_frame);
        }
        stt.setPadding(2, 2, 2, 2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stt.setLayoutParams(layoutParams);

        statusBar.addView(stt, 0);

    }

    public void removeAddonStatus(String tag, String status) {
        if (getView() == null) {
            return;
        }
        LinearLayout statusBar = (LinearLayout) getView().findViewById(getTempStatusId(tag));

        TextView stt = (TextView) statusBar.findViewWithTag(status);
        if (stt != null) {
            statusBar.removeView(stt);
        }
    }

    public int getSubstitute(String tag) {
        if (getView() == null) {
            return R.drawable.sprites_substitute;
        }

        tag = tag.substring(0, 2);
        switch (tag) {
            case "p1":
                return R.drawable.sprites_substitute_back;
            default:
                return R.drawable.sprites_substitute;
        }
    }

    public int getLastVisibleSpike(String tag, boolean nextInvisible) {
        if (getView() == null) {
            return R.id.field_spikes1;
        }

        tag = tag.substring(0, 2);
        switch (tag) {
            case "p1":
                View layer1 = getView().findViewById(R.id.field_spikes1);
                if (layer1.getVisibility() == View.INVISIBLE) {
                    return R.id.field_spikes1;
                } else {
                    View layer2 = getView().findViewById(R.id.field_spikes2);
                    if (layer2.getVisibility() == View.INVISIBLE) {
                        if (nextInvisible) {
                            return R.id.field_spikes2;
                        } else {
                            return R.id.field_spikes1;
                        }
                    } else {
                        View layer3 = getView().findViewById(R.id.field_spikes3);
                        if (layer3.getVisibility() == View.INVISIBLE) {
                            if (nextInvisible) {
                                return R.id.field_spikes3;
                            } else {
                                return R.id.field_spikes2;
                            }
                        } else {
                            return R.id.field_spikes3;
                        }
                    }
                }
            default:
                layer1 = getView().findViewById(R.id.field_spikes1_o);
                if (layer1.getVisibility() == View.INVISIBLE) {
                    return R.id.field_spikes1_o;
                } else {
                    View layer2 = getView().findViewById(R.id.field_spikes2_o);
                    if (layer2.getVisibility() == View.INVISIBLE) {
                        if (nextInvisible) {
                            return R.id.field_spikes2_o;
                        } else {
                            return R.id.field_spikes1_o;
                        }
                    } else {
                        View layer3 = getView().findViewById(R.id.field_spikes3_o);
                        if (layer3.getVisibility() == View.INVISIBLE) {
                            if (nextInvisible) {
                                return R.id.field_spikes3_o;
                            } else {
                                return R.id.field_spikes2_o;
                            }
                        } else {
                            return R.id.field_spikes3_o;
                        }
                    }
                }
        }
    }

    public int getLastVisibleTSpike(String tag, boolean nextInvisible) {
        if (getView() == null) {
            return R.id.field_tspikes1;
        }

        tag = tag.substring(0, 2);
        switch (tag) {
            case "p1":
                View layer1 = getView().findViewById(R.id.field_tspikes1);
                if (layer1.getVisibility() == View.INVISIBLE) {
                    return R.id.field_tspikes1;
                } else {
                    View layer2 = getView().findViewById(R.id.field_tspikes2);
                    if (layer2.getVisibility() == View.INVISIBLE) {
                        if (nextInvisible) {
                            return R.id.field_tspikes2;
                        } else {
                            return R.id.field_tspikes1;
                        }
                    } else {
                        return R.id.field_tspikes2;
                    }
                }
            default:
                layer1 = getView().findViewById(R.id.field_tspikes1_o);
                if (layer1.getVisibility() == View.INVISIBLE) {
                    return R.id.field_tspikes1_o;
                } else {
                    View layer2 = getView().findViewById(R.id.field_tspikes2_o);
                    if (layer2.getVisibility() == View.INVISIBLE) {
                        if (nextInvisible) {
                            return R.id.field_tspikes2_o;
                        } else {
                            return R.id.field_tspikes1_o;
                        }
                    } else {
                        return R.id.field_tspikes2_o;
                    }
                }
        }
    }

    public void hidePokemon(String tag) {
        if (getView() == null) {
            return;
        }

        RelativeLayout relativeLayout;
        int layoutId;

        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                layoutId = R.id.p1a;
                break;
            case "p1b":
                layoutId = R.id.p1b;
                break;
            case "p1c":
                layoutId = R.id.p1c;
                break;
            case "p2a":
                layoutId = R.id.p2a;
                break;
            case "p2b":
                layoutId = R.id.p2b;
                break;
            case "p2c":
                layoutId = R.id.p2c;
                break;
            default:
                layoutId = R.id.p2c;
        }

        relativeLayout = (RelativeLayout) getView().findViewById(layoutId);
        relativeLayout.setVisibility(View.INVISIBLE);
    }

    public void displayPokemon(String tag) {
        if (getView() == null) {
            return;
        }

        RelativeLayout relativeLayout;
        int layoutId;

        tag = tag.substring(0, 3);
        switch (tag) {
            case "p1a":
                layoutId = R.id.p1a;
                ((LinearLayout) getView().findViewById(R.id.p1a_temp_status)).removeAllViews();
                break;
            case "p1b":
                layoutId = R.id.p1b;
                ((LinearLayout) getView().findViewById(R.id.p1b_temp_status)).removeAllViews();
                break;
            case "p1c":
                layoutId = R.id.p1c;
                ((LinearLayout) getView().findViewById(R.id.p1c_temp_status)).removeAllViews();
                break;
            case "p2a":
                layoutId = R.id.p2a;
                ((LinearLayout) getView().findViewById(R.id.p2a_temp_status)).removeAllViews();
                break;
            case "p2b":
                layoutId = R.id.p2b;
                ((LinearLayout) getView().findViewById(R.id.p2b_temp_status)).removeAllViews();
                break;
            default:
                layoutId = R.id.p2c;
                ((LinearLayout) getView().findViewById(R.id.p2c_temp_status)).removeAllViews();
        }
        relativeLayout = (RelativeLayout) getView().findViewById(layoutId);
        relativeLayout.setVisibility(View.VISIBLE);
        getView().findViewById(getSpriteId(tag)).setAlpha(1f);
        ImageView sub = (ImageView) relativeLayout.findViewWithTag("Substitute");
        if (sub != null) {
            relativeLayout.removeView(sub);
        }
    }

    public int processHpFraction(String hpFraction) {
        int status = hpFraction.indexOf(' ');
        hpFraction = (status == -1) ? hpFraction : hpFraction.substring(status);
        int fraction = hpFraction.indexOf('/');
        if (fraction == -1) {
            return 0;
        } else {
            int remaining = Integer.parseInt(hpFraction.substring(0, fraction));
            int total = Integer.parseInt(hpFraction.substring(fraction + 1));
            return (int) (((float) remaining / (float) total) * 100);
        }
    }

    public void replacePokemon(String playerTag, String oldPkm, String newPkm) {
        if (playerTag.startsWith("p1")) {
            int index = findPokemonInTeam(getTeamNameArrayList(mPlayer1Team), oldPkm);
            if (index != -1) {
                mPlayer1Team.put(index, new PokemonInfo(getActivity(), newPkm));
            }
        } else {
            int index = findPokemonInTeam(getTeamNameArrayList(mPlayer2Team), oldPkm);
            if (index != -1) {
                mPlayer2Team.put(index, new PokemonInfo(getActivity(), newPkm));
            }
        }
    }

    public HashMap<Integer, PokemonInfo> getTeam(String playerTag) {
        if (playerTag.startsWith("p1")) {
            return mPlayer1Team;
        } else {
            return mPlayer2Team;
        }
    }

    public void setTeam(String playerTag, HashMap<Integer, PokemonInfo> playerTeam) {
        if (playerTag.startsWith("p1")) {
            mPlayer1Team = playerTeam;
        } else {
            mPlayer2Team = playerTeam;
        }
    }

    public String[] getTeamName(HashMap<Integer, PokemonInfo> teamMap) {
        String[] team = new String[teamMap.size()];
        for (Integer i = 0; i < teamMap.size(); i++) {
            PokemonInfo pkm = teamMap.get(i);
            team[i] = pkm.getName();
        }
        return team;
    }

    public ArrayList<String> getTeamNameArrayList(HashMap<Integer, PokemonInfo> teamMap) {
        ArrayList<String> team = new ArrayList<>();
        for (Integer i = 0; i < teamMap.size(); i++) {
            PokemonInfo pkm = teamMap.get(i);
            team.add(pkm.getName());
        }
        return team;
    }

    public int findPokemonInTeam(ArrayList<String> playerTeam, String pkm) {
        String[] specialPkm = {"Arceus", "Gourgeist", "Genesect", "Pumpkaboo"};
        boolean special = false;
        String species = "";
        for (String sp : specialPkm) {
            if (pkm.contains(sp)) {
                special = true;
                species = sp;
                break;
            }
        }
        if (!special) {
            return playerTeam.indexOf(pkm);
        } else {
            for (int i = 0; i < playerTeam.size(); i++) {
                if (playerTeam.get(i).contains(species)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public String getPrintableOutputPokemonSide(String split) {
        return getPrintableOutputPokemonSide(split, true);
    }

    public String getPrintableOutputPokemonSide(String split, boolean start) {
        StringBuilder sb = new StringBuilder();
        if (split.startsWith("p2")) {
            if (start) {
                sb.append("The opposing ");
            } else {
                sb.append("the opposing ");
            }
        }

        int separator = split.indexOf(':');
        String toAppend = (separator == -1) ? split.trim() : split.substring(separator + 1).trim();
        sb.append(toAppend);
        return sb.toString();
    }

    public String getPrintable(String split) {
        int separator = split.indexOf(':');
        return (separator == -1) ? split.trim() : split.substring(separator + 1).trim();
    }

    public void processBoost(String playerTag, String stat, int boost) {
        if (getView() == null) {
            return;
        }
        LinearLayout tempStat = (LinearLayout) getView().findViewById(getTempStatusId(playerTag));
        TextView statBoost;
        int currentBoost;
        int index;
        if (tempStat.findViewWithTag(stat) == null) {
            statBoost = new TextView(getActivity());
            statBoost.setTag(stat);
            statBoost.setTextSize(10);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            statBoost.setLayoutParams(layoutParams);
            currentBoost = boost;
            index = tempStat.getChildCount();
        } else {
            statBoost = (TextView) tempStat.findViewWithTag(stat);
            index = tempStat.indexOfChild(statBoost);
            tempStat.removeView(statBoost);
            String boostDetail = statBoost.getText().toString();
            currentBoost = Integer.parseInt(boostDetail.substring(0, boostDetail.indexOf(" "))) + boost;
        }
        if (currentBoost == 0) {
            return;
        } else {
            if (currentBoost > 0) {
                statBoost.setBackgroundResource(R.drawable.editable_frame);
            } else {
                statBoost.setBackgroundResource(R.drawable.editable_frame_light_orange);
            }
        }
        statBoost.setText(Integer.toString(currentBoost) + " " + stat.substring(0, 1).toUpperCase() + stat.substring(1));
        statBoost.setPadding(2, 2, 2, 2);
        tempStat.addView(statBoost, index);
    }

    public void invertBoost(String playerTag, String[] stats) {
        if (getView() == null) {
            return;
        }
        LinearLayout tempStat = (LinearLayout) getView().findViewById(getTempStatusId(playerTag));
        for (String stat : stats) {
            TextView statBoost = (TextView) tempStat.findViewWithTag(stat);
            if (statBoost != null) {
                String boostDetail = statBoost.getText().toString();
                int currentBoost = -1 * Integer.parseInt(boostDetail.substring(0, boostDetail.indexOf(" ")));
                statBoost.setText(Integer.toString(currentBoost) + boostDetail.substring(boostDetail.indexOf(" ")));
            }
        }
    }

    public void swapBoost(String org, String dest, String... stats) {
        org = org.substring(0, 3);
        dest = dest.substring(0, 3);
        if (getView() == null) {
            return;
        }

        LinearLayout orgTempStat = (LinearLayout) getView().findViewById(getTempStatusId(org));
        LinearLayout destTempStat = (LinearLayout) getView().findViewById(getTempStatusId(dest));

        for (String stat : stats) {
            TextView orgStat = (TextView) orgTempStat.findViewWithTag(stat);
            int orgIndex = orgTempStat.indexOfChild(orgStat);
            TextView destStat = (TextView) destTempStat.findViewWithTag(stat);
            int destIndex = destTempStat.indexOfChild(destStat);
            orgIndex = (orgIndex == -1) ? orgTempStat.getChildCount() : orgIndex;
            orgTempStat.removeView(orgStat);
            destIndex = (destIndex == -1) ? destTempStat.getChildCount() : destIndex;
            destTempStat.removeView(destStat);

            if (destStat != null) {
                orgTempStat.addView(destStat, orgIndex);
            }
            if (orgStat != null) {
                destTempStat.addView(orgStat, destIndex);
            }
        }
    }

    public void copyBoost(String org, String dest) {
        org = org.substring(0, 3);
        dest = dest.substring(0, 3);
        if (getView() == null) {
            return;
        }

        LinearLayout orgTempStat = (LinearLayout) getView().findViewById(getTempStatusId(org));
        LinearLayout destTempStat = (LinearLayout) getView().findViewById(getTempStatusId(dest));

        for (String stat : stats) {
            TextView orgStat = (TextView) orgTempStat.findViewWithTag(stat);
            if (orgStat != null) {
                TextView destStat = new TextView(getActivity());
                destStat.setTag(stat);
                destStat.setPadding(2, 2, 2, 2);
                destStat.setTextSize(10);
                destStat.setText(orgStat.getText());
                destStat.setBackground(orgStat.getBackground());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                destStat.setLayoutParams(layoutParams);
                destTempStat.addView(destStat);
            }
        }
    }

    public AnimatorSet createFlyingMessage(final String tag, AnimatorSet toast, final Spannable message) {
        if (getView() == null) {
            return null;
        }
        message.setSpan(new RelativeSizeSpan(0.8f), 0, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        final TextView flyingMessage = new TextView(getActivity());
        flyingMessage.setText(message);
        flyingMessage.setBackgroundResource(R.drawable.editable_frame);
        flyingMessage.setPadding(2, 2, 2, 2);
        flyingMessage.setAlpha(0f);

        toast.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (getView() == null) {
                    return;
                }
                ImageView imageView = (ImageView) getView().findViewById(getSpriteId(tag));

                RelativeLayout relativeLayout = (RelativeLayout) getView().findViewById(getPkmLayoutId(tag));
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.addRule(RelativeLayout.ALIGN_TOP, getSpriteId(tag));
                layoutParams.addRule(RelativeLayout.ALIGN_LEFT, getSpriteId(tag));
                layoutParams.setMargins((int) (imageView.getWidth() * 0.25f), (int) (imageView.getHeight() * 0.5f), 0, 0);
                relativeLayout.addView(flyingMessage, layoutParams);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (getView() == null) {
                    return;
                }

                RelativeLayout relativeLayout = (RelativeLayout) getView().findViewById(getPkmLayoutId(tag));
                relativeLayout.removeView(flyingMessage);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        ObjectAnimator flyingObject = ObjectAnimator.ofFloat(flyingMessage, "y", flyingMessage.getY());
        flyingObject.setDuration(ANIMATION_SHORT);
        flyingObject.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(flyingMessage, "alpha", 0f, 1f);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(ANIMATION_SHORT / 4);

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(flyingMessage, "alpha", 1f, 0f);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setStartDelay(ANIMATION_SHORT / 2);
        fadeOut.setDuration(ANIMATION_SHORT / 4);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(toast);
        animatorSet.play(fadeIn).with(toast);
        animatorSet.play(flyingObject).after(fadeIn);
        animatorSet.play(fadeOut).after(fadeIn);

        return animatorSet;
    }

}
