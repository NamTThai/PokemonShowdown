package com.pokemonshowdown.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pokemonshowdown.data.BattleFieldData;
import com.pokemonshowdown.data.MyApplication;

import java.util.ArrayList;
import java.util.HashMap;

public class BattleLogDialog extends DialogFragment {
    public static final String BTAG = BattleLogDialog.class.getName();
    private String mRoomId;
    private String mPlayer1;
    private String mPlayer2;
    private boolean weatherExist;
    private String currentWeather;
    private HashMap<String, Integer> mPlayer1Team = new HashMap<>();
    private HashMap<String, Integer> mPlayer2Team = new HashMap<>();

    public static BattleLogDialog newInstance(String roomId) {
        BattleLogDialog fragment = new BattleLogDialog();
        Bundle args = new Bundle();
        args.putString(BattleFragment.ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    public BattleLogDialog() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_battlelog, container);

        if (getArguments() != null) {
            mRoomId = getArguments().getString(BattleFragment.ROOM_ID);
        }

        final EditText chatBox = (EditText) view.findViewById(R.id.battle_chat_box);
        chatBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String message = chatBox.getText().toString();
                    message = mRoomId + "|" + message;
                    if (MyApplication.getMyApplication().verifySignedInBeforeSendingMessage()) {
                        MyApplication.getMyApplication().sendClientMessage(message);
                    }
                    chatBox.setText(null);
                    return false;
                }
                return false;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        BattleFieldData.RoomData roomData = BattleFieldData.get(getActivity()).getRoomDataHashMap().get(mRoomId);
        if (roomData != null) {
            mPlayer1 = roomData.getPlayer1();
            mPlayer2 = roomData.getPlayer2();

            ((TextView) getView().findViewById(R.id.battlelog)).setText(roomData.getChatBox());
            final ScrollView scrollView = (ScrollView) getView().findViewById(R.id.battlelog_scrollview);
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            });

            ArrayList<Spannable> pendingMessages = roomData.getServerMessageOnHold();
            for (Spannable message : pendingMessages) {
                processServerMessage(message);
            }

            roomData.setMessageListener(false);
            roomData.setServerMessageOnHold(new ArrayList<Spannable>());
        }
    }

    @Override
    public void onPause() {
        BattleFieldData.RoomData roomData = BattleFieldData.get(getActivity()).getRoomInstance(mRoomId);
        if (roomData != null) {
            roomData.setMessageListener(true);
            CharSequence text = ((TextView) getView().findViewById(R.id.battlelog)).getText();
            roomData.setChatBox(text);
        }
        super.onPause();
    }

    public void processServerMessage(Spannable message) {
        appendServerMessage(message);
    }

    public void processMajorAction(String message) {
        BattleFieldData.RoomData roomData = BattleFieldData.get(getActivity()).getRoomInstance(mRoomId);
        String command = (message.indexOf('|') == -1) ? message : message.substring(0, message.indexOf('|'));
        final String messageDetails = message.substring(message.indexOf('|') + 1);
        if (command.startsWith("-")) {
            processMinorAction(command, messageDetails);
            return;
        }

        int separator = messageDetails.indexOf('|');
        int start;
        String remaining;
        String toAppend;
        StringBuilder toAppendBuilder;
        Spannable toAppendSpannable;
        switch (command) {
            case "init":
            case "title":
            case "join":
            case "j":
            case "J":
            case "leave":
            case "l":
            case "L":
                break;
            case "chat":
            case "c":
                String user = messageDetails.substring(0, separator);
                String userMessage = messageDetails.substring(separator + 1);
                toAppend = user + ": " + userMessage;
                toAppendSpannable = new SpannableString(toAppend);
                toAppendSpannable.setSpan(new ForegroundColorSpan(ChatRoomFragment.getColorStrong(user)), 0, user.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "tc":
            case "c:":
                // String timeStamp = messageDetails.substring(0, separator);
                String messageDetailsWithStamp = messageDetails.substring(separator + 1);
                separator = messageDetailsWithStamp.indexOf('|');
                String userStamp = messageDetailsWithStamp.substring(0, separator);
                String userMessageStamp = messageDetailsWithStamp.substring(separator + 1);
                toAppend = userStamp + ": " + userMessageStamp;
                toAppendSpannable = new SpannableString(toAppend);
                toAppendSpannable.setSpan(new ForegroundColorSpan(ChatRoomFragment.getColorStrong(userStamp)), 0, userStamp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "raw":
                appendServerMessage(new SpannableString(Html.fromHtml(messageDetails).toString()));
                break;
            case "message":
                appendServerMessage(new SpannableString(messageDetails));
                break;
            case "gametype":
            case "gen":
                break;
            case "player":
                String playerType;
                String playerName;
                if (separator == -1) {
                    playerType = messageDetails;
                    playerName = "";
                } else {
                    playerType = messageDetails.substring(0, separator);
                    String playerDetails = messageDetails.substring(separator + 1);
                    separator = playerDetails.indexOf('|');
                    playerName = playerDetails.substring(0, separator);
                }
                if (playerType.equals("p1")) {
                    roomData.setPlayer1(playerName);
                    mPlayer1 = playerName;
                } else {
                    roomData.setPlayer2(playerName);
                    mPlayer2 = playerName;
                }
                break;
            case "tier":
                toAppend = "Format:" + "\n" + messageDetails;
                toAppendSpannable = new SpannableString(toAppend);
                toAppendSpannable.setSpan(new StyleSpan(Typeface.BOLD), toAppend.indexOf('\n') + 1, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "rated":
                toAppend = command.toUpperCase();
                toAppendSpannable = new SpannableString(toAppend);
                toAppendSpannable.setSpan(new ForegroundColorSpan(R.color.dark_blue), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "rule":
                toAppendSpannable = new SpannableString(messageDetails);
                toAppendSpannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, messageDetails.indexOf(':') + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "":
                toAppendSpannable = new SpannableString("");
                appendServerMessage(toAppendSpannable);
                break;
            case "clearpoke":
                mPlayer1Team = new HashMap<>();
                mPlayer2Team = new HashMap<>();
                break;
            case "poke":
                playerType = messageDetails.substring(0, separator);
                String pokeName = messageDetails.substring(separator + 1);
                if (playerType.equals("p1")) {
                    mPlayer1Team.put(pokeName, 100);
                } else {
                    mPlayer2Team.put(pokeName, 100);
                }
                break;
            case "teampreview":
                toAppendBuilder = new StringBuilder();
                toAppendBuilder.append(mPlayer1).append("'s Team: ");
                String[] p1Team = mPlayer1Team.keySet().toArray(new String[mPlayer1Team.size()]);
                for (int i = 0; i < p1Team.length - 1; i++) {
                    toAppendBuilder.append(p1Team[i]).append("/");
                }
                toAppendBuilder.append(p1Team[p1Team.length - 1]);

                toAppendBuilder.append("\n").append(mPlayer2).append("'s Team: ");
                String[] p2Team = mPlayer2Team.keySet().toArray(new String[mPlayer2Team.size()]);
                for (int i = 0; i < p2Team.length - 1; i++) {
                    toAppendBuilder.append(p2Team[i]).append("/");
                }
                toAppendBuilder.append(p2Team[p2Team.length - 1]);
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                appendServerMessage(toAppendSpannable);
                break;
            case "request":
                appendServerMessage(new SpannableString(messageDetails));
                break;
            case "inactive":
            case "inactiveoff":
                toAppendSpannable = new SpannableString(messageDetails);
                toAppendSpannable.setSpan(new ForegroundColorSpan(R.color.dark_red), 0, messageDetails.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "start":
                toAppend = roomData.getPlayer1() + " vs. " + roomData.getPlayer2();
                toAppendSpannable = new SpannableString(toAppend);
                toAppendSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "move":
                String attacker = messageDetails.substring(5, separator);
                remaining = messageDetails.substring(separator + 1);
                toAppendBuilder = new StringBuilder();
                if (remaining.startsWith("p2")) {
                    toAppendBuilder.append("The opposing's ");
                }
                toAppendBuilder.append(attacker).append(" used ");
                String move = remaining.substring(0, remaining.indexOf('|'));
                toAppendBuilder.append(move).append("!");
                toAppend = toAppendBuilder.toString();
                start = toAppend.indexOf(move);
                toAppendSpannable = new SpannableString(toAppend);
                toAppendSpannable.setSpan(new StyleSpan(Typeface.BOLD), start, start + move.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "switch":
            case "drag":
            case "replace":
                //TODO need to handle roar & cie
                toAppendBuilder = new StringBuilder();
                attacker = messageDetails.substring(5, separator);
                remaining = messageDetails.substring(separator + 1);
                separator = remaining.indexOf(',');
                if (separator == -1) {
                    //for genderless
                    separator = remaining.indexOf('|');
                }
                String species = remaining.substring(0, separator);

                attacker = (!attacker.equals(species)) ? attacker + " (" + species + ")" : attacker;
                if (messageDetails.startsWith("p1")) {
                    toAppendBuilder.append("Go! ").append(attacker).append('!');
                } else {
                    toAppendBuilder.append(mPlayer2).append(" sent out ").append(attacker).append("!");
                }
                appendServerMessage(new SpannableStringBuilder(toAppendBuilder));
                break;
            case "detailschange":
                break;
            case "faint":
                attacker = messageDetails.substring(5);
                toAppendBuilder = new StringBuilder();
                if (messageDetails.startsWith("p2")) {
                    toAppendBuilder.append("The opposing ");
                }
                toAppendBuilder.append(attacker).append(" fainted!");
                appendServerMessage(new SpannableStringBuilder(toAppendBuilder));
                break;
            case "turn":
                toAppend = "TURN " + messageDetails;
                toAppendSpannable = new SpannableString(toAppend.toUpperCase());
                toAppendSpannable.setSpan(new UnderlineSpan(), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                toAppendSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                toAppendSpannable.setSpan(new RelativeSizeSpan(1.25f), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                toAppendSpannable.setSpan(new ForegroundColorSpan(R.color.dark_blue), 0, toAppend.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                appendServerMessage(toAppendSpannable);
                break;
            case "win":
                toAppend = messageDetails + " has won the battle!";
                appendServerMessage(new SpannableString(toAppend));
                break;
            case "cant":
                //todo (cant attack bec frozen/para etc)
                break;
            default:
                appendServerMessage(new SpannableString(message));
                break;
        }
    }


    private String getPrintableOutputPokemonSide(String split) {
        return getPrintableOutputPokemonSide(split, true);
    }

    private String getPrintableOutputPokemonSide(String split, boolean start) {
        StringBuilder sb = new StringBuilder();
        if (split.startsWith("p2")) {
            if (start) {
                sb.append("The opposing ");
            } else {
                sb.append("the opposing ");
            }
        }

        int separator = split.indexOf(':');
        sb.append(split.substring(separator + 1).trim());
        return sb.toString();
    }

    private String getPrintable(String split) {
        int separator = split.indexOf(':');
        return split.substring(separator + 1).trim();
    }

    private String toId(String str) {
        return str.toLowerCase().replaceAll("\\s+", "");
    }


    private void processMinorAction(String command, String messageDetails) {
        int separator;
        int start;
        Integer oldHP;
        int lostHP;
        int intAmount;
        String remaining;
        String toAppend;
        StringBuilder toAppendBuilder = new StringBuilder();
        Spannable toAppendSpannable;
        String move, ability;
        boolean flag, eat, weaken;

        String fromEffect = null;
        String fromEffectId = null;
        String ofSource = null;
        String trimmedOfEffect = null;

        String attacker, defender, side, stat, statAmount;
        String attackerOutputName;
        String defenderOutputName;

        int from = messageDetails.indexOf("[from]");
        if (from != -1) {
            remaining = messageDetails.substring(from + 7);
            separator = remaining.indexOf('|');
            fromEffect = (separator == -1) ? remaining : remaining.substring(0, separator);
            //trim
            fromEffectId = toId(fromEffect);
        }
        int of = messageDetails.indexOf("[of]");
        if (of != -1) {
            remaining = messageDetails.substring(of + 5);
            separator = remaining.indexOf('|');
            ofSource = (separator == -1) ? remaining : remaining.substring(remaining.indexOf(':'), separator);

            trimmedOfEffect = toId(ofSource);
        }

        separator = messageDetails.indexOf('|');
        String[] split = messageDetails.split("\\|");

        switch (command) {
            case "-damage":
                attacker = getPrintable(split[0]);
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);

                if (messageDetails.startsWith("p2")) {
                    oldHP = mPlayer2Team.get(attacker);
                    if (oldHP == null) {
                        oldHP = 100;
                        mPlayer2Team.put(attacker, oldHP);
                    }
                } else {
                    oldHP = mPlayer1Team.get(attacker);
                    if (oldHP == null) {
                        oldHP = 100;
                        mPlayer1Team.put(attacker, oldHP);
                    }
                }

                remaining = split[1];
                separator = remaining.indexOf("/");
                if (separator == -1) { // fainted
                    intAmount = 0;
                } else {
                    String hp = remaining.substring(0, separator);
                    intAmount = Integer.parseInt(hp);
                }
                lostHP = oldHP - intAmount;

                if (fromEffectId != null) {
                    switch (getPrintable(fromEffectId)) {
                        case "stealthrock":
                            attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                            toAppendBuilder.append("Pointed stones dug into ").append(attackerOutputName).append("!");
                            break;
                        case "spikes":
                            toAppendBuilder.append(attackerOutputName).append(" is hurt by the spikes!");
                            break;
                        case "brn":
                            toAppendBuilder.append(attackerOutputName).append(" was hurt by its burn!");
                            break;
                        case "psn":
                            toAppendBuilder.append(attackerOutputName).append(" was hurt by poison!");
                            break;
                        case "lifeorb":
                            toAppendBuilder.append(attackerOutputName).append(" lost some of its HP!");
                            break;
                        case "recoil":
                            toAppendBuilder.append(attackerOutputName).append(" is damaged by recoil!");
                            break;
                        case "sandstorm":
                            toAppendBuilder.append(attackerOutputName).append(" is buffeted by the sandstorm!");
                            break;
                        case "hail":
                            toAppendBuilder.append(attackerOutputName).append(" is buffeted by the hail!");
                            break;
                        case "baddreams":
                            toAppendBuilder.append(attackerOutputName).append(" is tormented!");
                            break;
                        case "nightmare":
                            toAppendBuilder.append(attackerOutputName).append(" is locked in a nightmare!");
                            break;
                        case "confusion":
                            toAppendBuilder.append("It hurt itself in its confusion!");
                            break;
                        case "leechseed":
                            toAppendBuilder.append(attackerOutputName).append("'s health is sapped by Leech Seed!");
                            break;
                        case "flameburst":
                            attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                            toAppendBuilder.append("The bursting flame hit ").append(attackerOutputName).append("!");
                            break;
                        case "firepledge":
                            toAppendBuilder.append(attackerOutputName).append(" is hurt by the sea of fire!");
                            break;
                        case "jumpkick":
                        case "highjumpkick":
                            toAppendBuilder.append(attackerOutputName).append(" kept going and crashed!");
                            break;
                        default:
                            if (ofSource != null) {
                                toAppendBuilder.append(attackerOutputName).append(" is hurt by ").append(getPrintable(ofSource)).append("'s ").append(getPrintable(fromEffect)).append("!");
                            } else if (fromEffectId.contains(":")) {
                                toAppendBuilder.append(attackerOutputName).append(" is hurt by its").append(getPrintable(fromEffect)).append("!");
                            } else {
                                toAppendBuilder.append(attackerOutputName).append(" lost some HP because of ").append(getPrintable(fromEffect)).append("!");
                            }
                            break;
                    }
                } else {
                    toAppendBuilder.append(attackerOutputName).append(" lost ");
                    toAppendBuilder.append(lostHP).append("% of its health!");
                }

                if (messageDetails.startsWith("p2")) {
                    mPlayer2Team.put(attacker, intAmount);
                } else {
                    mPlayer1Team.put(attacker, intAmount);
                }

                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-heal":
                attacker = getPrintable(split[0]);
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                if (messageDetails.startsWith("p2")) {
                    oldHP = mPlayer2Team.get(attacker);
                    if (oldHP == null) {
                        // in randbats , we dont get the pokemon list
                        mPlayer2Team.put(attacker, 100);
                        oldHP = mPlayer2Team.get(attacker);
                    }
                } else {
                    oldHP = mPlayer1Team.get(attacker);
                    if (oldHP == null) {
                        // in randbats , we dont get the pokemon list
                        mPlayer1Team.put(attacker, 100);
                        oldHP = mPlayer1Team.get(attacker);
                    }
                }
                remaining = messageDetails.substring(separator + 1);
                separator = remaining.indexOf("/");
                if (separator == -1) {
                    intAmount = 0; // shouldnt happen sicne we're healing
                } else {
                    String hp = remaining.substring(0, separator);
                    intAmount = Integer.parseInt(hp);
                }
                lostHP = intAmount - oldHP;

                if (fromEffectId != null) {
                    switch (getPrintable(fromEffectId)) {
                        case "ingrain":
                            toAppendBuilder.append(attackerOutputName).append(" absorbed nutrients with its roots!");
                            break;
                        case "aquaring":
                            attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                            toAppendBuilder.append("Aqua Ring restored ").append(attackerOutputName).append("'s HP!");
                            break;
                        case "raindish":
                        case "dryskin":
                        case "icebody":
                            toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" heals it!");
                            break;
                        case "healingwish":
                            attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                            toAppendBuilder.append("The healing wish came true for ").append(attackerOutputName);
                            break;
                        case "lunardance":
                            toAppendBuilder.append(attackerOutputName).append(" became cloaked in mystical moonlight!");
                            break;
                        case "wish":
                            //TODO TRY
                            String wisher;
                            if (messageDetails.contains("[wish]")) {
                                separator = messageDetails.substring(messageDetails.indexOf("[wish]")).indexOf("|");
                                if (separator != -1) {
                                    wisher = messageDetails.substring(messageDetails.indexOf("[wish]") + 5, separator);
                                } else {
                                    wisher = messageDetails.substring(messageDetails.indexOf("[wish]") + 5);
                                }
                                toAppendBuilder.append(getPrintableOutputPokemonSide(wisher)).append("'s wish came true!");
                            }
                            break;
                        case "drain":
                            if (trimmedOfEffect != null) {
                                toAppendBuilder.append(getPrintableOutputPokemonSide(ofSource)).append(" had its energy drained!");
                                break;
                            }
                            // we should never enter here
                            toAppendBuilder.append(attackerOutputName).append(" drained health!");
                            break;

                        case "leftovers":
                        case "shellbell":
                            toAppendBuilder.append(attackerOutputName).append(" restored a little HP using its ").append(getPrintable(fromEffect)).append("!");
                            break;
                        default:
                            toAppendBuilder.append(attackerOutputName).append(" restored HP using its ").append(getPrintable(fromEffect)).append("!");
                            break;
                    }
                } else {
                    toAppendBuilder.append(attackerOutputName);
                    toAppendBuilder.append(" healed ").append(lostHP).append("% of it's health!");
                }
                if (messageDetails.startsWith("p2")) {
                    mPlayer2Team.put(attacker, intAmount);
                } else {
                    mPlayer1Team.put(attacker, intAmount);
                }

                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-sethp":
                // |-sethp|p2a: Latios|46/100|p1a: Rotom-Wash|137/262|[from] move: Pain Split
                switch (getPrintable(fromEffectId)) {
                    case "painsplit":
                        toAppendBuilder.append("The battlers shared their pain!");
                        break;
                }
                // todo actually switch hps

                defender = getPrintable(split[0]);
                separator = split[1].indexOf("/");
                intAmount = Integer.parseInt(split[1].substring(0, separator));
                if (split[0].startsWith("p2")) {
                    mPlayer2Team.put(defender, intAmount);
                } else {
                    mPlayer1Team.put(defender, intAmount);
                }

                int currentHp, totalHp;
                attacker = getPrintable(split[2]);
                separator = split[3].indexOf("/");
                currentHp = Integer.parseInt(split[3].substring(0, separator));
                totalHp = Integer.parseInt(split[3].substring(separator + 1));
                intAmount = ((currentHp * 100) / totalHp);

                if (split[2].startsWith("p2")) {
                    mPlayer2Team.put(attacker, intAmount);
                } else {
                    mPlayer1Team.put(attacker, intAmount);
                }


                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-boost":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                stat = split[1];
                statAmount = "";
                switch (stat) {
                    case "atk":
                        stat = "Attack";
                        break;
                    case "def":
                        stat = "Defense";
                        break;
                    case "spa":
                        stat = "Special Attack";
                        break;
                    case "spd":
                        stat = "Special Defense";
                        break;
                    case "spe":
                        stat = "Speed";
                        break;
                    default:
                        break;
                }
                String amount = split[2];
                intAmount = Integer.parseInt(amount);
                if (intAmount == 2) {
                    statAmount = " sharply";
                } else if (intAmount > 2) {
                    statAmount = " drastically";
                }

                if (fromEffect != null) {
                    if (fromEffect.contains("item:")) {
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append("The ").append(getPrintable(fromEffect)).append(statAmount).append(" raised ").append(attackerOutputName).append("'s ").append(stat).append("!");
                    } else {
                        toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(statAmount).append(" raised its ").append(stat).append("!");
                    }
                } else {
                    toAppendBuilder.append(attackerOutputName).append("'s ").append(stat).append(statAmount).append(" rose!");
                }

                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-unboost":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                stat = split[1];
                statAmount = "";

                switch (stat) {
                    case "atk":
                        stat = "Attack";
                        break;
                    case "def":
                        stat = "Defense";
                        break;
                    case "spa":
                        stat = "Special Attack";
                        break;
                    case "spd":
                        stat = "Special Defense";
                        break;
                    case "spe":
                        stat = "Speed";
                        break;
                    default:
                        break;
                }
                amount = split[2];
                intAmount = Integer.parseInt(amount);
                if (intAmount == 2) {
                    statAmount = " harshly";
                } else if (intAmount >= 3) {
                    statAmount = " severely";
                }

                if (fromEffect != null) {
                    if (fromEffect.contains("item:")) {
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append("The ").append(getPrintable(fromEffect)).append(statAmount).append(" lowered ").append(attackerOutputName).append("'s ").append(stat).append("!");
                    } else {
                        toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(statAmount).append(" lowered its ").append(stat).append("!");
                    }
                } else {
                    toAppendBuilder.append(attackerOutputName).append("'s ").append(stat).append(statAmount).append(" fell!");
                }

                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-setboost":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                if (fromEffect != null) {
                    switch (getPrintable(fromEffectId)) {
                        case "bellydrum":
                            toAppendBuilder.append(attackerOutputName).append(" cut its own HP and maximized its Attack!");
                            break;

                        case "angerpoint":
                            toAppendBuilder.append(attackerOutputName).append(" maxed its Attack!");
                            break;
                    }
                }
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-swapboost":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                if (fromEffect != null) {
                    switch (getPrintable(fromEffectId)) {
                        case "guardswap":
                            toAppendBuilder.append(attackerOutputName).append(" switched all changes to its Defense and Sp. Def with the target!");
                            break;

                        case "heartswap":
                            toAppendBuilder.append(attackerOutputName).append(" switched stat changes with the target!");
                            break;

                        case "powerswap":
                            toAppendBuilder.append(attackerOutputName).append(" switched all changes to its Attack and Sp. Atk with the target!");
                            break;
                    }
                }
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-restoreboost":
                //nothign here
                toAppendSpannable = new SpannableStringBuilder("");
                break;

            case "-copyboost":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                defenderOutputName = getPrintableOutputPokemonSide(split[1], false);
                toAppendBuilder.append(attackerOutputName).append(" copied ").append(defenderOutputName).append("'s stat changes!");
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-clearboost":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                toAppendBuilder.append(attackerOutputName).append("'s stat changes were removed!");
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-invertboost":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                toAppendBuilder.append(attackerOutputName).append("'s stat changes were inverted!");
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-clearallboost":
                toAppendBuilder.append("All stat changes were eliminated!");
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;
            case "-crit":
                toAppendSpannable = new SpannableString("It's a critical hit!");
                break;
            case "-supereffective":
                toAppendSpannable = new SpannableString("It's super effective!");
                break;
            case "-resisted":
                toAppendSpannable = new SpannableString("It's not very effective...");
                break;
            case "-immune":
                attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                toAppendBuilder.append("It doesn't affect ");
                toAppendBuilder.append(attackerOutputName);
                toAppendBuilder.append(".");
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-miss":
                if (split.length > 1) {
                    // there was a target
                    defenderOutputName = getPrintableOutputPokemonSide(split[1]);
                    toAppendBuilder.append(defenderOutputName).append(" avoided the attack!");
                } else {
                    attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                    toAppendBuilder.append(attackerOutputName).append("'s attack missed!");
                }
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-fail":
                // todo
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                if (split.length > 1) {
                    remaining = split[1];

                    switch (remaining) {
                        case "brn":
                            toAppendBuilder.append(attackerOutputName).append(" is already burned.");
                            break;
                        case "tox":
                        case "psn":
                            toAppendBuilder.append(attackerOutputName).append(" is already poisoned.");
                            break;
                        case "slp":
                            if (fromEffect != null && getPrintable(fromEffectId).equals("uproar")) {
                                attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                                toAppendBuilder.append("But the uproar kept ").append(attackerOutputName).append(" awake!");
                            } else {
                                toAppendBuilder.append(attackerOutputName).append(" is already asleep.");
                            }
                            break;
                        case "par":
                            toAppendBuilder.append(attackerOutputName).append(" is already paralyzed.");
                            break;
                        case "frz":
                            toAppendBuilder.append(attackerOutputName).append(" is already frozen.");
                            break;
                        case "substitute":
                            if (messageDetails.contains("[weak]")) {
                                toAppendBuilder.append(attackerOutputName).append("It was too weak to make a substitute!");
                            } else {
                                toAppendBuilder.append(attackerOutputName).append(" already has a substitute!");
                            }
                            break;
                        case "skydrop":
                            if (messageDetails.contains("[heavy]")) {
                                toAppendBuilder.append(attackerOutputName).append(" is too heavy to be lifted!");
                            } else {
                                toAppendBuilder.append("But it failed!");
                            }
                            break;
                        case "unboost":
                            toAppendBuilder.append(attackerOutputName).append("'s stats were not lowered!");
                            break;

                        default:
                            toAppendBuilder.append("But it failed!");
                            break;
                    }
                } else {
                    toAppendBuilder.append("But it failed!");
                }


                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-notarget":
                toAppendSpannable = new SpannableString("But there was no target...");
                break;

            case "-ohko":
                toAppendSpannable = new SpannableString("It's a one-hit KO!");
                break;

            case "-hitcount":
                try {
                    String hitCountS = split[split.length - 1];
                    int hitCount = Integer.parseInt(hitCountS);
                    toAppendBuilder.append("Hit ").append(hitCount).append(" time");
                    if (hitCount > 1) {
                        toAppendBuilder.append("s");
                    }
                    toAppendBuilder.append("!");
                    toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                } catch (NumberFormatException e) {
                    // todo handle
                    toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                }
                break;

            case "-nothing":
                toAppendSpannable = new SpannableString("But nothing happened! ");
                break;

            case "-waiting":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                defenderOutputName = getPrintableOutputPokemonSide(split[1], false);
                toAppendBuilder.append(attackerOutputName).append(" is waiting for ").append(defenderOutputName).append("'s move...");
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-combine":
                toAppendSpannable = new SpannableString("The two moves are joined! It's a combined move!");
                break;

            case "-prepare":
                // todo
                toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                break;

            case "-status":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                toAppendBuilder.append(attackerOutputName);
                remaining = split[1];
                switch (remaining) {
                    case "brn":
                        toAppendBuilder.append(" was burned");
                        if (fromEffect != null) {
                            toAppendBuilder.append(" by the ").append(getPrintable(fromEffect));
                        }
                        toAppendBuilder.append("!");
                        break;

                    case "tox":
                        toAppendBuilder.append(" was badly poisoned");
                        if (fromEffect != null) {
                            toAppendBuilder.append(" by the ").append(getPrintable(fromEffect));
                        }
                        toAppendBuilder.append("!");
                        break;

                    case "psn":
                        toAppendBuilder.append(" was poisoned!");
                        break;

                    case "slp":
                        if (fromEffect != null && fromEffectId.equals("move:rest")) {
                            toAppendBuilder.append(" slept and became healthy!");
                        } else {
                            toAppendBuilder.append(" fell asleep!");
                        }
                        break;

                    case "par":
                        toAppendBuilder.append(" is paralyzed! It may be unable to move!");
                        break;

                    case "frz":
                        toAppendBuilder.append(" was frozen solid!");
                        break;
                }
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-curestatus":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                flag = false;
                if (fromEffectId != null) {
                    fromEffectId = getPrintable(fromEffectId);
                    switch (getPrintable(fromEffectId)) {
                        // seems like this doenst happen at this time
                        // |move|p2a: Sigilyph|Psycho Shift|p1a: Chansey
                        // |-status|p1a: Chansey|tox
                        //  |-curestatus|p2a: Sigilyph|tox
                        // todo buffer last move?
                        case "psychoshift":
                            //ofeffect should always be !null at that time
                            defenderOutputName = getPrintableOutputPokemonSide(ofSource, false);
                            toAppendBuilder.append(attackerOutputName).append(" moved its status onto ").append(defenderOutputName);
                            flag = true;
                            break;
                    }
                    if (fromEffectId.contains("ability:")) {
                        toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" heals its status!");
                        flag = true;
                    }
                }

                if (!flag) {
                    //split1 is cured status
                    switch (split[1]) {
                        case "brn":
                            if (fromEffectId != null && fromEffectId.contains("item:")) {
                                toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" healed its burn!");
                                break;
                            }
                            if (split[0].startsWith("p2")) {
                                toAppendBuilder.append(attackerOutputName).append("'s burn was healed.");
                            } else {
                                toAppendBuilder.append(attackerOutputName).append(" healed its burn!.");
                            }
                            break;

                        case "tox":
                        case "psn":
                            if (fromEffectId != null && fromEffectId.contains("item:")) {
                                toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" cured its poison!");
                                break;
                            }
                            toAppendBuilder.append(attackerOutputName).append(" was cured of its poisoning.");
                            break;

                        case "slp":
                            if (fromEffectId != null && fromEffectId.contains("item:")) {
                                toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" woke it up!");
                                break;
                            }
                            toAppendBuilder.append(attackerOutputName).append(" woke up!");
                            break;

                        case "par":
                            if (fromEffectId != null && fromEffectId.contains("item:")) {
                                toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" cured its paralysis!");
                                break;
                            }
                            toAppendBuilder.append(attackerOutputName).append(" was cured of paralysis.");

                            break;

                        case "frz":
                            if (fromEffectId != null && fromEffectId.contains("item:")) {
                                toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" defrosted it!");
                                break;
                            }
                            toAppendBuilder.append(attackerOutputName).append(" thawed out!");
                            break;

                        default:
                            //confusion
                            toAppendBuilder.append(attackerOutputName).append("'s status cleared!");
                            break;
                    }
                }
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-cureteam":
                if (fromEffectId != null) {
                    switch (getPrintable(fromEffectId)) {
                        case "aromatherapy":
                            toAppendBuilder.append("A soothing aroma wafted through the area!");
                            break;

                        case "healbell":
                            toAppendBuilder.append("A bell chimed!");
                            break;
                    }
                } else {
                    attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                    toAppendBuilder.append(attackerOutputName);
                    toAppendBuilder.append(" 's team was cured");
                }
                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-item":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                String item = getPrintable(split[1]);
                if (fromEffect != null) {
                    // not to deal with item: or ability: or move:
                    switch (getPrintable(fromEffectId)) {
                        case "recycle":
                        case "pickup":
                            toAppendBuilder.append(attackerOutputName).append(" found one ").append(item).append("!");
                            break;

                        case "frisk":
                            toAppendBuilder.append(attackerOutputName).append(" frisked its target and found one ").append(item).append("!");
                            break;

                        case "thief":
                        case "covet":
                            defenderOutputName = getPrintableOutputPokemonSide(ofSource, false);
                            toAppendBuilder.append(attackerOutputName).append("  stole  ").append(defenderOutputName).append("'s ").append(item).append("!");
                            break;

                        case "harvest":
                            toAppendBuilder.append(attackerOutputName).append(" harvested one ").append(item).append("!");
                            break;

                        case "bestow":
                            defenderOutputName = getPrintableOutputPokemonSide(ofSource, false);
                            toAppendBuilder.append(attackerOutputName).append(" received ").append(item).append(" from ").append(defenderOutputName).append("!");
                            break;

                        default:
                            toAppendBuilder.append(attackerOutputName).append(" obtained one ").append(item).append(".");
                            break;
                    }
                } else {
                    switch (item) {
                        case "Air Balloon":
                            toAppendBuilder.append(attackerOutputName).append(" floats in the air with its Air Balloon!");
                            break;

                        default:
                            toAppendBuilder.append(attackerOutputName).append("has ").append(item).append("!");
                            break;
                    }
                }


                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-enditem":
                eat = messageDetails.contains("[eat]");
                weaken = messageDetails.contains("[weaken]");
                attacker = getPrintable(split[0]);
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                item = split[1].trim();

                if (eat) {
                    toAppendBuilder.append(attackerOutputName).append(" ate its ").append(item).append("!");
                } else if (weaken) {
                    toAppendBuilder.append(attackerOutputName).append(" weakened the damage to ").append(item).append("!");
                } else if (fromEffect != null) {
                    switch (getPrintable(fromEffectId)) {
                        case "fling":
                            toAppendBuilder.append(attackerOutputName).append(" flung its ").append(item).append("!");
                            break;

                        case "knockoff":
                            defenderOutputName = getPrintableOutputPokemonSide(ofSource);
                            attackerOutputName = getPrintableOutputPokemonSide(split[0], false);

                            toAppendBuilder.append(defenderOutputName).append(" knocked off ").append(attackerOutputName).append("'s ").append(item).append("!");
                            break;

                        case "stealeat":
                            defenderOutputName = getPrintableOutputPokemonSide(ofSource);
                            toAppendBuilder.append(defenderOutputName).append(" stole and ate its target's ").append(item).append("!");
                            break;

                        case "gem":
                            separator = messageDetails.indexOf("[move]");
                            move = "";
                            if (separator != -1) {
                                move = messageDetails.substring(separator + 6);
                                if (move.contains("|")) {
                                    move.substring(0, move.indexOf("|"));
                                }
                            }
                            toAppendBuilder.append("The ").append(item).append(" strengthened ").append(move).append("'s power!");
                            break;

                        case "incinerate":
                            toAppendBuilder.append(attackerOutputName).append("'s ").append(item).append(" was burnt up!");
                            break;

                        default:
                            toAppendBuilder.append(attackerOutputName).append(" lost its").append(item).append("!");
                            break;
                    }
                } else {
                    String itemId = toId(item);
                    switch (itemId) {
                        case "airballoon":
                            toAppendBuilder.append(attackerOutputName).append("'s Air Balloon popped!");
                            break;

                        case "focussash":
                            toAppendBuilder.append(attackerOutputName).append(" hung on using its Focus Sash!");
                            break;

                        case "focusband":
                            toAppendBuilder.append(attackerOutputName).append(" hung on using its Focus Band!");
                            break;

                        case "mentalherb":
                            toAppendBuilder.append(attackerOutputName).append(" used its Mental Herb to come back to its senses!");
                            break;

                        case "whiteherb":
                            toAppendBuilder.append(attackerOutputName).append("restored its status using its White Herb!");
                            break;

                        case "ejectbutton":
                            toAppendBuilder.append(attackerOutputName).append(" is switched out with the Eject Button!");
                            break;

                        case "redcard":
                            defenderOutputName = getPrintableOutputPokemonSide(ofSource, false);
                            toAppendBuilder.append(attackerOutputName).append(" held up its Red Card against ").append(defenderOutputName).append("!");
                            break;

                        default:
                            toAppendBuilder.append(attackerOutputName).append("'s ").append(item).append(" activated!");
                            break;
                    }
                }

                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-ability":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                ability = split[1];

                if (fromEffect != null) {
                    switch (getPrintable(fromEffectId)) {
                        case "trace":
                            defenderOutputName = getPrintableOutputPokemonSide(ofSource, false);
                            toAppendBuilder.append(attackerOutputName).append(" traced ").append(defenderOutputName).append("'s ").append(getPrintable(ability)).append("!");
                            break;

                        case "roleplay":
                            defenderOutputName = getPrintableOutputPokemonSide(ofSource, false);
                            toAppendBuilder.append(attackerOutputName).append(" copied ").append(defenderOutputName).append("'s ").append(getPrintable(ability)).append("!");
                            break;

                        case "mummy":
                            toAppendBuilder.append(attackerOutputName).append("'s Ability became Mummy!");
                            break;
                    }
                } else {
                    switch (toId(ability)) {
                        case "pressure":
                            toAppendBuilder.append(attackerOutputName).append(" is exerting its pressure!");
                            break;

                        case "moldbreaker":
                            toAppendBuilder.append(attackerOutputName).append(" breaks the mold!");
                            break;

                        case "turboblaze":
                            toAppendBuilder.append(attackerOutputName).append(" is radiating a blazing aura!");
                            break;

                        case "teravolt":
                            toAppendBuilder.append(attackerOutputName).append(" is radiating a bursting aura!");
                            break;

                        case "intimidate":
                            toAppendBuilder.append(attackerOutputName).append(" intimidates ").append(getPrintable(ofSource)).append("!");
                            break;

                        case "unnerve":
                            if (split[0].startsWith("p2")) {
                                side = "your team";
                            } else {
                                side = "the opposing team";
                            }
                            toAppendBuilder.append(attackerOutputName).append(" 's Unnerve makes ").append(side).append(" too nervous to eat Berries!");
                            break;

                        case "aurabreak":
                            toAppendBuilder.append(attackerOutputName).append(" reversed all other Pokémon's auras!");
                            break;

                        case "fairyaura":
                            toAppendBuilder.append(attackerOutputName).append(" is radiating a fairy aura!");
                            break;

                        case "darkaura":
                            toAppendBuilder.append(attackerOutputName).append(" is radiating a dark aura!");
                            break;

                        case "airlock":
                        case "cloudnine":
                            toAppendBuilder.append("The effects of weather disappeared.");
                            break;

                        default:
                            toAppendBuilder.append(attackerOutputName).append(" has ").append(getPrintable(ability)).append("!");
                            break;
                    }
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-endability":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                ability = split[1];

                if (fromEffect != null) {
                    switch (getPrintable(fromEffectId)) {
                        case "mummy":
                            attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                            toAppendBuilder.append("(").append(attackerOutputName).append("'s Ability was previously ").append(getPrintable(ability)).append(")");
                            break;

                        default:
                            toAppendBuilder.append(attackerOutputName).append("\\'s Ability was suppressed!");
                            break;
                    }
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-transform":
                attacker = getPrintableOutputPokemonSide(split[0]);
                defender = getPrintable(split[1]);
                toAppend = attacker + " transformed into " + defender + "!";
                toAppendSpannable = new SpannableString(toAppend);
                break;

            case "-formechange":
                // nothing here
                toAppendSpannable = new SpannableString("");
                break;

            case "-start":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                switch (getPrintable(toId(split[1]))) {
                    case "typechange":
                        if (fromEffect != null) {
                            if (getPrintable(fromEffectId).equals("reflecttype")) {
                                toAppendBuilder.append(attackerOutputName).append("'s type changed to match ").append(getPrintable(ofSource)).append("'s!");
                            } else {
                                toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" made it the ").append(getPrintable(split[2])).append(" type!");
                            }
                        } else {
                            toAppendBuilder.append(attackerOutputName).append(" transformed into the ").append(getPrintable(split[2])).append(" type!");
                        }
                        break;

                    case "typeadd":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append(getPrintable(split[2])).append(" type was added to ").append(attackerOutputName).append(" type!");
                        break;

                    case "powertrick":
                        toAppendBuilder.append(attackerOutputName).append(" switched its Attack and Defense!");
                        break;

                    case "foresight":
                    case "miracleeye":
                        toAppendBuilder.append(attackerOutputName).append(" was identified!");
                        break;

                    case "telekinesis":
                        toAppendBuilder.append(attackerOutputName).append(" was hurled into the air!");
                        break;

                    case "confusion":
                        if (messageDetails.contains("[already]")) {
                            toAppendBuilder.append(attackerOutputName).append(" is already confused!");
                        } else {
                            toAppendBuilder.append(attackerOutputName).append(" became confused!");
                        }
                        break;

                    case "leechseed":
                        toAppendBuilder.append(attackerOutputName).append(" was seeded!");
                        break;

                    case "mudsport":
                        toAppendBuilder.append("Electricity's power was weakened!");
                        break;

                    case "watersport":
                        toAppendBuilder.append("Fire's power was weakened!");
                        break;

                    case "yawn":
                        toAppendBuilder.append(attackerOutputName).append(" grew drowsy!");
                        break;

                    case "flashfire":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append("The power of ").append(attackerOutputName).append("'s Fire-type moves rose!");
                        break;

                    case "taunt":
                        toAppendBuilder.append(attackerOutputName).append(" fell for the taunt!");
                        break;

                    case "imprison":
                        toAppendBuilder.append(attackerOutputName).append(" sealed the opponent's move(s)!");
                        break;

                    case "disable":
                        toAppendBuilder.append(attackerOutputName).append("'s").append(getPrintable(split[2])).append(" was disabled!");
                        break;

                    case "embargo":
                        toAppendBuilder.append(attackerOutputName).append(" can't use items anymore!");
                        break;

                    case "ingrain":
                        toAppendBuilder.append(attackerOutputName).append(" planted its roots!");
                        break;

                    case "aquaring":
                        toAppendBuilder.append(attackerOutputName).append(" surrounded itself with a veil of water!");
                        break;

                    case "stockpile1":
                        toAppendBuilder.append(attackerOutputName).append(" stockpiled 1!");
                        break;

                    case "stockpile2":
                        toAppendBuilder.append(attackerOutputName).append(" stockpiled 2!");
                        break;

                    case "stockpile3":
                        toAppendBuilder.append(attackerOutputName).append(" stockpiled 3!");
                        break;

                    case "perish0":
                        toAppendBuilder.append(attackerOutputName).append("'s perish count fell to 0.");
                        break;

                    case "perish1":
                        toAppendBuilder.append(attackerOutputName).append("'s perish count fell to 1.");
                        break;

                    case "perish2":
                        toAppendBuilder.append(attackerOutputName).append("'s perish count fell to 2.");
                        break;

                    case "perish3":
                        toAppendBuilder.append(attackerOutputName).append("'s perish count fell to 3.");
                        break;

                    case "encore":
                        toAppendBuilder.append(attackerOutputName).append(" received an encore!");
                        break;

                    case "bide":
                        toAppendBuilder.append(attackerOutputName).append(" is storing energy!");
                        break;

                    case "slowstart":
                        toAppendBuilder.append(attackerOutputName).append(" can't get it going because of its Slow Start!");
                        break;

                    case "attract":
                        if (fromEffect != null) {
                            toAppendBuilder.append(attackerOutputName).append(" fell in love from the ").append(getPrintable(fromEffect)).append("!");
                        } else {
                            toAppendBuilder.append(attackerOutputName).append(" fell in love!");
                        }
                        break;

                    case "autotomize":
                        toAppendBuilder.append(attackerOutputName).append(" became nimble!");
                        break;

                    case "focusenergy":
                        toAppendBuilder.append(attackerOutputName).append(" is getting pumped!");
                        break;

                    case "curse":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append(getPrintableOutputPokemonSide(ofSource)).append(" cut its own HP and laid a curse on ").append(attackerOutputName).append("!");
                        break;

                    case "nightmare":
                        toAppendBuilder.append(attackerOutputName).append(" began having a nightmare!");
                        break;

                    case "magnetrise":
                        toAppendBuilder.append(attackerOutputName).append(" levitated with electromagnetism!");
                        break;

                    case "smackdown":
                        toAppendBuilder.append(attackerOutputName).append(" fell straight down!");
                        break;

                    case "substitute":
                        if (messageDetails.contains("[damage]")) {
                            attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                            toAppendBuilder.append("The substitute took damage for ").append(attackerOutputName).append("!");
                        } else if (messageDetails.contains("[block]")) {
                            toAppendBuilder.append("But it failed!");
                        } else if (messageDetails.contains("[already]")) {
                            toAppendBuilder.append(attackerOutputName).append(" already has a substitute!");
                        } else {
                            toAppendBuilder.append(attackerOutputName).append(" put in a substitute!");
                        }
                        break;

                    case "uproar":
                        if (messageDetails.contains("[upkeep]")) {
                            toAppendBuilder.append(attackerOutputName).append(" is making an uproar!");
                        } else {
                            toAppendBuilder.append(attackerOutputName).append(" caused an uproar!");
                        }
                        break;

                    case "doomdesire":
                        toAppendBuilder.append(attackerOutputName).append(" chose Doom Desire as its destiny!");
                        break;

                    case "futuresight":
                        toAppendBuilder.append(attackerOutputName).append(" foresaw an attack!");
                        break;

                    case "mimic":
                        toAppendBuilder.append(attackerOutputName).append(" learned ").append(getPrintable(split[2])).append("!");
                        break;

                    case "followme":
                    case "ragepowder":
                        toAppendBuilder.append(attackerOutputName).append(" became the center of attention!");
                        break;

                    case "powder":
                        toAppendBuilder.append(attackerOutputName).append(" is covered in powder!");
                        break;

                    default:
                        toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(split[1])).append(" started!");
                        break;
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-end":
                attacker = split[0];
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                switch (getPrintable(toId(split[1]))) {
                    case "powertrick":
                        toAppendBuilder.append(attackerOutputName).append(" switched its Attack and Defense!");
                        break;

                    case "telekinesis":
                        toAppendBuilder.append(attackerOutputName).append(" was freed from the telekinesis!");
                        break;

                    case "confusion":
                        if (fromEffect.contains("item:")) {
                            toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(fromEffect)).append(" snapped out of its confusion!");
                        } else {
                            if (attacker.startsWith("p2")) {
                                toAppendBuilder.append(attackerOutputName).append(" snapped out of confusion!");
                            } else {
                                toAppendBuilder.append(attackerOutputName).append(" snapped out of its confusion.");
                            }
                        }
                        break;

                    case "leechseed":
                        if (fromEffect != null && fromEffectId.equals("rapidspin")) {
                            toAppendBuilder.append(attackerOutputName).append(" was freed from Leech Seed!");
                        }
                        break;

                    case "healblock":
                        toAppendBuilder.append(attackerOutputName).append("'s Heal Block wore off!");
                        break;

                    case "taunt":
                        toAppendBuilder.append(attackerOutputName).append("'s taunt wore off!");
                        break;

                    case "disable":
                        toAppendBuilder.append(attackerOutputName).append(" is no longer disabled!");
                        break;

                    case "embargo":
                        toAppendBuilder.append(attackerOutputName).append(" can use items again!");
                        break;

                    case "torment":
                        toAppendBuilder.append(attackerOutputName).append("'s torment wore off!");
                        break;

                    case "encore":
                        toAppendBuilder.append(attackerOutputName).append("'s encore ended!");
                        break;

                    case "bide":
                        toAppendBuilder.append(attackerOutputName).append(" unleashed energy!");
                        break;

                    case "magnetrise":
                        if (attacker.startsWith("p2")) {
                            toAppendBuilder.append("The electromagnetism of ").append(attackerOutputName).append(" wore off!");
                        } else {
                            toAppendBuilder.append(attackerOutputName).append("s electromagnetism wore off!");
                        }
                        break;

                    case "perishsong":
                        break;

                    case "substitute":
                        toAppendBuilder.append(attackerOutputName).append("'s substitute faded!");
                        break;

                    case "uproar":
                        toAppendBuilder.append(attackerOutputName).append(" calmed down.");
                        break;

                    case "stockpile":
                        toAppendBuilder.append(attackerOutputName).append("'s stockpiled effect wore off!");
                        break;

                    case "infestation":
                        toAppendBuilder.append(attackerOutputName).append(" was freed from Infestation!");
                        break;

                    default:
                        if (split[1].contains("move:")) {
                            toAppendBuilder.append(attackerOutputName).append(" took the ").append(getPrintable(split[1])).append(" attack!");
                        } else {
                            toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(split[1])).append(" ended!");
                        }
                        break;
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-singleturn":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                switch (getPrintable(toId(split[1]))) {
                    case "roost":
                        toAppendBuilder.append(attackerOutputName).append(" landed on the ground!");
                        break;

                    case "quickguard":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append("Quick Guard protected ").append(attackerOutputName).append(" landed on the ground!");
                        break;

                    case "wideguard":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append("Wide Guard protected ").append(attackerOutputName).append(" landed on the ground!");
                        break;

                    case "protect":
                        toAppendBuilder.append(attackerOutputName).append(" protected itself!");
                        break;

                    case "endure":
                        toAppendBuilder.append(attackerOutputName).append(" braced itself!");
                        break;

                    case "helpinghand":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append(getPrintableOutputPokemonSide(ofSource)).append(" is ready to help ").append(attackerOutputName).append("!");
                        break;

                    case "focuspunch":
                        toAppendBuilder.append(attackerOutputName).append(" is tightening its focus!");
                        break;

                    case "snatch":
                        toAppendBuilder.append(attackerOutputName).append("  waits for a target to make a move!");
                        break;

                    case "magiccoat":
                        toAppendBuilder.append(attackerOutputName).append(" shrouded itself with Magic Coat!'");
                        break;

                    case "matblock":
                        toAppendBuilder.append(attackerOutputName).append(" intends to flip up a mat and block incoming attacks!");
                        break;

                    case "electrify":
                        toAppendBuilder.append(attackerOutputName).append("'s moves have been electrified!");
                        break;
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-singlemove":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                switch (getPrintable(toId(split[1]))) {
                    case "grudge":
                        toAppendBuilder.append(attackerOutputName).append(" wants its target to bear a grudge!");
                        break;
                    case "destinybond":
                        toAppendBuilder.append(attackerOutputName).append(" is trying to take its foe down with it!");
                        break;
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-activate":
                attacker = split[0];
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                switch (getPrintable(toId(split[1]))) {
                    case "confusion":
                        toAppendBuilder.append(attackerOutputName).append(" is confused!");
                        break;

                    case "destinybond":
                        toAppendBuilder.append(attackerOutputName).append(" took its attacker down with it!");
                        break;

                    case "snatch":
                        toAppendBuilder.append(attackerOutputName).append(" snatched ").append(getPrintable(ofSource)).append("'s move!");
                        break;

                    case "grudge":
                        toAppendBuilder.append(attackerOutputName).append("'s").append(getPrintable(split[2])).append(" lost all its PP due to the grudge!");
                        break;

                    case "quickguard":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append("Quick Guard protected ").append(attackerOutputName).append("!");
                        break;

                    case "wideguard":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append("Wide Guard protected ").append(attackerOutputName).append("!");
                        break;

                    case "protect":
                        toAppendBuilder.append(attackerOutputName).append(" protected itself!");
                        break;

                    case "substitute":
                        if (messageDetails.contains("[damage]")) {
                            attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                            toAppendBuilder.append("The substitute took damage for ").append(attackerOutputName).append(" protected itself!");
                        } else if (messageDetails.contains("[block]")) {
                            toAppendBuilder.append(attackerOutputName).append("'s Substitute blocked").append(getPrintable(split[2])).append("!");
                        }
                        break;

                    case "attract":
                        toAppendBuilder.append(attackerOutputName).append(" is in love with ").append(getPrintable(ofSource)).append("!");
                        break;

                    case "bide":
                        toAppendBuilder.append(attackerOutputName).append(" is storing energy!");
                        break;

                    case "mist":
                        toAppendBuilder.append(attackerOutputName).append(" is protected by the mist!");
                        break;

                    case "trapped":
                        toAppendBuilder.append(attackerOutputName).append(" can no longer escape!");
                        break;

                    case "stickyweb":
                        toAppendBuilder.append(attackerOutputName).append(" was caught in a sticky web!");
                        break;

                    case "happyhour":
                        toAppendBuilder.append("Everyone is caught up in the happy atmosphere!");
                        break;

                    case "celebrate":
                        if (attacker.startsWith("p2")) {
                            side = mPlayer2;
                        } else {
                            side = mPlayer1;
                        }
                        toAppendBuilder.append("Congratulations, ").append(side).append("!");

                        break;

                    case "trick":
                    case "switcheroo":
                        toAppendBuilder.append(attackerOutputName).append(" switched items with its target!");
                        break;

                    case "brickbreak":
                        if (toId(ofSource).startsWith("p2")) {
                            side = "the opposing team";
                        } else {
                            side = "your team";
                        }
                        toAppendBuilder.append(attackerOutputName).append(" shattered ").append(side).append(" protections!");
                        break;

                    case "pursuit":
                        toAppendBuilder.append(attackerOutputName).append(" is being sent back!");
                        break;

                    case "feint":
                        toAppendBuilder.append(attackerOutputName).append(" fell for the feint!");
                        break;

                    case "spite":
                        toAppendBuilder.append("It reduced the PP of ").append(attackerOutputName).append("'s ").append(getPrintable(split[2])).append(" by ").append(getPrintable(split[3])).append("!");
                        break;

                    case "gravity":
                        toAppendBuilder.append(attackerOutputName).append(" couldn't stay airborne because of gravity!");
                        break;

                    case "magnitude":
                        toAppendBuilder.append("Magnitude ").append(getPrintable(split[2])).append("!");
                        break;

                    case "sketch":
                        toAppendBuilder.append(attackerOutputName).append(" sketched ").append(getPrintable(split[2])).append("!");
                        break;

                    case "skillswap":
                        toAppendBuilder.append(attackerOutputName).append(" swapped Abilities with its target!");
                        if (ofSource != null) {
                            toAppendBuilder.append("\n").append(attackerOutputName).append(" acquired ").append(getPrintable(split[2])).append("!");
                            toAppendBuilder.append("\n").append(getPrintable(ofSource)).append(" acquired ").append(getPrintable(split[3])).append("!");
                        }
                        break;

                    case "charge":
                        toAppendBuilder.append(attackerOutputName).append(" began charging power!");
                        break;

                    case "struggle":
                        toAppendBuilder.append(attackerOutputName).append(" has no moves left!");
                        break;

                    case "bind":
                        toAppendBuilder.append(attackerOutputName).append(" was squeezed by ").append(getPrintable(ofSource)).append("!");
                        break;

                    case "wrap":
                        toAppendBuilder.append(attackerOutputName).append(" was wrapped by ").append(getPrintable(ofSource)).append("!");
                        break;

                    case "clamp":
                        toAppendBuilder.append(getPrintable(ofSource)).append(" clamped ").append(attackerOutputName).append("!");
                        break;

                    case "whirlpool":
                        toAppendBuilder.append(attackerOutputName).append(" became trapped in the vortex!");
                        break;

                    case "firespin":
                        toAppendBuilder.append(attackerOutputName).append(" became trapped in the fiery vortex!");
                        break;

                    case "magmastorm":
                        toAppendBuilder.append(attackerOutputName).append(" became trapped by swirling magma!");
                        break;

                    case "sandtomb":
                        toAppendBuilder.append(attackerOutputName).append(" became trapped by Sand Tomb!");
                        break;

                    case "infestation":
                        toAppendBuilder.append(attackerOutputName).append(" has been afflicted with an infestation by ").append(getPrintable(ofSource)).append("!");
                        break;

                    case "afteryou":
                        toAppendBuilder.append(attackerOutputName).append(" took the kind offer!");
                        break;

                    case "quash":
                        toAppendBuilder.append(attackerOutputName).append("'s move was postponed!");
                        break;

                    case "powersplit":
                        toAppendBuilder.append(attackerOutputName).append(" shared its power with the target!");
                        break;

                    case "guardsplit":
                        toAppendBuilder.append(attackerOutputName).append(" shared its guard with the target!");
                        break;

                    case "ingrain":
                        toAppendBuilder.append(attackerOutputName).append(" anchored itself with its roots!");
                        break;

                    case "matblock":
                        toAppendBuilder.append(getPrintable(split[2])).append(" was blocked by the kicked-up mat!");
                        break;

                    case "powder":
                        toAppendBuilder.append("When the flame touched the powder on the Pokémon, it exploded!");
                        break;

                    case "fairylock":
                        toAppendBuilder.append("No one will be able to run away during the next turn!");
                        break;

                    //abilities
                    case "sturdy":
                        toAppendBuilder.append(attackerOutputName).append(" held on thanks to Sturdy!");
                        break;

                    case "magicbounce":
                    case "magiccoat":
                    case "rebound":
                        break;

                    case "wonderguard":
                        toAppendBuilder.append(attackerOutputName).append("'s Wonder Guard evades the attack!");
                        break;

                    case "speedboost":
                        toAppendBuilder.append(attackerOutputName).append("'s' Speed Boost increases its speed!");
                        break;

                    case "forewarn":
                        toAppendBuilder.append(attackerOutputName).append("'s Forewarn alerted it to ").append(getPrintable(split[2])).append("!");
                        break;

                    case "anticipation":
                        toAppendBuilder.append(attackerOutputName).append(" shuddered!");
                        break;

                    case "telepathy":
                        toAppendBuilder.append(attackerOutputName).append(" avoids attacks by its ally Pok&#xE9;mon!");
                        break;

                    case "suctioncups":
                        toAppendBuilder.append(attackerOutputName).append(" anchors itself!");
                        break;

                    case "symbiosis":
                        attackerOutputName = getPrintableOutputPokemonSide(split[0], false);
                        toAppendBuilder.append(getPrintable(ofSource)).append(" shared its ").append(getPrintable(split[2])).append(" with ").append(attackerOutputName);
                        break;

                    //items
                    case "custapberry":
                    case "quickclaw":
                        toAppendBuilder.append(attackerOutputName).append("'s ").append(getPrintable(split[1])).append(" let it move first!");
                        break;

                    case "leppaberry":
                        toAppendBuilder.append(attackerOutputName).append(" restored ").append(getPrintable(split[2])).append("'s PP using its Leppa Berry!");
                        break;

                    default:
                        toAppendBuilder.append(attackerOutputName).append("'s ").append(" activated!");
                        break;
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-sidestart":
                if (messageDetails.startsWith("p2")) {
                    side = "the opposing team";
                } else {
                    side = "your team";
                }

                fromEffect = split[1];
                fromEffectId = getPrintable(toId(fromEffect));
                switch (fromEffectId) {
                    case "stealthrock":
                        toAppendBuilder.append("Pointed stones float in the air around ").append(side).append("!");
                        break;

                    case "spikes":
                        toAppendBuilder.append("Spikes were scattered all around the feet of ").append(side).append("!");
                        break;

                    case "toxicspikes":
                        toAppendBuilder.append("Toxic spikes were scattered all around the feet of ").append(side).append("!");
                        break;

                    case "stickyweb":
                        toAppendBuilder.append("A sticky web spreads out beneath ").append(side).append("'s feet!");
                        break;

                    case "tailwind":
                        toAppendBuilder.append("The tailwind blew from behind ").append(side).append("!");
                        break;

                    case "reflect":
                        toAppendBuilder.append("Reflect raised ").append(side).append("'s Defense!");
                        break;

                    case "lightscreen":
                        toAppendBuilder.append("Light Screen raised ").append(side).append("'s Special Defense!");
                        break;

                    case "safeguard":
                        side = Character.toUpperCase(side.charAt(0)) + side.substring(1);
                        toAppendBuilder.append(side).append(" became cloaked in a mystical veil!");
                        break;

                    case "mist":
                        side = Character.toUpperCase(side.charAt(0)) + side.substring(1);
                        toAppendBuilder.append(side).append(" became shrouded in mist!");
                        break;

                    case "luckychant":
                        toAppendBuilder.append("The Lucky Chant shielded ").append(side).append(" from critical hits!");
                        break;

                    case "firepledge":
                        toAppendBuilder.append("A sea of fire enveloped ").append(side).append("!");
                        break;

                    case "waterpledge":
                        toAppendBuilder.append("A rainbow appeared in the sky on ").append(side).append("'s side!");
                        break;

                    case "grasspledge":
                        toAppendBuilder.append("A swamp enveloped ").append(side).append("!");
                        break;

                    default:
                        toAppendBuilder.append(getPrintable(fromEffect)).append(" started!");
                        break;
                }

                toAppendSpannable = new SpannableStringBuilder(toAppendBuilder);
                break;

            case "-sideend":
                if (messageDetails.startsWith("p2")) {
                    side = "the opposing team";
                } else {
                    side = "your team";
                }

                fromEffect = split[1];
                fromEffectId = getPrintable(toId(fromEffect));

                switch (fromEffectId) {
                    case "stealthrock":
                        toAppendBuilder.append("The pointed stones disappeared from around ").append(side).append("!");
                        break;

                    case "spikes":
                        toAppendBuilder.append("The spikes disappeared from around ").append(side).append("!");
                        break;

                    case "toxicspikes":
                        toAppendBuilder.append("The poison spikes disappeared from around ").append(side).append("!");
                        break;

                    case "stickyweb":
                        toAppendBuilder.append("The sticky web has disappeared from beneath ").append(side).append("'s feet!");
                        break;

                    case "tailwind":
                        side = Character.toUpperCase(side.charAt(0)) + side.substring(1);
                        toAppendBuilder.append(side).append("'s tailwind petered out!");
                        break;

                    case "reflect":
                        side = Character.toUpperCase(side.charAt(0)) + side.substring(1);
                        toAppendBuilder.append(side).append("'s Reflect wore off!");
                        break;

                    case "lightscreen":
                        side = Character.toUpperCase(side.charAt(0)) + side.substring(1);
                        toAppendBuilder.append(side).append("'s Reflect wore off!");
                        break;

                    case "safeguard":
                        side = Character.toUpperCase(side.charAt(0)) + side.substring(1);
                        toAppendBuilder.append(side).append(" is no longer protected by Safeguard!");
                        break;

                    case "mist":
                        side = Character.toUpperCase(side.charAt(0)) + side.substring(1);
                        toAppendBuilder.append(side).append(" is no longer protected by mist!");
                        break;

                    case "luckychant":
                        side = Character.toUpperCase(side.charAt(0)) + side.substring(1);
                        toAppendBuilder.append(side).append("'s Lucky Chant wore off!");
                        break;

                    case "firepledge":
                        toAppendBuilder.append("The sea of fire around ").append(side).append(" disappeared!");
                        break;

                    case "waterpledge":
                        toAppendBuilder.append("The rainbow on ").append(side).append("'s side disappeared!");
                        break;

                    case "grasspledge":
                        toAppendBuilder.append("The swamp around ").append(side).append(" disappeared!");
                        break;

                    default:
                        toAppendBuilder.append(getPrintable(fromEffect)).append(" ended!");
                        break;
                }


                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;


            case "-weather":
                String weather = split[0];
                boolean upkeep = false;
                if (split.length > 1) {
                    upkeep = true;
                }
                switch (weather) {
                    case "RainDance":
                        if (upkeep) {
                            toAppendBuilder.append("Rain continues to fall!");
                        } else {
                            toAppendBuilder.append("It started to rain!");
                            weatherExist = true;
                        }
                        break;
                    case "Sandstorm":
                        if (upkeep) {
                            toAppendBuilder.append("The sandstorm rages.");
                        } else {
                            toAppendBuilder.append("A sandstorm kicked up!");
                            weatherExist = true;
                        }
                        break;
                    case "SunnyDay":
                        if (upkeep) {
                            toAppendBuilder.append("The sunlight is strong!");
                        } else {
                            toAppendBuilder.append("The sunlight turned harsh!");
                            weatherExist = true;
                        }
                        break;
                    case "Hail":
                        if (upkeep) {
                            toAppendBuilder.append("The hail crashes down.");
                        } else {
                            toAppendBuilder.append("It started to hail!");
                            weatherExist = true;
                        }
                        break;
                    case "none":
                        if (weatherExist) {
                            switch (currentWeather) {
                                case "RainDance":
                                    toAppendBuilder.append("The rain stopped.");
                                    break;
                                case "SunnyDay":
                                    toAppendBuilder.append("The sunlight faded.");
                                    break;
                                case "Sandstorm":
                                    toAppendBuilder.append("The sandstorm subsided.");
                                    break;
                                case "Hail":
                                    toAppendBuilder.append("The hail stopped.");
                                    break;
                            }
                        }
                        weatherExist = false;
                        break;
                }
                currentWeather = weather;
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;


            case "-fieldstart":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                switch (getPrintable(toId(split[1]))) {
                    case "trickroom":
                        toAppendBuilder.append(attackerOutputName).append(" twisted the dimensions!");
                        break;

                    case "wonderroom":
                        toAppendBuilder.append("It created a bizarre area in which the Defense and Sp. Def stats are swapped!");
                        break;

                    case "magicroom":
                        toAppendBuilder.append("It created a bizarre area in which Pok&#xE9;mon's held items lose their effects!");
                        break;

                    case "gravity":
                        toAppendBuilder.append("Gravity intensified!");
                        break;

                    case "mudsport":
                        toAppendBuilder.append("Electric's power was weakened!");
                        break;

                    case "watersport":
                        toAppendBuilder.append("Fire's power was weakened!");
                        break;

                    default:
                        toAppendBuilder.append(getPrintable(split[1])).append(" started!");
                        break;
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-fieldend":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                switch (getPrintable(toId(split[1]))) {
                    case "trickroom":
                        toAppendBuilder.append("The twisted dimensions returned to normal!");
                        break;

                    case "wonderroom":
                        toAppendBuilder.append("'Wonder Room wore off, and the Defense and Sp. Def stats returned to normal!");
                        break;

                    case "magicroom":
                        toAppendBuilder.append("Magic Room wore off, and the held items' effects returned to normal!");
                        break;

                    case "gravity":
                        toAppendBuilder.append("Gravity returned to normal!");
                        break;

                    case "mudsport":
                        toAppendBuilder.append("The effects of Mud Sport have faded.");
                        break;

                    case "watersport":
                        toAppendBuilder.append("The effects of Water Sport have faded.");
                        break;

                    default:
                        toAppendBuilder.append(getPrintable(split[1])).append(" ended!");
                        break;
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;

            case "-fieldactivate":
                attackerOutputName = getPrintableOutputPokemonSide(split[0]);
                switch (getPrintable(toId(split[1]))) {
                    case "perishsong":
                        toAppendBuilder.append("All Pok&#xE9;mon hearing the song will faint in three turns!");
                        break;

                    case "payday":
                        toAppendBuilder.append("Coins were scattered everywhere!");
                        break;

                    case "iondeluge":
                        toAppendBuilder.append("A deluge of ions showers the battlefield!");
                        break;

                    default:
                        toAppendBuilder.append(getPrintable(split[1])).append(" hit!");
                        break;
                }
                toAppendSpannable = new SpannableString(toAppendBuilder);
                break;


            case "-message":
                toAppendSpannable = new SpannableString(messageDetails);
                break;

            case "-anim":
                toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                break;

            default:
                toAppendSpannable = new SpannableString(command + ":" + messageDetails);
                break;
        }

        if (messageDetails.contains("[silent]")) {
            return;
        }
        toAppendSpannable.setSpan(new RelativeSizeSpan(0.8f), 0, toAppendSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        appendServerMessage(toAppendSpannable);
    }

    private void appendServerMessage(final Spannable message) {
        if (getView() != null) {
            final TextView chatlog = (TextView) getView().findViewById(R.id.battlelog);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chatlog.append(message);
                    chatlog.append("\n");

                    final ScrollView scrollView = (ScrollView) getView().findViewById(R.id.battlelog_scrollview);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            });
        }
    }
}