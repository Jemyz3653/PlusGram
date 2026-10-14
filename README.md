# PlusGram

Неофициальный клиент Telegram для Android мятного цвета. Основа — официальный исходный код Telegram для Android 13.0.2, поверх него перенесены фишки exteraGram и AyuGram и добавлены свои.

*Unofficial Telegram client for Android, based on the official Telegram for Android 13.x sources, with features ported from exteraGram and AyuGram.*

## Что внутри

В настройках сверху три раздела, по порядку: **exteraGram → AyuGram → PlusGram**.

**exteraGram**
- скрывать клавиатуру при прокрутке чата
- не переходить к следующему каналу свайпом
- секунды во времени сообщений, скрытие времени у стикеров
- скрыть свой номер в настройках, ID в профилях (тап — копировать)
- числа без округления (12 345 вместо 12.3K)
- ускорение загрузки и отправки файлов

**AyuGram**
- режим призрака: без отметок о прочтении, статуса «в сети», «печатает» и просмотров историй — включается одним переключателем или по отдельности
- «читать чат после своего сообщения»
- сохранение удалённых сообщений (остаются в чате с меткой 🗑, плюс отдельный список)
- история изменений сообщений (в меню сообщения)
- скрытие рекламы в каналах

**PlusGram**
- двойное время: в сообщениях сначала твоё время, потом время собеседника — `19:55 / 15:55`. Пояс задаётся в личном чате (⋮ → «Часовой пояс собеседника») или берётся из бизнес-часов собеседника
- мятная тема по умолчанию во всех встроенных темах
- облегчённый режим: выключает тяжёлые анимации одним переключателем
- своя иконка, отдельный package (`com.plusgram.messenger`) — ставится рядом с официальным Telegram

## Сборка

APK собирает GitHub Actions (`.github/workflows/build.yml`) — вкладка **Actions** → последний запуск → артефакт `PlusGram-…`. По умолчанию собирается только `arm64-v8a`; другие ABI можно выбрать при ручном запуске.

Секреты репозитория (Settings → Secrets and variables → Actions):

| Секрет | Зачем |
|---|---|
| `PLUS_API_ID`, `PLUS_API_HASH` | свои ключи с [my.telegram.org](https://my.telegram.org) — без них вход не работает |
| `PLUS_KEYSTORE_BASE64`, `PLUS_KEYSTORE_PASSWORD`, `PLUS_KEY_ALIAS`, `PLUS_KEY_PASSWORD` | своя подпись APK. Без них используется публичный тестовый ключ из исходников Telegram |

Локально (Android Studio, NDK 27.2.12479018, SDK 36):

```bash
git clone --recursive https://github.com/Jemyz3653/PlusGram.git
```

В `local.properties` добавь `PLUS_API_ID=...` и `PLUS_API_HASH=...`, затем `./gradlew :TMessagesProj_App:assembleAfatRelease -PPLUS_ABIS=arm64-v8a`.

Свой ключ подписи создаётся так:

```bash
keytool -genkeypair -v -keystore plusgram.keystore -alias plusgram -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 plusgram.keystore   # это значение -> секрет PLUS_KEYSTORE_BASE64
```

## Ограничения

- Пуш-уведомления через Firebase принадлежат официальному приложению, поэтому PlusGram держит фоновое соединение (включено по умолчанию в «Уведомления → Фоновое соединение»).
- Карты Google в геопозиции могут не грузиться — ключ Maps привязан к официальному приложению.
- Режим призрака и сохранение удалённых сообщений противоречат правилам Telegram API для клиентов. Используй свои `api_id`/`api_hash`, на свой риск.

## Обновление до новой версии Telegram

```bash
git remote add upstream https://github.com/DrKLO/Telegram.git
git fetch upstream --depth=50
git merge upstream/master
```

Все правки PlusGram в коде Telegram помечены комментарием `// PlusGram`, свой код лежит в `TMessagesProj/src/main/java/com/plusgram/`.

## Благодарности и лицензия

- [Telegram for Android](https://github.com/DrKLO/Telegram) — GPL-2.0
- [exteraGram](https://github.com/exteraSquad/exteraGram) — идеи и настройки раздела exteraGram
- [AyuGram](https://github.com/AyuGram/AyuGram4A) от Radolyn Labs — режим призрака и сохранение сообщений

PlusGram распространяется под GPL-2.0 (см. `LICENSE`).
