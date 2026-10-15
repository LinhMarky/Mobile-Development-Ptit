package com.example.roomly.push;

import android.app.Application;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.repository.SessionRepository;

public final class RoomlyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.initialize(this);
        SessionRepository.getInstance().restoreLocalSession();
        PushManager.initialize(this);
    }
}
