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
                // Swallow per-node failures
            }
        }
    }

    suspend fun hasConnectedPhone(context: Context): Boolean {
        val nodeClient = Wearable.getNodeClient(context)
        return nodeClient.connectedNodes.await().isNotEmpty()
    }

    /**
     * Asks the paired phone to re-push the current medication list.
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
