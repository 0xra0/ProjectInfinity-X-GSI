/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.metrics;

/**
 * Stub TelephonyMetrics class.
 *
 * The full implementation was removed in android-16.0.0_r4. This stub
 * exists solely to prevent NoSuchMethodError crashes in MediaTek IMS
 * vendor blobs (com.mediatek.ims) that invoke these methods.
 */
public class TelephonyMetrics {

    private static final TelephonyMetrics sInstance = new TelephonyMetrics();

    public TelephonyMetrics() {
    }

    public static TelephonyMetrics getInstance() {
        return sInstance;
    }

    public void writeRilSendSms(int phoneId, int rilSerial, int tech, int format) {
        // No-op stub for MediaTek IMS compatibility
    }

    public synchronized void writeRilSendSms(int phoneId, int rilSerial, int tech, int format,
            long messageId) {
        // No-op stub for MediaTek IMS compatibility
    }

    /**
     * No-op stub. Called by ImsRILAdapter.processResponseDoneInternal when
     * processing solicited RIL responses.
     */
    public void writeOnRilSolicitedResponse(int phoneId, int rilSerial, int rilError,
            int rilRequest, Object ret) {
        // No-op stub for MediaTek IMS compatibility
    }

    /**
     * No-op stub. Called by ImsRILAdapter when a RIL request times out.
     */
    public void writeOnRilTimeoutResponse(int phoneId, int rilSerial, int rilRequest) {
        // No-op stub for MediaTek IMS compatibility
    }

    /**
     * No-op stub. Called by ImsRILAdapter when answering a call.
     */
    public void writeRilAnswer(int phoneId, int rilSerial, int rilError, int rilRequest) {
        // No-op stub for MediaTek IMS compatibility
    }
}
