package de.yisrime.dimming;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.List;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class App extends Application {
    private static final String TAG = "PseudoDcBacklight.App";
    public static volatile SharedPreferences remotePreferences = null;

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService service) {
                try {
                    var preferences = service.getRemotePreferences(ServiceDiscovery.PREFERENCE_GROUP);
                    importLegacyPreferences(preferences);
                    remotePreferences = preferences;
                } catch (RuntimeException e) {
                    Log.e(TAG, "failed to get remote preferences", e);
                }
            }

            @Override
            public void onServiceDied(XposedService service) {
                remotePreferences = null;
            }
        });
    }

    private void importLegacyPreferences(SharedPreferences preferences) {
        if (!preferences.getAll().isEmpty()) return;
        var name = ServiceDiscovery.PREFERENCE_GROUP;
        var direct = createDeviceProtectedStorageContext();
        var editor = preferences.edit();
        var migrated = false;
        for (var legacy : List.of(direct.getSharedPreferences(name, MODE_PRIVATE),
                getSharedPreferences(name, MODE_PRIVATE))) {
            for (var entry : legacy.getAll().entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                else if (value instanceof Float) editor.putFloat(key, (Float) value);
                else continue;
                migrated = true;
            }
        }
        if (!migrated) return;
        editor.apply();
        direct.deleteSharedPreferences(name);
        deleteSharedPreferences(name);
    }
}
