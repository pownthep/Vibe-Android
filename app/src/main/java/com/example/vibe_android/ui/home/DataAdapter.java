package com.example.vibe_android.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.vibe_android.AndroidVersion;
import com.example.vibe_android.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    private ArrayList<AndroidVersion> android;
    private Context context;
    private OnCardClickListener mListener;

    public interface OnCardClickListener {
        void onCardClick(int position);
    }

    public void setOnCardClickListener(OnCardClickListener listener) {
        mListener = listener;
    }

    public DataAdapter(Context context, ArrayList<AndroidVersion> android) {
        this.android = android;
        this.context = context;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_layout, viewGroup, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(DataAdapter.ViewHolder viewHolder, int i) {
        Picasso.get().load(android.get(i).getAndroid_image_url()).into(viewHolder.img_android);
    }

    @Override
    public int getItemCount() {
        return android.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView img_android;

        public ViewHolder(View view, final OnCardClickListener listener) {
            super(view);
            img_android = (ImageView) view.findViewById(R.id.img_android);
            img_android.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onCardClick(position);
                        }
                    }
                }
            });
        }
    }
}
