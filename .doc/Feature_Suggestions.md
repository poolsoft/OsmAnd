# Car Launcher Geliştirme Planı (Android Auto UI Tabanlı)

> **Son Güncelleme:** 17 May 2026
> **Branch:** right-panel-plugin
> **Son Commit:** 8225707dfc

---

## 🎯 Yeni Tasarım: Android Auto UI

### Layout Yapısı

```
┌──────────────────────┬─────────────────────┐
│                      │  🎵 Müzik /         │
│                      │  📞 Bildirim        │
│      HARİTA          │  (İçerik Paneli)    │
│      (Büyük Alan)    │                     │
│                      │  - Müzik player     │
│                      │  - Gelen çağrı      │
│                      │  - App drawer list  │
├──────────────────────┴─────────────────────┤
│  Dock (Sol, yuvarlatılmış köşeler)         │
│  🌐 Maps  🏠 Home  📋 Apps  🔄 Layout    │
└────────────────────────────────────────────┘
```

### Temel Davranış Kuralları (Android Auto Mantığı)

| Kural | Açıklama |
|-------|----------|
| **1. Harita her zaman ekranda** | Hiçbir modda kaybolmaz, sadece boyutu değişir |
| **2. Sağ panel içerik panelidir** | Widget listesi değil, aktif içerik gösterir (müzik/bildirim/drawer) |
| **3. Müzik + Bildirim üst üste** | Müzik player'ı gösterilir, çağrı gelirse müziğin üstünde bildirim |
| **4. App Drawer** | Tıklanınca harita küçülür, sağ panel app drawer listesi olur |
| **5. Müziğe tıkla** | Müzik player harita alanında büyük açılır, sağ panel tekrar harita |
| **6. Dock** | Sol tarafta dikey, yuvarlatılmış köşeler. Layout mod butonu dock'da |
| **7. Yuvarlak köşeler** | Harita ve sağ panel kartları yuvarlatılmış |

---

## 📋 Yapılacaklar Listesi

### 1. Faz: Temel Layout Değişikliği
- [ ] **activity_car_launcher.xml** - Harita + Sağ Panel + Dock yapısı (ConstraintLayout)
- [ ] **bg_card_rounded_black.xml** - Yuvarlatılmış köşe drawable'ı (harita için)
- [ ] **bg_panel_rounded.xml** - Sağ panel için yuvarlatılmış arkaplan
- [ ] **CarLayoutManager.java** - Yeni layout sistemine göre yeniden yazılacak
- [ ] **Dock drawable** - Yuvarlatılmış dock arkaplanı
- [ ] **Layout mod butonu** - Dock'a eklenecek

### 2. Faz: İçerik Paneli Sistemi
- [ ] **RightPanelFragment.java** - Sağ panel fragment (müzik/bildirim/drawer gösterir)
- [ ] **PanelContentManager.java** - Panel içeriğini yöneten sınıf
- [ ] **Müzik + Bildirim üst üste** gösterim mantığı
- [ ] **Harita yeniden boyutlandırma** - Panel içeriğine göre harita boyutu

### 3. Faz: App Drawer ve Geçişler
- [ ] **App Drawer** - Harita küçülür, sağ panel drawer listesi
- [ ] **Müzik player** - Harita alanında açılır, panel tekrar harita
- [ ] **Geçiş animasyonları** - Kaydırmalı/fade geçişler

### 4. Faz: Dock İyileştirmeleri
- [ ] **Dock yuvarlatılmış köşeler**
- [ ] **Layout mod butonu** (Normal/No Widgets/Full Screen) dock'a eklenecek
- [ ] **Dock item boyutlandırma** - Dock size ayarına göre

---

## 📁 Değişecek Dosyalar

| Dosya | Değişiklik |
|-------|-----------|
| `res/layout/activity_car_launcher.xml` | Yeni Android Auto layout yapısı |
| `res/drawable/bg_card_rounded_black.xml` | Harita yuvarlak köşe |
| `res/drawable/bg_panel_rounded.xml` | Sağ panel yuvarlak köşe |
| `res/drawable/bg_dock_modern.xml` | Dock yuvarlak köşe |
| `ui/CarLauncherInterface.java` | openAppDrawer(), openMusicPlayer() metotları |
| `ui/CarLayoutManager.java` | Yeni layout sistemi |
| `ui/RightPanelFragment.java` | **YENİ** - Sağ içerik paneli |
| `ui/PanelContentManager.java` | **YENİ** - Panel içerik yöneticisi |
| `ui/AppDockFragment.java` | Layout mod butonu eklenecek |
| `widgets/WidgetManager.java` | Widget listesi panel içeriğine dönüşecek |

---

## 🚀 İlerleme

- [x] Mevcut kod yedeği alındı (push: 8225707d)
- [ ] 1. Faz başlatılacak
- [ ] 2. Faz
- [ ] 3. Faz
- [ ] 4. Faz