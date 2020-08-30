package com.pownthep.vibe_android.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;


import java.io.IOException;
import java.net.URL;

public class GetImageBitmap extends AsyncTask<String, String, Bitmap> {
    private final TaskListener taskListener;
    private String url;

    public interface TaskListener {
        void onFinished(Bitmap result);
    }

    public GetImageBitmap(String url, TaskListener taskListener) {
        this.taskListener = taskListener;
        this.url = url;
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
        Bitmap image = null;
        try {
            URL url = new URL(this.url);
            image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch(IOException e) {
            System.out.println(e);
        }
        return image;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if(this.taskListener != null) {
            this.taskListener.onFinished(bitmap);
        }
    }
}
