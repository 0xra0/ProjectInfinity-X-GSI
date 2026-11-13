package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.KeyEvent;

import vendor.samsung.hardware.biometrics.fingerprint.ISehFingerprint;
import vendor.samsung.hardware.biometrics.fingerprint.SehResult;

/** Samsung capacitive sensors, driven the way One UI's SemFpGestureManager does. */
final class SamsungFingerprintNavigation implements FingerprintNavigation.Backend {
    private static final String TAG = "SamsungFpNavigation";

    private static final int SEH_GESTURE_MODE_ON = 15;
    private static final int SEH_GESTURE_MODE_OFF = 16;
    private static final int SEH_GESTURE_SCREEN_STATE = 17;
    private static final int VENDOR_SWIPE_UP = 20001;
    private static final int VENDOR_SWIPE_DOWN = 20002;

    @NonNull private final ISehFingerprint mSeh;
    private final int mSensorId;

    @Nullable
    static SamsungFingerprintNavigation create(@NonNull IBinder extension, int sensorId)
            throws RemoteException {
        // Other vendors hang their own interface off the same extension slot.
        if (!ISehFingerprint.DESCRIPTOR.equals(extension.getInterfaceDescriptor())) {
            return null;
        }
        return new SamsungFingerprintNavigation(ISehFingerprint.Stub.asInterface(extension),
                sensorId);
    }

    private SamsungFingerprintNavigation(@NonNull ISehFingerprint seh, int sensorId) {
        mSeh = seh;
        mSensorId = sensorId;
    }

    @Override
    public void setEnabled(boolean enabled) throws RemoteException {
        request(enabled ? SEH_GESTURE_MODE_ON : SEH_GESTURE_MODE_OFF, 0);
    }

    @Override
    public void setScreenOn(boolean screenOn) throws RemoteException {
        request(SEH_GESTURE_SCREEN_STATE, screenOn ? 1 : 0);
    }

    @Override
    public int keyCodeFor(int vendorCode) {
        switch (vendorCode) {
            case VENDOR_SWIPE_DOWN:
                return KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN;
            case VENDOR_SWIPE_UP:
                return KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP;
            default:
                return KeyEvent.KEYCODE_UNKNOWN;
        }
    }

    private void request(int command, int param) throws RemoteException {
        final SehResult result = mSeh.sehRequest(mSensorId, command, param, new byte[0]);
        Slog.d(TAG, "sehRequest(" + command + ", " + param + "): " + result.retValue);
    }
}
