# XYAuto Arac Multimedyasi Entegrasyon Detaylari

Bu dokuman, XYAuto (acloud) platformuna sahip Android multimedya sistemlerindeki **Yerel Muzik**, **Yerel Radyo** ve **Bluetooth Muzik** bilesenlerinin yayin (Broadcast) formatlarini, durum degiskenlerini ve kontrol mekanizmalarini icerir.

---

## 1. Yerel Muzik (com.acloud.stub.localmusic)

### Durum Yayini (Broadcasts)
Muzik calar durumunu ve calan sarkiyi bildiren yayinlardir:

| Action | Extra Degiskenler | Aciklama |
| :--- | :--- | :--- |
| `update.widget.playbtnstate` | `PlayState` (boolean) | Oynatma durumunu belirtir (true: caliyor, false: duraklatildi). |
| `update.widget.songname` | `curplaysong` (String)<br>`artistPicPath` (String)<br>`PlayState` (boolean)<br>`curMusicIndex` (int)<br>`totalMusicCount` (int) | Sarki ismi, kapak resmi yolu ve oynatma durumu. |
| `update.widget.update_proBar` | `proBarmax` (int)<br>`proBarvalue` (int)<br>`curplaysong` (String)<br>`artistPicPath` (String) | Sarki suresi, anlik oynatma konumu ve diger metadata detaylari. |

### Sistem Ayari Uzerinden Durum Sorgulama
Yayinlarin kacirilmasi durumunda, muzik calarin aktif calip calmadigi dogrudan Android sistem ayarlarindan okunabilir:
* **Settings.System Anahtari:** `"ui_play_status"`
* **Okuma Kodu:** `Settings.System.getInt(context.getContentResolver(), "ui_play_status", 0)`
* **Degerler:** `1` (Caliyor), `0` (Duraklatildi / Kapali)

### Kontrol Komutlari (Service & Broadcast)
Kontrol islemleri icin hem dogrudan servise (`XYPlayerService`) baslatma intenti gonderilmeli hem de broadcast yayinlanmalidir:

* **Servis Komutlari (startService):**
  * Paket Adi: `"com.acloud.stub.localmusic"`
  * Servis Sinifi: `"com.acloud.stub.service.XYPlayerService"`
  * Action Degerleri:
    * `"init_widget"` (Widget durum guncellemelerini tetikler)
    * `"xy.cdwidget.play"` (Oynat)
    * `"xy.cdwidget.pause"` (Duraklat)
    * `"xy.cdwidget.next"` (Sonraki Sarki)
    * `"xy.cdwidget.prev"` (Onceki Sarki)
* **Broadcast Komutlari:**
  * Oynat/Duraklat: `"xy.android.playpause"`
  * Sonraki Sarki: `"xy.android.nextmedia"`
  * Onceki Sarki: `"xy.android.previousmedia"`

---

## 2. Yerel Radyo (com.acloud.stub.extradio)

### Durum Yayini (Broadcasts)
Radyoda calan frekans ve band bilgilerini yakalamak icin kullanilir:

* **Broadcast Action:** `"com.android.radio.widget.freq_volue"` *(Not: Teyp yazilimindaki yazim hatasi nedeniyle freq_volume yerine freq_volue olarak tanimlanmistir).*
* **Ekstra Parametreler (Extras):**
  * `"fmoram"` (String): `"FM"`, `"FM1"`, `"fm"`, `"AM"`, `"AM1"` bandlarini belirtir.
  * `"freq"` (int): Radyo frekansi.

### Frekans Okuma Mantigi
* **AM / AM1 ise:** Birim `"KHz"` olup, frekans degeri dogrudan okunur (Ornek: `531`).
* **FM / fm ise:** Birim `"MHz"` olup, frekans `freq / 100.0f` olarak hesaplanir (Ornek: gelen deger `9880` ise `98.8` MHz).
* **FM1 ise:** Birim `"MHz"` olup, frekans `freq / 1000` formuluyle hesaplanir (Ornek: gelen deger `98800` ise `98.8` MHz).

### Kontrol Komutlari
Radyo uygulamasi (`com.acloud.stub.extradio`) `com.radio.receiver.MusicIntentReceiver` alicisi uzerinden su aksiyonlari dinler:
* `"xy.setfm.freq"` veya `"xy.open.freq"` -> `freq` (int) parametresiyle frekans ayarlar.
* `"xy.android.fm_scan_prev"` -> Geriye dogru frekans arar.
* `"xy.android.fm_scan_next"` -> Ileriye dogru frekans arar.

---

## 3. Bluetooth Muzik (com.acloud.stub.localradio vb.)

### Durum Yayini (Broadcasts)
Bluetooth muzik oynaticisinin oynatma durumu ve kontrol baglantilari:

* **Broadcast Action:** `"com.acloud.intent.play_status"`
* **Extras:**
  * `"play_status"` (byte): `1` (Caliyor - BT_MUSIC_PLAYING_STATE) veya `16` (Durduruldu - BT_MUSIC_STOP_STATE).

### Kontrol Komutlari
Sistem genelinde gonderilen broadcast intentleri ile kontrol saglanir:
* Oynat: `"xy.android.forceplay"`
* Duraklat: `"xy.android.forcepause"`
* Sonraki Sarki: `"xy.android.nextmedia"`
* Onceki Sarki: `"xy.android.previousmedia"`
