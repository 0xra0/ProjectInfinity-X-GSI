package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.biometrics.BiometricFingerprintConstants;
import android.hardware.biometrics.fingerprint.IFingerprint;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerGlobal;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

/**
 * Fingerprint swipe gestures for HALs that report them as vendor acquired codes instead of input
 * events. A vendor backend found through the HAL's binder extension arms the sensor while the
 * swipe setting is on, and the reported swipes are injected as system navigation keys so they
 * take the same path as sensors that emit those keys through a keylayout.
 */
final class FingerprintNavigation {
    private static final String TAG = "FingerprintNavigation";

    /** Lets Settings offer the swipe toggle when no input device maps the navigation keys. */
    private static final String PROP_SUPPORTED = "sys.phh.fp_nav_keys";

    interface Backend {
        void setEnabled(boolean enabled) throws RemoteException;

        void setScreenOn(boolean screenOn) throws RemoteException;

        /** Returns the navigation key a vendor acquired code stands for, or KEYCODE_UNKNOWN. */
        int keyCodeFor(int vendorCode);
    }

    @Nullable
    private static Backend createBackend(@NonNull IBinder extension, int sensorId)
            throws RemoteException {
        return SamsungFingerprintNavigation.create(extension, sensorId);
    }

    @Nullable private static volatile FingerprintNavigation sInstance;

    @NonNull private final Context mContext;
    @NonNull private final Handler mHandler;
    @NonNull private final Backend mBackend;
    private boolean mEnabled;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                case Intent.ACTION_SCREEN_OFF:
                    if (mEnabled) {
                        setScreenOn(isInteractive());
                    }
                    break;
                default:
                    updateEnabled();
                    break;
            }
        }
    };

    private final ContentObserver mSettingObserver;

    static void attach(@NonNull Context context, @NonNull Handler handler,
            @NonNull IFingerprint daemon, int sensorId) {
        final Backend backend;
        try {
            final IBinder extension = daemon.asBinder().getExtension();
            backend = extension != null ? createBackend(extension, sensorId) : null;
        } catch (RemoteException e) {
            Slog.w(TAG, "Unable to query the fingerprint HAL extension", e);
            return;
        }
        if (backend == null) {
            return;
        }
        Slog.i(TAG, "Using " + backend.getClass().getSimpleName());

        final FingerprintNavigation previous = sInstance;
        if (previous != null) {
            previous.detach();
        }
        final FingerprintNavigation navigation =
                new FingerprintNavigation(context, handler, backend);
        sInstance = navigation;
        SystemProperties.set(PROP_SUPPORTED, "1");
        // Before boot completes, BOOT_COMPLETED triggers the first update.
        if (SystemProperties.getBoolean("sys.boot_completed", false)) {
            handler.post(navigation::updateEnabled);
        }
    }

    /** Returns true if the acquired event was a swipe and should not reach the client. */
    static boolean onAcquired(int acquiredInfo, int vendorCode) {
        final FingerprintNavigation navigation = sInstance;
        if (navigation == null
                || acquiredInfo != BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_VENDOR) {
            return false;
        }
        final int keyCode = navigation.mBackend.keyCodeFor(vendorCode);
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return false;
        }
        // Inject from our own thread so the permission check sees system_server, not the HAL.
        navigation.mHandler.post(() -> {
            if (navigation.isInteractive()) {
                navigation.injectKey(keyCode);
            }
        });
        return true;
    }

    private FingerprintNavigation(@NonNull Context context, @NonNull Handler handler,
            @NonNull Backend backend) {
        mContext = context;
        mHandler = handler;
        mBackend = backend;
        mSettingObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                updateEnabled();
            }
        };

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, handler);
        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED), false,
                mSettingObserver, UserHandle.USER_ALL);
    }

    private void detach() {
        mContext.unregisterReceiver(mReceiver);
        mContext.getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    private boolean isInteractive() {
        return mContext.getSystemService(PowerManager.class).isInteractive();
    }

    private void updateEnabled() {
        final boolean enabled = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
        if (enabled == mEnabled) {
            return;
        }
        try {
            mBackend.setEnabled(enabled);
            mEnabled = enabled;
        } catch (RemoteException e) {
            Slog.w(TAG, "Unable to " + (enabled ? "enable" : "disable") + " swipe gestures", e);
        }
    }

    private void setScreenOn(boolean screenOn) {
        try {
            mBackend.setScreenOn(screenOn);
        } catch (RemoteException e) {
            Slog.w(TAG, "Unable to report the screen state", e);
        }
    }

    private void injectKey(int keyCode) {
        final long now = SystemClock.uptimeMillis();
        for (int action : new int[] {KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP}) {
            final KeyEvent event = new KeyEvent(now, now, action, keyCode, 0 /* repeat */,
                    0 /* metaState */, KeyCharacterMap.VIRTUAL_KEYBOARD, 0 /* scancode */,
                    KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD);
            InputManagerGlobal.getInstance().injectInputEvent(event,
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    }
}
