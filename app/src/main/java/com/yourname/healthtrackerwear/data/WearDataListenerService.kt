package com.yourname.healthtrackerwear.data

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * Background service that receives data pushed from the paired phone.
 *
 * Supported paths:
 *   DataClient (putDataItem):
 *       /med-list  — DataMap with key "list" containing a JSON array string
 *   MessageClient (sendMessage):
 *       /med-list         — raw JSON array bytes (fallback)
 *       /med-list-request — watch is asking the phone to push the list again;
 *                           the phone's WearMessageListenerService handles this
 *                           path and responds by calling syncMedicineList().
 *
 * Expected "list" value (both transport methods):
 *   ["İlaç A", "İlaç B", "İlaç C"]
 *   or: [{"id":1,"name":"İlaç A"}, ...]
 */
class WearDataListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Called when the phone sends data via DataClient.putDataItem().
     *
     * PutDataMapRequest stores data inside a DataMap under a named key —
     * NOT as raw bytes in dataItem.data. We must read it through DataMapItem.
     */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.dataItem.uri.path != "/med-list") continue

            // Primary path: phone used PutDataMapRequest (key = "list")
            val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
            val jsonFromMap = dataMapItem.dataMap.getString("list")
            if (jsonFromMap != null) {
                parseMedList(jsonFromMap)?.let { names ->
                    MedicineRepository.update(this, names)
                }
                continue
            }

            // Fallback: phone sent raw bytes (legacy / alternative implementation)
            val bytes = event.dataItem.data ?: continue
            parseMedList(String(bytes, Charsets.UTF_8))?.let { names ->
                MedicineRepository.update(this, names)
            }
        }
    }

    /**
     * Called when the phone sends a Message via MessageClient.sendMessage().
     * Handles both the /med-list push and the watch-initiated /med-list-request.
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/med-list" -> {
                val json = String(messageEvent.data, Charsets.UTF_8)
                parseMedList(json)?.let { names ->
                    MedicineRepository.update(this, names)
                }
            }
            // The watch sends /med-list-request on cold start; the phone
            // receives this in its own WearMessageListenerService and responds
            // by calling DataClient.putDataItem("/med-list", ...).
            // Nothing to do on the watch side for this path.
        }
    }

    /**
     * Parses a JSON payload into a list of medication name strings.
     * Accepts both a plain string array and an array of objects with a "name" key.
     * Returns null if the JSON is malformed.
     */
    private fun parseMedList(json: String): List<String>? = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val element = arr.get(i)
            if (element is String) element
            else arr.getJSONObject(i).getString("name")
        }
    } catch (e: Exception) {
        null
    }
}
