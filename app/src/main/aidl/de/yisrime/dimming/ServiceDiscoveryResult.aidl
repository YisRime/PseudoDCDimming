// ServiceDiscoveryResult.aidl
package de.yisrime.dimming;

import de.yisrime.dimming.IBacklightOverrideService;
// Declare any non-default types here with import statements

parcelable ServiceDiscoveryResult {
    int version;
    IBinder service;
}
