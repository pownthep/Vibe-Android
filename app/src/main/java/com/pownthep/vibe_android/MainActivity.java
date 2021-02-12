package com.pownthep.vibe_android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // Generated
    public final static String TAG = "MainActivity";

    // Google Drive
    public static final int REQUEST_CODE_SIGN_IN = 1;
    public static String accessToken;

    //directory
    public static String APP_DATA;

    public static final String SHARED_PREFS = "vibe_preferences";
    public static final String EXTERNAL_DATA = "vibe_data";
    public static final String CACHE_OPTION = "cache_option";
    public static final String LIBRARY_ARRAY = "vibe_lib";
    public static final String REFRESH_TOKEN = "refresh token";
    public static long expirationDate = -1;

    public static boolean isCacheEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        isCacheEnabled = sharedPreferences.getBoolean(CACHE_OPTION, false);
        //Google Drive.
        if (sharedPreferences.getString(REFRESH_TOKEN, null) == null) {
            requestSignIn();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("VIBE START", "STARTING!!!!!");
        APP_DATA = String.valueOf(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                handleSignInResult(resultData);
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

    /**
     * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
     */
    public void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestServerAuthCode("511377925028-ar09unh2rtfs8h0vh2u08p8nn4i9plqm.apps.googleusercontent.com")
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
    public void handleSignInResult(Intent result) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String authCode = account.getServerAuthCode();
            AsyncTask.execute(() -> {
                try {
                    GoogleTokenResponse tokenResponse =
                            new GoogleAuthorizationCodeTokenRequest(
                                    new NetHttpTransport(),
                                    JacksonFactory.getDefaultInstance(),
                                    "https://oauth2.googleapis.com/token",
                                    "511377925028-ar09unh2rtfs8h0vh2u08p8nn4i9plqm.apps.googleusercontent.com",
                                    "QlADgLswq3Syw3e44OwNS4oa",
                                    authCode,
                                    "")  // Specify the same redirect URI that you use with your web
                                    // app. If you don't have a web version of your app, you can
                                    // specify an empty string.
                                    .execute();

                    String refreshToken = tokenResponse.getRefreshToken();
                    SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                    if (sharedPreferences.getString(REFRESH_TOKEN, null) == null) {
                        SharedPreferences.Editor edit = sharedPreferences.edit();
                        edit.putString(REFRESH_TOKEN, refreshToken);
                        edit.commit();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // TODO(developer): send code to server and exchange for access/refresh/ID tokens
        } catch (ApiException e) {
            Log.w(TAG, "Sign-in failed", e);
        }
//        GoogleSignIn.getSignedInAccountFromIntent(result)
//                .addOnSuccessListener(googleAccount -> {
//                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());
//
//                    // Use the authenticated account to sign in to the Drive service.
//                    GoogleAccountCredential credential =
//                            GoogleAccountCredential.usingOAuth2(
//                                    this, Collections.singleton(DriveScopes.DRIVE));
//                    credential.setSelectedAccount(googleAccount.getAccount());
//                    AsyncTask.execute(() -> {
//                        try {
//                            accessToken = credential.getToken();
//                        } catch (IOException | GoogleAuthException e) {
//                            e.printStackTrace();
//                        }
//                    });
//                })
//                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }
}

