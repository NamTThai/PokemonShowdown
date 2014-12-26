package com.pokemonshowdown.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.pokemonshowdown.data.BattleFieldData;
import com.pokemonshowdown.data.MyApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


public class BattleFieldFragment extends Fragment {
    public final static String BTAG = BattleFieldFragment.class.getName();
    private BattleFieldPagerAdapter mBattleFieldPagerAdapter;
    private ViewPager mViewPager;

    private ArrayList<String> mRoomList;
    private int mPosition;

    private ActionBar.TabListener mTabListener = new ActionBar.TabListener() {
        @Override
        public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {

        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {

        }
    };

    public static BattleFieldFragment newInstance() {
        BattleFieldFragment fragment = new BattleFieldFragment();
        return fragment;
    }
    public BattleFieldFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mRoomList = BattleFieldData.get(getActivity()).getRoomList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_battle_field, container, false);
        mPosition = 0;

        v.setFocusableInTouchMode(true);
        mBattleFieldPagerAdapter = new BattleFieldPagerAdapter(getChildFragmentManager());
        mViewPager = (ViewPager) v.findViewById(R.id.battle_field_pager);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        getActivity().getActionBar().setSelectedNavigationItem(position);
                    }
                });
        mViewPager.setAdapter(mBattleFieldPagerAdapter);
        return v;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActivity().getActionBar().removeAllTabs();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem cancel = menu.findItem(R.id.cancel);
        cancel.setVisible(true);
        cancel.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                removeCurrentRoom();
                return true;
            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ActionBar actionBar = getActivity().getActionBar();

        setAvailableFormat();

        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {

            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {

            }
        };

        for (int i = 0; i < mRoomList.size(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(i == 0 ? mRoomList.get(i) : "battle" + i)
                            .setTabListener(tabListener)
            );
        }

    }

    public void setAvailableFormat() {
        if (mPosition == 0) {
            FindBattleFragment fragment = (FindBattleFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + 0);
            if (fragment != null) {
                fragment.setAvailableFormat();
            }
        }
    }

    public void processServerMessage(String roomId, String message) {
        int index = mRoomList.indexOf(roomId);
        BattleFragment fragment = (BattleFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + index);
        if (fragment != null) {
            fragment.processServerMessage(message);
        }
    }

    private void removeCurrentRoom() {
        ActionBar actionBar = getActivity().getActionBar();
        ActionBar.Tab tab = actionBar.getSelectedTab();
        int tabPosition = tab.getPosition();
        String roomId = mRoomList.get(tabPosition);
        if (roomId.equals("global")) {
            return;
        }
        BattleFragment fragment = (BattleFragment) getChildFragmentManager()
                .findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + tab.getPosition());
        if (fragment != null) {
            if (fragment.getBattling() != 0) {
                MyApplication.getMyApplication().sendClientMessage(roomId + "|/forfeit");
            }
            getChildFragmentManager().beginTransaction().remove(fragment).commit();
        }
        BattleFieldData.get(getActivity()).leaveRoom(roomId);
        mBattleFieldPagerAdapter.notifyDataSetChanged();
        mViewPager.setAdapter(mBattleFieldPagerAdapter);
        actionBar.removeTab(tab);

        FindBattleFragment findBattleFragment = (FindBattleFragment) getChildFragmentManager()
                .findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + 0);

        if (findBattleFragment != null) {
            findBattleFragment.setQuota(false);
        }
        //decrementBattleFragmentTag(tabPosition, tabCount - 1);
    }
/*
    public void decrementBattleFragmentTag(int start, int end) {
        ArrayDeque<String> roomList = new ArrayDeque<>();
        ActionBar actionBar = getActivity().getActionBar();

        for (int i = start; i < end; i++) {
            BattleFragment fragment = (BattleFragment) getChildFragmentManager()
                    .findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + i);

            if (fragment != null) {
                roomList.addLast(fragment.getRoomId());

                getChildFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();

                mRoomList.remove(i);
            }

            actionBar.removeTabAt(i);
        }

        mBattleFieldPagerAdapter.notifyDataSetChanged();
        mViewPager.setAdapter(mBattleFieldPagerAdapter);

        while (!roomList.isEmpty()) {
            String roomId = roomList.pollFirst();
            mRoomList.add(roomId);

            actionBar.addTab(
                    actionBar.newTab()
                            .setText("Battle" + (mRoomList.size() - 1))
                            .setTabListener(mTabListener)
            );
        }

        mBattleFieldPagerAdapter.notifyDataSetChanged();
        mViewPager.setAdapter(mBattleFieldPagerAdapter);

    }*/

    public void processNewRoomRequest(String roomId) {
        ActionBar actionBar = getActivity().getActionBar();
        if (mRoomList.contains(roomId)) {
            actionBar.setSelectedNavigationItem(mRoomList.indexOf(roomId));
        } else {
            mRoomList.add(roomId);
            BattleFieldData.get(getActivity()).joinRoom(roomId, false);
            mBattleFieldPagerAdapter.notifyDataSetChanged();

            actionBar.addTab(
                    actionBar.newTab()
                            .setText("Battle" + (mRoomList.size() - 1))
                            .setTabListener(mTabListener)
            );
            actionBar.setSelectedNavigationItem(mRoomList.indexOf(roomId));

            FindBattleFragment fragment = (FindBattleFragment) getChildFragmentManager()
                    .findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + 0);

            if (fragment != null) {
                fragment.setQuota(true);
            }
        }
    }

    public void generateAvailableWatchBattleDialog() {
        // this is so hacky
        FindBattleFragment fragment = (FindBattleFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + mViewPager.getId() + ":" + 0);
        fragment.dismissWaitingDialog();
        HashMap<String, String> battleList = BattleFieldData.get(getActivity()).getAvailableWatchBattleList();
        if (battleList.isEmpty()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.no_available_battle)
                    .create()
                    .show();
            return;
        }
        final String[] key = new String[battleList.size()];
        String[] value = new String[battleList.size()];
        int count = 0;
        Set<String> iterators = battleList.keySet();
        for (String iterator : iterators) {
            key[count] = iterator;
            value[count] = battleList.get(iterator);
            count++;
        }
        new AlertDialog.Builder(getActivity())
                .setItems(value, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BattleFieldData.get(getActivity()).joinRoom(key[which], true);
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private class BattleFieldPagerAdapter extends FragmentPagerAdapter {
        public BattleFieldPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (i == 0) {
                return FindBattleFragment.newInstance();
            }
            return BattleFragment.newInstance(mRoomList.get(i));
        }

        @Override
        public int getCount() {
            return mRoomList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Battle" + position;
        }
    }

}
