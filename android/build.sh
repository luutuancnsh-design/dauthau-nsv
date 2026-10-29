#!/bin/bash
# Build APK không cần Gradle (app không dùng thư viện ngoài). Cần: ANDROID_HOME, JDK, keystore.
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-/opt/android}; BT=$SDK/build-tools/35.0.0; JAR=$SDK/platforms/android-35/android.jar
VC=${VC:-1}; VN=${VN:-1.0}
OUT=build; rm -rf $OUT; mkdir -p $OUT/gen $OUT/classes $OUT/dex
sed "s/<manifest /<manifest package=\"vn.dauthau.nsv\" /" app/src/main/AndroidManifest.xml > $OUT/AndroidManifest.xml
sed -i "s/VERSION = \"[^\"]*\"/VERSION = \"$VN\"/" app/src/main/java/vn/dauthau/nsv/BuildConfigLite.java
$BT/aapt2 compile --dir app/src/main/res -o $OUT/res.zip
$BT/aapt2 link -I $JAR --manifest $OUT/AndroidManifest.xml -o $OUT/base.apk --java $OUT/gen \
  --min-sdk-version 24 --target-sdk-version 34 --version-code $VC --version-name $VN $OUT/res.zip
javac -nowarn --release 11 -cp $JAR -encoding UTF-8 -d $OUT/classes \
  $(find app/src/main/java $OUT/gen -name '*.java')
$BT/d8 --release --min-api 24 --lib $JAR --output $OUT/dex $(find $OUT/classes -name '*.class')
cp $OUT/base.apk $OUT/unsigned.apk; (cd $OUT/dex && zip -q -j ../unsigned.apk classes.dex)
$BT/zipalign -f -p 4 $OUT/unsigned.apk $OUT/aligned.apk
$BT/apksigner sign --ks "$DT_KEYSTORE" --ks-key-alias dauthau --ks-pass env:DT_KS_PASS --key-pass env:DT_KS_PASS --out $OUT/dauthau.apk $OUT/aligned.apk
$BT/apksigner verify $OUT/dauthau.apk && ls -la $OUT/dauthau.apk
