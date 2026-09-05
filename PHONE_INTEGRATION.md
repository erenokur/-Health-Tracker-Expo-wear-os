# Telefon Uygulaması — Wear OS Entegrasyon Kılavuzu

Bu kılavuz, telefon (Expo / React Native) tarafına eklenmesi gereken **iki** şeyi açıklar:

1. **Watch → Phone kayıt:** Watch'tan gelen `/bp-log` ve `/med-log` mesajlarını yakalayıp veritabanına kaydetme.
2. **Phone → Watch ilaç listesi sync:** İlaç listesi değiştiğinde watch'a otomatik gönderme.

---

## 1. Native Android Modülü — `HealthWearListenerService`

Expo projesinde `android/app/src/main/java/<paket-adınız>/` altına aşağıdaki dosyayı ekleyin.

### `HealthWearListenerService.kt`

```kotlin
package com.erenokur.healthtracker   // kendi paket adınızla değiştirin

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

/**
 * Arka planda çalışarak watch'tan gelen /bp-log ve /med-log mesajlarını
 * alır ve React Native event bus'a (veya doğrudan DB'ye) iletir.
 * Telefon uygulaması kapalıyken bile Android bu servisi uyandırır.
 */
class HealthWearListenerService : WearableListenerService() {

    companion object {
        const val TAG = "HealthWearListener"
        const val ACTION_BP_LOG  = "com.erenokur.healthtracker.BP_LOG"
        const val ACTION_MED_LOG = "com.erenokur.healthtracker.MED_LOG"
    }

    override fun onMessageReceived(event: MessageEvent) {
        Log.d(TAG, "Message received: path=${event.path}")
        try {
            val payload = String(event.data, Charsets.UTF_8)
            when (event.path) {
                "/bp-log"  -> handleBpLog(payload)
                "/med-log" -> handleMedLog(payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }

    private fun handleBpLog(payload: String) {
        val json = JSONObject(payload)
        Log.d(TAG, "BP Log: $json")
        // Secenek A — Local broadcast (React Native event emitter ile yakala):
        sendBroadcast(Intent(ACTION_BP_LOG).apply {
            putExtra("payload", payload)
            setPackage(packageName)
        })
        // Secenek B — Room/SQLite'a dogrudan yaz:
        // val db = AppDatabase.getInstance(this)
        // db.bpDao().insert(BpRecord(
        //     sys       = json.getInt("sys"),
        //     dia       = json.getInt("dia"),
        //     pulse     = json.optInt("pulse"),
        //     timestamp = json.getString("timestamp"),
        //     note      = json.optString("note", ""),
        //     source    = "watch"
        // ))
    }

    private fun handleMedLog(payload: String) {
        val json = JSONObject(payload)
        Log.d(TAG, "Med Log: $json")
        sendBroadcast(Intent(ACTION_MED_LOG).apply {
            putExtra("payload", payload)
            setPackage(packageName)
        })
        // Secenek B — Room/SQLite'a dogrudan yaz:
        // val db = AppDatabase.getInstance(this)
        // db.medDao().insert(MedRecord(
        //     medName   = json.getString("medName"),
        //     mealType  = json.getString("mealType"),   // "Ac" veya "Tok"
        //     timestamp = json.getString("timestamp"),
        //     source    = "watch"
        // ))
    }
}
```

### `android/app/src/main/AndroidManifest.xml` — `<application>` icine ekleyin

```xml
<service
    android:name=".HealthWearListenerService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.google.android.gms.wearable.MESSAGE_RECEIVED" />
        <data android:host="*" android:pathPrefix="/bp-log"  android:scheme="wear" />
        <data android:host="*" android:pathPrefix="/med-log" android:scheme="wear" />
    </intent-filter>
</service>
```

---

## 2. Watch'a ilac Listesi Gondermek

### `WearSyncModule.kt`

