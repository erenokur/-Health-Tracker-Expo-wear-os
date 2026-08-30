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
}
