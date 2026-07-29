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
│   └── carlauncher/
│       ├── music/                    # Dahili oynatıcı, MediaSession adaptörleri ve çalma sırası
│       ├── media/                    # Android Auto MediaBrowserService
│       ├── widgets/                  # Widget sistemi
│       ├── dock/                     # App dock ve uygulama çekmecesi
│       ├── voice/                    # Çevrimdışı sesli komut servisi
│       ├── headunit/                 # Üretici/teyp adaptörleri
│       └── ui/                       # Launcher ekran bileşenleri
└── res/
    └── layout/
        └── activity_car_launcher.xml  # CarLauncher layout
```

## 🔧 Gradle Yapılandırması

```gradle
sourceSets {
    carlauncher {
        java.srcDirs = ["src-carlauncher/java", "src-osmand"]
        res.srcDirs = ["src-carlauncher/res"]
        assets.srcDirs = ["src-carlauncher/assets"]
        manifest.srcFile "AndroidManifest-carlauncher.xml"
        
        // Ana src/ altındaki dosyalar exclude edilir
        java.excludes = [
            "**/activities/MapActivity.java"
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

## 🎵 Müzik mimarisi

Müzik tarafındaki üç kavram birbirinden ayrıdır:

- **Kaynak liste:** Kullanıcının o anda gördüğü filtrelenmiş/sıralanmış parçalardır.
- **Çalma sırası:** Dahili oynatıcının sonraki/önceki işlemlerinde kullandığı anlık kopyadır.
- **Kayıtlı liste:** `PlaylistManager` tarafından kalıcı tutulan kullanıcı listesidir.

Bir parçaya dokunulduğunda `PlaybackQueueBuilder`, görünür kaynak listenin anlık
kopyasını alır ve parçayı `mediaId` ile bulur. Kimlik bulunamazsa mevcut sıra
değiştirilmez. MediaStore parçalarında content URI, doğrudan taranan USB
dosyalarında volume + bağıl yol kimliği kullanılır. Eski mutlak-yol playlist ve
favori kayıtları geriye uyumlu olarak çözümlenir.

`MusicRepository` taramaları tek bir I/O kuyruğunda yürütür. Aynı anda birden
fazla tarama istenirse yalnızca en yeni sonuç ana thread'e yayınlanır.

## 📄 Manifest kaynağı

Car Launcher flavor'ının etkin manifesti
`OsmAnd/AndroidManifest-carlauncher.xml` dosyasıdır. Launcher activity, sesli
komut, medya bildirimi ve MediaBrowser servis değişiklikleri bu dosyaya
eklenmelidir. `OsmAnd/src-carlauncher/AndroidManifest.xml` build giriş noktası
değildir.

## ✅ Değişiklik doğrulama

Proje politikası gereği AI ajanları Gradle görevi çalıştırmaz. Kaynak
değişikliklerinde en azından:

- `git diff --check`
- Java çağrı/imza taraması
- manifest servis ve izin kontrolü
- XML/Java UTF-8 BOM kontrolü

yapılır. APK derlemesi geliştirici tarafından veya GitHub Actions üzerinden
çalıştırılır.

## 🔗 İlgili Dosyalar

- `.github/workflows/build-carlauncher-apk.yml` - CI/CD workflow
- `OsmAnd/build.gradle` - Gradle yapılandırması
- `OsmAnd/AndroidManifest-carlauncher.xml` - CarLauncher manifest
