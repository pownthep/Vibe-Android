package com.example.vibe_android.player;

import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.example.vibe_android.MainActivity.APP_DATA;
import static com.example.vibe_android.MainActivity.externalData;

public class PlayerActivity extends AppCompatActivity implements IVLCVout.Callback {
    private JSONObject data;
    private JSONArray episodes;
    public final static String TAG = "PlayerActivity";
    private SurfaceView mSurface;
    private SurfaceView mSurfaceSubtitles;
    private SurfaceHolder holder;
    private SurfaceHolder subHolder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private Slider slider;
    private FloatingActionButton audio;
    private FloatingActionButton sub;
    private RecyclerView recyclerView;
    private FloatingActionButton togglePlay;
    private FrameLayout frame_surface;
    private ProgressBar progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        getData();
//        progress = findViewById(R.id.progressBar);
//        progress.setVisibility(View.INVISIBLE);
        mSurface = findViewById(R.id.surface);
        mSurfaceSubtitles = findViewById(R.id.sub_surface);
        mSurfaceSubtitles.setZOrderOnTop(true);
        subHolder = mSurfaceSubtitles.getHolder();
        subHolder.setFormat(PixelFormat.TRANSPARENT);
        frame_surface = findViewById(R.id.frame_surface);
        togglePlay = findViewById(R.id.play);
        holder = mSurface.getHolder();
        audio = findViewById(R.id.audio_cycle);
        sub = findViewById(R.id.sub_cycle);
        slider = findViewById(R.id.slider);
        initViews();

