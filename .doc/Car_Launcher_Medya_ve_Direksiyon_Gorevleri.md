# Car Launcher Medya ve Direksiyon Görevleri

Bu belge, Car Launcher medya/direksiyon mimarisinin kalıcı durum kaydıdır.
Yeni çalışma başlatılırken konu yeniden analiz edilmek yerine bu listedeki durumlar
ve test kapıları esas alınmalıdır.

Son güncelleme: 29 Temmuz 2026

## Değişmez kurallar

- OsmAnd core dosyalarına dokunulmaz.
- Uygulama kodu ve kaynakları `OsmAnd/src-carlauncher` içinde tutulur.
- Car Launcher manifest değişiklikleri yalnız `OsmAnd/AndroidManifest-carlauncher.xml`
  üzerinde yapılır.
- Vendor broadcast adı tahmin edilerek üretim yönlendirmesi yapılmaz. Önce gerçek
  cihaz tanılama kaydı alınır.
- Bir adapter, kendi gönderdiği broadcast'i tekrar komut olarak işlememelidir.

## Tamamlanan işler

- [x] Activity `KeyEvent` olaylarında keyCode, scanCode, deviceId ve input source kaydı
- [x] MediaSession `ACTION_MEDIA_BUTTON` olaylarının kaydı
- [x] Bilinen teyp broadcast action ve extras bilgilerinin kaydı
- [x] Tanılama kaydını uygulama özel alanında tutma
- [x] Kaydı USB veya kullanıcı tarafından seçilen konuma dışa aktarma
- [x] Tanılama kaydını temizleme
- [x] XYAuto algılamasında `com.xyauto.common`, ACloud paketleri ve fingerprint kullanımı
- [x] Android 13+ dinamik receiver flag desteği
- [x] XYAuto ve HCN head-unit adapter seçimi
- [x] Harici uygulama gerçekten çalarken Car MediaSession sahipliğini bırakma
- [x] Dahili oynatıcı veya boş durumda Car MediaSession sahipliğini alma
- [x] Smart Focus geçici odağı ile ayarlardaki kalıcı varsayılanı ayırma
- [x] Boştayken varsayılan uygulamayı başlatıp MediaSession oluşunca komutu iletme
- [x] Müzik ekranından seçilen kaynağı varsayılandan bağımsız çalıştırma

İlgili commitler:

- `01607fb53e` — Add head unit hardware event diagnostics
- `391c6b7a41` — Fix Smart Focus media key routing

## Kritik işler

### K1 — Tek MediaSession ve notification sahibi

Durum: Tamamlandı; gerçek cihaz doğrulaması bekliyor.

Hedef:

- `CarMediaService` tek MediaSession sahibi olacak.
- Dahili müzik notification'ı aynı servisten üretilecek.
- Notification `MediaStyle` gerçek `mediaSession.getSessionToken()` değerini kullanacak.
- Notification Play/Pause/Next/Previous eylemleri ortak `MusicManager` yönlendirmesine gidecek.
- Ayrı `MusicPlaybackService` kaldırılacak.
- Session metadata ve playback state güncel tutulacak.

Uygulananlar:

- `MusicPlaybackService` kaldırıldı.
- Notification üretimi `CarMediaService` içine alındı.
- Notification gerçek Car MediaSession token'ını kullanıyor.
- Dahili notification eylemleri yalnız gösterilen dahili kaynağı kontrol ediyor.
- Hardware MediaSession callback'leri Smart Focus yönlendirmesine gidiyor.
- Session, metadata, playback state ve notification tek yaşam döngüsünde tutuluyor.

Kabul ölçütleri:

- Manifestte yalnız tek medya playback servisi bulunur.
- Kodda `setMediaSession(null)` kalmaz.
- Donanım, notification ve MediaSession callback'leri aynı yönlendirme metodunu kullanır.
- Harici uygulama çalarken Car MediaSession aktif kalmaz.

### K2 — Vendor direksiyon olaylarını ortak modele dönüştürme

