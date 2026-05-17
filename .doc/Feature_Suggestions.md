# Car Launcher Geliştirme Planı (Android Auto UI)

> **Branch:** android-auto-ui
> **Son Commit:** 8002e4634e
> **Referans Resimler:** .doc/0001.webp, 002.webp, 003.webp, 005.webp, 006.webp

---

## ✅ Mevcut ve Korunacak Özellikler

| Özellik | Durum |
|---------|-------|
| Dock pozisyonu (alt/sol/sağ) | **Dinamik, değişmeyecek** |
| Dock boyutu | **Ayarlanabilir, değişmeyecek** |
| Dock kısayolları (AppDockFragment) | **Çalışıyor, değişmeyecek** |
| Widget sistemi (WidgetManager) | **Kalacak, içerik panelinde kullanılacak** |
| Layout mod butonu (AppDockFragment) | **Zaten var, kalacak** |
| CarLauncherSettings | **Tüm ayarlar kalacak** |

---

## 🎯 Hedef: Android Auto UI

### Layout Değişikliği

```
┌──────────┬───────────────────────┬──────────────┐
│          │                       │              │
│   DOCK   │      HARİTA           │  İÇERİK      │
│  (sol)   │  (yuvarlak köşe)     │  PANELİ      │
│  dikey   │                       │  (yuvarlak)  │
│  aynı    │ • Her zaman görünür  │              │
│          │ • Yuvarlatılmış       │ • Müzik      │
│ 🌐 🏠 📋 │ • cam kart efekt     │ • Bildirim   │
│          │                       │ • Drawer     │
└──────────┴───────────────────────┴──────────────┘
```

### Değişecek Ana Şeyler

| Eski | Yeni |
|------|------|
| `widget_panel` widget listesi gösterir | **İçerik paneli** (müzik/bildirim/drawer) |
| `map_container` bazen kaybolur | Harita **her zaman** görünür |
| Düz kare köşeler | **Yuvarlatılmış köşeler** (tüm kartlar) |
| Widget listesi dikey kayar | İçerik paneli **dinamik** (müzik↔drawer) |

---

## 📋 Faz 1: Layout + Görünüm

- [X] android-auto-ui branch'i oluşturuldu
- [X] Referans resimler eklendi
- [ ] **`bg_card_rounded_dark.xml`** (YENİ) - Yuvarlak köşe drawable (20dp radius, koyu cam)
- [ ] **`bg_panel_rounded.xml`** (YENİ) - Panel için yuvarlak köşe drawable
- [ ] **`activity_car_launcher.xml`** - ConstraintLayout güncellemesi (yuvarlak köşe referansları)
- [ ] **`CarLayoutManager.java`** - map_container her zaman görünür, widget_panel içerik paneli
- [ ] **MapActivity.java** - Yuvarlak köşe background ataması

## 📋 Faz 2: İçerik Paneli Sistemi

- [ ] **`RightPanelFragment.java`** (YENİ) - Sağ içerik paneli fragment
- [ ] **`PanelContentManager.java`** (YENİ) - Panel içeriğini yönetir
- [ ] Müzik player → panelde göster (varsa)
- [ ] Bildirim → müzik üstünde banner (gelirse)
- [ ] App Drawer → panelde app listesi

## 📋 Faz 3: App Drawer + Geçişler

- [ ] App Drawer tıklandı → harita küçülür, panel drawer listesi
- [ ] Müziğe tıklandı → harita alanında player açılır
- [ ] Geçiş animasyonları

## 📋 Faz 4: İyileştirmeler

- [ ] Glassmorphism efekti
- [ ] Adaptive layout (portrait/landscape)
- [ ] Widget'lar panel içeriğine entegre

---

## 🚀 İlerleme

- [X] Branch oluşturuldu
- [X] Referans resimler eklendi
- [ ] **1. Faz başlatılacak**
- [ ] 2. Faz
- [ ] 3. Faz
- [ ] 4. Faz