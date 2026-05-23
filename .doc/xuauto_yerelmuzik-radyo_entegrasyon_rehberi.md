# XYAuto Arac Multimedyasi Entegrasyon Rehberi

Bu rehber, XYAuto (acloud) platformuna sahip Android tabanli arac multimedya sistemlerinde yer alan **Yerel Muzik** (`com.acloud.stub.localmusic`) ve **Yerel Radyo** (`com.acloud.stub.extradio`) uygulamalarini kendi gelistirdiginiz baslatici (Launcher) uygulamasi uzerinden kontrol edebilmeniz icin hazirlanmistir.

Rehber icerisinde hem pratik **Broadcast Intent** yontemi hem de ileri duzey muzik kontrolu saglayan **AIDL Servis** baglantisi yer almaktadir. Ayrica, bu entegrasyonu diger projenizde calisan bir yapay zeka kod asistanina otomatik olarak yaptirabilmeniz icin rehberin sonunda hazir bir **Yapay Zeka Promptu** bulunmaktadir.

---

## 1. Broadcast Intent ile Kontrol Yontemi (Radyo ve Temel Muzik)

Sistem genelinde calisan alicilar (Broadcast Receivers) belirli niyetleri (Intents) dinler. Bu yayinlari gondererek radyo frekansini degistirebilir veya muzigi kontrol edebilirsiniz.

### Desteklenen Eylemler (Actions) ve Parametreler

| Bilesen | Intent Action | Ekstra Parametre (Extra) | Aciklama |
| :--- | :--- | :--- | :--- |
| **Radyo** | `xy.setfm.freq` | `freq` (int - kHz cinsinden) | Radyo frekansini ayarlar. Ornek: 98.8 MHz icin `98800` gonderilir. |
| **Radyo** | `xy.android.seek_next` | Yok | Frekansi ileri dogru tarar ve sonraki istasyonu bulur. |
| **Radyo** | `xy.android.seek_prev` | Yok | Frekansi geri dogru tarar ve onceki istasyonu bulur. |
| **Radyo** | `xy.android.radio` | Yok | Yerel radyo uygulamasini acar. |
| **Muzik** | `xy.android.playpause` | Yok | Muzigi oynatir veya duraklatir. |
| **Muzik** | `xy.android.nextmedia` | Yok | Sonraki sarkiya gecer. |
| **Muzik** | `xy.android.previousmedia` | Yok | Onceki sarkiya gecer. |
| **Muzik** | `xy.android.music` | Yok | Yerel muzik uygulamasini acar. |

### Ornek Kotlin Kodu

```kotlin
import android.content.Context
import android.content.Intent

class BaslaticiKontrolor {
    // Radyo frekansini ayarlar (Ornek: 98800 degeri 98.8 MHz yapar)
    fun radyoFrekansiAyarla(baglam: Context, frekansKHz: Int) {
        val niyet = Intent("xy.setfm.freq")
        niyet.putExtra("freq", frekansKHz)
        baglam.sendBroadcast(niyet)
    }

    // Radyoda sonraki istasyonu arar
    fun radyoSonrakiKanal(baglam: Context) {
        val niyet = Intent("xy.android.seek_next")
        baglam.sendBroadcast(niyet)
    }

    // Muzigi oynatir veya duraklatir
    fun muzigiOynatVeyaDuraklat(baglam: Context) {
        val niyet = Intent("xy.android.playpause")
        baglam.sendBroadcast(niyet)
    }

    // Sonraki sarkiya gecer
    fun sonrakiSarki(baglam: Context) {
        val niyet = Intent("xy.android.nextmedia")
        baglam.sendBroadcast(niyet)
    }

    // Radyo uygulamasini ekrana getirir
    fun radyoUygulamasiniAc(baglam: Context) {
        val niyet = Intent("xy.android.radio")
        niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        baglam.startActivity(niyet)
    }
}
```

---

## 2. AIDL ile Servis Baglantisi (Ileri Duzey Muzik Kontrolu)

Yerel Muzik uygulamasi, diger uygulamalarin baglanabilmesi icin disariya acik bir servis barindirir. Bu servise baglanarak sarkinin suresini, oynatma konumunu ve durumunu okuyabilirsiniz.

### Arayuz Tanimi (AIDL Dosyasi)

Projenizde `src/main/aidl/com/acloud/stub/service/aidl/IPlayService.aidl` yolunda bu dosyayi olusturun:

