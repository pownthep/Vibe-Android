package com.example.vibe_android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import org.json.JSONException;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;


import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements IVLCVout.Callback {
    // Generated
    public final static String TAG = "MainActivity";

    // Google Drive
    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private DriveServiceHelper mDriveServiceHelper;
    private String mOpenFileId;

    private String accessToken = "";
    private static final int BUFFER_SIZE = 4096;

    // VLC
    private String mFilePath;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int audioTrackCount = 0;
    private int subTrackCount = 0;

    // http
    private JSONArray externalData = new JSONArray();

    //nav
    private com.google.android.material.bottomnavigation.BottomNavigationView BottomNavigationView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        //Google Drive
        // Authenticate the user. For most apps, this should be done when the user performs an
        // action that requires Drive access rather than in onCreate.
        requestSignIn();






        //VLC
//        mSurface = (SurfaceView) findViewById(R.id.surface);
//        holder = mSurface.getHolder();
        //getExternalData();

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
        TinyWebServer.stopServer();
    }

    private void cycleAudio() {
        if (mMediaPlayer != null) {

        }
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
    private void createPlayer(java.io.File media) {
        releasePlayer();
        try {
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
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
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
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
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();
            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    /**
     * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
     */
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /**
     * Handles the {@code result} of a completed sign-in activity initiated from {@link
     * #requestSignIn()}.
     */
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());

                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                accessToken = credential.getToken();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (GoogleAuthException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Drive API Migration")
                                    .build();

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    private void download(String fileId) {
        Log.d(TAG, String.valueOf(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)));
        String saveDir = String.valueOf(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
        if (accessToken.length() > 0) {
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
                        int responseCode = urlConnection.getResponseCode();

                        if (responseCode == HttpURLConnection.HTTP_OK) {
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
                            String saveFilePath = saveDir + java.io.File.separator + "test3.mkv";

                            // opens an output stream to save into file
                            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                            int bytesRead = -1;
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int count = 0;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                                count++;
                                if (count == 1) {
                                    createPlayer(new java.io.File(saveFilePath));
                                }
                            }

                            outputStream.close();
                            inputStream.close();

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
        }

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Downloading a file");
            java.io.File targetFile = new java.io.File(String.valueOf(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" + fileId));
//            mDriveServiceHelper.downloadFile(targetFile, fileId)
//                    .addOnSuccessListener(fileList -> {
//                        Log.d(TAG, "File download success!");
//                    })
//                    .addOnFailureListener(exception -> Log.e(TAG, "Unable to download files.", exception));
            releasePlayer();


        }
    }

    private void getExternalData() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String fileURL = "https://boring-northcutt-5fd361.netlify.app/full.json";

                try {
                    JSONArray json = readJsonFromUrl(fileURL);
                    System.out.println(json.getJSONObject(0).get("name"));
                    //System.out.println(json.get("id"));
                    externalData = json;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

}

