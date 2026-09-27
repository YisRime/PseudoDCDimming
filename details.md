## How it works (detailed)

Current OLED screens have a separate brightness control input to control brightness at the highest input signal. If we want to display white at a brightness of 20 cd/m<sup>2</sup>, we can do either of the following:

1. Adjust the brightness control so that the brightness at the highest input signal is 20 cd/m<sup>2</sup>, then use the highest signal ("white") as input;
2. With a higher brightness control input, say 160 cd/m<sup>2</sup>, use a lower signal ("gray") as input to produce 12.5% of maximum brightness (20 cd/m<sup>2</sup> m<sup>2</sup>) output.

Some OLED screens use a higher PWM frequency and duty cycle in the latter case, which some users feel helps reduce discomfort when viewing the screen for a long time or in dark environments.

The relationship between signal level and output brightness is not linear, so the second method is not simply multiplying the signal by a factor. However, given the following premises, we can achieve precise control:

1. The Android system assumes that the screen has an sRGB response curve;
2. Based on the sRGB response curve, the system framework and HAL provide RGB matrix transformation in linear space;
3. Starting from Android 12, the system framework and HAL interface support absolute brightness control in cd/m<sup>2</sup> (or nits).

The framework hands the display adapter a matched pair: a backlight level and the absolute luminance that level is expected to produce. This module raises both together. Writing a higher level while leaving the original luminance in place would let the absolute-brightness path look the requested nits up again and discard the raised level, so the pair has to stay self-consistent.

On some devices the framework's own luminance table is empty and the lookup only ever returns the "not applicable" sentinel. The module therefore learns the level-to-luminance curve from the requests it intercepts: every pair the framework itself produces is a sample, kept in a monotone table and interpolated with the power law through its neighbours, using a bounded exponent where the requested level is above everything observed so far. Until at least two samples exist the module stays out of the way rather than guessing, and the table is rebuilt after each framework restart.

