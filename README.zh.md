# 伪 DC 调光

[English](README.md) | [下载最新版本](https://github.com/YisRime/PseudoDCDimming/releases/latest)

通过软件增益为部分 OLED 屏幕在低亮度下启用类 DC 调光方式。

这是一个 Xposed 模块，要求宿主框架实现 libxposed 规范（LSPosed 2.x 及以上），仅支持 Android 12 及以上版本。

## 原理

通过限制亮度控制的最小值，并通过矩阵变换功能缩小输出信号，使实际显示亮度匹配预期亮度，同时保持较高的 PWM 频率/占空比。

[详细原理](details.zh.md)

## 限制

* 严重依赖厂商对屏幕亮度控制以及响应曲线的校准。如果厂商在校准时使用了不同的响应曲线，或者亮度控制存在非线性行为，都可能影响开启模块后的显示质量；
* 可能与其他颜色变换功能冲突；
* 可能与 HDR 显示冲突。

## 配置

通过相机的高速快门模式放大频闪效应，或使用专业仪器测量 PWM 频率及占空比，选择一个可以接受的亮度值作为最小硬件亮度。

## 版本与作者

- 版本：1.0.0（versionCode 100），包名 `de.yisrime.dimming`
- 原项目与作者：[dantmnf/PseudoDCDimming](https://github.com/dantmnf/PseudoDCDimming)
- libxposed 适配与维护：[Yis_Rime](https://github.com/YisRime)

本分支将模块入口从 `assets/xposed_init` 迁移到 `META-INF/xposed/java_init.list`，hook 层改用 libxposed API，配置读写改由 libxposed RemotePreferences 承载，不再产生全局可读的 XML 偏好文件（对应 LSPosed 2.3.0 移除 `XSharedPreferences`）。因不再走 legacy Xposed API，LSPosed 1.x、LSPatch 等只支持旧规范的框架无法加载它；包名变更意味着不能覆盖安装上游的包，已保存的配置与作用域也不会带过来。

## 致谢

灵感来自 [ztc1997/FakeDCBacklight](https://github.com/ztc1997/FakeDCBacklight)。本项目额外实现了立即应用以及稳定启用前后亮度的功能。
