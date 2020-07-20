package com.pownthep.vibe_android.utils;

import android.os.AsyncTask;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GetDataList extends AsyncTask<String, String, String> {
    private final String fileURL = "https://data.pownthep.vercel.app/merged.json";
    private final TaskListener taskListener;

    public interface TaskListener {
        void onFinished(String result) throws JSONException;
    }

    public GetDataList(TaskListener taskListener) {
        this.taskListener = taskListener;
    }

    @Override
    protected String doInBackground(String... strings) {
        return getExternalData();
    }

    private String getExternalData() {
        String json = "[]";
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

    public String readJsonFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return jsonText;
        }
    }

    @Override
    protected void onPostExecute(String jsonString) {
        super.onPostExecute(jsonString);
        // In onPostExecute we check if the listener is valid
        if(this.taskListener != null) {
            // And if it is we call the callback function on it.
            try {
                this.taskListener.onFinished(jsonString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