```kotlin
package com.erenokur.healthtracker

import com.facebook.react.bridge.*
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

class WearSyncModule(reactContext: ReactApplicationContext)
    : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "WearSync"

    /**
     * JS tarafindan cagrilir:
     *   WearSync.syncMedicineList(["Ilac A", "Ilac B"])
     *
     * DataClient kullandigi icin watch bagli olmasa bile veriyi senkronize eder.
     */
    @ReactMethod
    fun syncMedicineList(names: ReadableArray, promise: Promise) {
        val nameList = (0 until names.size()).map { names.getString(it) }
        val jsonPayload = JSONArray(nameList).toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = PutDataMapRequest.create("/med-list").apply {
                    dataMap.putString("list", jsonPayload)
                    dataMap.putLong("ts", System.currentTimeMillis()) // degisiklik zorla
                }
                Wearable.getDataClient(reactApplicationContext)
                    .putDataItem(request.asPutDataRequest().setUrgent())
                    .await()
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject("WEAR_SYNC_ERROR", e.message, e)
            }
        }
    }
}
```

> `WearSyncModule`'u `ReactPackage` implementasyonunuza ekleyin ve
> `MainApplication.kt`'deki `getPackages()` listesine kaydedin.

### JavaScript — ilac listesi gondermek

```js
import { NativeModules } from "react-native";
const { WearSync } = NativeModules;

async function pushMedListToWatch(medicines) {
  const names = medicines.map((m) => m.name); // ["Ilac A", "Ilac B"]
  await WearSync.syncMedicineList(names);
}
```

---

## 3. Watch'tan Gelen Kayitlari JS'de Yakalamak

### `WearEventModule.kt`

```kotlin
package com.erenokur.healthtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class WearEventModule(reactContext: ReactApplicationContext)
    : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "WearEvents"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val payload = intent.getStringExtra("payload") ?: return
            val eventName = when (intent.action) {
                HealthWearListenerService.ACTION_BP_LOG  -> "onBpLog"
                HealthWearListenerService.ACTION_MED_LOG -> "onMedLog"
                else -> return
            }
            reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, payload)
        }
    }

    @ReactMethod
    fun startListening() {
        val filter = IntentFilter().apply {
            addAction(HealthWearListenerService.ACTION_BP_LOG)
            addAction(HealthWearListenerService.ACTION_MED_LOG)
        }
        reactApplicationContext.registerReceiver(receiver, filter)
    }

    @ReactMethod
    fun stopListening() {
        runCatching { reactApplicationContext.unregisterReceiver(receiver) }
    }
}
```

### JavaScript — kayit dinleme + DB'ye yazma

```js
import { NativeModules, NativeEventEmitter } from "react-native";
const { WearEvents } = NativeModules;
const emitter = new NativeEventEmitter(WearEvents);

WearEvents.startListening();

emitter.addListener("onBpLog", async (payload) => {
  const { sys, dia, pulse, timestamp, note } = JSON.parse(payload);
  await db.insertBloodPressure({
    sys,
    dia,
    pulse,
    timestamp,
    note,
    source: "watch",
  });
});

emitter.addListener("onMedLog", async (payload) => {
  const { medName, mealType, timestamp } = JSON.parse(payload);
  await db.insertMedication({ medName, mealType, timestamp, source: "watch" });
});
```

---

## Ozet Akis

```
TELEFON                                    WATCH
--------                                   ------
Ilac listesi degisti
  WearSync.syncMedicineList(names)
  -> DataClient.putDataItem("/med-list") -> WearDataListenerService.onDataChanged
                                              -> MedicinePrefs.saveList(...)
                                              -> MedScreen'de picker guncellenir

Watch'ta ilac secilip kaydedildi
                                           PhoneSync.sendMessage("/med-log", json)
  HealthWearListenerService.onMessageReceived <-
  -> Broadcast(ACTION_MED_LOG)
  -> WearEventModule -> JS "onMedLog"
  -> db.insertMedication(...)

Watch'ta tansiyon kaydedildi
                                           PhoneSync.sendMessage("/bp-log", json)
  HealthWearListenerService.onMessageReceived <-
  -> Broadcast(ACTION_BP_LOG)
  -> WearEventModule -> JS "onBpLog"
  -> db.insertBloodPressure(...)
```

---

## Bagimliliklar (telefon tarafı `build.gradle`)

```groovy
implementation 'com.google.android.gms:play-services-wearable:19.0.0'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0'
```
