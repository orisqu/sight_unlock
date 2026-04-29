#!/usr/bin/env bash
# Builds dist/sight-unlock.apk from sources in app/.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
APP="$ROOT/app"
OUT="$ROOT/build"
DIST="$ROOT/dist"
ANDROID_JAR="${ANDROID_JAR:-/opt/android-platforms/android-34/android.jar}"
KEYSTORE="$ROOT/debug.keystore"

if [[ ! -f "$ANDROID_JAR" ]]; then
    echo "android.jar not found at $ANDROID_JAR" >&2
    echo "Set ANDROID_JAR or place the file there." >&2
    exit 1
fi

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/gen" "$DIST"

echo "[1/6] aapt package (resources + R.java)"
aapt package -f -m \
    -M "$APP/AndroidManifest.xml" \
    -S "$APP/res" \
    -I "$ANDROID_JAR" \
    -J "$OUT/gen" \
    -F "$OUT/app.unsigned.apk"

echo "[2/6] javac"
SOURCES=$(find "$APP/src" "$OUT/gen" -name '*.java' | tr '\n' ' ')
# JDK 21 javac with --release 8 ignores -bootclasspath; android.jar via -classpath
# is enough for our APIs.
javac -encoding UTF-8 -source 1.8 -target 1.8 -nowarn \
    -cp "$ANDROID_JAR" \
    -d "$OUT/classes" \
    -Xlint:-options \
    $SOURCES

echo "[3/6] dx (classes.dex)"
( cd "$OUT" && dalvik-exchange --dex --no-warning --output="classes.dex" "classes/" )

echo "[4/6] aapt add classes.dex into APK"
( cd "$OUT" && aapt add "app.unsigned.apk" "classes.dex" >/dev/null )

echo "[5/6] zipalign"
zipalign -f -p 4 "$OUT/app.unsigned.apk" "$OUT/app.aligned.apk"

if [[ ! -f "$KEYSTORE" ]]; then
    echo "[5b/6] generating debug keystore"
    keytool -genkeypair -keystore "$KEYSTORE" \
        -alias androiddebugkey -storepass android -keypass android \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Sight Unlock Debug, OU=Dev, O=Sight Unlock, L=NA, ST=NA, C=US"
fi

echo "[6/6] apksigner"
apksigner sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$DIST/sight-unlock.apk" \
    "$OUT/app.aligned.apk"

apksigner verify --print-certs "$DIST/sight-unlock.apk" >/dev/null
echo
echo "Built: $DIST/sight-unlock.apk"
ls -la "$DIST/sight-unlock.apk"
