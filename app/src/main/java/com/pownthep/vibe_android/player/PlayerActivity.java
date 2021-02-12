package com.pownthep.vibe_android.player;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
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

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.github.rubensousa.previewseekbar.PreviewBar;
import com.github.rubensousa.previewseekbar.PreviewSeekBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.pownthep.vibe_android.R;
import com.pownthep.vibe_android.http.HttpServer;
import com.pownthep.vibe_android.utils.GetDataList;
import com.pownthep.vibe_android.utils.GetImageBitmap;
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
import java.util.HashMap;
import java.util.Map;

import static com.pownthep.vibe_android.MainActivity.LIBRARY_ARRAY;
import static com.pownthep.vibe_android.MainActivity.REFRESH_TOKEN;
import static com.pownthep.vibe_android.MainActivity.SHARED_PREFS;
import static com.pownthep.vibe_android.MainActivity.accessToken;
import static com.pownthep.vibe_android.MainActivity.expirationDate;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class PlayerActivity extends AppCompatActivity implements IVLCVout.Callback {
    private boolean ENDED = false;
    private JSONObject data;
    private JSONArray episodes;
    public final static String TAG = "PlayerActivity";
    private SurfaceView mSurface;
    private SurfaceView mSurfaceSubtitles;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
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
    private long restartMediaTime = -1;
    private LinearLayout infoBar;
    private FloatingActionButton audio;
    private FloatingActionButton sub;
    private ImageView previewImageView;
    private TextView previewTextView;
    private PreviewSeekBar previewSeekBar;
    private Bitmap previewSprites;
    private FrameLayout previewFrameLayout;
    private FrameLayout.LayoutParams overlayParams;
    private FrameLayout.LayoutParams videoParams;
    private FrameLayout.LayoutParams subParams;
    private FrameLayout.LayoutParams thumbnailParams;
    private FrameLayout.LayoutParams darkOverlayParams;
    private FrameLayout.LayoutParams videoProgressParams;
    private FrameLayout.LayoutParams sliderParams;
    private FrameLayout.LayoutParams previewFrameParams;
    private FrameLayout.LayoutParams timeProgressParams;
    private int statusBarHeight;
    private int dp;
    SharedPreferences sharedPreferences;
    public Uri episodesUri;

    private RequestQueue MyRequestQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        setContentView(R.layout.activity_player);
        mSurface = findViewById(R.id.surface);
        mSurfaceSubtitles = findViewById(R.id.sub_surface);
        mSurfaceSubtitles.setZOrderOnTop(true);
        SurfaceHolder subHolder = mSurfaceSubtitles.getHolder();
        subHolder.setFormat(PixelFormat.TRANSPARENT);
        togglePlay = findViewById(R.id.play);
        holder = mSurface.getHolder();
        audio = findViewById(R.id.audio_cycle);
        sub = findViewById(R.id.sub_cycle);
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
        previewImageView = findViewById(R.id.previewImageView);
        previewTextView = findViewById(R.id.previewTextView);
        FloatingActionButton backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(view -> {
            onBackPressed();
        });
        previewFrameLayout = findViewById(R.id.previewFrameLayout);
        statusBarHeight = statusBarHeight(getResources());
        dp = (int) getResources().getDisplayMetrics().density;

        previewSeekBar = findViewById(R.id.previewSeekBar);

        previewSeekBar.setPreviewLoader((currentPosition, max) -> {
            if (previewSprites != null) {
                int number = (int) (((float) currentPosition / (float) max) * 119); // 0-119
                int y = number / 10;
                int x = number % 10;
                Bitmap preview = Bitmap.createBitmap(previewSprites, 160 * x, 90 * y, 160, 90);
                previewImageView.setImageBitmap(preview);
            }
            previewTextView.setText(msToReadable(currentPosition));
        });
        MyRequestQueue = Volley.newRequestQueue(this);

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

        previewSeekBar.addOnScrubListener(new PreviewBar.OnScrubListener() {
            @Override
            public void onScrubStart(PreviewBar previewBar) {
                restartMediaTime = -1;
                seeking = true;
                pauseMedia();
                togglePlay.setVisibility(View.INVISIBLE);
                timeProgress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onScrubMove(PreviewBar previewBar, int progress, boolean fromUser) {

            }

            @Override
            public void onScrubStop(PreviewBar previewBar) {
                if (mMediaPlayer == null) return;
                togglePlay.setVisibility(View.VISIBLE);
                timeProgress.setVisibility(View.VISIBLE);
                Episode currentEp = episodeArrayList.get(currentMediaId);
                episodesUri = Uri.parse("http://localhost:8080?id=" + currentEp.getId() + "&size=" + currentEp.getBytes());
                if (server == null || !server.isAlive()) {
                    server = new HttpServer();
                    server.start();
                }
                long value = previewBar.getProgress();
                if (ENDED) {
                    download(currentEp.getId(), currentEp.getBytes());
                }
                mMediaPlayer.setTime(value);
                loadingIndicator.setVisibility(View.VISIBLE);
                seeking = false;
                playMedia();
            }
        });
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mHeight = displayMetrics.heightPixels;
        mWidth = displayMetrics.widthPixels;

        overlayParams = (FrameLayout.LayoutParams) overlay.getLayoutParams();
        videoParams = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
        subParams = (FrameLayout.LayoutParams) mSurfaceSubtitles.getLayoutParams();
        thumbnailParams = (FrameLayout.LayoutParams) mEpisodeThumbnail.getLayoutParams();
        darkOverlayParams = (FrameLayout.LayoutParams) darkOverlay.getLayoutParams();
        videoProgressParams = (FrameLayout.LayoutParams) videoProgress.getLayoutParams();
        sliderParams = (FrameLayout.LayoutParams) previewSeekBar.getLayoutParams();
        previewFrameParams = (FrameLayout.LayoutParams) previewFrameLayout.getLayoutParams();
        timeProgressParams = (FrameLayout.LayoutParams) timeProgress.getLayoutParams();
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
            int j = 0;
            for (int i = 0; i < episodes.length(); i++) {
                try {
                    episodes.getJSONObject(i).get("size");
                    Episode episode = new Episode();
                    long sizeBytes = Long.parseLong(episodes.getJSONObject(i).get("size").toString());
                    episode.setSize(humanReadableByteCountBin(sizeBytes));
                    episode.setName(episodes.getJSONObject(i).get("name") + "");
                    episode.setId(episodes.getJSONObject(i).get("id") + "");
                    episode.setBytes(episodes.getJSONObject(i).get("size").toString());
                    episode.setIndex(j);
                    tmp.add(episode);
                    j++;
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
            restartMediaTime = currentMediaTime;
            Episode currentEp = episodeArrayList.get(currentMediaId);
            episodesUri = Uri.parse("http://localhost:8080?id=" + currentEp.getId() + "&size=" + currentEp.getBytes());
            if (server == null || !server.isAlive()) {
                server = new HttpServer();
                server.start();
            }
            createPlayer(episodesUri);
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

            timeProgressParams.bottomMargin = 40 * dp;
            sliderParams.gravity = Gravity.BOTTOM;
            sliderParams.bottomMargin = 20 * dp;
            sliderParams.leftMargin = 60 * dp;
            sliderParams.rightMargin = 60 * dp;
            sliderParams.topMargin = 0;
            previewFrameParams.bottomMargin = 60 * dp;
            videoParams.topMargin = 0;
            overlayParams.topMargin = 0;
            subParams.topMargin = 0;
            recyclerView.setVisibility(View.INVISIBLE);

        } else {
            infoBar.setVisibility(View.VISIBLE);
            showSystemUI();
            addToLibBtn.setVisibility(View.VISIBLE);

            overlayParams.height = 630;
            videoParams.height = 610;
            subParams.height = 610;
            thumbnailParams.height = 780 + statusBarHeight;
            darkOverlayParams.height = mHeight + statusBarHeight;

            videoParams.width = mWidth;
            subParams.width = mWidth;
            darkOverlayParams.width = mWidth;
            timeProgressParams.bottomMargin = 20 * dp;
            sliderParams.gravity = Gravity.TOP;
            sliderParams.topMargin = 585;
            sliderParams.bottomMargin = 0;
            sliderParams.leftMargin = 0;
            sliderParams.rightMargin = 0;
            previewFrameParams.bottomMargin = 20 * dp;
            videoParams.topMargin = statusBarHeight;
            overlayParams.topMargin = statusBarHeight;
            subParams.topMargin = statusBarHeight;
            videoProgressParams.topMargin = 0;
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void createPlayer(Uri media) {
        releasePlayer();
        try {
            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            mSurface.setVisibility(View.VISIBLE);
            mSurfaceSubtitles.setVisibility(View.VISIBLE);
            audio.setVisibility(View.VISIBLE);
            sub.setVisibility(View.VISIBLE);
            lockRotate.setVisibility(View.VISIBLE);
            previewSeekBar.setVisibility(View.VISIBLE);
            timeProgress.setVisibility(View.VISIBLE);
            boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            ArrayList<String> options = new ArrayList<>();
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
                    player.ENDED = true;
                    break;
                case MediaPlayer.Event.Playing: {
                    Log.d("VIBE", "Playing");
                    Log.d("VIBE", "Setting loading off");
                    player.loadingIndicator.setVisibility(View.INVISIBLE);
                    if (player.mMediaPlayer.getLength() > 0) {
                        player.previewSeekBar.setMax((int) player.mMediaPlayer.getLength());
                    }
                    if (player.restartMediaTime > -1) {
                        player.mMediaPlayer.setTime(player.restartMediaTime);
                    }
                    break;
                }
                case MediaPlayer.Event.Paused: {
                    Log.d("VIBE", "Paused Event");
                    break;
                }
                case MediaPlayer.Event.Stopped:
                    Log.d("VIBE", "Stopped Event");
                    break;
                case MediaPlayer.Event.MediaChanged:
                    Log.d("VIBE", "MediaChanged Event");
                    break;
                case MediaPlayer.Event.Buffering: {
                    Log.d("VIBE", "Buffering Event");
                    break;
                }
                case MediaPlayer.Event.TimeChanged:
                    if (player.mMediaPlayer.getTime() > 0 && !player.seeking) {
                        int currentTime = (int) player.mMediaPlayer.getTime();
                        long length = player.mMediaPlayer.getLength();
                        player.previewSeekBar.setProgress(currentTime);
                        player.timeProgress.setText(player.msToReadable(currentTime) + " / " + player.msToReadable(length));
                        player.currentMediaTime = currentTime;
                    }
                    break;
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

    private void download(String fileId, String fileSize) {
        ENDED = false;
        loadingIndicator.setVisibility(View.VISIBLE);
        previewSprites = null;
        GetImageBitmap task = new GetImageBitmap("https://pownthep-storage.b-cdn.net/previews/" + fileId + ".png", result -> {
            previewSprites = result;
        });
        task.execute();
        changeMediaOrCreatePlayer(Uri.parse("http://localhost:8080?id=" + fileId + "&size=" + fileSize));
        playMedia();
    }

    private void changeMediaOrCreatePlayer(Uri uri) {
        if (server == null || !server.isAlive()) {
            server = new HttpServer();
            server.start();
        }
        createPlayer(uri);
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
                ((TextView) findViewById(R.id.textGenre)).setText("Genre: " + data.get("keywords").toString().trim().replaceAll(",", " "));
                ((TextView) findViewById(R.id.textRating)).setText("Rating: " + data.get("rating").toString());
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