// BacklightOverridePreference.aidl
package de.yisrime.dimming;

// Declare any non-default types here with import statements

parcelable BacklightOverridePreference {
    boolean enabled;
    float minimumOverrideBacklightLevel;
    boolean duplicateApplicationWorkaround;
}