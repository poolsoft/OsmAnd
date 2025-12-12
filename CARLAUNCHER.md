# OsmAnd CarLauncher

## 🚗 CarLauncher Variant

OsmAnd'ın araba launcher'ı olarak çalışan özelleştirilmiş versiyonu.

## 🏗️ Build

### ☁️ GitHub Actions (Önerilen - Uzaktan Build)

1. GitHub repository → **Actions** sekmesi
2. **"Build carlauncher apk"** workflow'unu seç
3. **"Run workflow"** → Build başlar
4. Tamamlandığında **Artifacts** bölümünden APK'yı indir

**Avantajları:**
- ✅ Dosyaları silmeden güvenli build
- ✅ GitHub sunucularında build olur
- ✅ %100 CarLauncher override garantisi

### 💻 Lokal Build

```bash
./gradlew :OsmAnd:assembleCarlauncherOpengldebugFatDebug
```

**NOT:** Lokal build'de:
- ⚠️ Gradle `java.excludes` kullanır (genelde yeterli)
- ⚠️ Bazen manifest merge sorunları olabilir
- ✅ **GitHub Actions kullanımı önerilir**

### 📦 Kurulum

```bash
adb install -r OsmAnd/build/outputs/apk/carlauncher/opengldebugFat/debug/*.apk
```

## 🎯 Özelleştirilmiş Dosyalar

```
OsmAnd/src-carlauncher/
├── java/net/osmand/plus/
│   ├── activities/
│   │   └── MapActivity.java          ✅ CarLauncher MapActivity
│   ├── plugins/
│   │   └── PluginsHelper.java        ✅ CarLauncher PluginsHelper
│   └── carlauncher/
│       ├── widgets/                  # Widget sistemi
│       ├── dock/                     # App dock
│       └── ui/                       # UI bileşenleri
└── res/
    └── layout/
        └── activity_car_launcher.xml  # CarLauncher layout
```

## 🔧 Gradle Yapılandırması

```gradle
sourceSets {
    carlauncher {
        java.srcDirs = ["src-carlauncher", "src-google", "src-osmand"]
        res.srcDirs = ["src-carlauncher/res"]
        manifest.srcFile "AndroidManifest-carlauncher.xml"
        
        // Ana src/ altındaki dosyalar exclude edilir
        java.excludes = [
            "**/activities/MapActivity.java",
            "**/PluginsHelper.java"
        ]
    }
}
```

## ❓ Sorun Giderme

### "Orijinal MapActivity açılıyor" sorunu

**Çözüm:** GitHub Actions kullanın (önerilen) veya lokal build için:

```bash
# Clean build deneyin
./gradlew clean
./gradlew :OsmAnd:assembleCarlauncherOpengldebugFatDebug
```

Hala sorun varsa → **GitHub Actions kullanın** (garantili çözüm)

## 📝 Not

- Lokal build'de kaynak dosyaları **asla silinmez**
- GitHub Actions'ta build sırasında **geçici** olarak silinir
- Tüm değişiklikler Git'te güvenle saklanır

## 🔗 İlgili Dosyalar

- `.github/workflows/build-carlauncher-apk.yml` - CI/CD workflow
- `OsmAnd/build.gradle` - Gradle yapılandırması
- `OsmAnd/AndroidManifest-carlauncher.xml` - CarLauncher manifest
