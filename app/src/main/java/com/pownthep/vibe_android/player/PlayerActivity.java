package com.pownthep.vibe_android.player;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pownthep.vibe_android.R;
import com.pownthep.vibe_android.http.HttpServer;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.pownthep.vibe_android.MainActivity.EXTERNAL_DATA;
import static com.pownthep.vibe_android.MainActivity.LIBRARY_ARRAY;
import static com.pownthep.vibe_android.MainActivity.SHARED_PREFS;

public class PlayerActivity extends AppCompatActivity implements IVLCVout.Callback {
    private JSONObject data;
    private JSONArray episodes;
    public final static String TAG = "PlayerActivity";
    private SurfaceView mSurface;
    private SurfaceView mSurfaceSubtitles;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private Slider slider;
    private RecyclerView recyclerView;
    private FloatingActionButton togglePlay;
    private ImageView mEpisodeThumbnail;
    private int mHeight;
    private int mWidth;
    private IVLCVout vout;
    private boolean seeking;
    private TextView title;
    private FrameLayout overlay;
    private View contextView;
    private FrameLayout darkOverlay;
    private ArrayList<Episode> episodeArrayList;
    private EpisodeAdapter episodeAdapter;
    private ProgressBar loadingIndicator;
    private ProgressBar videoProgress;
    private FloatingActionButton lockRotate;
    private boolean isRotateLock = false;
    private FloatingActionButton addToLibBtn;
    private HttpServer server;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mSurface = findViewById(R.id.surface);
        mSurfaceSubtitles = findViewById(R.id.sub_surface);
        mSurfaceSubtitles.setZOrderOnTop(true);
        SurfaceHolder subHolder = mSurfaceSubtitles.getHolder();
        subHolder.setFormat(PixelFormat.TRANSPARENT);
        togglePlay = findViewById(R.id.play);
        holder = mSurface.getHolder();
        FloatingActionButton audio = findViewById(R.id.audio_cycle);
        FloatingActionButton sub = findViewById(R.id.sub_cycle);
        slider = findViewById(R.id.slider);
        mEpisodeThumbnail = findViewById(R.id.episode_thumbnail);
        title = findViewById(R.id.title);
        overlay = findViewById(R.id.player_overlay);
        contextView = findViewById(R.id.player_view);
        darkOverlay = findViewById(R.id.overlay);
        TextInputEditText episodeInput = findViewById(R.id.search_text_episode);
        loadingIndicator = findViewById(R.id.progressBar);
        videoProgress = findViewById(R.id.progressBar2);
        lockRotate = findViewById(R.id.lock_rotate);
        addToLibBtn = findViewById(R.id.add_lib_btn);
        FloatingActionButton backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(view -> {
            onBackPressed();
        });

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String libString = sharedPreferences.getString(LIBRARY_ARRAY, "");
        String index = getIntent().getStringExtra("DATA_INDEX");
        server = new HttpServer();
        server.start();

        if (libString.contains(index)) {
            addToLibBtn.setImageResource(R.drawable.ic_baseline_library_add_check_24);
            addToLibBtn.setEnabled(false);
        }

