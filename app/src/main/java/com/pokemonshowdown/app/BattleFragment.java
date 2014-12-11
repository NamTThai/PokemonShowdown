package com.pokemonshowdown.app;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pokemonshowdown.data.BattleFieldData;
import com.pokemonshowdown.data.BattleMessage;
import com.pokemonshowdown.data.PokemonInfo;
import com.pokemonshowdown.data.ServerRequest;

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
    public final static String[] STATS = {"atk", "def", "spa", "spd", "spe", "accuracy", "evasion"};
    public final static String[] STTUS = {"psn", "tox", "frz", "par", "slp", "brn"};
    public final static String[][] TEAMMATES = {{"p1a", "p1b", "p1c"}, {"p2a", "p2b", "p2c"}};
    public final static String[] MORPHS = {"Arceus", "Gourgeist", "Genesect", "Pumpkaboo"};

    public enum ViewBundle {
        PLAYER1_NAME, PLAYER1_AVATAR, PLAYER2_NAME, PLAYER2_AVATAR,
        BATTLE_BACKGROUND, WEATHER_BACKGROUND, TURN, WEATHER, TEAMPREVIEW,
        ICON1, ICON2, ICON3, ICON4, ICON5, ICON6,
        ICON1_O, ICON2_O, ICON3_O, ICON4_O, ICON5_O, ICON6_O,
        P1A_PREV, P1B_PREV, P1C_PREV, P1D_PREV, P1E_PREV, P1F_PREV,
        P2A_PREV, P2B_PREV, P2C_PREV, P2D_PREV, P2E_PREV, P2F_PREV,
        REFLECT, LIGHTSCREEN, ROCKS, SPIKE1, SPIKE2, SPIKE3, TSPIKE1, TSPIKE2,
        REFLECT_O, LIGHTSCREEN_O, ROCKS_O, SPIKE1_O, SPIKE2_O, SPIKE3_O, TSPIKE1_O, TSPIKE2_O,
        P1A, P1A_PKM, P1A_GENDER, P1A_HP, P1A_HP_BAR, P1A_SPRITE, P1A_STATUS,
        P1B, P1B_PKM, P1B_GENDER, P1B_HP, P1B_HP_BAR, P1B_SPRITE, P1B_STATUS,
        P1C, P1C_PKM, P1C_GENDER, P1C_HP, P1C_HP_BAR, P1C_SPRITE, P1C_STATUS,
        P2A, P2A_PKM, P2A_GENDER, P2A_HP, P2A_HP_BAR, P2A_SPRITE, P2A_STATUS,
        P2B, P2B_PKM, P2B_GENDER, P2B_HP, P2B_HP_BAR, P2B_SPRITE, P2B_STATUS,
        P2C, P2C_PKM, P2C_GENDER, P2C_HP, P2C_HP_BAR, P2C_SPRITE, P2C_STATUS,
        ANIMATION_QUEUE
    }

    private ArrayDeque<AnimatorSet> mAnimatorSetQueue;
    public int[] progressBarHolder = new int[6];

    private String mRoomId;
    /**
     * 0 if it's a simple watch battle
     * 1 if player is p1
     * -1 if player is p2
     */
    private int mBattling;
    private String mPlayer1;
    private String mPlayer2;
    private ArrayList<PokemonInfo> mPlayer1Team = new ArrayList<>();
    private ArrayList<PokemonInfo> mPlayer2Team = new ArrayList<>();

    private String mCurrentWeather;
    private boolean mWeatherExist;
    private int mTurnNumber;
    private boolean mMyTurn;
    private boolean mTeamPreview = false;

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
        BattleFieldData.RoomData roomData = BattleFieldData.get(getActivity()).getAnimationInstance(mRoomId);
        if (roomData != null) {
            roomData.setMessageListener(false);

            ArrayList<String> pendingMessages = roomData.getServerMessageOnHold();
            for (String message : pendingMessages) {
                processServerMessage(message);
            }

            roomData.clearServerMessageOnHold();

            if (getView() != null) {
                HashMap<ViewBundle, Object> viewBundle = roomData.getViewBundle();
                if (viewBundle != null) {
                    ((TextView) getView().findViewById(R.id.username))
                            .setText((CharSequence) viewBundle.get(ViewBundle.PLAYER1_NAME));
                    ((ImageView) getView().findViewById(R.id.avatar))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.PLAYER1_AVATAR));
                    ((TextView) getView().findViewById(R.id.username_o))
                            .setText((CharSequence) viewBundle.get(ViewBundle.PLAYER2_NAME));
                    ((ImageView) getView().findViewById(R.id.avatar_o))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.PLAYER2_AVATAR));
                    ((ImageView) getView().findViewById(R.id.battle_background))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.BATTLE_BACKGROUND));
                    ((ImageView) getView().findViewById(R.id.weather_background))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.WEATHER_BACKGROUND));
                    ((ImageView) getView().findViewById(R.id.icon1))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON1));
                    ((ImageView) getView().findViewById(R.id.icon2))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON2));
                    ((ImageView) getView().findViewById(R.id.icon3))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON3));
                    ((ImageView) getView().findViewById(R.id.icon4))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON4));
                    ((ImageView) getView().findViewById(R.id.icon5))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON5));
                    ((ImageView) getView().findViewById(R.id.icon6))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON6));
                    ((ImageView) getView().findViewById(R.id.icon1_o))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON1_O));
                    ((ImageView) getView().findViewById(R.id.icon2_o))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON2_O));
                    ((ImageView) getView().findViewById(R.id.icon3_o))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON3_O));
                    ((ImageView) getView().findViewById(R.id.icon4_o))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON4_O));
                    ((ImageView) getView().findViewById(R.id.icon5_o))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON5_O));
                    ((ImageView) getView().findViewById(R.id.icon6_o))
                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.ICON6_O));
                    Boolean isTeamPreview = (Boolean) viewBundle.get(ViewBundle.TEAMPREVIEW);
                    FrameLayout frameLayout = (FrameLayout) getView().findViewById(R.id.battle_interface);
                    frameLayout.removeAllViews();
                    if (isTeamPreview) {
                        setTeamPreview(true);
                        getActivity().getLayoutInflater()
                                .inflate(R.layout.fragment_battle_teampreview, frameLayout);
                        ((ImageView) getView().findViewById(R.id.p1a_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1A_PREV));
                        ((ImageView) getView().findViewById(R.id.p1b_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1B_PREV));
                        ((ImageView) getView().findViewById(R.id.p1c_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1C_PREV));
                        ((ImageView) getView().findViewById(R.id.p1d_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1D_PREV));
                        ((ImageView) getView().findViewById(R.id.p1e_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1E_PREV));
                        ((ImageView) getView().findViewById(R.id.p1f_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1F_PREV));
                        ((ImageView) getView().findViewById(R.id.p2a_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2A_PREV));
                        ((ImageView) getView().findViewById(R.id.p2b_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2B_PREV));
                        ((ImageView) getView().findViewById(R.id.p2c_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2C_PREV));
                        ((ImageView) getView().findViewById(R.id.p2d_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2D_PREV));
                        ((ImageView) getView().findViewById(R.id.p2e_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2E_PREV));
                        ((ImageView) getView().findViewById(R.id.p2f_prev))
                                .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2F_PREV));
                    } else {
                        setTeamPreview(false);
                        getActivity().getLayoutInflater()
                                .inflate(R.layout.fragment_battle_animation, frameLayout);
                        getView().findViewById(R.id.turn).setVisibility(View.VISIBLE);
                        ((TextView) getView().findViewById(R.id.turn))
                                .setText((CharSequence) viewBundle.get(ViewBundle.TURN));
                        ((TextView) getView().findViewById(R.id.weather))
                                .setText((CharSequence) viewBundle.get(ViewBundle.WEATHER));
                        setVisibility(getView().findViewById(R.id.field_reflect),
                                (Integer) viewBundle.get(ViewBundle.REFLECT));
                        setVisibility(getView().findViewById(R.id.field_lightscreen),
                                (Integer) viewBundle.get(ViewBundle.LIGHTSCREEN));
                        setVisibility(getView().findViewById(R.id.field_rocks),
                                (Integer) viewBundle.get(ViewBundle.ROCKS));
                        setVisibility(getView().findViewById(R.id.field_spikes1),
                                (Integer) viewBundle.get(ViewBundle.SPIKE1));
                        setVisibility(getView().findViewById(R.id.field_spikes2),
                                (Integer) viewBundle.get(ViewBundle.SPIKE2));
                        setVisibility(getView().findViewById(R.id.field_spikes3),
                                (Integer) viewBundle.get(ViewBundle.SPIKE3));
                        setVisibility(getView().findViewById(R.id.field_tspikes1),
                                (Integer) viewBundle.get(ViewBundle.TSPIKE1));
                        setVisibility(getView().findViewById(R.id.field_tspikes2),
                                (Integer) viewBundle.get(ViewBundle.TSPIKE2));
                        setVisibility(getView().findViewById(R.id.field_reflect_o),
                                (Integer) viewBundle.get(ViewBundle.REFLECT_O));
                        setVisibility(getView().findViewById(R.id.field_lightscreen_o),
                                (Integer) viewBundle.get(ViewBundle.LIGHTSCREEN_O));
                        setVisibility(getView().findViewById(R.id.field_rocks_o),
                                (Integer) viewBundle.get(ViewBundle.ROCKS_O));
                        setVisibility(getView().findViewById(R.id.field_spikes1_o),
                                (Integer) viewBundle.get(ViewBundle.SPIKE1_O));
                        setVisibility(getView().findViewById(R.id.field_spikes2_o),
                                (Integer) viewBundle.get(ViewBundle.SPIKE2_O));
                        setVisibility(getView().findViewById(R.id.field_spikes3_o),
                                (Integer) viewBundle.get(ViewBundle.SPIKE3_O));
                        setVisibility(getView().findViewById(R.id.field_tspikes1_o),
                                (Integer) viewBundle.get(ViewBundle.TSPIKE1_O));
                        setVisibility(getView().findViewById(R.id.field_tspikes2_o),
                                (Integer) viewBundle.get(ViewBundle.TSPIKE2_O));
                        if ((Boolean) viewBundle.get(ViewBundle.P1A)) {
                            getView().findViewById(R.id.p1a).setVisibility(View.VISIBLE);
                            ((TextView) getView().findViewById(R.id.p1a_pkm))
                                    .setText((CharSequence) viewBundle.get(ViewBundle.P1A_PKM));
                            ((ImageView) getView().findViewById(R.id.p1a_gender))
                                    .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1A_GENDER));
                            ((TextView) getView().findViewById(R.id.p1a_hp))
                                    .setText((CharSequence) viewBundle.get(ViewBundle.P1A_HP));
                            ((ProgressBar) getView().findViewById(R.id.p1a_bar_hp))
                                    .setProgress((Integer) viewBundle.get(ViewBundle.P1A_HP_BAR));
                            ((ImageView) getView().findViewById(R.id.p1a_icon))
                                    .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1A_SPRITE));
                            addAllChild((LinearLayout) getView().findViewById(R.id.p1a_temp_status),
                                    (View[]) viewBundle.get(ViewBundle.P1A_STATUS));
                            if ((Boolean) viewBundle.get(ViewBundle.P1B)) {
                                getView().findViewById(R.id.p1b).setVisibility(View.VISIBLE);
                                ((TextView) getView().findViewById(R.id.p1b_pkm))
                                        .setText((CharSequence) viewBundle.get(ViewBundle.P1B_PKM));
                                ((ImageView) getView().findViewById(R.id.p1b_gender))
                                        .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1B_GENDER));
                                ((TextView) getView().findViewById(R.id.p1b_hp))
                                        .setText((CharSequence) viewBundle.get(ViewBundle.P1B_HP));
                                ((ProgressBar) getView().findViewById(R.id.p1b_bar_hp))
                                        .setProgress((Integer) viewBundle.get(ViewBundle.P1B_HP_BAR));
                                ((ImageView) getView().findViewById(R.id.p1b_icon))
                                        .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1B_SPRITE));
                                addAllChild((LinearLayout) getView().findViewById(R.id.p1b_temp_status),
                                        (View[]) viewBundle.get(ViewBundle.P1B_STATUS));
                                if ((Boolean) viewBundle.get(ViewBundle.P1C)) {
                                    getView().findViewById(R.id.p1c).setVisibility(View.VISIBLE);
                                    ((TextView) getView().findViewById(R.id.p1c_pkm))
                                            .setText((CharSequence) viewBundle.get(ViewBundle.P1C_PKM));
                                    ((ImageView) getView().findViewById(R.id.p1c_gender))
                                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1C_GENDER));
                                    ((TextView) getView().findViewById(R.id.p1c_hp))
                                            .setText((CharSequence) viewBundle.get(ViewBundle.P1C_HP));
                                    ((ProgressBar) getView().findViewById(R.id.p1c_bar_hp))
                                            .setProgress((Integer) viewBundle.get(ViewBundle.P1C_HP_BAR));
                                    ((ImageView) getView().findViewById(R.id.p1c_icon))
                                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P1C_SPRITE));
                                    addAllChild((LinearLayout) getView().findViewById(R.id.p1c_temp_status),
                                            (View[]) viewBundle.get(ViewBundle.P1C_STATUS));
                                }
                            }
                        }
                        if ((Boolean) viewBundle.get(ViewBundle.P2A)) {
                            getView().findViewById(R.id.p2a).setVisibility(View.VISIBLE);
                            ((TextView) getView().findViewById(R.id.p2a_pkm))
                                    .setText((CharSequence) viewBundle.get(ViewBundle.P2A_PKM));
                            ((ImageView) getView().findViewById(R.id.p2a_gender))
                                    .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2A_GENDER));
                            ((TextView) getView().findViewById(R.id.p2a_hp))
                                    .setText((CharSequence) viewBundle.get(ViewBundle.P2A_HP));
                            ((ProgressBar) getView().findViewById(R.id.p2a_bar_hp))
                                    .setProgress((Integer) viewBundle.get(ViewBundle.P2A_HP_BAR));
                            ((ImageView) getView().findViewById(R.id.p2a_icon))
                                    .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2A_SPRITE));
                            addAllChild((LinearLayout) getView().findViewById(R.id.p2a_temp_status),
                                    (View[]) viewBundle.get(ViewBundle.P2A_STATUS));
                            if ((Boolean) viewBundle.get(ViewBundle.P2B)) {
                                getView().findViewById(R.id.p2b).setVisibility(View.VISIBLE);
                                ((TextView) getView().findViewById(R.id.p2b_pkm))
                                        .setText((CharSequence) viewBundle.get(ViewBundle.P2B_PKM));
                                ((ImageView) getView().findViewById(R.id.p2b_gender))
                                        .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2B_GENDER));
                                ((TextView) getView().findViewById(R.id.p2b_hp))
                                        .setText((CharSequence) viewBundle.get(ViewBundle.P2B_HP));
                                ((ProgressBar) getView().findViewById(R.id.p2b_bar_hp))
                                        .setProgress((Integer) viewBundle.get(ViewBundle.P2B_HP_BAR));
                                ((ImageView) getView().findViewById(R.id.p2b_icon))
                                        .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2B_SPRITE));
                                addAllChild((LinearLayout) getView().findViewById(R.id.p2b_temp_status),
                                        (View[]) viewBundle.get(ViewBundle.P2B_STATUS));
                                if ((Boolean) viewBundle.get(ViewBundle.P2C)) {
                                    getView().findViewById(R.id.p2c).setVisibility(View.VISIBLE);
                                    ((TextView) getView().findViewById(R.id.p2c_pkm))
                                            .setText((CharSequence) viewBundle.get(ViewBundle.P2C_PKM));
                                    ((ImageView) getView().findViewById(R.id.p2c_gender))
                                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2C_GENDER));
                                    ((TextView) getView().findViewById(R.id.p2c_hp))
                                            .setText((CharSequence) viewBundle.get(ViewBundle.P2C_HP));
                                    ((ProgressBar) getView().findViewById(R.id.p2c_bar_hp))
                                            .setProgress((Integer) viewBundle.get(ViewBundle.P2C_HP_BAR));
                                    ((ImageView) getView().findViewById(R.id.p2c_icon))
                                            .setImageDrawable((Drawable) viewBundle.get(ViewBundle.P2C_SPRITE));
                                    addAllChild((LinearLayout) getView().findViewById(R.id.p2c_temp_status),
                                            (View[]) viewBundle.get(ViewBundle.P2C_STATUS));
                                }
                            }
                        }
                    }

                    mAnimatorSetQueue = (ArrayDeque<AnimatorSet>) viewBundle.get(ViewBundle.ANIMATION_QUEUE);
                    if (mAnimatorSetQueue != null && !mAnimatorSetQueue.isEmpty()) {
                        mAnimatorSetQueue.peekFirst().start();
                    }
                    roomData.setViewBundle(null);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        BattleFieldData.RoomData roomData = BattleFieldData.get(getActivity()).getAnimationInstance(mRoomId);
        if (roomData != null) {
            roomData.setMessageListener(true);

            if (getView() != null) {
                HashMap<ViewBundle, Object> viewBundle = new HashMap<>();
                viewBundle.put(ViewBundle.PLAYER1_NAME,
                        ((TextView) getView().findViewById(R.id.username)).getText());
                viewBundle.put(ViewBundle.PLAYER1_AVATAR,
                        ((ImageView) getView().findViewById(R.id.avatar)).getDrawable());
                viewBundle.put(ViewBundle.PLAYER2_NAME,
                        ((TextView) getView().findViewById(R.id.username_o)).getText());
                viewBundle.put(ViewBundle.PLAYER2_AVATAR,
                        ((ImageView) getView().findViewById(R.id.avatar_o)).getDrawable());
                viewBundle.put(ViewBundle.BATTLE_BACKGROUND,
                        ((ImageView) getView().findViewById(R.id.battle_background)).getDrawable());
                viewBundle.put(ViewBundle.WEATHER_BACKGROUND,
                        ((ImageView) getView().findViewById(R.id.weather_background)).getDrawable());
                viewBundle.put(ViewBundle.ICON1,
                        ((ImageView) getView().findViewById(R.id.icon1)).getDrawable());
                viewBundle.put(ViewBundle.ICON2,
                        ((ImageView) getView().findViewById(R.id.icon2)).getDrawable());
                viewBundle.put(ViewBundle.ICON3,
                        ((ImageView) getView().findViewById(R.id.icon3)).getDrawable());
                viewBundle.put(ViewBundle.ICON4,
                        ((ImageView) getView().findViewById(R.id.icon4)).getDrawable());
                viewBundle.put(ViewBundle.ICON5,
                        ((ImageView) getView().findViewById(R.id.icon5)).getDrawable());
                viewBundle.put(ViewBundle.ICON6,
                        ((ImageView) getView().findViewById(R.id.icon6)).getDrawable());
                viewBundle.put(ViewBundle.ICON1_O,
                        ((ImageView) getView().findViewById(R.id.icon1_o)).getDrawable());
                viewBundle.put(ViewBundle.ICON2_O,
                        ((ImageView) getView().findViewById(R.id.icon2_o)).getDrawable());
                viewBundle.put(ViewBundle.ICON3_O,
                        ((ImageView) getView().findViewById(R.id.icon3_o)).getDrawable());
                viewBundle.put(ViewBundle.ICON4_O,
                        ((ImageView) getView().findViewById(R.id.icon4_o)).getDrawable());
                viewBundle.put(ViewBundle.ICON5_O,
                        ((ImageView) getView().findViewById(R.id.icon5_o)).getDrawable());
                viewBundle.put(ViewBundle.ICON6_O,
                        ((ImageView) getView().findViewById(R.id.icon6_o)).getDrawable());
                if (isTeamPreview()) {
                    viewBundle.put(ViewBundle.TEAMPREVIEW, true);
                    viewBundle.put(ViewBundle.P1A_PREV,
                            ((ImageView) getView().findViewById(R.id.p1a_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P1B_PREV,
                            ((ImageView) getView().findViewById(R.id.p1b_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P1C_PREV,
                            ((ImageView) getView().findViewById(R.id.p1c_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P1D_PREV,
                            ((ImageView) getView().findViewById(R.id.p1d_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P1E_PREV,
                            ((ImageView) getView().findViewById(R.id.p1e_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P1F_PREV,
                            ((ImageView) getView().findViewById(R.id.p1f_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P2A_PREV,
                            ((ImageView) getView().findViewById(R.id.p2a_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P2B_PREV,
                            ((ImageView) getView().findViewById(R.id.p2b_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P2C_PREV,
                            ((ImageView) getView().findViewById(R.id.p2c_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P2D_PREV,
                            ((ImageView) getView().findViewById(R.id.p2d_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P2E_PREV,
                            ((ImageView) getView().findViewById(R.id.p2e_prev)).getDrawable());
                    viewBundle.put(ViewBundle.P2F_PREV,
                            ((ImageView) getView().findViewById(R.id.p2f_prev)).getDrawable());
                } else {
                    viewBundle.put(ViewBundle.TEAMPREVIEW, false);
                    viewBundle.put(ViewBundle.TURN,
                            ((TextView) getView().findViewById(R.id.turn)).getText());
                    viewBundle.put(ViewBundle.WEATHER,
                            ((TextView) getView().findViewById(R.id.weather)).getText());
                    viewBundle.put(ViewBundle.REFLECT,
                            getView().findViewById(R.id.field_reflect).getVisibility());
                    viewBundle.put(ViewBundle.LIGHTSCREEN,
                            getView().findViewById(R.id.field_lightscreen).getVisibility());
                    viewBundle.put(ViewBundle.ROCKS,
                            getView().findViewById(R.id.field_rocks).getVisibility());
                    viewBundle.put(ViewBundle.SPIKE1,
                            getView().findViewById(R.id.field_spikes1).getVisibility());
                    viewBundle.put(ViewBundle.SPIKE2,
                            getView().findViewById(R.id.field_spikes2).getVisibility());
                    viewBundle.put(ViewBundle.SPIKE3,
                            getView().findViewById(R.id.field_spikes3).getVisibility());
                    viewBundle.put(ViewBundle.TSPIKE1,
                            getView().findViewById(R.id.field_tspikes1).getVisibility());
                    viewBundle.put(ViewBundle.TSPIKE2,
                            getView().findViewById(R.id.field_tspikes2).getVisibility());
                    viewBundle.put(ViewBundle.REFLECT_O,
                            getView().findViewById(R.id.field_reflect_o).getVisibility());
                    viewBundle.put(ViewBundle.LIGHTSCREEN_O,
                            getView().findViewById(R.id.field_lightscreen_o).getVisibility());
                    viewBundle.put(ViewBundle.ROCKS_O,
                            getView().findViewById(R.id.field_rocks_o).getVisibility());
                    viewBundle.put(ViewBundle.SPIKE1_O,
                            getView().findViewById(R.id.field_spikes1_o).getVisibility());
                    viewBundle.put(ViewBundle.SPIKE2_O,
                            getView().findViewById(R.id.field_spikes2_o).getVisibility());
                    viewBundle.put(ViewBundle.SPIKE3_O,
                            getView().findViewById(R.id.field_spikes3_o).getVisibility());
                    viewBundle.put(ViewBundle.TSPIKE1_O,
                            getView().findViewById(R.id.field_tspikes1_o).getVisibility());
                    viewBundle.put(ViewBundle.TSPIKE2_O,
                            getView().findViewById(R.id.field_tspikes2_o).getVisibility());
                    if (getView().findViewById(R.id.p1a).getVisibility() == View.VISIBLE) {
                        viewBundle.put(ViewBundle.P1A, true);
                        viewBundle.put(ViewBundle.P1A_PKM,
                                ((TextView) getView().findViewById(R.id.p1a_pkm)).getText());
                        viewBundle.put(ViewBundle.P1A_GENDER,
                                ((ImageView) getView().findViewById(R.id.p1a_gender)).getDrawable());
                        viewBundle.put(ViewBundle.P1A_HP,
                                ((TextView) getView().findViewById(R.id.p1a_hp)).getText());
                        viewBundle.put(ViewBundle.P1A_HP_BAR,
                                ((ProgressBar) getView().findViewById(R.id.p1a_bar_hp)).getProgress());
                        viewBundle.put(ViewBundle.P1A_SPRITE,
                                ((ImageView) getView().findViewById(R.id.p1a_icon)).getDrawable());
                        viewBundle.put(ViewBundle.P1A_STATUS,
                                getAllChild((LinearLayout) getView().findViewById(R.id.p1a_temp_status)));
                        if (getView().findViewById(R.id.p1a).getVisibility() == View.VISIBLE) {
                            viewBundle.put(ViewBundle.P1B, true);
                            viewBundle.put(ViewBundle.P1B_PKM,
                                    ((TextView) getView().findViewById(R.id.p1a_pkm)).getText());
                            viewBundle.put(ViewBundle.P1B_GENDER,
                                    ((ImageView) getView().findViewById(R.id.p1a_gender)).getDrawable());
                            viewBundle.put(ViewBundle.P1B_HP,
                                    ((TextView) getView().findViewById(R.id.p1a_hp)).getText());
                            viewBundle.put(ViewBundle.P1B_HP_BAR,
                                    ((ProgressBar) getView().findViewById(R.id.p1a_bar_hp)).getProgress());
                            viewBundle.put(ViewBundle.P1B_SPRITE,
                                    ((ImageView) getView().findViewById(R.id.p1a_icon)).getDrawable());
                            viewBundle.put(ViewBundle.P1B_STATUS,
                                    getAllChild((LinearLayout) getView().findViewById(R.id.p1a_temp_status)));
                            if (getView().findViewById(R.id.p1c).getVisibility() == View.VISIBLE) {
                                viewBundle.put(ViewBundle.P1C, true);
                                viewBundle.put(ViewBundle.P1C_PKM,
                                        ((TextView) getView().findViewById(R.id.p1c_pkm)).getText());
                                viewBundle.put(ViewBundle.P1C_GENDER,
                                        ((ImageView) getView().findViewById(R.id.p1c_gender)).getDrawable());
                                viewBundle.put(ViewBundle.P1C_HP,
                                        ((TextView) getView().findViewById(R.id.p1c_hp)).getText());
                                viewBundle.put(ViewBundle.P1C_HP_BAR,
                                        ((ProgressBar) getView().findViewById(R.id.p1c_bar_hp)).getProgress());
                                viewBundle.put(ViewBundle.P1C_SPRITE,
                                        ((ImageView) getView().findViewById(R.id.p1c_icon)).getDrawable());
                                viewBundle.put(ViewBundle.P1C_STATUS,
                                        getAllChild((LinearLayout) getView().findViewById(R.id.p1c_temp_status)));
                            } else {
                                viewBundle.put(ViewBundle.P1C, false);
                            }
                        } else {
                            viewBundle.put(ViewBundle.P1B, false);
                        }
                    } else {
                        viewBundle.put(ViewBundle.P1A, false);
                    }
                    if (getView().findViewById(R.id.p2a).getVisibility() == View.VISIBLE) {
                        viewBundle.put(ViewBundle.P2A, true);
                        viewBundle.put(ViewBundle.P2A_PKM,
                                ((TextView) getView().findViewById(R.id.p2a_pkm)).getText());
                        viewBundle.put(ViewBundle.P2A_GENDER,
                                ((ImageView) getView().findViewById(R.id.p2a_gender)).getDrawable());
                        viewBundle.put(ViewBundle.P2A_HP,
                                ((TextView) getView().findViewById(R.id.p2a_hp)).getText());
                        viewBundle.put(ViewBundle.P2A_HP_BAR,
                                ((ProgressBar) getView().findViewById(R.id.p2a_bar_hp)).getProgress());
                        viewBundle.put(ViewBundle.P2A_SPRITE,
                                ((ImageView) getView().findViewById(R.id.p2a_icon)).getDrawable());
                        viewBundle.put(ViewBundle.P2A_STATUS,
                                getAllChild((LinearLayout) getView().findViewById(R.id.p2a_temp_status)));
                        if (getView().findViewById(R.id.p2a).getVisibility() == View.VISIBLE) {
                            viewBundle.put(ViewBundle.P2B, true);
                            viewBundle.put(ViewBundle.P2B_PKM,
                                    ((TextView) getView().findViewById(R.id.p2a_pkm)).getText());
                            viewBundle.put(ViewBundle.P2B_GENDER,
                                    ((ImageView) getView().findViewById(R.id.p2a_gender)).getDrawable());
                            viewBundle.put(ViewBundle.P2B_HP,
                                    ((TextView) getView().findViewById(R.id.p2a_hp)).getText());
                            viewBundle.put(ViewBundle.P2B_HP_BAR,
                                    ((ProgressBar) getView().findViewById(R.id.p2a_bar_hp)).getProgress());
                            viewBundle.put(ViewBundle.P2B_SPRITE,
                                    ((ImageView) getView().findViewById(R.id.p2a_icon)).getDrawable());
                            viewBundle.put(ViewBundle.P2B_STATUS,
                                    getAllChild((LinearLayout) getView().findViewById(R.id.p2a_temp_status)));
                            if (getView().findViewById(R.id.p2c).getVisibility() == View.VISIBLE) {
                                viewBundle.put(ViewBundle.P2C, true);
                                viewBundle.put(ViewBundle.P2C_PKM,
                                        ((TextView) getView().findViewById(R.id.p2c_pkm)).getText());
                                viewBundle.put(ViewBundle.P2C_GENDER,
                                        ((ImageView) getView().findViewById(R.id.p2c_gender)).getDrawable());
                                viewBundle.put(ViewBundle.P2C_HP,
                                        ((TextView) getView().findViewById(R.id.p2c_hp)).getText());
                                viewBundle.put(ViewBundle.P2C_HP_BAR,
                                        ((ProgressBar) getView().findViewById(R.id.p2c_bar_hp)).getProgress());
                                viewBundle.put(ViewBundle.P2C_SPRITE,
                                        ((ImageView) getView().findViewById(R.id.p2c_icon)).getDrawable());
                                viewBundle.put(ViewBundle.P2C_STATUS,
                                        getAllChild((LinearLayout) getView().findViewById(R.id.p2c_temp_status)));
                            } else {
                                viewBundle.put(ViewBundle.P2C, false);
                            }
                        } else {
                            viewBundle.put(ViewBundle.P2B, false);
                        }
                    } else {
                        viewBundle.put(ViewBundle.P2A, false);
                    }
                }
                viewBundle.put(ViewBundle.ANIMATION_QUEUE, mAnimatorSetQueue);
                roomData.setViewBundle(viewBundle);
            }
        }
    }

    public String getPlayer1() {
        if (mPlayer1 == null) {
            mPlayer1 = BattleFieldData.get(getActivity()).getAnimationInstance(getRoomId()).getPlayer1();
        }
        return mPlayer1;
    }

    public void setPlayer1(String player1) {
        mPlayer1 = player1;
    }

    public String getPlayer2() {
        if (mPlayer2 == null) {
            mPlayer2 = BattleFieldData.get(getActivity()).getAnimationInstance(getRoomId()).getPlayer2();
        }
        return mPlayer2;
    }

    public void setPlayer2(String player2) {
        mPlayer2 = player2;
    }

    public ArrayList<PokemonInfo> getPlayer1Team() {
        return mPlayer1Team;
    }

    public void setPlayer1Team(ArrayList<PokemonInfo> player1Team) {
        mPlayer1Team = player1Team;
    }

    public ArrayList<PokemonInfo> getPlayer2Team() {
        return mPlayer2Team;
    }

    public void setPlayer2Team(ArrayList<PokemonInfo> player2Team) {
        mPlayer2Team = player2Team;
    }

    public String getCurrentWeather() {
        return mCurrentWeather;
    }

    public void setCurrentWeather(String currentWeather) {
        this.mCurrentWeather = currentWeather;
    }

    public boolean isWeatherExist() {
        return mWeatherExist;
    }

    public void setWeatherExist(boolean weatherExist) {
        this.mWeatherExist = weatherExist;
    }

    public int getTurnNumber() {
        return mTurnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        mTurnNumber = turnNumber;
    }

    public boolean isMyTurn() {
        return mMyTurn;
    }

    public void setMyTurn(boolean myTurn) {
        this.mMyTurn = myTurn;
    }

    public boolean isTeamPreview() {
        return mTeamPreview;
    }

    public void setTeamPreview(boolean teamPreview) {
        mTeamPreview = teamPreview;
    }

    public String getRoomId() {
        return mRoomId;
    }

    public int getBattling() {
        return mBattling;
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

    private void switchUpPlayer() {
        // Switch player name
        if (getView() == null) {
            return;
        }

        String holderString = mPlayer1;
        mPlayer1 = mPlayer2;
        mPlayer2 = holderString;

        ArrayList<PokemonInfo> holderTeam = mPlayer1Team;
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
        BattleFieldData.BattleLog battleLog = BattleFieldData.get(getActivity()).getRoomInstance(mRoomId);
        if (battleLog != null && battleLog.isMessageListener()) {
            if (logMessage.length() > 0) {
                battleLog.addServerMessageOnHold(logMessage);
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

    public void formChange(String playerTag, String oldPkm, String newPkm) {
        PokemonInfo oldInfo, newInfo;
        if (playerTag.startsWith("p1")) {
            int index = findPokemonInTeam(mPlayer1Team, oldPkm);
            if (index == -1) {
                return;
            } else {
                oldInfo = mPlayer1Team.get(index);
                newInfo = new PokemonInfo(getActivity(), newPkm);
                mPlayer1Team.set(index, newInfo);
            }
        } else {
            int index = findPokemonInTeam(mPlayer2Team, oldPkm);
            if (index == -1) {
                return;
            } else {
                oldInfo = mPlayer2Team.get(index);
                newInfo = new PokemonInfo(getActivity(), newPkm);
                mPlayer2Team.set(index, newInfo);
            }
        }

        newInfo.setNickname(oldInfo.getNickname());
        newInfo.setLevel(oldInfo.getLevel());
        newInfo.setGender(oldInfo.getGender());
        newInfo.setShiny(oldInfo.isShiny());
        newInfo.setActive(oldInfo.isActive());
        newInfo.setHp(oldInfo.getHp());
        newInfo.setStatus(oldInfo.getStatus());
        newInfo.setMoves(oldInfo.getMoves());
        newInfo.setName(oldInfo.getName());
        newInfo.setItem(oldInfo.getItem(getActivity()));
    }

    public ArrayList<PokemonInfo> getTeam(String playerTag) {
        if (playerTag.startsWith("p1")) {
            return mPlayer1Team;
        } else {
            return mPlayer2Team;
        }
    }

    public void setTeam(String playerTag, ArrayList<PokemonInfo> playerTeam) {
        if (playerTag.startsWith("p1")) {
            mPlayer1Team = playerTeam;
        } else {
            mPlayer2Team = playerTeam;
        }
    }

    public String[] getTeamNameStringArray(ArrayList<PokemonInfo> teamMap) {
        String[] team = new String[teamMap.size()];
        for (Integer i = 0; i < teamMap.size(); i++) {
            PokemonInfo pkm = teamMap.get(i);
            team[i] = pkm.getName();
        }
        return team;
    }

    public int findPokemonInTeam(ArrayList<PokemonInfo> playerTeam, String pkm) {
        boolean special = false;
        String species = "";
        for (String sp : MORPHS) {
            if (pkm.contains(sp)) {
                special = true;
                species = sp;
                break;
            }
        }

        ArrayList<String> teamName = getTeamNameArrayList(playerTeam);

        if (!special) {
            return teamName.indexOf(pkm);
        } else {
            for (int i = 0; i < teamName.size(); i++) {
                if (teamName.get(i).contains(species)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public ArrayList<String> getTeamNameArrayList(ArrayList<PokemonInfo> playerTeam) {
        ArrayList<String> teamName = new ArrayList<>();
        for (PokemonInfo pkm : playerTeam) {
            teamName.add(pkm.getName());
        }
        return teamName;
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

        for (String stat : STATS) {
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

    public void setVisibility(View view, int visibility) {
        switch (visibility) {
            case View.VISIBLE:
                view.setVisibility(View.VISIBLE);
                break;
            case View.INVISIBLE:
                view.setVisibility(View.INVISIBLE);
                break;
            case View.GONE:
                view.setVisibility(View.GONE);
                break;
            default:
                view.setVisibility(View.GONE);
        }
    }

    public View[] getAllChild(LinearLayout layout) {
        if (layout == null) {
            return null;
        }

        View[] childs = new View[layout.getChildCount()];
        for (int i = 0; i < layout.getChildCount(); i++) {
            childs[i] = layout.getChildAt(i);
        }
        return childs;
    }

    public void addAllChild(LinearLayout layout, View[] childs) {
        for (View child : childs) {
            layout.addView(child);
        }
    }

    public void showPossibleActions(ServerRequest serverRequest) {
        //todo handle doubles+ triples
        int currentAction = 0;
        boolean showSwitchFragment = false;
        boolean teamPreview = serverRequest.isTeamPreview();
        if (serverRequest.getForceSwitch().size() > 0) {
            showSwitchFragment = serverRequest.getForceSwitch().get(currentAction);
        }
        if (teamPreview) {
            BattleSwitchFragment fragment = BattleSwitchFragment.newInstance(serverRequest, currentAction, getRoomId(), teamPreview);

            FragmentManager fm = getActivity().getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.action_fragment_container, fragment, "")
                    .commit();
        } else if (showSwitchFragment) {
            BattleSwitchFragment fragment = BattleSwitchFragment.newInstance(serverRequest, currentAction, getRoomId(), false);

            FragmentManager fm = getActivity().getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.action_fragment_container, fragment, "")
                    .commit();
        } else {
            BattleMoveOrSwitchFragment fragment = BattleMoveOrSwitchFragment.newInstance(serverRequest, currentAction, getRoomId());
            FragmentManager fm = getActivity().getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.action_fragment_container, fragment, "")
                    .commit();

        }

    }

}
