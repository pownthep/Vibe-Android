package com.example.vibe_android.Player;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.vibe_android.MainActivity.externalData;
import static com.example.vibe_android.MainActivity.APP_DATA;

import com.example.vibe_android.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

import static com.example.vibe_android.MainActivity.accessToken;

public class PlayerActivity extends AppCompatActivity implements IVLCVout.Callback {
    private JSONObject data;
    private JSONArray episodes;
    private ImageView imageView;
    public final static String TAG = "PlayerActivity";
    private String mFilePath;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private Slider slider;
    private FloatingActionButton audio;
    private FloatingActionButton sub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
//        imageView = findViewById(R.id.imageView);
        getData();
//        try {
//            Picasso.get().load(data.get("banner")+"").into(imageView);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        audio = (FloatingActionButton) findViewById(R.id.audio_cycle);

        audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                    int count = mMediaPlayer.getAudioTracksCount();
                    int current = mMediaPlayer.getAudioTrack();
                    System.out.println("Current audio track index:" + current + "/" + count);
                    int next = (current + 1) >= count ? 1 : (current + 1);
                    System.out.println("Next audio track index:" + next + "/" + count);
                    mMediaPlayer.setAudioTrack(next);
                    mMediaPlayer.play();
                }
            }
        });

        sub = (FloatingActionButton) findViewById(R.id.sub_cycle);

        sub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                    int count = mMediaPlayer.getSpuTracksCount();
                    int current = mMediaPlayer.getSpuTrack();
                    System.out.println("Current sub track index:" + current + "/" + count + ":" + mMediaPlayer.getSpuTracks()[1].name);
                    int next = (current + 1) >= count ? 1 : (current + 1);
                    System.out.println("Next sub track index:" + next + "/" + count + ":" + mMediaPlayer.getSpuTracks()[1].name);
                    mMediaPlayer.setSpuTrack(1);
                    mMediaPlayer.play();
                }
            }
        });

        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();
        initViews();


    }

    private void getData() {
        try {
            String index = getIntent().getStringExtra("DATA_INDEX");
            data = externalData.getJSONObject(Integer.parseInt(index));
            episodes = (JSONArray) data.get("episodes");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.episode_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        ArrayList<Episode> episodeArrayList = prepareData();
        EpisodeAdapter episodeAdapter = new EpisodeAdapter(this, episodeArrayList);
        recyclerView.setAdapter(episodeAdapter);

        episodeAdapter.setOnCardClickListener(new EpisodeAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(int position) throws JSONException {
                System.out.println(episodes.getJSONObject(position).get("name") + "");
                download(episodes.getJSONObject(position).get("id") + "", episodes.getJSONObject(position).get("size") + "");
            }
        });
    }

    private ArrayList<Episode> prepareData() {

        ArrayList<Episode> episodeArrayList = new ArrayList<>();
        try {
            for (int i = 0; i < episodes.length(); i++) {
                Episode episode = new Episode();
                episode.setName(episodes.getJSONObject(i).get("name") + "");
                episode.setId(episodes.getJSONObject(i).get("id") + "");
                episodeArrayList.add(episode);
            }
            return episodeArrayList;

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            return episodeArrayList;
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        slider = (Slider) findViewById(R.id.slider);

        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                mMediaPlayer.pause();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                float value = slider.getValue();
                mMediaPlayer.setTime((long) value);
                mMediaPlayer.play();
            }
        });

        slider.setLabelFormatter((value) -> {
            int minutes = (int) (value / 1000) / 60;
            int seconds = (int) ((value / 1000) % 60);
            return minutes + ":" + seconds;
        });
    }

    /**
     * Used to set size for SurfaceView
     *
     * @param width
     * @param height
     */
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;
        if (holder == null || mSurface == null)
            return;
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }
        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;
        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /**
     * Creates MediaPlayer and plays video
     *
     * @param media
     */
    private void createPlayer(File media) {
        releasePlayer();
        try {
            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            //options.add("-vvv"); // verbosity
            libvlc = new LibVLC(this, options);
            holder.setKeepScreenOn(true);
            // Creating media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);
            // Seting up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();
            Media m = new Media(libvlc, Uri.fromFile(media));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
            mMediaPlayer.setAudioTrack(2);
            mMediaPlayer.setSpuTrack(2);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    /**
     * Registering callbacks
     */
    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;
        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<PlayerActivity> mOwner;

        public MyPlayerListener(PlayerActivity owner) {
            mOwner = new WeakReference<PlayerActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            PlayerActivity player = mOwner.get();
            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                    if (player.mMediaPlayer.getLength() > 0)
                        player.slider.setValueTo(player.mMediaPlayer.getLength());
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                case MediaPlayer.Event.TimeChanged:
                    if (player.mMediaPlayer.getTime() > 0)
                        player.slider.setValue(player.mMediaPlayer.getTime());
                default:
                    break;
            }
        }
    }

    private void download(String fileId, String fileSize) {
        String filePath = APP_DATA + File.separator + fileId + ".mkv";
        File tempFile = new File(filePath);
        System.out.println(tempFile.length() + ":" + fileSize);
        System.out.println((tempFile.length() + "").equals(fileSize));

        if ((!tempFile.exists() || !(tempFile.length() + "").equals(fileSize)) && accessToken.length() > 0) {
            System.out.println("Downloading the file");
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    String fileURL = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";
                    URL url = null;
                    HttpURLConnection urlConnection = null;
                    try {
                        url = new URL(fileURL);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
                        //urlConnection.setRequestProperty("Range", "bytes=0-10000000");
                        Log.d("token", accessToken);
                        int responseCode = urlConnection.getResponseCode();

                        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                            String fileName = "";
                            String disposition = urlConnection.getHeaderField("Content-Disposition");
                            String contentType = urlConnection.getContentType();
                            int contentLength = urlConnection.getContentLength();

                            if (disposition != null) {
                                // extracts file name from header field
                                int index = disposition.indexOf("filename=");
                                if (index > 0) {
                                    fileName = disposition.substring(index + 10,
                                            disposition.length() - 1);
                                }
                            } else {
                                // extracts file name from URL
                                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                                        fileURL.length());
                            }

                            System.out.println("Content-Type = " + contentType);
                            System.out.println("Content-Disposition = " + disposition);
                            System.out.println("Content-Length = " + contentLength);
                            System.out.println("fileName = " + fileName);

                            // opens input stream from the HTTP connection
                            InputStream inputStream = urlConnection.getInputStream();

                            // opens an output stream to save into file
                            FileOutputStream outputStream = new FileOutputStream(filePath);

                            int bytesRead = -1;
                            byte[] buffer = new byte[4096];
                            int count = 0;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                                count++;
                            }

                            outputStream.close();
                            inputStream.close();
                            changeMediaOrCreatePlayer(tempFile);
                            System.out.println("File downloaded");
                        } else {
                            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        urlConnection.disconnect();
                    }
                }
            });
        } else {
            System.out.println("File exist and playing the file");
            changeMediaOrCreatePlayer(tempFile);
        }

    }

    private void changeMediaOrCreatePlayer(File file) {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            Media m = new Media(libvlc, Uri.fromFile(file));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
        } else createPlayer(file);
    }
}