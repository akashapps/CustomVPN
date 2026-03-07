package com.akash.customvpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // Check if VPN is prepared (permission was already granted)
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent == null) {
                val serviceIntent = Intent(context, UrlBlockingVpnService::class.java)
                context.startService(serviceIntent)
            }
        }
    }
}