        togglePlay.setOnClickListener(view -> {
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
                pauseMedia();
                int count = mMediaPlayer.getAudioTracksCount();
                int current = mMediaPlayer.getAudioTrack();
                Log.d("VIBE","Current audio track index:" + current + "/" + count);
                int next = (current + 1) >= count ? 1 : (current + 1);
                Log.d("VIBE","Next audio track index:" + next + "/" + count);
                mMediaPlayer.setAudioTrack(next);
                playMedia();
            }
        });
        sub.setOnClickListener(view -> {
            if (mMediaPlayer != null) {
                pauseMedia();
                int count = mMediaPlayer.getSpuTracksCount();
                int currentId = mMediaPlayer.getSpuTrack();
                int currentIndex = 0;
                for (int i = 0; i < mMediaPlayer.getSpuTracks().length; i++) {
                    if (currentId == mMediaPlayer.getSpuTracks()[i].id) currentIndex = i;
                    Log.d("VIBE","sub track id:" + mMediaPlayer.getSpuTracks()[i].id + "/" + mMediaPlayer.getSpuTracks()[i].name);
                }
                int nextIndex = (currentIndex + 1) >= count ? 0 : (currentIndex + 1);
                Log.d("VIBE","Next sub track id:" + mMediaPlayer.getSpuTracks()[nextIndex].id + "/" + count);
                mMediaPlayer.setSpuTrack(mMediaPlayer.getSpuTracks()[nextIndex].id);
                playMedia();
            }
        });
        mSurface.setOnClickListener(view -> {
            if (slider.getVisibility() == View.VISIBLE) {
                Log.d("VIBE", "Toggle control off");
                slider.setVisibility(View.INVISIBLE);
                audio.setVisibility(View.INVISIBLE);
                sub.setVisibility(View.INVISIBLE);
                togglePlay.setVisibility(View.INVISIBLE);
                frame_surface.setVisibility(View.INVISIBLE);
            } else {
                Log.d("VIBE", "Toggle control on");
                slider.setVisibility(View.VISIBLE);
                audio.setVisibility(View.VISIBLE);
                sub.setVisibility(View.VISIBLE);
                togglePlay.setVisibility(View.VISIBLE);
                frame_surface.setVisibility(View.VISIBLE);
            }
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                pauseMedia();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                float value = slider.getValue();
                mMediaPlayer.setTime((long) value);
                playMedia();
            }
        });
        slider.setLabelFormatter((value) -> {
            int minutes = (int) (value / 1000) / 60;
            int seconds = (int) ((value / 1000) % 60);
            return minutes + ":" + seconds;
        });
        setSize(1,1);
    }

    private void playMedia() {
        if (mMediaPlayer != null) {
            mMediaPlayer.play();
            togglePlay.setImageResource(R.drawable.ic_round_pause_24);
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
            data = externalData.getJSONObject(Integer.parseInt(index));
            episodes = (JSONArray) data.get("episodes");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.episode_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        ArrayList<Episode> episodeArrayList = prepareData();
        EpisodeAdapter episodeAdapter = new EpisodeAdapter(this, episodeArrayList);
        recyclerView.setAdapter(episodeAdapter);

        episodeAdapter.setOnCardClickListener(position -> {
            Log.d("VIBE",episodes.getJSONObject(position).get("name") + "");
            download(episodes.getJSONObject(position).get("id") + "", episodes.getJSONObject(position).get("size") + "");
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
            Log.d("VIBE", String.valueOf(e));
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
    }

    /**
     * Used to set size for SurfaceView
     *
     * @param width
     * @param height
     */
    private void setSize(int width, int height) {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (!isPortrait) {
            LayoutParams lp = mSurface.getLayoutParams();
            lp.height = LayoutParams.MATCH_PARENT;
            mSurface.setLayoutParams(lp);
            recyclerView.setVisibility(View.INVISIBLE);
            ((FrameLayout.LayoutParams) audio.getLayoutParams()).gravity = Gravity.BOTTOM | Gravity.LEFT;
            ((FrameLayout.LayoutParams) sub.getLayoutParams()).gravity = Gravity.BOTTOM | Gravity.RIGHT;
            ((FrameLayout.LayoutParams) togglePlay.getLayoutParams()).gravity = Gravity.BOTTOM | Gravity.CENTER;
            ((FrameLayout.LayoutParams) slider.getLayoutParams()).gravity = Gravity.BOTTOM;
            ((FrameLayout.LayoutParams) slider.getLayoutParams()).bottomMargin = 100;
            ((FrameLayout.LayoutParams) frame_surface.getLayoutParams()).gravity = Gravity.BOTTOM;
            ((FrameLayout.LayoutParams) frame_surface.getLayoutParams()).topMargin = 0;

        } else {
            recyclerView.setVisibility(View.VISIBLE);
            ((FrameLayout.LayoutParams) audio.getLayoutParams()).gravity = Gravity.TOP | Gravity.LEFT;
            ((FrameLayout.LayoutParams) sub.getLayoutParams()).gravity = Gravity.TOP | Gravity.RIGHT;
            ((FrameLayout.LayoutParams) togglePlay.getLayoutParams()).gravity = Gravity.TOP | Gravity.CENTER;
            ((FrameLayout.LayoutParams) slider.getLayoutParams()).gravity = Gravity.TOP;
            ((FrameLayout.LayoutParams) frame_surface.getLayoutParams()).gravity = Gravity.TOP;
            ((FrameLayout.LayoutParams) frame_surface.getLayoutParams()).topMargin = 410;
            LayoutParams lp = mSurface.getLayoutParams();
            lp.height = 610;
            mSurface.setLayoutParams(lp);
        }

        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;
        if (holder == null || mSurface == null)
            return;
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

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
        subHolder.setFixedSize(mVideoWidth, mVideoHeight);
        LayoutParams lp = mSurface.getLayoutParams();

        lp.width = w;
        lp.height = isPortrait ? h : LayoutParams.MATCH_PARENT;

        mSurface.setLayoutParams(lp);
        mSurfaceSubtitles.setLayoutParams(lp);
        mSurface.invalidate();
        mSurfaceSubtitles.invalidate();
    }

    /**
     * Creates MediaPlayer and plays video
     *
     * @param media
     */
    private void createPlayer(Uri media) {
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
            vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();
            Media m = new Media(libvlc, media);
            mMediaPlayer.setMedia(m);
            playMedia();
        } catch (Exception e) {
            Log.d("VIBE",e.toString());
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
                case MediaPlayer.Event.Playing: {
                    Log.d("VIBE","Playing");
                    if (player.mMediaPlayer.getLength() > 0) {
                        player.slider.setValueTo(player.mMediaPlayer.getLength());
                        //player.progress.setVisibility(View.INVISIBLE);
                    }
                }
                case MediaPlayer.Event.Paused: {
                    Log.d("VIBE","Paused Event");
                    //player.progress.setVisibility(View.INVISIBLE);
                }
                case MediaPlayer.Event.Stopped:
                case MediaPlayer.Event.MediaChanged:
                case MediaPlayer.Event.Buffering:
                    //player.progress.setVisibility(View.VISIBLE);
                case MediaPlayer.Event.TimeChanged:
                    if (player.mMediaPlayer.getTime() > 0)
                        player.slider.setValue(player.mMediaPlayer.getTime());
                default:
                    break;
            }
        }
    }

    private void download(String fileId, String fileSize) {
        //1RrCqsjCEGwy_fclfEh1-4lGO4ipydsK5
        changeMediaOrCreatePlayer(Uri.parse("http://localhost:8080?id=" + fileId + "&size=" + fileSize));
        //changeMediaOrCreatePlayer(Uri.fromFile(new File(APP_DATA+ File.separator+"[1RrCqsjCEGwy_fclfEh1-4lGO4ipydsK5]-Bakemonogatari-01. Hitagi Crab, Part One")));
    }

    private void changeMediaOrCreatePlayer(Uri uri) {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            Media m = new Media(libvlc, uri);
            mMediaPlayer.setMedia(m);
            playMedia();
        } else createPlayer(uri);
    }
}