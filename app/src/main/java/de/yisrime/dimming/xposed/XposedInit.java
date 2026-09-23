package de.yisrime.dimming.xposed;

import android.content.SharedPreferences;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.util.Log;

import java.util.Locale;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import de.yisrime.dimming.BacklightRequest;
import de.yisrime.dimming.BuildConfig;
import de.yisrime.dimming.IBacklightOverrideService;
import de.yisrime.dimming.ServiceDiscovery;
import de.yisrime.dimming.ServiceDiscoveryResult;

public class XposedInit extends XposedModule {
    private static final String TAG = "PseudoDcBacklight.Xposed";

    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        final var classLoader = param.getClassLoader();
        final SharedPreferences preferences;
        try {
            preferences = getRemotePreferences(ServiceDiscovery.PREFERENCE_GROUP);
        } catch (Exception e) {
            Log.e(TAG, "failed to request remote preferences, module disabled", e);
            return;
        }

        if (Build.VERSION.SDK_INT >= 34) {
            DisplayControlProxy.initialize(classLoader);
        }

        final var overrideService = new BacklightOverrideService(classLoader, preferences);

        final var localDisplayDevice = Compat.findClass("com.android.server.display.LocalDisplayAdapter$LocalDisplayDevice", classLoader);

        final var backlightAdapter = Compat.findClass("com.android.server.display.LocalDisplayAdapter$BacklightAdapter", classLoader);

        final var localDisplayAdapter = Compat.findClass("com.android.server.display.LocalDisplayAdapter", classLoader);

        final var backlightAdapter_setBacklight = Compat.findMethod(
            backlightAdapter,
            "setBacklight",
            float.class,  // sdrBacklight
            float.class,  // sdrNits
            float.class,  // backlight
            float.class   // nits
        );
        // Xiaomi HyperOS 2 compat
        var backlightAdapter_setBacklight_hookTarget = backlightAdapter_setBacklight;
        var miuiVersion = Compat.callStatic(
            Compat.findClass("android.os.SystemProperties", classLoader),
            "get",
            "ro.mi.os.version.name"
        );
        if("OS2.0".equals(miuiVersion)) {
            try {
                final var backlightAdapter_setBacklight_miui = Compat.findMethod(
                    backlightAdapter,
                    "setBacklight",
                    float.class,  // sdrBacklight
                    float.class,  // sdrNits
                    float.class,  // backlight
                    float.class,  // nits
                    boolean.class     // galleryHdrBoost
                );
                Log.i(TAG, "HyperOS 2 detected, using dedicated hook target");
                backlightAdapter_setBacklight_hookTarget = backlightAdapter_setBacklight_miui;
            } catch (NoSuchMethodError e) {
                Log.i(TAG, "HyperOS 2 detected, but the HyperOS-specific hook target couldn't be found; ignoring");
            }
        }

        hook(Compat.findConstructor(localDisplayDevice,
            localDisplayAdapter,     // [surrounding this]
            android.os.IBinder.class,     // displayToken
            long.class,        // physicalDisplayId
            Compat.findClass("android.view.SurfaceControl$StaticDisplayInfo", classLoader),
            Compat.findClass("android.view.SurfaceControl$DynamicDisplayInfo", classLoader),
            Compat.findClass("android.view.SurfaceControl$DesiredDisplayModeSpecs", classLoader),
            boolean.class     // isDefaultDisplay
        )).intercept(chain -> {
            chain.proceed();
            // chain.getArg(3) = StaticDisplayInfo, chain.getArg(6) = isDefaultDisplay
            if (!isInternalDisplay(chain.getArg(3), (boolean) chain.getArg(6))) return null;
            final var displayDeviceConfig = new DisplayDeviceConfigProxy(Compat.call(chain.getThisObject(), "getDisplayDeviceConfig"));
            overrideService.lateInitialize(displayDeviceConfig);
            return null;
        });
        hook(Compat.findConstructor(backlightAdapter,
                android.os.IBinder.class,
                boolean.class,
                Compat.findClass("com.android.server.display.LocalDisplayAdapter$SurfaceControlProxy", classLoader)
        )).intercept(chain -> {
            chain.proceed();
            final var token = (android.os.IBinder) chain.getArg(0);
            final var staticInfo = SurfaceControlCompat.getStaticDisplayInfo(token);
            if (staticInfo == null) return null;
            final var isInternal = staticInfo.isInternal;
            Compat.putInternalDisplay(chain.getThisObject(), isInternal);
            if (isInternal) {
                overrideService.backlightAdapter = new BacklightAdapterProxy(chain.getThisObject(), backlightAdapter_setBacklight);
            }
            return null;
        });

