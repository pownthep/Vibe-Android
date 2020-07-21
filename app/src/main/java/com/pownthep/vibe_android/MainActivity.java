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

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import com.pownthep.vibe_android.http.HttpServer;

import java.io.IOException;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    // Generated
    public final static String TAG = "MainActivity";

    // Google Drive
    private static final int REQUEST_CODE_SIGN_IN = 1;
    public static String accessToken;

    //directory
    public static String APP_DATA;

    public static final String SHARED_PREFS = "vibe_preferences";
    public static final String EXTERNAL_DATA = "vibe_data";
    public static final String CACHE_OPTION = "cache_option";
    public static final String LIBRARY_ARRAY = "vibe_lib";

    public static boolean isCacheEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        isCacheEnabled = sharedPreferences.getBoolean(CACHE_OPTION, true);
        //Google Drive.
        requestSignIn();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("VIBE START", "STARTING!!!!!");
        APP_DATA = String.valueOf(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS));
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
                                    this, Collections.singleton(DriveScopes.DRIVE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    AsyncTask.execute(() -> {
                        try {
                            accessToken = credential.getToken();
                        } catch (IOException | GoogleAuthException e) {
                            e.printStackTrace();
                        }
                    });
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }
}

