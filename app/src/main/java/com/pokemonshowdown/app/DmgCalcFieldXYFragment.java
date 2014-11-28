package com.pokemonshowdown.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pokemonshowdown.data.FieldFragment;

public class DmgCalcFieldXYFragment extends FieldFragment {

    private DmgCalcActivity.FieldConditionsListener mFieldConditionsListener = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFieldConditionsListener = (DmgCalcActivity.FieldConditionsListener) getArguments().getSerializable("listener");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dmgcalc_field_xy, parent, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final TextView singles = (TextView) getView().findViewById(R.id.dmg_calc_field_singles);
        final TextView doubles = (TextView) getView().findViewById(R.id.dmg_calc_field_doubles);
        final TextView gravity = (TextView) getView().findViewById(R.id.dmg_calc_field_gravity);
        final TextView none = (TextView) getView().findViewById(R.id.dmg_calc_field_none);
        final TextView sun = (TextView) getView().findViewById(R.id.dmg_calc_field_sun);
        final TextView rain = (TextView) getView().findViewById(R.id.dmg_calc_field_rain);
        final TextView sand = (TextView) getView().findViewById(R.id.dmg_calc_field_sand);
        final TextView hail = (TextView) getView().findViewById(R.id.dmg_calc_field_hail);
        final TextView sr = (TextView) getView().findViewById(R.id.dmg_calc_field_sr);
        final TextView spike0 = (TextView) getView().findViewById(R.id.dmg_calc_field_0spike);
        final TextView spike1 = (TextView) getView().findViewById(R.id.dmg_calc_field_1spike);
        final TextView spike2 = (TextView) getView().findViewById(R.id.dmg_calc_field_2spike);
        final TextView spike3 = (TextView) getView().findViewById(R.id.dmg_calc_field_3spike);
        final TextView reflect = (TextView) getView().findViewById(R.id.dmg_calc_field_reflect);
        final TextView lightscreen = (TextView) getView().findViewById(R.id.dmg_calc_field_lightscreen);
        final TextView foresight = (TextView) getView().findViewById(R.id.dmg_calc_field_foresight);
        final TextView helpinghand = (TextView) getView().findViewById(R.id.dmg_calc_field_helpinghand);


        singles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singles.setTypeface(null, Typeface.BOLD);
                doubles.setTypeface(null, Typeface.ITALIC);