        hook(backlightAdapter_setBacklight_hookTarget).intercept(chain -> {
            // void setBacklight(float sdrBacklight, float sdrNits, float backlight, float nits)
            final var self = chain.getThisObject();
            if (!Compat.isInternalDisplay(self)) return chain.proceed();
            final var args = chain.getArgs().toArray();
            final var requestSdrBacklight = (float) args[0];
            final var requestSdrNits = (float) args[1];
            final var requestBacklight = (float) args[2];
            final var requestNits = (float) args[3];

            var request = new BacklightRequest(requestSdrBacklight, requestSdrNits, requestBacklight, requestNits);

            var prevOverride = overrideService.getLastBacklightOverrideState();
            var overrideState = overrideService.getOverrideBacklightAndGain(request);

            var overrideBacklight = overrideState.overrideRequest;
            args[0] = overrideBacklight.sdrBacklightLevel;
            args[1] = overrideBacklight.sdrBacklightNits;
            args[2] = overrideBacklight.backlightLevel;
            args[3] = overrideBacklight.backlightNits;
            // HyperOS 2: leave galleryHdrBoost argument unchanged
            Log.d(TAG, String.format(Locale.ROOT, "setBacklight(%f->%f, %f->%f, %f->%f, %f->%f)",
                    requestSdrBacklight,
                    overrideBacklight.sdrBacklightLevel,
                    requestSdrNits,
                    overrideBacklight.sdrBacklightNits,
                    requestBacklight,
                    overrideBacklight.backlightLevel,
                    requestNits,
                    overrideBacklight.backlightNits));

            float setGainAfterBrightness;
            if (overrideBacklight.backlightLevel > prevOverride.overrideRequest.backlightLevel) {
                // increased hardware brightness:
                // set gain -> darker
                // set brightness -> brighter
                setGainAfterBrightness = -1.0f;
                overrideService.setTransformGain(overrideState.gain);
            } else {
                // decreased hardware brightness:
                // set brightness -> darker
                // set gain -> brighter
                // a dark spike is more acceptable than a bright spike
                setGainAfterBrightness = overrideState.gain;
            }

            var result = chain.proceed(args);

            if (setGainAfterBrightness > 0.0f) {
                overrideService.setTransformGain(setGainAfterBrightness);
            }
            overrideService.notifyAllListeners();
            return result;
        });

        final var binderServiceClass = Compat.findClass("com.android.server.display.DisplayManagerService$BinderService", classLoader);
        hook(Compat.findMethod(
            Compat.findClass("android.hardware.display.IDisplayManager$Stub", classLoader),
            "onTransact",
            int.class, Parcel.class, Parcel.class, int.class
        )).intercept(chain -> {
            if (!chain.getThisObject().getClass().equals(binderServiceClass)) return chain.proceed();

            final int code = (int) chain.getArg(0);

            if (code != ServiceDiscovery.TRANSACTION_SERVICE_DISCOVERY) return chain.proceed();

            final Parcel reply = (Parcel) chain.getArg(2);

            final var pmb = ServiceManager.getService("package");
            // PMS not initialized yet, skip hook.
            if (pmb == null) return chain.proceed();

            final int uid = Binder.getCallingUid();
            final var pm = IPackageManager.Stub.asInterface(pmb);
            final var callingpackage = pm.getNameForUid(uid);

            if (!BuildConfig.APPLICATION_ID.equals(callingpackage)) return chain.proceed();

            if (reply == null) {
                return false;
            }
            final var response = new ServiceDiscoveryResult();
            response.version = IBacklightOverrideService.VERSION;
            response.service = overrideService.binderService.asBinder();
            response.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
            return true;
        });
    }

    private static boolean isInternalDisplay(Object staticInfo, boolean isDefaultDisplay) {
        if (staticInfo == null) return isDefaultDisplay;
        try {
            return Compat.getBooleanField(staticInfo, "isInternal");
        } catch (Throwable t) {
            return isDefaultDisplay;
        }
    }
}
