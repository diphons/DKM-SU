[English](README_EN.md) | [Indonesia](/docs/README.md) | **简体中文** | [Türkçe](README_TR.md) | [Rusia](/docs/README_RU.md)

# DKM SU

<img src="/assets/dkm.png" style="width: 96px;" alt="logo">

安卓基于内核的 Root 方案

[![Latest release](https://img.shields.io/github/v/release/diphons/DKM-SU?label=Release&logo=github)](https://github.com/diphons/DKM-SU/releases/latest)
[![Nightly Release](https://img.shields.io/badge/Nightly%20release-gray?logo=hackthebox&logoColor=fff)](https://nightly.link/diphons/DKM-SU/workflows/build-manager/next/manager)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![GitHub License](https://img.shields.io/github/license/diphons/DKM-SU?logo=gnu)](/LICENSE)

## 特性

1. 基于内核的 SU 和权限管理
2. 基于动态挂载系统 [Magic Mount ](https://topjohnwu.github.io/Magisk/details.html#magic-mount) / [OverlayFS](https://en.wikipedia.org/wiki/OverlayFS) 的模块系统。
3. [App Profile](https://kernelsu.org/guide/app-profile.html)：把 Root 权限关进笼子里

## 兼容状态

DKM SU 支持从 4.9 到 6.6 的大多数安卓内核
 - GKI 2.0（5.10+）内核可运行预置镜像和 LKM/KMI
 - GKI 1.0（4.19 - 5.4）内核需要使用 KernelSU 内核驱动重新编译
 - EOL (<4.19) 内核也需要使用 KernelSU 内核驱动重新编译

目前只支持 `arm64-v8a` 架构

## 安全性

有关报告 DKM SU 漏洞的信息，请参阅 [SECURITY.md](/SECURITY.md).

## 许可证

- 目录 `kernel` 下所有文件为 [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
- `kernel` 目录以外的其他部分均为 [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html)

## 鸣谢

- [kernel-assisted-superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/): KernelSU 的灵感.
- [Magisk](https://github.com/topjohnwu/Magisk): 强大的 Root 工具.
- [genuine](https://github.com/brevent/genuine/): apk v2 签名验证。
- [Diamorphine](https://github.com/m0nad/Diamorphine): 一些 Rootkit 技巧。
- [KernelSU Next](https://github.com/rifsxd/KernelSU-Next): 感谢 rifsxd 的基础.
- [KernelSU](https://github.com/tiann/KernelSU): 感谢 tiann，否则 DKM SU 根本不会存在。