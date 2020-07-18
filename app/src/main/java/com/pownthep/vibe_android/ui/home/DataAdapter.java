package com.pownthep.vibe_android.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pownthep.vibe_android.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    private ArrayList<Show> shows;
    private OnCardClickListener mListener;

    public void filterList(ArrayList<Show> filteredList) {
        shows = filteredList;
        notifyDataSetChanged();
    }

    public interface OnCardClickListener {
        void onCardClick(int position);
    }

    public void setOnCardClickListener(OnCardClickListener listener) {
        mListener = listener;
    }

    public DataAdapter(ArrayList<Show> shows) {
        this.shows = shows;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @NonNull
    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_layout, viewGroup, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(DataAdapter.ViewHolder viewHolder, int i) {
        viewHolder.setId(shows.get(i).getId());
        Picasso.get().load(shows.get(i).getImg()).into(viewHolder.thumbnail);
    }

    @Override
    public int getItemCount() {
        return shows.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;

        public void setId(String id) {
            this.id = id;
        }

        private String id;

        public ViewHolder(View view, final OnCardClickListener listener) {
            super(view);
            thumbnail = view.findViewById(R.id.show_thumbnail);
            thumbnail.setOnClickListener(view1 -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onCardClick(Integer.parseInt(id));
                    }
                }
            });
        }
    }
}
