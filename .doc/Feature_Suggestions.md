# Car Launcher Geliştirme Planı (Android Auto UI Tabanlı)

> **Branch:** android-auto-ui
> **Son Commit:** 8e69bbfcca
> **Referans Resimler:** .doc/0001.webp, 002.webp, 003.webp, 005.webp, 006.webp

---

## 🎯 Hedef: Android Auto UI

### Layout Yapısı (Resimlerden Analiz)

```
┌──────────┬───────────────────────────┬──────────────┐
│          │                           │              │
│   DOCK   │       HARİTA              │  SAĞ PANEL   │
│  (sol)   │                           │  (müzik/     │
│  dikey   │   • Full screen alan      │   bildirim/  │
│  yuvarlak│   • Yuvarlatılmış köşeler │   drawer)    │
│  köşeler │   • Siyah arkaplan        │              │
│          │                           │  • Yuvarlak  │
│ 🌐 🏠 📋 │                           │  • Cam efekti│
│          │                           │              │
└──────────┴───────────────────────────┴──────────────┘
```

### Görsel Detaylar (Referans Resimlerden)

| Özellik | Detay |
|---------|-------|
| **Arkaplan** | Siyah (#000000) |
| **Köşe yarıçapı** | ~20dp (harita ve panel) |
| **Dock** | Sol dikey, ~56dp genişlik, koyu cam efekti |
| **Dock ikonları** | Beyaz ikonlar, 4 adet (Maps, Home, Apps, Layout) |
| **Harita** | Büyük, ortalanmış, yuvarlatılmış cam kart |
| **Sağ Panel** | ~280dp genişlik, yuvarlatılmış, içerik paneli |
| **Müzik player** | Sağ panelde, üst kısımda |
| **Bildirim** | Müzik üstünde, küçük banner |
| **App drawer** | Sağ panelde liste görünümü |

### Davranış Kuralları (Resimlerden)

| Kural | Açıklama |
|-------|----------|
| **1. Harita her zaman ekranda** | Full ekran, hiç kaybolmaz |
| **2. Sağ panel içerik paneli** | Widget listesi DEĞİL - müzik/bildirim/drawer gösterir |
| **3. Müzik + Bildirim üst üste** | Müzik player her zaman, çağrı gelince üstünde bildirim |
| **4. App Drawer** | Tıklanınca harita daralır, sağ panel app listesi |
| **5. Müziğe tıkla** | Müzik player harita alanında büyük açılır |
| **6. Dock** | Sol dikey, yuvarlak köşe, layout mod butonu |
| **7. Modern cam efekti** | Yarı saydam cam görünümü (glassmorphism) |

---

## 📋 Faz 1: Layout + Drawable

- [X] `android-auto-ui` branch'i oluşturuldu
- [X] Referans resimler eklendi
- [ ] `activity_car_launcher.xml` - Yeni layout (ConstraintLayout)
- [ ] `bg_card_rounded_dark.xml` - Harita/sağ panel için yuvarlak köşe drawable
- [ ] `bg_dock_rounded.xml` - Dock için yuvarlak köşe drawable
- [ ] `CarLayoutManager.java` - Güncelleme (dock artık sabit sol)
- [ ] MapActivity.java'da dock pozisyon ayarı güncelle

## 📋 Faz 2: İçerik Paneli Sistemi

- [ ] `RightPanelFragment.java` - YENİ - Sağ içerik paneli
- [ ] `PanelContentManager.java` - YENİ - Panel içerik yöneticisi
- [ ] Müzik player panel içeriği
- [ ] Bildirim panel içeriği
- [ ] Harita yeniden boyutlandırma

## 📋 Faz 3: App Drawer + Geçişler

- [ ] App drawer -> harita daralır, sağ panel drawer
- [ ] Müzik -> harita alanında player açılır
- [ ] Geçiş animasyonları
- [ ] Layout mod butonu dock'a entegre

## 📋 Faz 4: İyileştirmeler

- [ ] Glassmorphism efekti (cam görünümü)
- [ ] Dock item görselleri
- [ ] Touch feedback iyileştirmeleri
- [ ] Adaptive layout (portrait/landscape)

---

## 📁 Değişecek/Yeni Dosyalar

| Dosya | Durum |
|-------|-------|
| `res/layout/activity_car_launcher.xml` | Değişecek |
| `res/drawable/bg_card_rounded_dark.xml` | **YENİ** |
| `res/drawable/bg_dock_rounded.xml` | **YENİ** |
| `res/values/colors.xml` | Güncellenecek |
| `ui/CarLayoutManager.java` | Değişecek |
| `ui/RightPanelFragment.java` | **YENİ** |
| `ui/PanelContentManager.java` | **YENİ** |
| `ui/AppDockFragment.java` | Dock yuvarlak köşe |
| `activities/MapActivity.java` | Dock pozisyonu güncelle |
| `.doc/Feature_Suggestions.md` | Plan dokümanı |

---

## 🚀 İlerleme

- [X] Branch oluşturuldu
- [X] Referans resimler eklendi
- [ ] **1. Faz başlatıldı**
- [ ] 2. Faz
- [ ] 3. Faz
- [ ] 4. Faz