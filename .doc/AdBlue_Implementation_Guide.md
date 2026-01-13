# AdBlue (DEF) Implementasyonu İçin Gereken Değişiklikler

`AdvancedVehicleMetricsPlugin` ile AdBlue (DEF Level) verisini okuyabilmek için, projenin **Core (Çekirdek)** dosyalarında aşağıdaki değişikliklerin yapılması gerekmektedir. Bu değişiklikler `OsmAnd-shared` ve `OsmAnd` modüllerini etkiler.

## 1. Shared Modül Değişiklikleri
Dosya Yolu: `OsmAnd-shared/src/commonMain/kotlin/net/osmand/shared/obd/OBDCommand.kt`

**Yapılacak İş:** `OBDCommand` enum sınıfına yeni AdBlue komutunu ekleyin.

```kotlin
// Mevcut OBD_FUEL_LEVEL_COMMAND'ın altına ekleyin:
OBD_DEF_LEVEL_COMMAND(0x01, 0x8D, 1, OBDUtils::parsePercentResponse, "vm_def_level");
```

---

Dosya Yolu: `OsmAnd-shared/src/commonMain/kotlin/net/osmand/shared/obd/OBDDataComputer.kt`

**A. Widget Tipi Ekleme:**
`OBDTypeWidget` enum sınıfına yeni tipi ekleyin.

```kotlin
// Enum listesinin sonuna (noktalı virgülden önce) ekleyin:
DEF_LEVEL(
    false,
    OBD_DEF_LEVEL_COMMAND,
    "obd_def_level",
    OBDComputerWidgetFormatter("%.1f"));
```

**B. Hesaplama Mantığı (Compute) Ekleme:**
`OBDComputerWidget` sınıfının `compute()` metodu içindeki `when (type)` bloğuna, yüzdelik hesaplama mantığını ekleyin. `FUEL_LEFT_PERCENT` ile aynı mantığı kullanır.

```kotlin
// when (type) bloğu içinde:
FUEL_LEFT_PERCENT, // Mevcut
DEF_LEVEL -> {     // Yeni Eklenen
    if (locValues.size > 0) {
        locValues[locValues.size - 1].value as Float
    } else {
        null
    }
}
```

---

## 2. Android Modül Değişiklikleri
Dosya Yolu: `OsmAnd/src/net/osmand/plus/views/mapwidgets/WidgetType.java`

**Yapılacak İş:** `WidgetType` enum sınıfına Android tarafındaki widget tanımını ekleyin.

```java
// VEHICLE_METRICS grubu içine ekleyin:
OBD_DEF_LEVEL("obd_def_level", R.string.obd_def_level, R.string.obd_def_level_desc, R.drawable.widget_obd_fuel_remaining_day, R.drawable.widget_obd_fuel_remaining_night, 0, VEHICLE_METRICS, RIGHT),
```

---

Dosya Yolu: `OsmAnd/res/values/strings.xml`

**Yapılacak İş:** Yeni widget için gerekli String tanımlarını ekleyin.

```xml
<string name="obd_def_level">AdBlue Level</string>
<string name="obd_def_level_desc">Show AdBlue (DEF) level</string>
```
