// IBacklightOverrideStateListener.aidl
package de.yisrime.dimming;

parcelable BacklightRequest;

interface IBacklightOverrideStateListener {
    oneway void onBacklightUpdated(in BacklightRequest request, in BacklightRequest override, float gain);
}
