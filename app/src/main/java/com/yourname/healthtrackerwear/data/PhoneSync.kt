package com.yourname.healthtrackerwear.data

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Sends a message to every currently connected node (in practice, the paired
 * phone). We don't require the receiving app to share this app's package
 * name or signing certificate — that's only required for Play Store
 * auto-install/companion provisioning, not for a WearableListenerService
 * registered for BIND_LISTENER to receive a plain MessageClient message.
 */
object PhoneSync {

    suspend fun sendMessage(context: Context, path: String, payloadJson: String) {
        val messageClient: MessageClient = Wearable.getMessageClient(context)
        val nodeClient = Wearable.getNodeClient(context)
        val nodes = nodeClient.connectedNodes.await()
        val data = payloadJson.toByteArray(Charsets.UTF_8)
        for (node in nodes) {
            try {
                messageClient.sendMessage(node.id, path, data).await()
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "HATA: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
                // Swallow per-node failures (e.g. phone briefly out of Bluetooth
                // range) — the caller shows its own success/failure UI based on
                // whether at least one node was reachable, see callers below.
            }
        }
    }

    suspend fun hasConnectedPhone(context: Context): Boolean {
        val nodeClient = Wearable.getNodeClient(context)
        return nodeClient.connectedNodes.await().isNotEmpty()
    }

    /**
     * Asks the paired phone to re-push the current medication list.
     * The phone's WearMessageListenerService handles "/med-list-request" and
     * responds by calling DataClient.putDataItem("/med-list", ...).
     *
     * Call this when MedScreen opens so the watch always starts with a fresh
     * list even after a cold start or a long disconnection period.
     * Silently no-ops if no phone is connected.
     */
    suspend fun requestMedicineList(context: Context) {
        val nodeClient = Wearable.getNodeClient(context)
        val nodes = nodeClient.connectedNodes.await()
        val messageClient = Wearable.getMessageClient(context)
        for (node in nodes) {
            try {
                messageClient.sendMessage(node.id, "/med-list-request", ByteArray(0)).await()
            } catch (_: Exception) { /* phone might not be connected — fine */ }
        }
    }
}
