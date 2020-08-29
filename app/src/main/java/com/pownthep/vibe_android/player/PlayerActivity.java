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
import android.widget.LinearLayout;
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
import com.pownthep.vibe_android.utils.GetDataList;
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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;

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
    private FloatingActionButton addToLibBtn;
    private TextView timeProgress;
    private HttpServer server;
    private int currentMediaId;
    private long currentMediaTime;
    private long restartMediaTime = 0;
    private LinearLayout infoBar;

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
        timeProgress = findViewById(R.id.time_progress);
        infoBar = findViewById(R.id.info);
        FloatingActionButton backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(view -> {
            onBackPressed();
        });

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String libString = sharedPreferences.getString(LIBRARY_ARRAY, null);
        String index = getIntent().getStringExtra("DATA_INDEX");
        getData();
        if (libString != null && index != null && libString.contains("\"id\":" + index + ",")) {
            addToLibBtn.setImageResource(R.drawable.ic_baseline_library_add_check_24);
            addToLibBtn.setEnabled(false);
        }

        addToLibBtn.setOnClickListener(view -> {
            if (data != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                try {
                    JSONArray tmp = sharedPreferences.getString(LIBRARY_ARRAY, null) == null ? new JSONArray() : new JSONArray(libString);
                    tmp.put(data);
                    editor.putString(LIBRARY_ARRAY, tmp.toString());
                    editor.apply();
                    Log.d(TAG, "onCreate: " + tmp.toString());
                    addToLibBtn.setImageResource(R.drawable.ic_baseline_library_add_check_24);
                    addToLibBtn.setEnabled(false);
                    Snackbar.make(contextView, "Added to library", Snackbar.LENGTH_SHORT)
                            .show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        lockRotate.setOnClickListener(view -> {
            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            if (isPortrait) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        });

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
            boolean visible = overlay.getAlpha() == 1.0f;
            if (visible) {
                overlay.animate().alpha(0.0f);
                darkOverlay.animate().alpha(0.0f);
            } else {
                overlay.animate().alpha(1.0f);
                darkOverlay.animate().alpha(0.5f);
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
                if(!server.isAlive()) {
                    server = new HttpServer();
                    server.start();
                }
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
                if (episodeArrayList != null) {
                    title.setText(String.format("%s", episodeArrayList.get(0).getName()));
                    download(episodeArrayList.get(0).getId(), episodeArrayList.get(0).getBytes());
                    togglePlay.setImageResource(R.drawable.ic_round_pause_24);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                        videoProgress.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        String index = getIntent().getStringExtra("DATA_INDEX");
        assert index != null;
        String url = "https://vibe-three.vercel.app/data/shows/" + index + ".json";
        loadData(url);
    }

    private void initViews() throws JSONException {
        recyclerView = findViewById(R.id.episode_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Picasso.get().load(data.get("banner") + "").into(mEpisodeThumbnail);
        Picasso.get().load(data.get("poster") + "").into((ImageView) findViewById(R.id.image_avatar));
        episodeAdapter = new EpisodeAdapter(episodeArrayList);
        recyclerView.setAdapter(episodeAdapter);

        episodeAdapter.setOnCardClickListener(position -> {
            title.setText(String.format("%s", episodeArrayList.get(position).getName()));
            currentMediaId = position;
            download(episodeArrayList.get(position).getId(), episodeArrayList.get(position).getBytes());
        });
        setSize();
    }

    private ArrayList<Episode> prepareData() {
        ArrayList<Episode> tmp = new ArrayList<>();
        try {
            for (int i = 0; i < episodes.length(); i++) {
                try {
                    episodes.getJSONObject(i).get("size");
                    Episode episode = new Episode();
                    long sizeBytes = Long.parseLong(episodes.getJSONObject(i).get("size").toString());
                    episode.setSize(humanReadableByteCountBin(sizeBytes));
                    episode.setName(episodes.getJSONObject(i).get("name") + "");
                    episode.setId(episodes.getJSONObject(i).get("id") + "");
                    episode.setBytes(episodes.getJSONObject(i).get("size").toString());
                    episode.setIndex(i);
                    tmp.add(episode);
                } catch (JSONException e) {
                }
            }
            return tmp;

        } catch (Exception e) {
            Log.d("VIBE", String.valueOf(e));
        }
        return tmp;

    }

    private String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseMedia();
        releasePlayer();
        try {
            if (server != null) server.stopServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseMedia();
        releasePlayer();
        try {
            if (server != null) server.stopServer();
            server = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        try {
            if (server != null) server.stopServer();
            server = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        try {
            Episode currentEp = episodeArrayList.get(currentMediaId);
            if (server == null) {
                server = new HttpServer();
                server.start();
            }
            createPlayer(Uri.parse("http://localhost:8080?id=" + currentEp.getId() + "&size=" + currentEp.getBytes()));
            restartMediaTime = currentMediaTime;
            playMedia();
        } catch (Exception e) {
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
            infoBar.setVisibility(View.INVISIBLE);
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
            infoBar.setVisibility(View.VISIBLE);
            showSystemUI();
            videoProgress.setVisibility(mMediaPlayer != null ? View.VISIBLE:View.INVISIBLE);
            addToLibBtn.setVisibility(View.VISIBLE);

            overlayParams.height = 610;
            videoParams.height = 610;
            subParams.height = mWidth;
            thumbnailParams.height = 780 + statusBarHeight;
            darkOverlayParams.height = mHeight+statusBarHeight;

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
            videoProgress.setVisibility(View.VISIBLE);
            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            ArrayList<String> options = new ArrayList<>();
            //options.add("--subsdec-encoding <encoding>");
//            options.add("--aout=opensles");
//            options.add("--audio-time-stretch"); // time stretching
            //options.add("-vvv"); // verbosity
            libvlc = new LibVLC(this, options);
            mSurface.getHolder().setKeepScreenOn(true);

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
                    break;
                case MediaPlayer.Event.Playing: {
                    Log.d("VIBE", "Playing");
                    Log.d("VIBE", "Setting loading off");
                    player.loadingIndicator.setVisibility(View.INVISIBLE);
                    player.mEpisodeThumbnail.setVisibility(View.INVISIBLE);
                    if (player.mMediaPlayer.getLength() > 0) {
                        player.slider.setValueTo(player.mMediaPlayer.getLength());
                    }
                    if (player.restartMediaTime > 0) {
                        player.mMediaPlayer.setTime(player.restartMediaTime);
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
                        long currentTime = player.mMediaPlayer.getTime();
                        long length = player.mMediaPlayer.getLength();
                        int progress = (int) (((float) currentTime / (float) length) * 100);

                        player.slider.setValue(currentTime);
                        player.videoProgress.setProgress(progress);
                        player.timeProgress.setText(player.msToReadable(currentTime) + "/" + player.msToReadable(length));
                        player.currentMediaTime = currentTime;
                    }
                default:
                    break;
            }
        }
    }

    private String msToReadable(long ms) {
        String seconds = (int) (ms / 1000) % 60 < 10 ? "0" + (int) (ms / 1000) % 60 : String.valueOf((int) (ms / 1000) % 60);
        String minutes = (int) ((ms / (1000 * 60)) % 60) < 10 ? "0" + (int) ((ms / (1000 * 60)) % 60) : String.valueOf((int) ((ms / (1000 * 60)) % 60));
        String hours = (int) ((ms / (1000 * 60 * 60)) % 24) < 10 ? "0" + (int) ((ms / (1000 * 60 * 60)) % 24) : String.valueOf((int) ((ms / (1000 * 60 * 60)) % 24));
        return (hours + ":" + minutes + ":" + seconds);
    }

    private void download(String fileId, String fileSize) { ;
        Picasso.get().load("https://lh3.googleusercontent.com/u/0/d/" + fileId).into(mEpisodeThumbnail);
        loadingIndicator.setVisibility(View.VISIBLE);
        changeMediaOrCreatePlayer(Uri.parse("http://localhost:8080?id=" + fileId + "&size=" + fileSize));
//        Uri path = Uri.parse("https://www.googleapis.com/drive/v3/files/"+fileId+"?alt=media&key=AIzaSyAv1WgLGclLIlhKvzIiIVOiqZqDA0EM9TI");
//        Log.d(TAG, "https://www.googleapis.com/drive/v3/files/"+fileId+"?alt=media&key=AIzaSyAv1WgLGclLIlhKvzIiIVOiqZqDA0EM9TI");
//        changeMediaOrCreatePlayer(path);
        playMedia();
    }

    private void changeMediaOrCreatePlayer(Uri uri) {
        if (server == null) {
            server = new HttpServer();
            server.start();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            Media m = new Media(libvlc, uri);
            mMediaPlayer.setMedia(m);
        } else {
            createPlayer(uri);
        }
        ;
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
        return (int) (30 * res.getDisplayMetrics().density);
    }

    private void loadData(String url) {
        GetDataList task = new GetDataList(url, result -> {
            try {
                data = new JSONObject(result);
                ((TextView) findViewById(R.id.textTitle)).setText(data.get("name").toString());
                episodes = (JSONArray) data.get("episodes");
                episodeArrayList = prepareData();
                ((TextView) findViewById(R.id.textGenre)).setText("Genre: " + (CharSequence) data.get("keywords").toString().trim().replaceAll(",", " "));
                ((TextView) findViewById(R.id.textRating)).setText("Rating: " + (CharSequence) data.get("rating").toString());
                ((TextView) findViewById(R.id.textEpisodeNum)).setText("Episodes: " + episodeArrayList.size());
            } catch (JSONException e) {
            }
            initViews();
            findViewById(R.id.data_loader).setVisibility(View.INVISIBLE);
            overlay.setVisibility(View.VISIBLE);
            infoBar.setVisibility(View.VISIBLE);
        });
        task.execute();
    }
}