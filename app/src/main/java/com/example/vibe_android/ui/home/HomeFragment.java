package com.example.vibe_android.ui.home;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibe_android.AndroidVersion;
import com.example.vibe_android.MainActivity;
import com.example.vibe_android.R;
import com.example.vibe_android.player.PlayerActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    private View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        root = inflater.inflate(R.layout.fragment_home, container, false);

        if (MainActivity.externalData == null) new GetDataList().execute();
        else {
            initViews(MainActivity.externalData);
        }
        return root;
    }

    private void launchActivity(int position) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("DATA_INDEX", position + "");
        startActivity(intent);
    }

    private void initViews(JSONArray json) {
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.card_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        ArrayList<AndroidVersion> androidVersions = prepareData(json);
        DataAdapter adapter = new DataAdapter(getContext(), androidVersions);
        recyclerView.setAdapter(adapter);

        adapter.setOnCardClickListener(new DataAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(int position) {
                launchActivity(position);
            }
        });
    }

    private ArrayList<AndroidVersion> prepareData(JSONArray json) {

        ArrayList<AndroidVersion> android_version = new ArrayList<>();
        try {
            for (int i = 0; i < json.length(); i++) {
                AndroidVersion androidVersion = new AndroidVersion();
                androidVersion.setAndroid_version_name(json.getJSONObject(i).get("name") + "");
                androidVersion.setAndroid_image_url(json.getJSONObject(i).get("poster") + "");
                android_version.add(androidVersion);
            }
            return android_version;

        } catch (Exception e) {
            Log.d("VIBE", String.valueOf(e));
        } finally {
            return android_version;
        }

    }

    class GetDataList extends AsyncTask<String, String, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... strings) {
            getExternalData();
            return getExternalData();
        }

        private JSONArray getExternalData() {
            //String fileURL = "https://boring-northcutt-5fd361.netlify.app/full.json";
            String fileURL = "https://data.pownthep.vercel.app/merged.json";

            JSONArray json = new JSONArray();
            try {
                json = readJsonFromUrl(fileURL);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                return json;
            }

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
            InputStream is = new URL(url).openStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String jsonText = readAll(rd);
                JSONArray json = new JSONArray(jsonText);
                MainActivity.externalData = json;
                return json;
            } finally {
                is.close();
            }
        }

        @Override
        protected void onPostExecute(JSONArray data) {
            super.onPostExecute(data);
            initViews(data);
        }

    }
}

