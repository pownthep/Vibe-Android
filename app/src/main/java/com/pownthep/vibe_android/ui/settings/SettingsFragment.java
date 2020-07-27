package com.pownthep.vibe_android.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;
import com.pownthep.vibe_android.R;

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Objects;

import static com.pownthep.vibe_android.MainActivity.APP_DATA;
import static com.pownthep.vibe_android.MainActivity.CACHE_OPTION;
import static com.pownthep.vibe_android.MainActivity.EXTERNAL_DATA;
import static com.pownthep.vibe_android.MainActivity.SHARED_PREFS;
import static com.pownthep.vibe_android.MainActivity.isCacheEnabled;

public class SettingsFragment extends Fragment {
    private CheckBox cacheOption;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        root.findViewById(R.id.clear_cache_btn).setOnClickListener(view -> {
            clearCache();
            initView(root);
        });
        cacheOption = root.findViewById(R.id.enable_caching_btn);
        cacheOption.setOnClickListener(view -> {
            saveData();
        });
        root.findViewById(R.id.refresh_library_btn).setOnClickListener(view -> {
            boolean committed = refreshData();
            Snackbar.make(getView(), committed ? "Reset catalogue completed":"Unable to reset the catalogue", Snackbar.LENGTH_SHORT)
                    .show();
        });
        root.findViewById(R.id.restore_default).setOnClickListener(view -> {
            restoreDefault();
            initView(root);
        });
        initView(root);
        return root;
    }

    private void saveData() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SHARED_PREFS, getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(CACHE_OPTION, cacheOption.isChecked());
        isCacheEnabled = cacheOption.isChecked();
        editor.apply();
    }

    private void initView(View root) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SHARED_PREFS, getContext().MODE_PRIVATE);
        boolean enableCaching = sharedPreferences.getBoolean(CACHE_OPTION, true);
        cacheOption.setChecked(enableCaching);
        long cacheSize = getCacheSize();
        ((TextView) root.findViewById(R.id.cache_text)).setText("Cache size: " + humanReadableByteCountBin(cacheSize) + "/5 GiB");
        ((ProgressBar) root.findViewById(R.id.cache_size)).setProgress((int) (cacheSize / 5368709120.0 * 100));
    }

    private long getCacheSize() {
        File folder = new File(APP_DATA);
        long cacheSize = 0;
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            cacheSize += fileEntry.length();
        }
        return cacheSize;
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

    private void clearCache() {
        File folder = new File(APP_DATA);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.getName().contains("@") && fileEntry.isFile()) fileEntry.delete();
        }
    }

    private boolean refreshData() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SHARED_PREFS, getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(EXTERNAL_DATA);
        return editor.commit();
    }

    public void restoreDefault() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SHARED_PREFS, getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().commit();
    }
}