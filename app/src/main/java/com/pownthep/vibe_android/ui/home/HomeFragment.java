package com.pownthep.vibe_android.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.pownthep.vibe_android.R;
import com.pownthep.vibe_android.player.PlayerActivity;
import com.pownthep.vibe_android.utils.GetDataList;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import static com.pownthep.vibe_android.MainActivity.EXTERNAL_DATA;
import static com.pownthep.vibe_android.MainActivity.SHARED_PREFS;

public class HomeFragment extends Fragment {

    private ArrayList<Show> shows;

    private View root;
    private DataAdapter adapter;
    private ProgressBar dataLoader;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        root = inflater.inflate(R.layout.fragment_home, container, false);
        dataLoader = root.findViewById(R.id.data_loading);

        TextInputEditText inputText = root.findViewById(R.id.search_text);
        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                filter(editable.toString());
            }
        });
        try {
            loadData();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return root;
    }

    private void filter(String text) {
        ArrayList<Show> filteredList = new ArrayList<>();
        for (Show item : shows) {
            if (item.getName().toLowerCase().contains(text)) filteredList.add(item);
        }
        adapter.filterList(filteredList);
    }

    private void launchActivity(int position) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("DATA_INDEX", position + "");
        startActivity(intent);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    private void initViews(JSONArray json) {
        dataLoader.setVisibility(View.INVISIBLE);
        RecyclerView recyclerView = root.findViewById(R.id.card_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);
        shows = prepareData(json);
        adapter = new DataAdapter(shows);
        recyclerView.setAdapter(adapter);
        adapter.setOnCardClickListener(this::launchActivity);
    }

    private ArrayList<Show> prepareData(JSONArray json) {
        ArrayList<Show> showsList = new ArrayList<>();
        try {
            for (int i = 0; i < json.length(); i++) {
                Show show = new Show();
                show.setName(json.getJSONObject(i).get("name") + "");
                show.setImg(json.getJSONObject(i).get("poster") + "");
                show.setId(json.getJSONObject(i).get("id") + "");
                showsList.add(show);
            }
        } catch (Exception e) {
            Log.d("VIBE", String.valueOf(e));
        }
        return showsList;
    }

    private void saveData(String jsonText) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SHARED_PREFS, getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(EXTERNAL_DATA, jsonText);
        editor.apply();
    }

    private void loadData() throws JSONException {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SHARED_PREFS, getContext().MODE_PRIVATE);
        String jsonString = sharedPreferences.getString(EXTERNAL_DATA, null);
        if (jsonString == null) {
            GetDataList task = new GetDataList("https://vibe-three.vercel.app/data/trimmed.json", result -> {
                Log.d("VIBE", "Loading data from online.");
                saveData(result);
                initViews(new JSONArray(result));
            });
            task.execute();
        } else {
            Log.d("VIBE", "Loading data from shared preferences.");
            initViews(new JSONArray(jsonString));
        }
    }
}

