package com.example.vibe_android.Player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.vibe_android.R;
import com.squareup.picasso.Picasso;


import org.json.JSONException;

import java.util.ArrayList;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {
    private ArrayList<Episode> episodes;
    private Context context;

    private OnCardClickListener mListener;

    public interface OnCardClickListener {
        void onCardClick(int position) throws JSONException;
    }

    public void setOnCardClickListener(OnCardClickListener listener) {
        mListener = listener;
    }

    public EpisodeAdapter(Context context, ArrayList<Episode> episodes) {
        this.episodes = episodes;
        this.context = context;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.episode_row_layout, viewGroup, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        viewHolder.name.setText(episodes.get(i).getName());
        Picasso.get().load("https://lh3.googleusercontent.com/u/0/d/"+episodes.get(i).getId()).into(viewHolder.img);
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView img;
        private TextView name;

        public ViewHolder(View view, final OnCardClickListener listener) {
            super(view);
            img = (ImageView) view.findViewById(R.id.thumbnail);
            name = (TextView) view.findViewById(R.id.episode_name);

            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            try {
                                listener.onCardClick(position);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }
}
