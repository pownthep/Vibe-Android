package com.pownthep.vibe_android.ui.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.pownthep.vibe_android.MainActivity;
import com.pownthep.vibe_android.R;
import com.pownthep.vibe_android.player.PlayerActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private ArrayList<Show> shows;

    private View root;
    private DataAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        root = inflater.inflate(R.layout.fragment_home, container, false);

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

        if (MainActivity.externalData == null) new GetDataList().execute();
        else {
            initViews(MainActivity.externalData);
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

    private void initViews(JSONArray json) {
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

    @SuppressLint("StaticFieldLeak")
    class GetDataList extends AsyncTask<String, String, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... strings) {
            getExternalData();
            return getExternalData();
        }

        private JSONArray getExternalData() {
            String fileURL = "https://data.pownthep.vercel.app/merged.json";

            JSONArray json = new JSONArray();
            try {
                json = readJsonFromUrl(fileURL);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return json;
        }

        private String readAll(Reader rd) throws IOException {
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }

        public JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
            try (InputStream is = new URL(url).openStream()) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String jsonText = readAll(rd);
                JSONArray json = new JSONArray(jsonText);
                MainActivity.externalData = json;
                return json;
            }
        }

        @Override
        protected void onPostExecute(JSONArray data) {
            super.onPostExecute(data);
            initViews(data);
        }

    }
}