        addToLibBtn.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(LIBRARY_ARRAY, libString + index + ",");
            editor.apply();
            addToLibBtn.setImageResource(R.drawable.ic_baseline_library_add_check_24);
            addToLibBtn.setEnabled(false);
            Snackbar.make(contextView, "Added to library", Snackbar.LENGTH_SHORT)
                    .show();
        });

        lockRotate.setOnClickListener(view -> {
            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            if (isRotateLock) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                lockRotate.setImageResource(R.drawable.ic_baseline_screen_lock_rotation_24);
                isRotateLock = false;
                return;
            }
            if (!isPortrait && !isRotateLock) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
            isRotateLock = true;
            lockRotate.setImageResource(R.drawable.ic_baseline_lock_open_24);
        });

        getData();
        try {
            initViews();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        episodeInput.addTextChangedListener(new TextWatcher() {
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

        togglePlay.setOnClickListener(view -> {
            if (mMediaPlayer == null) {
                playMedia();
                return;
            }
            int state = mMediaPlayer.getPlayerState();
            Log.d("VIBE", String.valueOf(state));
            if (state == 3) {
                pauseMedia();
            } else {
                playMedia();
            }
        });
        audio.setOnClickListener(view -> {
            if (mMediaPlayer != null) {
                int count = mMediaPlayer.getAudioTracksCount();
                int currentId = mMediaPlayer.getAudioTrack();
                MediaPlayer.TrackDescription[] tracks = mMediaPlayer.getAudioTracks();
                int currentIndex = 0;
                for (int i = 0; i < count; i++) {
                    if (currentId == tracks[i].id) currentIndex = i;
                    Log.d("VIBE", "audio track id:" + tracks[i].id + "/" + tracks[i].name);
                }
                int nextIndex = (currentIndex + 1) >= count ? 0 : (currentIndex + 1);
                Log.d("VIBE", "Next audio track id:" + tracks[nextIndex].id + "/" + count);
                mMediaPlayer.setAudioTrack(tracks[nextIndex].id);
                Snackbar.make(contextView, tracks[nextIndex].name, Snackbar.LENGTH_SHORT)
                        .show();
            }
        });
        sub.setOnClickListener(view -> {
            if (mMediaPlayer != null) {
                int count = mMediaPlayer.getSpuTracksCount();
                int currentId = mMediaPlayer.getSpuTrack();
                MediaPlayer.TrackDescription[] tracks = mMediaPlayer.getSpuTracks();
                int currentIndex = 0;
                for (int i = 0; i < count; i++) {
                    if (currentId == tracks[i].id) currentIndex = i;
                    Log.d("VIBE", "sub track id:" + tracks[i].id + "/" + tracks[i].name);
                }
                int nextIndex = (currentIndex + 1) >= count ? 0 : (currentIndex + 1);
                Log.d("VIBE", "Next sub track id:" + tracks[nextIndex].id + "/" + count);
                mMediaPlayer.setSpuTrack(tracks[nextIndex].id);
                Snackbar.make(contextView, tracks[nextIndex].name, Snackbar.LENGTH_SHORT)
                        .show();
            }
        });
        mSurface.setOnClickListener(view -> {
            boolean visible = overlay.getVisibility() == View.VISIBLE;
            if (visible) {
                overlay.setVisibility(View.INVISIBLE);
                darkOverlay.setVisibility(View.INVISIBLE);
            } else {
                overlay.setVisibility(View.VISIBLE);
                darkOverlay.setVisibility(View.VISIBLE);
            }
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                seeking = true;
                pauseMedia();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (mMediaPlayer == null) return;
                float value = slider.getValue();
                mMediaPlayer.setTime((long) value);
                loadingIndicator.setVisibility(View.VISIBLE);
                seeking = false;
                playMedia();
            }
        });
        slider.setLabelFormatter((value) -> {
            int minutes = (int) (value / 1000) / 60;
            int seconds = (int) ((value / 1000) % 60);
            return minutes + ":" + seconds;
        });
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mHeight = displayMetrics.heightPixels;
        mWidth = displayMetrics.widthPixels;
        setSize();

    }

    private void filter(String text) {
        ArrayList<Episode> filteredList = new ArrayList<>();

        for (Episode item : episodeArrayList) {
            if (item.getName().toLowerCase().contains(text)) filteredList.add(item);
        }

        episodeAdapter.filterList(filteredList);

    }

    private void playMedia() {
        if (mMediaPlayer != null) {
            mMediaPlayer.play();
            togglePlay.setImageResource(R.drawable.ic_round_pause_24);
        } else {
            try {
                if (episodes != null) {
                    Log.d("VIBE", episodes.getJSONObject(0).get("name") + "");
                    title.setText(String.format("%s", episodes.getJSONObject(0).get("name")));
                    download(episodes.getJSONObject(0).get("id") + "", episodes.getJSONObject(0).get("size") + "");
                    togglePlay.setImageResource(R.drawable.ic_round_pause_24);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                        videoProgress.setVisibility(View.VISIBLE);
                }
            } catch (JSONException e) {
                Snackbar.make(contextView, "Unable to play", Snackbar.LENGTH_SHORT);
            }
        }
    }

    private void pauseMedia() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            togglePlay.setImageResource(R.drawable.ic_round_play_arrow_24);
        }
    }

    private void getData() {
        try {
            String index = getIntent().getStringExtra("DATA_INDEX");
            assert index != null;
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
            String jsonString = sharedPreferences.getString(EXTERNAL_DATA, "[]");
            JSONArray jsonArray = new JSONArray(jsonString);
            data = jsonArray.getJSONObject(Integer.parseInt(index));
            ((TextInputLayout) findViewById(R.id.search_container)).setHint((CharSequence) data.get("name"));
            episodes = (JSONArray) data.get("episodes");
            title.setText(data.get("name").toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initViews() throws JSONException {
        recyclerView = findViewById(R.id.episode_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        episodeArrayList = prepareData();
        Picasso.get().load(data.get("banner") + "").into(mEpisodeThumbnail);
        episodeAdapter = new EpisodeAdapter(episodeArrayList);
        recyclerView.setAdapter(episodeAdapter);

        episodeAdapter.setOnCardClickListener(position -> {
            Log.d("VIBE", episodes.getJSONObject(position).get("name") + "");
            title.setText(String.format("%s", episodes.getJSONObject(position).get("name")));
            download(episodes.getJSONObject(position).get("id") + "", episodes.getJSONObject(position).get("size") + "");
        });
    }

    private ArrayList<Episode> prepareData() {
        ArrayList<Episode> episodeArrayList = new ArrayList<>();
        try {
            for (int i = 0; i < episodes.length(); i++) {
                try {
                    episodes.getJSONObject(i).get("size");
                    Episode episode = new Episode();
                    episode.setName(episodes.getJSONObject(i).get("name") + "");
                    episode.setId(episodes.getJSONObject(i).get("id") + "");
                    episode.setIndex(i);
                    episodeArrayList.add(episode);
                } catch (JSONException e) {}
            }
            return episodeArrayList;

        } catch (Exception e) {
            Log.d("VIBE", String.valueOf(e));
        }
        return episodeArrayList;

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize();
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
        try {
            server.stopServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to set size for SurfaceView
     */
    private void setSize() {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        FrameLayout.LayoutParams overlayParams = (FrameLayout.LayoutParams) overlay.getLayoutParams();
        FrameLayout.LayoutParams videoParams = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
        FrameLayout.LayoutParams subParams = (FrameLayout.LayoutParams) mSurfaceSubtitles.getLayoutParams();
        FrameLayout.LayoutParams thumbnailParams = (FrameLayout.LayoutParams) mEpisodeThumbnail.getLayoutParams();
        FrameLayout.LayoutParams darkOverlayParams = (FrameLayout.LayoutParams) darkOverlay.getLayoutParams();
        FrameLayout.LayoutParams videoProgressParams = (FrameLayout.LayoutParams) videoProgress.getLayoutParams();
        int statusBarHeight = statusBarHeight(getResources());

        if (vout != null)
            vout.setWindowSize(isPortrait ? mWidth : mHeight, isPortrait ? 610 : mWidth);
        if (!isPortrait) {
            hideSystemUI();
            videoProgress.setVisibility(View.INVISIBLE);
            addToLibBtn.setVisibility(View.INVISIBLE);

            overlayParams.height = mWidth;
            videoParams.height = mWidth;
            subParams.height = mWidth;
            thumbnailParams.height = mWidth;
            darkOverlayParams.height = mWidth;

            videoParams.width = mHeight;
            subParams.width = mHeight;
            darkOverlayParams.width = mHeight;

            videoParams.topMargin = 0;
            overlayParams.topMargin = 0;
            subParams.topMargin = 0;
            recyclerView.setVisibility(View.INVISIBLE);

        } else {
            showSystemUI();
            videoProgress.setVisibility(mMediaPlayer != null ? View.VISIBLE : View.INVISIBLE);
            addToLibBtn.setVisibility(View.VISIBLE);

            overlayParams.height = 610;
            videoParams.height = 610;
            subParams.height = mWidth;
            thumbnailParams.height = 610 + statusBarHeight;
            darkOverlayParams.height = 610 + statusBarHeight;

            videoParams.width = mWidth;
            subParams.width = mWidth;
            darkOverlayParams.width = mWidth;

            videoParams.topMargin = statusBarHeight;
            overlayParams.topMargin = statusBarHeight;
            subParams.topMargin = statusBarHeight;
            videoProgressParams.topMargin = 610 + statusBarHeight;
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void createPlayer(Uri media) {
        releasePlayer();
        try {
            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            ArrayList<String> options = new ArrayList<>();
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
            vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            vout.setWindowSize(isPortrait ? mWidth : mHeight, isPortrait ? 610 : mWidth);
            vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, media);
            mMediaPlayer.setMedia(m);
            playMedia();
        } catch (Exception e) {
            Log.d("VIBE", e.toString());
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
    }

    /**
     * Registering callbacks
     */
    private final MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    @Override
    public void onSurfacesCreated(IVLCVout vout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private final WeakReference<PlayerActivity> mOwner;

        public MyPlayerListener(PlayerActivity owner) {
            mOwner = new WeakReference<>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            PlayerActivity player = mOwner.get();
            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing: {
                    Log.d("VIBE", "Playing");
                    Log.d("VIBE", "Setting loading off");
                    player.loadingIndicator.setVisibility(View.INVISIBLE);
                    player.mEpisodeThumbnail.setVisibility(View.INVISIBLE);
                    if (player.mMediaPlayer.getLength() > 0) {
                        player.slider.setValueTo(player.mMediaPlayer.getLength());
                    }
                }
                case MediaPlayer.Event.Paused: {
                    Log.d("VIBE", "Paused Event");
                }
                case MediaPlayer.Event.Stopped:
                case MediaPlayer.Event.MediaChanged:
                case MediaPlayer.Event.Buffering: {
                }
                case MediaPlayer.Event.TimeChanged:
                    if (player.mMediaPlayer.getTime() > 0 && !player.seeking) {
                        float currentTime = player.mMediaPlayer.getTime();
                        float length = player.mMediaPlayer.getLength();
                        float progress = currentTime / length * 100;
                        player.slider.setValue(currentTime);
                        player.videoProgress.setProgress((int) progress);
                    }
                default:
                    break;
            }
        }
    }

    private void download(String fileId, String fileSize) {
        Picasso.get().load("https://lh3.googleusercontent.com/u/0/d/" + fileId).into(mEpisodeThumbnail);
        loadingIndicator.setVisibility(View.VISIBLE);
        changeMediaOrCreatePlayer(Uri.parse("http://localhost:8080?id=" + fileId + "&size=" + fileSize));
    }

    private void changeMediaOrCreatePlayer(Uri uri) {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            Media m = new Media(libvlc, uri);
            mMediaPlayer.setMedia(m);
            playMedia();
        } else createPlayer(uri);
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private static int statusBarHeight(android.content.res.Resources res) {
        return (int) (28 * res.getDisplayMetrics().density);
    }

}