# Sight Unlock

Treble-clef sight reading as your lock screen. Every time the screen turns
on, the app draws a note on the staff and you have to tap the right letter
(A–G) to dismiss it. A Leitner-box spaced repetition algorithm picks notes
you've been getting wrong more often than ones you've mastered.

The prebuilt APK is at `dist/sight-unlock.apk`.

## Install

You need `adb` on your computer and **USB debugging** turned on on the phone
(`Settings → System → Developer options → USB debugging`).

```bash
adb install -r dist/sight-unlock.apk
```

Then on the phone:

1. Open **Sight Unlock**.
2. Tap **Grant overlay permission** → toggle it on → back.
3. Tap **Disable battery optimisation** → choose *Allow*.
4. Tap **Start lock service**. A persistent notification appears.

That's it. Lock and wake the phone — the challenge appears.

> The lock service auto-starts on every boot, so you only do steps 1–4 once.

## How it works

- A foreground service (`LockService`) registers a runtime receiver for
  `ACTION_SCREEN_ON` and `ACTION_USER_PRESENT`.
- When the screen turns on, `LockActivity` is launched with
  `setShowWhenLocked(true)` so it sits on top of the keyguard.
- Back / home are non-functional; only a correct letter dismisses it.
- After a correct answer the activity finishes and (if you have no PIN)
  also dismisses the keyguard via `KeyguardManager.requestDismissKeyguard`.
- Progress lives in `SharedPreferences`. Each of the 15 notes (C4 → C6)
  has a Leitner box 1–5; correct answers promote, wrong answers reset to 1.
  Lower-box notes are weighted more heavily when picking the next prompt.

## Rebuild from source

The toolchain is plain javac + dx + aapt + apksigner — no Gradle, no Android
Studio. On Ubuntu/Debian:

```bash
sudo apt install aapt apksigner zipalign android-sdk-build-tools \
                 android-sdk-platform-23 dalvik-exchange default-jdk

# A newer android.jar is needed for the modern APIs we compile against.
# Any android-34 platform jar works; for example:
sudo mkdir -p /opt/android-platforms/android-34
sudo curl -L -o /opt/android-platforms/android-34/android.jar \
  https://github.com/Sable/android-platforms/raw/master/android-34/android.jar

./build.sh
# → dist/sight-unlock.apk
```

The first build creates a self-signed `debug.keystore` (gitignored).
Subsequent builds reuse it, so updates install with `adb install -r`.

## Layout

```
app/
  AndroidManifest.xml
  res/
    drawable/ic_launcher.xml
    values/{strings,styles}.xml
  src/com/sightunlock/
    MainActivity.java     ← settings + permissions + stats
    LockActivity.java     ← the challenge screen
    LockService.java      ← foreground service
    ScreenReceiver.java   ← launches LockActivity on screen-on
    BootReceiver.java     ← restarts service after reboot
    StaffView.java        ← Canvas-drawn 5-line staff + clef + note
    SrsState.java         ← Leitner-box state in SharedPreferences
build.sh                  ← one-shot build pipeline
dist/sight-unlock.apk     ← signed APK
```

## Caveats

- This does not literally replace Android's keyguard — that requires
  Device-Owner privileges. Sight Unlock runs as a fullscreen activity
  *over* the keyguard. If you have a PIN/biometric set, you still enter
  it after answering the challenge. Without a secure lock, the challenge
  is effectively your lock screen.
- Some manufacturers (Xiaomi, Oppo, Vivo) aggressively kill background
  services. If the challenge stops appearing after a day or two, find the
  vendor-specific *Autostart* / *Protected apps* setting and whitelist
  Sight Unlock.
- The treble clef glyph is rendered with the Unicode music symbol
  `𝄞` (U+1D11E). On the rare device without Noto Music, the app falls
  back to a stylised `G`.
