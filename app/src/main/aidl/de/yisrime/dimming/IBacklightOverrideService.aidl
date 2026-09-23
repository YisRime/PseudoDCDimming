// IBacklightOverrideService.aidl
package de.yisrime.dimming;

import de.yisrime.dimming.IBacklightOverrideStateListener;
import de.yisrime.dimming.BacklightOverridePreference;

interface IBacklightOverrideService {
    const int VERSION = 3;
    BacklightOverridePreference getPreference();
    void putPreference(in BacklightOverridePreference pref);

    void registerBacklightOverrideStateListener(IBacklightOverrideStateListener listener);
    void removeBacklightOverrideStateListener(IBacklightOverrideStateListener listener);
}
