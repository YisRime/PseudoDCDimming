# Pseudo DC Dimming

[简体中文](README.zh.md) | [下载最新版本](https://github.com/YisRime/PseudoDCDimming/releases/latest)

Enable alternative dimming mode (likely DC-like) on low brightness for some OLED displays by using software brightness gain.

Requires Android 12+ and a framework implementing the libxposed specification (LSPosed 2.x or later).

[Download latest release](https://github.com/YisRime/PseudoDCDimming/releases/latest)

## How it works

By limiting the minimum brightness and scaling down the output signal through a degamma-gain-regamma transform, the actual display brightness matches the expected brightness while maintaining a higher PWM frequency and duty cycle.

[details](details.md)

## Limitations

* Rely heavily on the manufacturer's calibration of screen brightness controls and response curves. If the manufacturer uses different response curves during calibration, or the brightness control has non-linear behavior, it may affect the display quality after turning on the module;
* May conflict with other color transform functions;
* May conflict with HDR content.

## Configuration

Use the high-speed shutter mode of the camera to amplify the stroboscopic effect, or use professional instruments to measure the PWM frequency and duty cycle. Choose an acceptable brightness value as the minimum hardware brightness.

## Version and authors

- Version: 1.1.0 (versionCode 110), package name `de.yisrime.dimming`
- Original project and author: [dantmnf/PseudoDCDimming](https://github.com/dantmnf/PseudoDCDimming)
- libxposed adaptation and maintenance: [Yis_Rime](https://github.com/YisRime)

This fork moves the module entry from `assets/xposed_init` to `META-INF/xposed/java_init.list`, replaces the hook layer with the libxposed API, and stores configuration through libxposed remote preferences, so no world-readable XML preference file is created any more - which is what LSPosed 2.3.0 removes along with `XSharedPreferences`. It requires the framework to implement the libxposed specification: LSPosed 1.x, LSPatch and other legacy-only frameworks cannot load it. The package name change means it cannot be installed over an upstream build, and the previously saved configuration and selected scope do not carry over.

## Acknowledgements

Inspired from [ztc1997/FakeDCBacklight](https://github.com/ztc1997/FakeDCBacklight). This project additionally implements immediate application as well as stabilizing the brightness before and after enabling.
