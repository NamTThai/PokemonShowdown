package com.pokemonshowdown.data;

import android.app.Activity;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pokemonshowdown.app.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class SearchableActivity extends ListActivity {
    public final static String STAG = SearchableActivity.class.getName();
    public final static int REQUEST_CODE_SEARCH_POKEMON = 0;
    public final static int REQUEST_CODE_SEARCH_ABILITY = 1;
    public final static int REQUEST_CODE_SEARCH_ITEM = 2;
    public final static int REQUEST_CODE_SEARCH_MOVES = 3;

    public final static String SEARCH_TYPE = "Search Type";
    public final static String SEARCH = "Search";

    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> mAdapterList;
    private int mSearchType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        getActionBar().setTitle(R.string.search_title);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mSearchType = getIntent().getExtras().getInt("Search Type");

        switch (mSearchType) {
            case REQUEST_CODE_SEARCH_POKEMON:
                HashMap<String, String> pokedex = Pokedex.get(getApplicationContext()).getPokedexEntries();
                mAdapterList = new ArrayList<>(pokedex.keySet());
                mAdapter = new PokemonAdapter(this, mAdapterList);
                setListAdapter(mAdapter);
                getActionBar().setTitle(R.string.search_label_pokemon);
                break;
            case REQUEST_CODE_SEARCH_ABILITY:
                HashMap<String, String> abilityDex = AbilityDex.get(getApplicationContext()).getAbilityDexEntries();
                mAdapterList = new ArrayList<>(abilityDex.keySet());
                mAdapter = new AbilityAdapter(this, mAdapterList);
                setListAdapter(mAdapter);
                getActionBar().setTitle(R.string.search_label_ability);
                break;
            case REQUEST_CODE_SEARCH_ITEM:
                HashMap<String, String> itemDex = ItemDex.get(getApplicationContext()).getItemDexEntries();
                mAdapterList = new ArrayList<>(itemDex.keySet());
                mAdapter = new ItemAdapter(this, mAdapterList);
                setListAdapter(mAdapter);
                getActionBar().setTitle(R.string.search_label_item);
                break;
            case REQUEST_CODE_SEARCH_MOVES:
                HashMap<String, String> moveDex = MoveDex.get(getApplicationContext()).getMoveDexEntries();
                mAdapterList = new ArrayList<>(moveDex.keySet());
                mAdapter = new MovesAdapter(this, mAdapterList);
                setListAdapter(mAdapter);
                getActionBar().setTitle(R.string.search_label_moves);
                break;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent();
        intent.putExtra(SEARCH, mAdapterList.get(position));
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            switch (mSearchType) {
                case REQUEST_CODE_SEARCH_POKEMON:
                    searchPokemon(query);
                    break;
                case REQUEST_CODE_SEARCH_ABILITY:
                    searchAbility(query);
                    break;
                case REQUEST_CODE_SEARCH_ITEM:
                    searchItem(query);
                    break;
                case REQUEST_CODE_SEARCH_MOVES:
                    searchMove(query);
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;
            case R.id.menu_search:
                onSearchRequested();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void searchPokemon(String query) {
        HashMap<String, String> pokedex = Pokedex.get(getApplicationContext()).getPokedexEntries();
        mAdapterList = new ArrayList<>();
        for (String pokemonName : pokedex.keySet()) {
            if (pokemonName.contains(query.toLowerCase())) {
                mAdapterList.add(pokemonName);
            }
        }
        mAdapter = new PokemonAdapter(this, mAdapterList);
        setListAdapter(mAdapter);
    }

    private void searchAbility(String query) {
        HashMap<String, String> abilityDex = AbilityDex.get(getApplicationContext()).getAbilityDexEntries();
        mAdapterList = new ArrayList<>();
        for (String abilityName : abilityDex.keySet()) {
            if (abilityName.contains(query.toLowerCase())) {
                mAdapterList.add(abilityName);
            }
        }
        mAdapter = new AbilityAdapter(this, mAdapterList);
        setListAdapter(mAdapter);
    }

    private void searchItem(String query) {
        HashMap<String, String> itemDex = ItemDex.get(getApplicationContext()).getItemDexEntries();
        mAdapterList = new ArrayList<>();
        for (String itemName : itemDex.keySet()) {
            if (itemName.contains(query.toLowerCase())) {
                mAdapterList.add(itemName);
            }
        }
        mAdapter = new ItemAdapter(this, mAdapterList);
        setListAdapter(mAdapter);
    }

    private void searchMove(String query) {
        HashMap<String, String> moveDex = MoveDex.get(getApplicationContext()).getMoveDexEntries();
        mAdapterList = new ArrayList<>();
        for (String moveName : moveDex.keySet()) {
            if (moveName.contains(query.toLowerCase())) {
                mAdapterList.add(moveName);
            }
        }
        mAdapter = new MovesAdapter(this, mAdapterList);
        setListAdapter(mAdapter);
    }

    private class PokemonAdapter extends ArrayAdapter<String> {
        private Activity mContext;

        public PokemonAdapter(Activity getContext, ArrayList<String> pokemonList) {
            super(getContext, 0, pokemonList);
            mContext = getContext;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mContext.getLayoutInflater().inflate(R.layout.fragment_pokemon_short, null);
            }

            String pokemonName = getItem(position);
            TextView textView = (TextView) convertView.findViewById(R.id.short_pokemon_name);
            textView.setText(Pokemon.getPokemonName(getApplicationContext(), pokemonName, true));
            textView.setCompoundDrawablesWithIntrinsicBounds(Pokemon.getPokemonIcon(getApplicationContext(), pokemonName), 0, 0, 0);
            Integer[] typesIcon = Pokemon.getPokemonTypeIcon(getApplicationContext(), pokemonName, true);
            ImageView type1 = (ImageView) convertView.findViewById(R.id.type_1);
            type1.setImageResource(typesIcon[0]);
            ImageView type2 = (ImageView) convertView.findViewById(R.id.type_2);
            if (typesIcon.length == 2) {
                type2.setImageResource(typesIcon[1]);
            } else {
                type2.setImageResource(0);
            }
            Integer[] baseStats = Pokemon.getPokemonBaseStats(getApplicationContext(), pokemonName, true);
            TextView hp = (TextView) convertView.findViewById(R.id.pokemon_short_hp);
            hp.setText(baseStats[0].toString());
            TextView atk = (TextView) convertView.findViewById(R.id.pokemon_short_Atk);
            atk.setText(baseStats[1].toString());
            TextView def = (TextView) convertView.findViewById(R.id.pokemon_short_Def);
            def.setText(baseStats[2].toString());
            TextView spa = (TextView) convertView.findViewById(R.id.pokemon_short_SpAtk);
            spa.setText(baseStats[3].toString());
            TextView spd = (TextView) convertView.findViewById(R.id.pokemon_short_SpDef);
            spd.setText(baseStats[4].toString());
            TextView spe = (TextView) convertView.findViewById(R.id.pokemon_short_Spd);
            spe.setText(baseStats[5].toString());
            int BST = baseStats[0] + baseStats[1] + baseStats[2] + baseStats[3] + baseStats[4] + baseStats[5];
            TextView bst = (TextView) convertView.findViewById(R.id.pokemon_short_BST);
            bst.setText(Integer.toString(BST));
            return convertView;
        }
    }

    private class AbilityAdapter extends ArrayAdapter<String> {
        private Activity mContext;

        public AbilityAdapter(Activity getContext, ArrayList<String> pokemonList) {
            super(getContext, 0, pokemonList);
            mContext = getContext;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mContext.getLayoutInflater().inflate(R.layout.fragment_ability_short, null);
            }

            try {
                String abilityName = getItem(position);

                JSONObject abilityJson = AbilityDex.get(getApplicationContext()).getAbilityJsonObject(abilityName);
                TextView textView = (TextView) convertView.findViewById(R.id.short_ability_name);
                textView.setText(abilityJson.getString("name"));
                textView.setCompoundDrawablesWithIntrinsicBounds(Pokedex.getUnownIcon(getApplicationContext(), abilityName), 0, 0, 0);
                ((TextView) convertView.findViewById(R.id.short_ability_description)).setText(abilityJson.getString("shortDesc"));
            } catch (JSONException e) {
                Log.d(STAG, e.toString());
            }
            return convertView;
        }
    }

    private class MovesAdapter extends ArrayAdapter<String> {
        private Activity mContext;

        public MovesAdapter(Activity getContext, ArrayList<String> pokemonList) {
            super(getContext, 0, pokemonList);
            mContext = getContext;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mContext.getLayoutInflater().inflate(R.layout.fragment_moves_short, null);
            }

            try {
                String move = getItem(position);

                JSONObject moveJson = MoveDex.get(getApplicationContext()).getMoveJsonObject(move);
                TextView textView = (TextView) convertView.findViewById(R.id.short_move_name);
                textView.setText(moveJson.getString("name"));
                ImageView type = (ImageView) convertView.findViewById(R.id.type);
                type.setImageResource(MoveDex.getTypeIcon(getApplicationContext(), moveJson.getString("type")));
                ImageView category = (ImageView) convertView.findViewById(R.id.category);
                category.setImageResource(MoveDex.getCategoryIcon(getApplicationContext(), moveJson.getString("category")));

                TextView power = (TextView) convertView.findViewById(R.id.move_power);
                String pow = moveJson.getString("basePower");
                if (pow.equals("0")) {
                    power.setText("--");
                } else {
                    power.setText(pow);
                }
                TextView acc = (TextView) convertView.findViewById(R.id.move_acc);
                String accuracy = moveJson.getString("accuracy");
                if (accuracy.equals("true")) {
                    accuracy = "--";
                }
                acc.setText(accuracy);
                TextView pp = (TextView) convertView.findViewById(R.id.move_pp);
                pp.setText(MoveDex.getMaxPP(moveJson.getString("pp")));
            } catch (JSONException e) {
                Log.d(STAG, e.toString());
            }
            return convertView;
        }
    }

    private class ItemAdapter extends ArrayAdapter<String> {
        private Activity mContext;

        public ItemAdapter(Activity getContext, ArrayList<String> pokemonList) {
            super(getContext, 0, pokemonList);
            mContext = getContext;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mContext.getLayoutInflater().inflate(R.layout.fragment_item_short, null);
            }

            try {
                String itemTag = getItem(position);
                JSONObject itemJson = ItemDex.get(getApplicationContext()).getItemJsonObject(itemTag);
                TextView textView = (TextView) convertView.findViewById(R.id.short_item_name);
                textView.setText(itemJson.getString("name"));
                textView.setCompoundDrawablesWithIntrinsicBounds(ItemDex.getItemIcon(getApplicationContext(), itemTag), 0, 0, 0);
                ((TextView) convertView.findViewById(R.id.short_item_description)).setText(itemJson.getString("desc"));
            } catch (JSONException e) {
                Log.d(STAG, e.toString());
            }
            return convertView;
        }

    }
}
