package com.pokemonshowdown.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pokemonshowdown.data.BattleFieldData;
import com.pokemonshowdown.data.MyApplication;
import com.pokemonshowdown.data.Onboarding;
import com.pokemonshowdown.data.PokemonTeam;

import java.util.ArrayList;
import java.util.Arrays;


public class FindBattleFragment extends Fragment {
    public final static String FTAG = FindBattleFragment.class.getName();
    public final static String RANDOM_TEAM_NAME = "Random Team";
    private ProgressDialog mWaitingDialog;
    private ArrayList<String> mFormatList;
    private Spinner mPokemonTeamSpinner;
    private ListView mFormatListView;

    private PokemonTeamListArrayAdapter mRandomTeamAdapter;
    private ArrayAdapter<String> mNoTeamsAdapter;
    private PokemonTeamListArrayAdapter mPokemonTeamListArrayAdapter;

    public static FindBattleFragment newInstance() {
        FindBattleFragment fragment = new FindBattleFragment();

        return fragment;
    }

    public FindBattleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_battle, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNoTeamsAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.empty_team_list_filler));
        mRandomTeamAdapter = new PokemonTeamListArrayAdapter(getActivity(), Arrays.asList(new PokemonTeam(RANDOM_TEAM_NAME)));
        mPokemonTeamSpinner = (Spinner) view.findViewById(R.id.teams_spinner);

        setAvailableFormat();
        mWaitingDialog = new ProgressDialog(getActivity());
        mFormatListView = (ListView) view.findViewById(R.id.available_formats);

        TextView findBattle = (TextView) view.findViewById(R.id.find_battle);
        findBattle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //first need to check if the user is logged in
                Onboarding onboarding = Onboarding.get(getActivity().getApplicationContext());
                if (!onboarding.isSignedIn()) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    OnboardingDialog fragment = new OnboardingDialog();
                    fragment.show(fm, OnboardingDialog.OTAG);
                    return;
                }
                // first we look the select format. if random -> send empty /utm
                // else export selected team for showdown verification
                String currentFormatString = (String) mFormatListView.getItemAtPosition(mFormatListView.getCheckedItemPosition());
                if (currentFormatString != null) {
                    BattleFieldData.Format currentFormat = null;
                    currentFormat = BattleFieldData.get(getActivity()).getFormat(currentFormatString);
                    if (currentFormat.isRandomFormat()) {
                        // we send /utm only
                        MyApplication.getMyApplication().sendClientMessage("|/utm");
                        MyApplication.getMyApplication().sendClientMessage("|/search " + MyApplication.toId(currentFormatString));
                    } else {
                        //we need to send the team for verification
                        Object pokemonTeamObject = mPokemonTeamSpinner.getSelectedItem();
                        // if we have no teams
                        if (!(pokemonTeamObject instanceof PokemonTeam)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(R.string.error_dialog_title);
                            builder.setIcon(android.R.drawable.ic_dialog_alert);
                            builder.setMessage(R.string.no_teams);
                            final AlertDialog alert = builder.create();
                            getActivity().runOnUiThread(new java.lang.Runnable() {
                                public void run() {
                                    alert.show();
                                }
                            });
                            return;
                        }
                        PokemonTeam pokemonTeam = (PokemonTeam) pokemonTeamObject;
                        String teamVerificationString = pokemonTeam.exportForVerification(getActivity().getApplicationContext());
                        MyApplication.getMyApplication().sendClientMessage("|/utm " + teamVerificationString);
                        MyApplication.getMyApplication().sendClientMessage("|/search " + MyApplication.toId(currentFormatString));
                    }
                }
            }
        });

        TextView watchBattle = (TextView) view.findViewById(R.id.watch_battle);
        watchBattle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.getMyApplication().sendClientMessage("|/cmd roomlist");
                mWaitingDialog.setMessage(getResources().getString(R.string.download_matches_inprogress));
                mWaitingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mWaitingDialog.setCancelable(true);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mWaitingDialog.show();
                    }
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // we relaod the pokemon teams
        PokemonTeam.loadPokemonTeams(getActivity());
        mPokemonTeamListArrayAdapter = new PokemonTeamListArrayAdapter(getActivity(), PokemonTeam.getPokemonTeamList());
        //we execute a click on the format view in order to select the appropriate team for the current format
        mFormatListView.performItemClick(null, mFormatListView.getCheckedItemPosition(), mFormatListView.getCheckedItemPosition());
    }

    public void setAvailableFormat() {
        View v = getView();
        if (v == null) {
            return;
        }

        mFormatList = new ArrayList<>();

        ArrayList<BattleFieldData.FormatType> formatTypes = BattleFieldData.get(getActivity()).getFormatTypes();
        for (BattleFieldData.FormatType formatType : formatTypes) {
            ArrayList<String> result = formatType.getSearchableFormatList();
            for (String name : result) {
                mFormatList.add(name);
            }
        }

        final ListView listView = (ListView) v.findViewById(R.id.available_formats);
        listView.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.fragment_user_list, mFormatList));
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.requestFocusFromTouch();
        BattleFieldData.get(getActivity()).setCurrentFormat(0);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BattleFieldData.get(getActivity()).setCurrentFormat(position);
                if (mFormatList.size() == 0) {
                    //can happen when no internet
                    return;
                }
                String currentFormatString = (String) listView.getItemAtPosition(position);
                BattleFieldData.Format currentFormat = null;
                if (currentFormatString != null) {
                    currentFormat = BattleFieldData.get(getActivity()).getFormat(currentFormatString);
                    if (currentFormat != null) {
                        if (currentFormat.isRandomFormat()) {
                            mPokemonTeamSpinner.setAdapter(mRandomTeamAdapter);
                            mPokemonTeamSpinner.setEnabled(false);
                        } else {
                            if (PokemonTeam.getPokemonTeamList().size() > 0) {
                                int currentSelectedTeam = mPokemonTeamSpinner.getSelectedItemPosition();
                                mPokemonTeamSpinner.setAdapter(mPokemonTeamListArrayAdapter);
                                mPokemonTeamSpinner.setEnabled(true);
                                int newSelectedTeam = -1;
                                for (int i = 0; i < PokemonTeam.getPokemonTeamList().size(); i++) {
                                    if (PokemonTeam.getPokemonTeamList().get(i).getTier().equals(currentFormatString)) {
                                        newSelectedTeam = i;
                                        break;
                                    }
                                }
                                if (newSelectedTeam > -1) {
                                    mPokemonTeamSpinner.setSelection(newSelectedTeam);
                                } else {
                                    mPokemonTeamSpinner.setSelection(currentSelectedTeam);
                                }
                            } else {
                                //there are no teams, we fill the spinner with a filler item an disable it
                                mPokemonTeamSpinner.setAdapter(mNoTeamsAdapter);
                                mPokemonTeamSpinner.setEnabled(false);
                            }
                        }
                    }
                }
            }
        });
        //this will call the onitemclick listener to set the ocrrect team according to the format at position 0
        listView.performItemClick(null, 0, 0);
    }

    public void dismissWaitingDialog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWaitingDialog.dismiss();
            }
        });

    }

}
