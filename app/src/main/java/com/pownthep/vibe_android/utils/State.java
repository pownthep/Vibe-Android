package com.pownthep.vibe_android.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class State extends ViewModel {
    private final MutableLiveData<String> currentMediaId = new MutableLiveData<>();

    public MutableLiveData<Long> getCurrentMediaTime() {
        return currentMediaTime;
    }

    private final MutableLiveData<Long> currentMediaTime = new MutableLiveData<>();


    public LiveData<String> getCurrentMediaId() {
        return currentMediaId;
    }

    public State() {
        // trigger user load.
    }

    void doAction() {
        // depending on the action, do necessary business logic calls and update the
        // userLiveData.
    }
}