```aidl
package com.acloud.stub.service.aidl;

interface IPlayService {
    void init();
    void start();
    void pause();
    void stop();
    int getDuration();
    int getPosition();
    int getState();
    void seekTo(int konum);
}
```

### Servis Baglantisi Icin Kotlin Kodu

```kotlin
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.acloud.stub.service.aidl.IPlayService

class MuzikServisBaglantisi(private val baglam: Context) {
    private var oynaticiServisi: IPlayService? = null
    private var bagliMi = false

    private val baglanti = object : ServiceConnection {
        override fun onServiceConnected(isim: ComponentName?, servis: IBinder?) {
            oynaticiServisi = IPlayService.Stub.asInterface(servis)
            bagliMi = true
        }

        override fun onServiceDisconnected(isim: ComponentName?) {
            oynaticiServisi = null
            bagliMi = false
        }
    }

    // Yerel muzik servisinin baglantisini baslatir
    fun serviseBaglan() {
        val niyet = Intent("com.acloud.stub.service.aidl.IPlayService")
        niyet.setClassName("com.acloud.stub.localmusic", "com.acloud.stub.service.XYPlayerService")
        baglam.bindService(niyet, baglanti, Context.BIND_AUTO_CREATE)
    }

    // Servis baglantisini kapatir
    fun servisiKapat() {
        if (bagliMi) {
            baglam.unbindService(baglanti)
            bagliMi = false
        }
    }

    // Muzigi oynatmaya baslatir
    fun baslat() {
        oynaticiServisi?.start()
    }

    // Muzigi duraklatir
    fun duraklat() {
        oynaticiServisi?.pause()
    }

    // Calan sarkinin toplam suresini milisaniye cinsinden alir
    fun toplamSureyiAl(): Int {
        return oynaticiServisi?.getDuration() ?: 0
    }

    // Calan sarkinin anlik oynatma konumunu alir
    fun anlikKonumuAl(): Int {
        return oynaticiServisi?.getPosition() ?: 0
    }
}
```

---

## 3. Kod Asistaniniz Icin Hazir Entegrasyon Promptu

Gelistirmekte oldugunuz launcher projenizi yoneten yapay zeka asistanina asagidaki promptu gondererek entegrasyonu otomatik olarak yapmasini saglayabilirsiniz:

```text
Gelistirmekte oldugum Android tabanli Launcher (baslatici) uygulamasina, XYAuto (acloud) arac multimedya sisteminde calisan Yerel Radyo ve Yerel Muzik uygulamalarini kontrol etmek icin entegrasyon bilesenleri eklemek istiyorum. 

Sistem gereksinimleri ve entegrasyon detaylari su sekildedir:

1. Radyo Kontrolu (Broadcast Intent):
- Radyo frekansini ayarlamak icin "xy.setfm.freq" action adresi kullanilmali ve "freq" (int) adli extra deger kHz cinsinden (örn: 98800) eklenerek broadcast gonderilmelidir.
- Arama islemleri icin "xy.android.seek_next" ve "xy.android.seek_prev" action adresleri kullanilmalidir.
- Arayuzden radyoyu acmak icin "xy.android.radio" intent adresi startActivity ile tetiklenmelidir.

2. Muzik Kontrolu (Broadcast Intent):
- Muzigi oynatmak/duraklatmak icin "xy.android.playpause" action adresi kullanilmalidir.
- Sonraki ve onceki sarkilar icin sirasiyla "xy.android.nextmedia" ve "xy.android.previousmedia" action adresleri kullanilmalidir.

3. Ileri Duzey Muzik Kontrolu (AIDL Servisi):
- Paket adi "com.acloud.stub.localmusic" ve servis sinif adi "com.acloud.stub.service.XYPlayerService" olan disariya acik bir muzik servisi bulunmaktadir.
- Bu servise "com.acloud.stub.service.aidl.IPlayService" AIDL arayuzu uzerinden baglanilarak muzik calarin kontrol edilmesi (start, pause, getDuration, getPosition) saglanmalidir.

Bu gereksinimlere gore, projemdeki arayuz butonlarinin tetikleyebilecegi, Kotlin dilinde yazilmis ve Turkce kelimeler barindiran (ancak Turkce karakter icermeyen degisken/sinif isimleriyle) kontrol siniflarini, servis baglanti bilesenlerini ve gerekli AIDL dosya yapisini olusturur musun? Arayuz butonlarimdan bu fonksiyonlari nasil cagiracagimi da ornekler misin?
```
