package com.pownthep.vibe_android.ui.library;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pownthep.vibe_android.R;
import com.pownthep.vibe_android.player.PlayerActivity;
import com.pownthep.vibe_android.ui.home.Show;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import static com.pownthep.vibe_android.MainActivity.LIBRARY_ARRAY;
import static com.pownthep.vibe_android.MainActivity.SHARED_PREFS;

public class LibraryFragment extends Fragment {
    private View root;
    private ArrayList<Show> shows;
    private DataAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_library, container, false);
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SHARED_PREFS, getContext().MODE_PRIVATE);
        String libArrayString = sharedPreferences.getString(LIBRARY_ARRAY, null);
        if (libArrayString != null) {
            root.findViewById(R.id.lib_placeholder).setVisibility(View.GONE);
            try {
                JSONArray libArray = new JSONArray(libArrayString);
                initViews(libArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        root.findViewById(R.id.explore_btn).setOnClickListener(view -> {
            Navigation.findNavController(view).navigate(R.id.navigation_home);
        });
        return root;
    }

    private void initViews(JSONArray json) {
        RecyclerView recyclerView = root.findViewById(R.id.shows_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
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
                show.setName(json.getJSONObject(i).get("name") + " - " + ((JSONArray) json.getJSONObject(i).get("episodes")).length() + " Episodes");
                show.setImg(json.getJSONObject(i).get("banner") + "");
                show.setId(json.getJSONObject(i).get("id") + "");
                showsList.add(show);
            }
        } catch (Exception e) {
            Log.d("VIBE", String.valueOf(e));
        }
        return showsList;
    }

    private void launchActivity(int position) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("DATA_INDEX", position + "");
        startActivity(intent);
    }

}