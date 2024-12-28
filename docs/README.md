[English](README_EN.md) | **Indonesia** | [简体中文](/docs/README_CN.md) | [Türkçe](/docs/README_TR.md)

# DKM SU

<img src="/assets/dkm.png" style="width: 96px;" alt="logo">

Solusi root berbasis Kernel untuk perangkat Android.

[![Latest release](https://img.shields.io/github/v/release/diphons/DKM-SU?label=Release&logo=github)](https://github.com/diphons/DKM-SU/releases/latest)
[![Nightly Release](https://img.shields.io/badge/Nightly%20release-gray?logo=hackthebox&logoColor=fff)](https://nightly.link/diphons/DKM-SU/workflows/build-manager/next/manager)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![GitHub License](https://img.shields.io/github/license/diphons/DKM-SU?logo=gnu)](/LICENSE)

## Fitur

1. Manajemen akses root dan `su` berbasis kernel.
2. Sistem modul berdasarkan [OverlayFS](https://en.wikipedia.org/wiki/OverlayFS).
3. [Profil Aplikasi](https://kernelsu.org/guide/app-profile.html): Mengunci kekuatan root dalam sebuah kandang.

## Compatibility State

DKM SU secara resmi mendukung sebagian besar kernel Android mulai dari 4.9 hingga 6.6.
- Kernel GKI 2.0 (5.10+) dapat menjalankan citra yang telah dibuat sebelumnya dan LKM/KMI.
- Kernel GKI 1.0 (4.19 - 5.4) perlu dibuat ulang dengan driver KernelSU.
- Kernel EOL (<4.19) juga perlu dibuat ulang dengan driver KernelSU.

Saat ini, hanya `arm64-v8a` yang didukung.

## Keamanan

Untuk informasi tentang pelaporan kerentanan keamanan di KernelSU, lihat [SECURITY.md](/SECURITY.md).

## Lisensi

- File di bawah direktori `kernel` adalah [hanya GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).
- Semua bagian lain kecuali direktori `kernel` adalah [GPL-3.0-atau-yang-lebih-baru](https://www.gnu.org/licenses/gpl-3.0.html).

## Kredit

- [kernel-assisted-superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/): ide KernelSU.
- [Magisk](https://github.com/topjohnwu/Magisk): alat root yang ampuh.
- [genuine](https://github.com/brevent/genuine/): validasi tanda tangan apk v2.
- [Diamorphine](https://github.com/m0nad/Diamorphine): beberapa keterampilan rootkit.
- [KernelSU Next](https://github.com/rifsxd/KernelSU-Next): terima kasih kepada rifsxd atas basisnya.
- [KernelSU](https://github.com/tiann/KernelSU): terima kasih kepada tiann atau kalau tidak DKM SU tidak akan ada.