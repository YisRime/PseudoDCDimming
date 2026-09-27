package de.yisrime.dimming.xposed;

import net.jcip.annotations.Immutable;

import de.yisrime.dimming.ServiceDiscovery;

@Immutable
public final class BacklightOverridePreferenceLocal {
    public final boolean enabled;
    public final float minimumOverrideBacklightLevel;
    public final float minimumGain;
    public final boolean duplicateApplicationWorkaround;
    public static final BacklightOverridePreferenceLocal DEFAULT = new BacklightOverridePreferenceLocal(false, 0.0f, ServiceDiscovery.DEFAULT_MINIMUM_GAIN, false);

    public BacklightOverridePreferenceLocal(boolean enabled, float minimumOverrideBacklightLevel, float minimumGain, boolean duplicateApplicationWorkaround) {
        this.enabled = enabled;
        this.minimumOverrideBacklightLevel = minimumOverrideBacklightLevel;
        this.minimumGain = minimumGain;
        this.duplicateApplicationWorkaround = duplicateApplicationWorkaround;
    }
}
