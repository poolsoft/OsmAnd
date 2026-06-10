# XYAuto Entegrasyon Dokumantasyonu (Brand XYAuto)

Bu dokuman, XYAuto (acloud) platformuna sahip Android tabanli arac multimedya sistemleri icin decompile edilmis kaynak kodlarindan elde edilen entegrasyon bilgilerini icermektedir.

---

## 1. Ses ve Ekolayzır Kontrolleri

### Donanimsal Ekolayzir (DSP / Ses Efektleri) Ekranini Acma
* **Paket Adi:** `sys.xy.tumu.app`
* **Aciklama:** Cihazin yerel donanimsal ses efekti ve ekolayzir ayar ekranini baslatir.
* **Kullanim:**
  ```java
  Intent intent = context.getPackageManager().getLaunchIntentForPackage("sys.xy.tumu.app");
  if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
  }
  ```

---

## 2. Sistem Kontrolleri

### Tek Tikla Bellek Temizleme (RAM Temizleyici)
* **Broadcast Action:** `xy.onekeyclean`
* **Aciklama:** Arka planda calisan gereksiz servisleri temizler ve sistemi rahatlatir.
* **Kullanim:**
  ```java
  Intent intent = new Intent("xy.onekeyclean");
  context.sendBroadcast(intent);
  ```

### Ekrani Tamamen Kapatma / Karartma (Night Screen Off)
* **Broadcast Action:** `xy.android.setScreenState`
* **Ekstra Parametreler:**
  * `screenstate` (int) = `2` (Ekrani kapatir, dokununca geri acilir)
* **Kullanim:**
  ```java
  Intent intent = new Intent("xy.android.setScreenState");
  intent.putExtra("screenstate", 2);
  context.sendBroadcast(intent);
  ```

---

## 3. Bluetooth Müzik (AVRCP) ve Medya Kontrolleri

### Bluetooth Medya Durumu ve Sarki Bilgisi Dinleme
* **Broadcast Action:** `com.acloud.intent.play_status`
* **Ekstra Parametreler:**
  * `play_status` (byte): Oynatma durumunu belirtir (`1` = Oynatiliyor, `16` = Durduruldu)
* **Aciklama:** Cihaza bagli telefondan AVRCP protokoluyle calan muzigin durumunu dondurur.

### Bluetooth Medya Kontrol Yayinlari
* **Oynat:** `xy.android.forceplay`
* **Duraklat:** `xy.android.forcepause`
* **Sonraki Sarki:** `xy.android.nextmedia`
* **Onceki Sarki:** `xy.android.previousmedia`
* **Kullanim:**
  ```java
  Intent intent = new Intent("xy.android.forceplay");
  context.sendBroadcast(intent);
  ```

---

## 4. Radyo Kontrolleri

### Radyo Frekansi Ayarlama
* **Broadcast Action:** `xy.setfm.freq`
* **Ekstra Parametreler:**
  * `freq` (int): kHz cinsinden radyo frekansi (Ornek: 98.8 MHz icin `98800` gonderilmelidir)
* **Kullanim:**
  ```java
  Intent intent = new Intent("xy.setfm.freq");
  intent.putExtra("freq", 98800);
  context.sendBroadcast(intent);
  ```

### Radyo Kanal Arama Kontrolleri
* **Sonraki Istasyon (Seek Next):** `xy.android.seek_next`
* **Onceki Istasyon (Seek Prev):** `xy.android.seek_prev`
* **Yerel Radyo Uygulamasini Acma:** `xy.android.radio`

---

## 5. CAN bus ve Araç Entegrasyonları (Opsiyonel)

> [!NOTE]
> Bu ozellikler arac uzerinde CAN bus adapter donanimi ve baglantisi bulunmasini gerektirir.

### Klima (Air Condition) Bilgilerini Okuma ve Kontrol
* **Klima Durum Bilgisi (Broadcast):** `com.xygala.canbus.aircon.status`
  * Gelen intent icerisindeki `airData` (int[]) dizisi uzerinden klimanin anlik durum verileri alinir:
    * `airData[0]`: Klima Acik/Kapali
    * `airData[1]`: AC Aktif/Pasif
    * `airData[8]`: Fan Hizi
    * `airData[9]`: Sol Sicaklik
    * `airData[10]`: Sag Sicaklik
    * `airData[12]` / `airData[13]`: Koltuk Isitma
* **Klima Panelini Ekrana Getirme:** `xy.canbus.showaircontrol` yayini gonderilerek sistemin yerel klima kontrol arayuzu acilir.

### Far ve Gece Modu Durumu
* **Broadcast Action:** `xy.auto.canbus.light` veya `xy.xygala.lamplet`
* **Aciklama:** Aracin farlarinin acik/kapali durumunu bildirir. Launcher uzerinde otomatik gece temasi gecisi icin kullanilabilir.

### Hiz ve Batarya Bilgileri
* **CAN bus Hiz Bilgisi (Broadcast):** `xy.auto.canbus.speed` veya `com.xygala.canbus.tata.speed`
* **Aku Voltaj / Batarya Bilgisi (Broadcast):** `xy.auto.canbus.battery`