                sendUpdateToListeners(DmgCalcActivity.FieldConditions.Singles, true);
            }
        });

        doubles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doubles.setTypeface(null, Typeface.BOLD);
                singles.setTypeface(null, Typeface.ITALIC);

                sendUpdateToListeners(DmgCalcActivity.FieldConditions.Doubles, true);
            }
        });

        gravity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Typeface typeface = gravity.getTypeface();
                if (typeface.isBold()) {
                    gravity.setTypeface(null, Typeface.ITALIC);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.Gravity, false);
                } else {
                    gravity.setTypeface(null, Typeface.BOLD);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.Gravity, true);
                }
            }
        });

        none.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                none.setTypeface(null, Typeface.BOLD);
                sun.setTypeface(null, Typeface.ITALIC);
                rain.setTypeface(null, Typeface.ITALIC);
                sand.setTypeface(null, Typeface.ITALIC);
                hail.setTypeface(null, Typeface.ITALIC);

                sendUpdateToListeners(DmgCalcActivity.FieldConditions.NoWeather, true);
            }
        });

        sun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                none.setTypeface(null, Typeface.ITALIC);
                sun.setTypeface(null, Typeface.BOLD);
                rain.setTypeface(null, Typeface.ITALIC);
                sand.setTypeface(null, Typeface.ITALIC);
                hail.setTypeface(null, Typeface.ITALIC);
                sendUpdateToListeners(DmgCalcActivity.FieldConditions.Sun, true);
            }
        });

        rain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                none.setTypeface(null, Typeface.ITALIC);
                sun.setTypeface(null, Typeface.ITALIC);
                rain.setTypeface(null, Typeface.BOLD);
                sand.setTypeface(null, Typeface.ITALIC);
                hail.setTypeface(null, Typeface.ITALIC);
                sendUpdateToListeners(DmgCalcActivity.FieldConditions.Rain, true);
            }
        });

        sand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                none.setTypeface(null, Typeface.ITALIC);
                sun.setTypeface(null, Typeface.ITALIC);
                rain.setTypeface(null, Typeface.ITALIC);
                sand.setTypeface(null, Typeface.BOLD);
                hail.setTypeface(null, Typeface.ITALIC);
                sendUpdateToListeners(DmgCalcActivity.FieldConditions.Sand, true);
            }
        });

        hail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                none.setTypeface(null, Typeface.ITALIC);
                sun.setTypeface(null, Typeface.ITALIC);
                rain.setTypeface(null, Typeface.ITALIC);
                sand.setTypeface(null, Typeface.ITALIC);
                hail.setTypeface(null, Typeface.BOLD);
                sendUpdateToListeners(DmgCalcActivity.FieldConditions.Hail, true);
            }
        });

        sr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Typeface typeface = sr.getTypeface();
                if (typeface.isBold()) {
                    sr.setTypeface(null, Typeface.ITALIC);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.StealthRock, false);
                } else {
                    sr.setTypeface(null, Typeface.BOLD);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.StealthRock, true);
                }
            }
        });

        spike0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spike0.setTypeface(null, Typeface.BOLD);
                spike1.setTypeface(null, Typeface.ITALIC);
                spike2.setTypeface(null, Typeface.ITALIC);
                spike3.setTypeface(null, Typeface.ITALIC);
                sendUpdateToListeners(DmgCalcActivity.FieldConditions.ZeroSpikes, true);
            }
        });

        spike1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spike0.setTypeface(null, Typeface.ITALIC);
                spike1.setTypeface(null, Typeface.BOLD);
                spike2.setTypeface(null, Typeface.ITALIC);
                spike3.setTypeface(null, Typeface.ITALIC);
                sendUpdateToListeners(DmgCalcActivity.FieldConditions.OneSpikes, true);
            }
        });

        spike2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spike0.setTypeface(null, Typeface.ITALIC);
                spike1.setTypeface(null, Typeface.ITALIC);
                spike2.setTypeface(null, Typeface.BOLD);
                spike3.setTypeface(null, Typeface.ITALIC);
                sendUpdateToListeners(DmgCalcActivity.FieldConditions.TwoSpikes, true);
            }
        });

        spike3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spike0.setTypeface(null, Typeface.ITALIC);
                spike1.setTypeface(null, Typeface.ITALIC);
                spike2.setTypeface(null, Typeface.ITALIC);
                spike3.setTypeface(null, Typeface.BOLD);
                sendUpdateToListeners(DmgCalcActivity.FieldConditions.ThreeSpikes, true);
            }
        });

        reflect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Typeface typeface = reflect.getTypeface();
                if (typeface.isBold()) {
                    reflect.setTypeface(null, Typeface.ITALIC);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.Reflect, false);
                } else {
                    reflect.setTypeface(null, Typeface.BOLD);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.Reflect, true);
                }
            }
        });

        lightscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Typeface typeface = lightscreen.getTypeface();
                if (typeface.isBold()) {
                    lightscreen.setTypeface(null, Typeface.ITALIC);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.LightScreen, false);
                } else {
                    lightscreen.setTypeface(null, Typeface.BOLD);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.LightScreen, true);
                }
            }
        });

        foresight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Typeface typeface = foresight.getTypeface();
                if (typeface.isBold()) {
                    foresight.setTypeface(null, Typeface.ITALIC);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.Foresight, false);
                } else {
                    foresight.setTypeface(null, Typeface.BOLD);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.Foresight, true);
                }
            }
        });

        helpinghand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Typeface typeface = helpinghand.getTypeface();
                if (typeface.isBold()) {
                    helpinghand.setTypeface(null, Typeface.ITALIC);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.HelpingHand, false);
                } else {
                    helpinghand.setTypeface(null, Typeface.BOLD);
                    sendUpdateToListeners(DmgCalcActivity.FieldConditions.HelpingHand, true);
                }
            }
        });
    }
}
