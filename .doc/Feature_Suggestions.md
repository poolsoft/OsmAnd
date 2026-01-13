# Car Launcher Geliştirme Önerileri

Mevcut projeye "Premium" hissiyatı katacak ve kullanıcı deneyimini zenginleştirecek özellik önerileri:

### 1. 🎵 Gelişmiş Medya Widget'ı (Spotify/Youtube Music Entegrasyonu)
*   **Açıklama:** Sadece dahili oynatıcıyı değil, Spotify, Youtube Music gibi harici uygulamaları da kontrol edebilen bir widget.
*   **Özellikler:** Albüm kapağı (Album Art) çekme, Şarkı/Sanatçı bilgisi, Play/Pause/Next/Prev kontrolleri.
*   **Yöntem:** Android `MediaSessionManager` veya `NotificationListenerService` kullanılarak diğer uygulamaların medya oturumlarına erişim sağlanır.

### 2. 🏎️ Grafiksel OBD Dashboard (Kadran/İbre Görünümü)
*   **Açıklama:** OBD verilerini (Hız, Devir, Hararet vb.) sadece metin olarak değil, görsel grafiklerle sunma.
*   **Özellikler:** Analog ibreler (Gauge), dairesel barlar, dinamik renk değişimi (Örn: Devir yükselince kırmızılaşan ibre).
*   **Yöntem:** Custom View çizimi veya grafik kütüphaneleri (MPAndroidChart vb.) kullanılarak `OBDWidget` görselleştirilir.

### 3. ⛽ Akıllı Yakıt ve Maliyet Asistanı
*   **Açıklama:** Teknik tüketim verisini (L/100km) finansal veriye çeviren asistan.
*   **Özellikler:** "Yolculuk Maliyeti: XX TL", "Km Başına: Y Kuruş". Kullanıcı benzin litre fiyatını ayarlardan girer.
*   **Yöntem:** `OBDDataComputer`'dan gelen tüketim verisi x Birim Fiyat.

### 4. 🛞 Lastik Basınç (TPMS) Görselleştirmesi
*   **Açıklama:** Lastik basınçlarını görsel araç şeması üzerinde gösterme.
*   **Özellikler:** 4 tekerlek üzerinde ayrı ayrı basınç (PSI/Bar) ve sıcaklık değerleri. Düşük basınçta uyarı (Kırmızı yanıp sönme).
*   **Yöntem:** Harici BLE TPMS sensörleri ile entegrasyon veya araçtan veri çekebiliyorsak bunu görselleştirme.

### 5. 🌦️ Dinamik Hava Durumu Asistanı (Rota Bazlı)
*   **Açıklama:** Sadece anlık konum değil, gidilecek rota üzerindeki hava durumunu proaktif söyleme.
*   **Özellikler:** "Varış noktasında yağmur bekleniyor", "Rotanın 50. kilometresinde sis var".
*   **Yöntem:** Mevcut hava durumu API'sini rota noktaları (waypoints) için sorgulama.
