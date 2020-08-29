package com.pownthep.vibe_android.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pownthep.vibe_android.R;
import com.squareup.picasso.Picasso;


import org.json.JSONException;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {
    private ArrayList<Episode> episodes;

    private OnCardClickListener mListener;

    public interface OnCardClickListener {
        void onCardClick(int position) throws JSONException;
    }

    public void filterList(ArrayList<Episode> filteredList) {
        episodes = filteredList;
        notifyDataSetChanged();
    }

    public void setOnCardClickListener(OnCardClickListener listener) {
        mListener = listener;
    }

    public EpisodeAdapter(ArrayList<Episode> episodes) {
        this.episodes = episodes;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_row_layout, viewGroup, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        viewHolder.size.setText(episodes.get(i).getSize());
        viewHolder.name.setText(episodes.get(i).getName());
        viewHolder.setIndex(episodes.get(i).getIndex());
        Picasso.get().load("https://lh3.googleusercontent.com/u/0/d/"+episodes.get(i).getId()).into(viewHolder.img);
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView img;
        private final TextView name;
        private final TextView size;

        public void setIndex(int index) {
            this.index = index;
        }

        private int index;

        public ViewHolder(View view, final OnCardClickListener listener) {
            super(view);
            img = view.findViewById(R.id.thumbnail);
            name = view.findViewById(R.id.episode_name);
            size = view.findViewById(R.id.size);

            img.setOnClickListener(view1 -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        try {
                            listener.onCardClick(index);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }
}
