# MS2Hash (Java)
[![Build](https://img.shields.io/github/actions/workflow/status/MainPlay-TG/MS2Hash.java/build.yml?label=Build)](https://github.com/MainPlay-TG/MS2Hash.java/actions/workflows/build.yml)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://github.com/bell-sw/Liberica/releases)
[![Version](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fgithub.com%2FMainPlay-TG%2FMS2Hash.java%2Freleases%2Flatest%2Fdownload%2Finfo.json&query=%24.version&label=Version) ![Date](https://img.shields.io/github/release-date/MainPlay-TG/MS2Hash.java?label=Date)](https://github.com/MainPlay-TG/MS2Hash.java/releases/latest)
## Описание
MS2Hash - формат хранения контрольной суммы и размера файла. При проверке сначала проверяется совпадение размера файла, потом содержимое
## Установка
1. Установите Java 21+, если она ещё не установлена
2. Скачайте [последний релиз](https://github.com/MainPlay-TG/MS2Hash.java/releases/latest) в любое место
3. Добавьте алиас для вашей консоли (например `ms2hash`)
```bash
# Пример для Bash

alias ms2hash="java -jar .../MS2Hash-x.x.x.jar"
# Укажите реальный путь к файлу JAR
```
4. Проверьте работает ли алиас
```bash
ms2hash --help
```
## Использование
### Генерация хеша
```
ms2hash gen [опции] <файл(ы)>

Опции:
    --algs       только показать список поддерживаемых алгоритмов
-b, --bar        отображать прогресс хеширования файла
-f, --force      перезаписывать старый хеш
-t, --type=ТИП   алгоритм хеширования (по умолчанию sha512)
```
При передаче файла не забудьте передать файл `{название файла}.MS2_hash`, т. к. в нём хранится контрольная сумма
### Проверка хеша
```
ms2hash check [опции] <файл(ы)>

Опции:
-b, --bar   отображать прогресс проверки файла
```
Чтобы проверить файл, в папке с ним должен быть файл `{название файла}.MS2_hash`
## Альтернативы
### MainShortcuts2 (Python)
Библиотека [MainShortcuts2](https://pypi.org/project/mainshortcuts2) имеет встроенные команды `ms2-hash_gen` и `ms2-hash_check`, имеющие аналогичный функционал, но работающие с меньшей скоростью
## Проверенные устройства
- ✅ Windows 10 amd64, [Bellsoft JDK 23 Full](https://github.com/bell-sw/Liberica/releases/download/23.0.1%2B13/bellsoft-jdk23.0.1+13-windows-amd64-full.zip)