Durum: Ortak yönlendirici ve tekrar filtresi tamamlandı; vendor eşleştirmesi gerçek
cihaz kaydı bekliyor.

Hedef:

- XYAuto, HCN, FYT/UIS7862 ve TS10 adapter'ları vendor olayını ortak medya tuşuna
  dönüştürecek.
- Ortak olay Play, Pause, Play/Pause, Next, Previous ve Stop komutlarını taşıyacak.
- Aynı fiziksel olayın KeyEvent, MediaSession ve vendor broadcast yollarından iki kez
  işlenmesini önleyen kısa süreli tekrar filtresi bulunacak.

Uygulanan altyapı:

- Activity, MediaSession ve head-unit adapter için ortak `HardwareMediaKeyRouter`
- Farklı iki kaynaktan 300 ms içinde gelen aynı medya tuşu için tekrar filtresi
- Adapter listener sözleşmesinde ortak medya tuşu callback'i
- Router kararlarının tanılama kaydına eklenmesi

Önemli engel:

XYAuto ve HCN adapter'larının dinlediği bazı action'lar aynı zamanda kontrol göndermek
için de kullanılıyor. Bunları doğrudan giriş olayı saymak uygulamanın kendi yayınını
yeniden yakalayıp sonsuz komut döngüsü oluşturabilir. Gerçek teyp logundaki action,
extras, sender davranışı ve zaman sırası görülmeden üretim eşleştirmesi yapılmayacak.

Kabul ölçütleri:

- Adapter'ın kendi gönderdiği broadcast tekrar işlenmez.
- Tek tuş basışında Smart Focus günlüğünde tek karar satırı oluşur.
- Hiçbir uygulama çalmıyorsa yapılandırılmış varsayılan uyanır.
- Harici uygulama çalıyorsa komut yalnız o uygulamaya gider.
- Dahili oynatıcı çalıyorsa komut yalnız dahili oynatıcıya gider.

## Cihaz test protokolü

Her cihaz/firmware için ayrı kayıt alınmalıdır.

1. Ayarlar > Donanım Olaylarını Kaydet açılır.
2. Hiçbir uygulama çalmazken Play/Pause, Next ve Previous tuşlarına birer kez basılır.
3. Dahili oynatıcı çalarken aynı tuşlara birer kez basılır.
4. Teybin kendi müzik/radyo uygulaması çalarken aynı tuşlara birer kez basılır.
5. Harici bir Android medya uygulaması çalarken aynı tuşlara birer kez basılır.
6. Her senaryo arasında en az iki saniye beklenir.
7. Kayıt USB'ye aktarılır ve `.doc` altında cihaz/firmware adıyla saklanır.

Kayıtta aranacak bilgiler:

- `MAP_ACTIVITY`
- `MEDIA_SESSION`
- `MEDIA_SESSION_KEY`
- `BROADCAST`
- `XYAUTO_ADAPTER` veya `HCN_ADAPTER`
- `SMART_FOCUS`
- action, extras, keyCode, scanCode, deviceId ve input source

## Sonraki işler

- [ ] FYT/UIS7862 adapter
- [ ] TS10 adapter
- [ ] Kısa, uzun ve çift basma atamaları
- [ ] Düşük RAM cihaz profili (`ActivityManager.isLowRamDevice()`)
- [ ] Widget ve görsel efekt bütçesi
- [ ] İlk frame / harita hazır / tamamen hazır performans geçmişi
- [ ] Çökme sonrası son sağlam layout ve ayarları geri yükleme
- [ ] Sürüş sırasında arayüz sadeleştirme

## Öncelik sırası

1. K1 — Tek MediaSession ve notification sahibi
2. XYAuto gerçek cihaz tanılama kaydı
3. K2 — Ortak vendor direksiyon olay modeli ve tekrar filtresi
4. HCN gerçek cihaz doğrulaması
5. Düşük RAM profili
6. FYT/UIS7862 ve TS10 adapter'ları
