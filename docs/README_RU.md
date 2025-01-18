[English](/docs/README_EN.md) | [Indonesia](/docs/README.md) | [简体中文](/docs/README_CN.md) | [Türkçe](/docs/README_TR.md) | **Rusia**

# DKM SU

<img src="/assets/dkm.png" style="width: 96px;" alt="logo">

Решение на основе ядра для Android-устройств.

[![Latest release](https://img.shields.io/github/v/release/diphons/DKM-SU?label=Release&logo=github)](https://github.com/diphons/DKM-SU/releases/latest)
[![Nightly Release](https://img.shields.io/badge/Nightly%20release-gray?logo=hackthebox&logoColor=fff)](https://nightly.link/diphons/DKM-SU/workflows/build-manager/next/manager)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-orange.svg?logo=gnu)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![GitHub License](https://img.shields.io/github/license/diphons/DKM-SU?logo=gnu)](/LICENSE)

## Особенность

1. Управление доступом root и `su` на основе ядра.
2. Корень файла и `su` находятся в открытом исходном коде. [Magic Mount ](https://topjohnwu.github.io/Magisk/details.html#magic-mount) / [OverlayFS](https://en.wikipedia.org/wiki/OverlayFS).
3. [Профиль приложения](https://kernelsu.org/guide/app-profile.html): Запирание корневой силы в клетке.

## Состояние совместимости

DKM SU официально поддерживает большинство ядер Android от 4.9 до 6.6.
- Ядро GKI 2.0 (5.10+) может запускать ранее созданные образы и LKM/KMI.
- Ядра GKI 1.0 (4.19 - 5.4) необходимо пересобрать с помощью драйвера KernelSU.
- Ядра EOL (<4.19) также необходимо пересобрать с помощью драйвера KernelSU.

В настоящее время поддерживается только `arm64-v8a`.

## Безопасность

Информацию о сообщении об уязвимостях безопасности в KernelSU см. [SECURITY.md](/SECURITY.md).

## Лицензия

- Файлы в каталоге `kernel`: [hanya GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).
- Все остальные части, за исключением каталога `kernel` [GPL-3.0-atau-yang-lebih-baru](https://www.gnu.org/licenses/gpl-3.0.html).

## Кредит

- [kernel-assisted-superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/): Идея KernelSU.
- [Magisk](https://github.com/topjohnwu/Magisk): мощный инструмент для получения прав root.
- [genuine](https://github.com/brevent/genuine/): проверка подписи apk v2.
- [Diamorphine](https://github.com/m0nad/Diamorphine): некоторые навыки работы с руткитами.
- [KernelSU Next](https://github.com/rifsxd/KernelSU-Next): спасибо rifsxd за базу.
- [KernelSU](https://github.com/tiann/KernelSU): спасибо tiann, иначе DKM SU не существовало бы.